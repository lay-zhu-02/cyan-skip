package app.cyanskip.app.ui.component

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_close
import app.cyanskip.app.resources.permission_go_grant
import app.cyanskip.app.resources.permission_restricted
import app.cyanskip.app.resources.restricted_settings_permission_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun CsPermissionDialog(
    title: String, message: String, confirmText: String, dismissText: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    CsAlertDialog(
        title = { Text(title) }, text = { Text(message) }, onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } })
}

@Composable
fun CsRestrictedSettingsDialog(onGrant: () -> Unit, onDismiss: () -> Unit) {
    CsPermissionDialog(
        stringResource(Res.string.permission_restricted),
        stringResource(Res.string.restricted_settings_permission_description),
        stringResource(Res.string.permission_go_grant),
        stringResource(Res.string.action_close),
        onGrant,
        onDismiss
    )
}
