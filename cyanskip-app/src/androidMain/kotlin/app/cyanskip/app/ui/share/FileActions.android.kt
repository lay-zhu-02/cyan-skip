package app.cyanskip.app.ui.share

import java.io.File
import kotlinx.coroutines.CoroutineScope
import app.cyanskip.app.platform.PlatformResult
import app.cyanskip.app.storage.FileSource
import app.cyanskip.app.ui.androidState
import app.cyanskip.app.ui.platform.UiHost

actual fun UiHost.launchFileAction(block: suspend CoroutineScope.() -> Unit) =
    mainVm.scope.launchUi(block = block)
actual suspend fun UiHost.pickBackupFile(): FileSource? =
    mainVm.androidState.activityResults.openDocument("application/zip")?.let { FileSource.Uri(it.toString()) }

actual suspend fun UiHost.pickRuleFile(): FileSource? =
    mainVm.androidState.activityResults.openDocument("*/*")?.let { FileSource.Uri(it.toString()) }

actual suspend fun UiHost.shareExportFile(title: String, create: suspend () -> File): PlatformResult<Unit> {
    // A native share target may read the file after returning; cache expiry releases it.
    shareFile(create(), title)
    return PlatformResult.Success(Unit)
}

actual suspend fun UiHost.saveExportFile(file: File) = saveFileToDownloads(file)
