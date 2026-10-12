package app.cyanskip.app.ui.share

import java.io.File
import app.cyanskip.app.backup.BackupManager
import app.cyanskip.app.platform.PlatformResult
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.backup_import_progress
import app.cyanskip.app.resources.platform_action_unsupported
import app.cyanskip.app.storage.FileExports
import app.cyanskip.app.storage.FileSource
import app.cyanskip.app.storage.StorageMaintenance
import app.cyanskip.app.storage.appStorage
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.settings.SettingsFeedback
import app.cyanskip.app.util.ToastUtils
import org.jetbrains.compose.resources.getString

suspend fun importBackup(source: FileSource) {
    ToastUtils.show(getString(Res.string.backup_import_progress))
    val skipped = BackupManager.importData(source)
    ToastUtils.show(SettingsFeedback.backupImported(skipped))
}

suspend fun UiHost.saveArchive(create: suspend () -> File) {
    FileExports.withTemporaryFile(
        create = create,
        delete = { StorageMaintenance.deleteSharedFile(appStorage(), it) },
        consume = { saveExportFile(it) },
    )
}

suspend fun UiHost.shareArchive(title: String, create: suspend () -> File) {
    if (shareExportFile(title, create) == PlatformResult.Unsupported) {
        ToastUtils.show(getString(Res.string.platform_action_unsupported))
    }
}
