package app.cyanskip.app.snapshot

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.model.ComplexSnapshot
import app.cyanskip.app.snapshot.platform.prepareSnapshotReplacement
import app.cyanskip.app.storage.ExportFileNames
import app.cyanskip.app.storage.SnapshotIssue
import app.cyanskip.app.storage.StorageException
import app.cyanskip.app.storage.ZipArchive
import app.cyanskip.app.storage.appStorage
import app.cyanskip.app.util.LogUtils
import app.cyanskip.db.Db
import app.cyanskip.db.Snapshot

object SnapshotStore {
    private val sharedDirectory get() = appStorage().sharedCache

    private val json = Json {
        ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true
    }
    private val keepNullJson = Json(json) { explicitNulls = true }
    private val mutationMutex = Mutex()
    private val fileLayout by lazy { SnapshotFileLayout(appStorage().snapshot) }

    suspend fun save(snapshot: ComplexSnapshot, screenshot: ByteArray) =
        save(snapshot.id, write = { files ->
            files.webpFile.writeBytes(screenshot)
            files.snapshotFile.writeText(keepNullJson.encodeToString(snapshot))
            files.minSnapshotFile.writeText(keepNullJson.encodeToString(snapshot.copy(nodes = emptyList())))
        }, publish = { Db.snapshotDao.insert(snapshot.toSnapshot()) })

    fun snapshotFile(id: Long): File = fileLayout.committed(id).snapshotFile

    fun screenshotFile(id: Long): File = fileLayout.committed(id).screenshotFile

    /** Reads one committed snapshot including its captured node list. */
    suspend fun loadSnapshot(id: Long): ComplexSnapshot {
        val text = withContext(Dispatchers.IO) {
            fileLayout.committed(id).snapshotFile.readText()
        }
        return withContext(Dispatchers.Default) {
            json.decodeFromString<ComplexSnapshot>(text)
        }
    }


    suspend fun getMinSnapshot(id: Long): JsonObject = mutationMutex.withLock {
        val files = fileLayout.committed(id)
        val cachedText = withContext(Dispatchers.IO) {
            files.minSnapshotFile.takeIf { it.isFile && it.length() > 0 }?.readText()
        }
        if (cachedText != null) {
            val cachedSnapshot = withContext(Dispatchers.Default) {
                try {
                    json.decodeFromString<JsonObject>(cachedText)
                } catch (_: SerializationException) {
                    null
                }
            }
            if (cachedSnapshot != null) return@withLock cachedSnapshot
        }
        val text = withContext(Dispatchers.IO) { files.snapshotFile.readText() }
        val snapshotJson = withContext(Dispatchers.Default) {
            // #1185
            json.decodeFromString<JsonObject>(text)
        }
        val minSnapshot = JsonObject(snapshotJson.toMutableMap().apply {
            this["nodes"] = JsonArray(emptyList())
        })
        withContext(Dispatchers.IO) {
            files.minSnapshotFile.writeText(keepNullJson.encodeToString(minSnapshot))
        }
        minSnapshot
    }

    suspend fun delete(snapshot: Snapshot) {
        mutationMutex.withLock {
            currentCoroutineContext().ensureActive()
            withContext(NonCancellable + Dispatchers.IO) {
                val directory = fileLayout.committed(snapshot.id).directory
                val staged = stageDeletion(directory)
                try {
                    Db.snapshotDao.delete(snapshot)
                } catch (e: Throwable) {
                    rollbackDeletion(directory, staged, e)
                    throw e
                }
                finishDeletion(staged)
            }
        }
    }

    suspend fun replaceScreenshot(snapshot: Snapshot, newBytes: ByteArray): Boolean =
        mutationMutex.withLock {
            withContext(Dispatchers.IO) {
                val files = fileLayout.committed(snapshot.id)
                val temporary =
                    Files.createTempFile(files.directory.toPath(), "replace-", ".tmp")
                        .toFile()
                try {
                    if (!prepareSnapshotReplacement(
                            files.screenshotFile,
                            newBytes,
                            temporary
                        )
                    ) return@withContext false
                    currentCoroutineContext().ensureActive()
                    withContext(NonCancellable) {
                        val previous = stageReplacement(files.webpFile)
                        try {
                            Files.move(
                                temporary.toPath(),
                                files.webpFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        } catch (e: Throwable) {
                            files.webpFile.delete()
                            restoreReplacement(files.webpFile, previous, e)
                            throw e
                        }
                        finishReplacement(previous)
                    }
                    true
                } finally {
                    temporary.delete()
                }
            }
        }

    suspend fun createArchive(
        snapshotId: Long,
        appId: String? = null,
        activityId: String? = null,
    ): File =
        mutationMutex.withLock {
            createArchiveLocked(snapshotId, appId, activityId)
        }

    private suspend fun createArchiveLocked(
        snapshotId: Long,
        appId: String? = null,
        activityId: String? = null,
    ): File =
        withContext(Dispatchers.IO) {
            val filename = (if (appId != null) {
                val appName = AppInfoRepository.snapshot?.apps?.get(appId)?.name
                val stem = if (activityId != null) {
                    "${(appName ?: appId).take(20)}_${
                        activityId.split('.').last().take(40)
                    }-${ExportFileNames.timestamp(snapshotId)}"
                } else {
                    "${(appName ?: appId).take(20)}-${
                        ExportFileNames.timestamp(
                            snapshotId
                        )
                    }"
                }
                "$stem.zip"
            } else {
                "${snapshotId}.zip"
            }).filterNot { char -> char in "\\/:*?\"<>|" || char <= ' ' }
            val outputDirectory = sharedDirectory.resolve(
                "snapshot-$snapshotId-${UUID.randomUUID()}"
            )
            Files.createDirectory(outputDirectory.toPath())
            val outputFile = outputDirectory.resolve(filename)
            try {
                val files = fileLayout.committed(snapshotId)
                if (!files.hasCompleteFiles) {
                    throw StorageException(
                        SnapshotIssue.FilesIncomplete,
                        snapshotId
                    )
                }
                ZipArchive.zipFiles(
                    listOf(files.snapshotFile, files.screenshotFile),
                    outputFile,
                )
                outputFile
            } catch (e: Throwable) {
                if (!outputDirectory.deleteRecursively()) {
                    e.addSuppressed(IOException("Cannot delete archive directory: $outputDirectory"))
                }
                throw e
            }
        }

    suspend fun deleteArchive(file: File) = withContext(NonCancellable + Dispatchers.IO) {
        val directory = file.parentFile ?: return@withContext
        if (directory.parentFile != sharedDirectory || !directory.name.startsWith("snapshot-")) {
            return@withContext
        }
        if (directory.exists() && !directory.deleteRecursively()) {
            LogUtils.d("无法清理快照压缩目录", directory.absolutePath)
        }
    }

    suspend fun save(
        id: Long,
        write: (SnapshotFileLayout.Files) -> Unit,
        publish: suspend () -> Unit
    ): Unit = mutationMutex.withLock {
        withContext(Dispatchers.IO) {
            commitSnapshotDirectory(
                fileLayout,
                id,
                write,
                publish
            )
        }
    }

    private fun stageDeletion(target: File): File? {
        if (!target.exists()) return null
        val staged = requireNotNull(target.parentFile)
            .resolve(".${target.name}.delete-${UUID.randomUUID()}")
        Files.move(target.toPath(), staged.toPath())
        return staged
    }

    private fun rollbackDeletion(target: File, staged: File?, cause: Throwable) {
        if (staged == null) return
        if (target.exists() && !target.deleteRecursively()) {
            cause.addSuppressed(IOException("Cannot delete directory during rollback: $target"))
            return
        }
        try {
            Files.move(staged.toPath(), target.toPath())
        } catch (e: IOException) {
            cause.addSuppressed(e)
        }
    }

    private fun finishDeletion(staged: File?) {
        if (staged != null && staged.exists() && !staged.deleteRecursively()) {
            LogUtils.d("无法清理已删除快照目录", staged.absolutePath)
        }
    }

    private fun stageReplacement(target: File): File? {
        if (!target.exists()) return null
        val staged = requireNotNull(target.parentFile)
            .resolve(".${target.name}.replace-${UUID.randomUUID()}")
        Files.move(target.toPath(), staged.toPath())
        return staged
    }

    private fun restoreReplacement(target: File, staged: File?, cause: Throwable) {
        if (staged == null) return
        try {
            Files.move(staged.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            cause.addSuppressed(e)
        }
    }

    private fun finishReplacement(staged: File?) {
        if (staged != null && staged.exists() && !staged.delete()) {
            LogUtils.d("无法清理旧快照截图", staged.absolutePath)
        }
    }

}
