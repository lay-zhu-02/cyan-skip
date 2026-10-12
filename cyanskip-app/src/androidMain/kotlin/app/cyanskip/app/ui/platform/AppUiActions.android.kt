package app.cyanskip.app.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import app.cyanskip.app.permission.PermissionStates
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.recents_lock_manual_hint
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.IntentUtils
import app.cyanskip.app.util.ToastUtils


@Composable
actual fun ignoresBatteryOptimizations() =
    PermissionStates.ignoreBatteryOptimizations.stateFlow.collectAsStateWithLifecycle().value

@Composable
actual fun RefreshPermissions() {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            withContext(Dispatchers.IO) {
                while (isActive) {
                    PermissionStates.refreshAll(); delay(1000)
                }
            }
        }
    }
}

actual fun openAppDetails() {
    IntentUtils.openAppDetailsSettings()
}

actual fun openRecents() {
    if (app.cyanskip.app.service.A11yService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS) != true) {
        ToastUtils.show(Res.string.recents_lock_manual_hint.getSync())
    }
}

actual fun openA11ySettings() {
    IntentUtils.openA11ySettings()
}



actual fun dynamicColorAvailable() = app.cyanskip.app.util.AndroidTarget.S
