package app.cyanskip.app.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_save
import app.cyanskip.app.resources.dialog_close
import org.jetbrains.compose.resources.stringResource

val LocalEditorWindowInsets = staticCompositionLocalOf<WindowInsets?> { null }

@Composable
fun CsEditorLayout(
    title: @Composable () -> Unit, saveEnabled: Boolean,
    onClose: () -> Unit, onSave: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    CsScaffold(
        contentWindowInsets = LocalEditorWindowInsets.current
            ?: ScaffoldDefaults.contentWindowInsets,
        topBar = {
            CsTopAppBar(
                title = title,
                navigationIcon = {
                    CsIconButton(
                        CsIcons.Close,
                        contentDescription = stringResource(Res.string.dialog_close),
                        onClick = onClose
                    )
                },
                actions = {
                    CsIconButton(
                        CsIcons.Check, contentDescription = stringResource(Res.string.action_save),
                        enabled = saveEnabled, onClick = onSave
                    )
                },
            )
        },
        content = content,
    )
}
