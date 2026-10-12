package app.cyanskip.app.ui.platform

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cyanskip.app.DesktopRuntime
import app.cyanskip.app.SimulatedService
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.platform_action_unsupported
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.ToastUtils


@Composable
actual fun ignoresBatteryOptimizations() =
    DesktopRuntime.requireCurrent().simulator.settings.collectAsStateWithLifecycle().value.environment().android.ignoreBatteryOptimizations

@Composable
actual fun RefreshPermissions() {
}

actual fun openAppDetails() {
    ToastUtils.show(Res.string.platform_action_unsupported.getSync())
}

actual fun openRecents() {
    ToastUtils.show(Res.string.platform_action_unsupported.getSync())
}

actual fun openA11ySettings() {
    DesktopRuntime.requireCurrent().services.setEnabled(SimulatedService.Accessibility, true)
}



actual fun dynamicColorAvailable() = true
