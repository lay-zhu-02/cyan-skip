package app.cyanskip.app.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cyanskip.app.ui.component.CsPermissionDialog
import app.cyanskip.app.ui.component.CsPermissionPrompt
import app.cyanskip.app.ui.component.PermissionPromptContent

class PermissionRequestContent(
    private val coordinator: PermissionRequestCoordinator,
    private val updateHostState: (resumed: Boolean, hasWindowFocus: Boolean) -> Unit,
    private val detachHost: () -> Unit,
) {
    @Composable
    fun Render(modifier: Modifier = Modifier) {
        BindHostLifecycle()
        RequestDialog()
        PromptOverlay(modifier)
    }

    @Composable
    private fun BindHostLifecycle() {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val hasWindowFocus = LocalWindowInfo.current.isWindowFocused
        var resumed by remember(lifecycle) {
            mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, _ ->
                resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                detachHost()
            }
        }
        SideEffect {
            updateHostState(resumed, hasWindowFocus)
        }
    }

    @Composable
    private fun RequestDialog() {
        val state = coordinator.dialog.collectAsStateWithLifecycle().value
        if (state != null) {
            CsPermissionDialog(
                state.title,
                state.message,
                state.confirmText,
                state.dismissText,
                { coordinator.confirmDialog(state.id) },
                { coordinator.dismissDialog(state.id) })
        }
    }

    @Composable
    private fun PromptOverlay(modifier: Modifier) {
        val prompt = coordinator.visiblePrompt.collectAsStateWithLifecycle().value
        CsPermissionPrompt(prompt?.let {
            PermissionPromptContent(
                it.title,
                it.message,
                it.displayDelayMillis
            )
        }, modifier)
    }
}
