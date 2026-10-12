package app.cyanskip.app.ui.snapshot

import java.io.File
import app.cyanskip.app.permission.PermissionStates
import app.cyanskip.app.platform.PlatformResult
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.snapshot_share
import app.cyanskip.app.storage.FileSource
import app.cyanskip.app.storage.saveToDownloads
import app.cyanskip.app.ui.androidState
import app.cyanskip.app.ui.platform.UiHost
import org.jetbrains.compose.resources.getString

actual fun UiHost.snapshotPlatformActions(): SnapshotPlatformActions {
    return object : SnapshotPlatformActions {
        override suspend fun ensureSavePermission() =
            mainVm.androidState.permissionRequests.ensurePermissions(PermissionStates.writeExternalStorage)

        override suspend fun share(file: File): PlatformResult<Unit> {
            shareFile(file, getString(Res.string.snapshot_share))
            return PlatformResult.Success(Unit)
        }

        override suspend fun save(file: File): PlatformResult<Boolean> =
            when (saveToDownloads(file)) {
                is PlatformResult.Success -> PlatformResult.Success(true)
                PlatformResult.Unsupported -> PlatformResult.Unsupported
            }

        override suspend fun pickImage(): FileSource? =
            mainVm.androidState.activityResults.pickImage()?.let { FileSource.Uri(it.toString()) }
    }
}
