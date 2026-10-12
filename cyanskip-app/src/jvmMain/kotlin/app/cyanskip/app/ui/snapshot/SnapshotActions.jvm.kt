package app.cyanskip.app.ui.snapshot

import java.io.File
import app.cyanskip.app.platform.PlatformResult
import app.cyanskip.app.storage.FileSource
import app.cyanskip.app.storage.appStorage
import app.cyanskip.app.ui.platform.UiHost

actual fun UiHost.snapshotPlatformActions(): SnapshotPlatformActions =
    object : SnapshotPlatformActions {
        override suspend fun ensureSavePermission() = true
        override suspend fun share(file: File) = PlatformResult.Unsupported
        override suspend fun save(file: File) =
            PlatformResult.Success(fileActions.saveAs(file))

        override suspend fun pickImage() =
            fileActions.choose(appStorage().sharedCache)?.let(FileSource::Local)
    }
