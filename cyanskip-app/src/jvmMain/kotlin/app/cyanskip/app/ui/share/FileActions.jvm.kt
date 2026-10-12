package app.cyanskip.app.ui.share

import java.io.File
import kotlinx.coroutines.CoroutineScope
import app.cyanskip.app.platform.PlatformResult
import app.cyanskip.app.storage.FileSource
import app.cyanskip.app.storage.appStorage
import app.cyanskip.app.ui.platform.UiHost

actual fun UiHost.launchFileAction(block: suspend CoroutineScope.() -> Unit) = state.mainVm.scope.launchUi {
    runtime.subscriptionInitialization.join()
    block()
}
actual suspend fun UiHost.pickBackupFile(): FileSource? {
    val file = fileActions.choose(appStorage().sharedCache) ?: return null
    return FileSource.Local(file)
}

actual suspend fun UiHost.pickRuleFile(): FileSource? =
    fileActions.choose(appStorage().sharedCache)?.let { FileSource.Local(it) }

actual suspend fun UiHost.shareExportFile(title: String, create: suspend () -> File): PlatformResult<Unit> =
    PlatformResult.Unsupported

actual suspend fun UiHost.saveExportFile(file: File) {
    fileActions.saveAs(file)
}
