package app.cyanskip.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.cyanskip.app.ui.component.CsEditorHost
import app.cyanskip.app.ui.page.EditorSession
import app.cyanskip.app.ui.platform.CsBackHandler

@Composable
fun CsEditor(
    editor: EditorSession,
    onSaved: (() -> Unit)? = null,
    navigator: AppNavigator,
    hideIme: () -> Boolean,
    scope: kotlinx.coroutines.CoroutineScope,
    content: @Composable (PaddingValues) -> Unit,
) {
    val entry = remember { navigator.topEntry }
    CsEditorHost(
        title = editor.titleContent,
        hasChanges = editor.hasChanges,
        onSave = editor.onSave,
        saveEnabled = editor.saveEnabled,
        onClose = { if (navigator.topEntry === entry) navigator.pop() },
        onSaved = {
            if (navigator.topEntry === entry) {
                if (onSaved != null) onSaved() else navigator.pop()
            }
        },
        beforeClose = { hideIme() },
        backHandler = { CsBackHandler(onBack = it) },
        actionScope = scope,
        content = content,
    )
}
