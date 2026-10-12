package app.cyanskip.app.ui.snapshot

import app.cyanskip.app.snapshot.SnapshotStore
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.db.Snapshot

data class SnapshotImage(val path: String, val modifiedAt: Long, val exists: Boolean)

/** Native file and permission operations; snapshot business workflows stay in commonMain. */
interface SnapshotPlatformActions {
    suspend fun ensureSavePermission(): Boolean
    suspend fun share(file: java.io.File): app.cyanskip.app.platform.PlatformResult<Unit>

    /** Success(false) means the user cancelled the destination picker. */
    suspend fun save(file: java.io.File): app.cyanskip.app.platform.PlatformResult<Boolean>
    suspend fun pickImage(): app.cyanskip.app.storage.FileSource?
}

interface SnapshotActions {
    fun share(snapshot: Snapshot)
    fun saveToDownloads(snapshot: Snapshot)
    fun saveToAlbum(snapshot: Snapshot)
    fun replace(snapshot: Snapshot)
    suspend fun saveSelectedToDownloads(snapshots: List<Snapshot>): Int?
    suspend fun saveSelectedToAlbum(snapshots: List<Snapshot>): Int?
    fun delete(snapshot: Snapshot)
}

fun Snapshot.image(): SnapshotImage = SnapshotStore.screenshotFile(id).let {
    SnapshotImage(it.absolutePath, it.lastModified(), it.isFile)
}

expect fun UiHost.snapshotPlatformActions(): SnapshotPlatformActions
