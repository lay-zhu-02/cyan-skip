package app.cyanskip.app.ui.home

import app.cyanskip.app.backup.BackupManager
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.backup_share
import app.cyanskip.app.resources.file_not_selected
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.share.importBackup
import app.cyanskip.app.ui.share.launchFileAction
import app.cyanskip.app.ui.share.pickBackupFile
import app.cyanskip.app.ui.share.saveArchive
import app.cyanskip.app.ui.share.shareArchive
import app.cyanskip.app.util.ToastUtils
import org.jetbrains.compose.resources.getString

fun UiHost.importAppBackup() = launchFileAction {
    val source = pickBackupFile()
    if (source == null) ToastUtils.show(getString(Res.string.file_not_selected))
    else importBackup(source)
}

fun UiHost.shareAppBackup() = launchFileAction {
    shareArchive(getString(Res.string.backup_share), BackupManager::exportData)
}

fun UiHost.saveAppBackup() = launchFileAction {
    saveArchive(BackupManager::exportData)
}
