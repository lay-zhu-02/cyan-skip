package app.cyanskip.app.ui.platform

import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import app.cyanskip.app.CsDesktopBackHandler
import app.cyanskip.app.ui.component.CsDialog

actual typealias UiHost = app.cyanskip.app.DesktopSession

@Composable
actual fun CsBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    val visible = LocalNavAnimatedContentScope.current.transition.targetState == EnterExitState.Visible
    CsDesktopBackHandler(enabled = enabled && visible, onBack = onBack)
}

@Composable
actual fun CsFullscreenDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    CsDesktopBackHandler(onBack = onDismiss)
    CsDialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize()
        ) { content() }
    }
}

@Composable
actual fun inputInsets(): Modifier = Modifier
actual fun UiHost.hideIme(): Boolean = this.state.dismissIme()

@Composable
actual fun UiHost.CsSystemBars(visible: Boolean) {
    val state = this.state
    val old = remember { state.environment.android.statusBarVisible }
    DisposableEffect(Unit) {
        onDispose {
            state.simulatorCommand {
                state.simulator.updateAndroid {
                    it.copy(
                        statusBarVisible = old
                    )
                }
            }
        }
    }
    LaunchedEffect(visible) {
        state.simulatorCommand {
            state.simulator.updateAndroid {
                it.copy(
                    statusBarVisible = visible
                )
            }
        }
    }
}
