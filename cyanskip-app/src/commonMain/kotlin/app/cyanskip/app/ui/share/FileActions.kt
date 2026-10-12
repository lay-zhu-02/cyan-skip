package app.cyanskip.app.ui.share

import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import app.cyanskip.app.platform.PlatformResult
import app.cyanskip.app.storage.FileSource
import app.cyanskip.app.ui.platform.UiHost

expect fun UiHost.launchFileAction(block: suspend CoroutineScope.() -> Unit): Job
expect suspend fun UiHost.pickBackupFile(): FileSource?
expect suspend fun UiHost.pickRuleFile(): FileSource?
expect suspend fun UiHost.shareExportFile(title: String, create: suspend () -> File): PlatformResult<Unit>
expect suspend fun UiHost.saveExportFile(file: File)
