package app.cyanskip.app.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_save_to_downloads
import app.cyanskip.app.resources.action_share
import app.cyanskip.app.resources.backup_export
import app.cyanskip.app.resources.backup_import_label
import app.cyanskip.app.ui.component.CsTextListDialog
import org.jetbrains.compose.resources.stringResource

@Composable
fun CsBackupDialogs(
    visible: Boolean, onDismiss: () -> Unit, onImport: () -> Unit,
    onShare: () -> Unit, onSaveDownloads: () -> Unit
) {
    var exporting by remember { mutableStateOf(false) }
    if (visible) {
        CsTextListDialog(
            onDismiss = onDismiss, textList = listOf(
                stringResource(Res.string.backup_import_label) to onImport,
                stringResource(Res.string.backup_export) to { exporting = true },
            )
        )
    }
    if (exporting) {
        CsTextListDialog(
            onDismiss = { exporting = false }, textList = listOf(
                stringResource(Res.string.action_share) to onShare,
                stringResource(Res.string.action_save_to_downloads) to onSaveDownloads,
            )
        )
    }
}
