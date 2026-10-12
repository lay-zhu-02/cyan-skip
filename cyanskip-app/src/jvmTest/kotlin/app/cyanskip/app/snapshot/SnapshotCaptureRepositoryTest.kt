package app.cyanskip.app.snapshot

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import app.cyanskip.app.model.ComplexSnapshot
import app.cyanskip.app.model.DeviceInfo
import app.cyanskip.db.Db
import java.io.IOException
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnapshotCaptureRepositoryTest(private val layout: app.cyanskip.app.storage.AppStorageLayout) {
    fun missingScreenshotStillCommitsCompatibleFilesAndMetadata() = runBlocking {
        fixture { f ->
            val result = f.capture { input(SnapshotScreenshotStatus.Unavailable) }
            assertEquals(SnapshotScreenshotStatus.Unavailable, result.screenshotStatus)
            val saved = Json.decodeFromString<ComplexSnapshot>(
                SnapshotStore.snapshotFile(result.snapshot.id).readText()
            )
            assertEquals(result.snapshot, saved)
            assertEquals(listOf(saved.toSnapshot()), Db.snapshotDao.query().first())
            assertContentEquals(
                input().screenshot,
                SnapshotStore.screenshotFile(saved.id).readBytes()
            )
        }
    }

    fun overlappingCaptureIsRejectedAndCancellationReleasesLock() = runBlocking {
        fixture { f ->
            val entered = CompletableDeferred<Unit>()
            val job = launch {
                f.capture { entered.complete(Unit); awaitCancellation() }
            }
            withTimeout(10_000) { entered.await() }
            assertTrue(f.isCapturing)
            assertFailsWith<SnapshotCaptureBusyException> {
                f.capture { error("Duplicate collector must not run") }
            }
            job.cancelAndJoin()
            assertFalse(f.isCapturing)
            assertTrue(Db.snapshotDao.query().first().isEmpty())
            f.capture { input() }
            assertEquals(1, Db.snapshotDao.query().first().size)
        }
    }

    fun collectorFailureDoesNotSaveAndNextCaptureCanSucceed() = runBlocking {
        fixture { f ->
            assertFailsWith<IOException> { f.capture { throw IOException("nodes") } }
            assertTrue(Db.snapshotDao.query().first().isEmpty())
            assertFalse(f.isCapturing)
            f.capture { input() }
            assertEquals(1, Db.snapshotDao.query().first().size)
        }
    }

    fun databaseFailureRollsBackFilesAndAllowsRetry() = runBlocking {
        fixture { f ->
            withSnapshotDbFailure(layout.database, "INSERT") {
                assertFails { f.capture { input() } }
            }
            assertTrue(layout.snapshot.listFiles().orEmpty().isEmpty())
            assertFalse(f.isCapturing)
            f.capture { input() }
            assertEquals(1, Db.snapshotDao.query().first().size)
        }
    }

    fun runAll() {
        missingScreenshotStillCommitsCompatibleFilesAndMetadata()
        overlappingCaptureIsRejectedAndCancellationReleasesLock()
        collectorFailureDoesNotSaveAndNextCaptureCanSucceed()
        databaseFailureRollsBackFilesAndAllowsRetry()
    }

    private fun input(status: SnapshotScreenshotStatus = SnapshotScreenshotStatus.Captured) =
        SnapshotCaptureInput(
            "dev.capture",
            "dev.capture.Main",
            2,
            2,
            false,
            emptyList(),
            null,
            null,
            DeviceInfo("test", "test", "test", "test", 0, "test"),
            byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a),
            status,
        )

    private suspend fun fixture(block: suspend (SnapshotCaptureRepository) -> Unit) {
        check(Db.snapshotDao.query().first().isEmpty())
        try {
            block(SnapshotCaptureRepository)
        } finally {
            Db.snapshotDao.query().first().forEach { SnapshotStore.delete(it) }
        }
    }
}
