package app.cyanskip.app.ui.snapshot

import androidx.compose.runtime.Composable
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.DesktopRuntime
import app.cyanskip.app.SimulatedService
import app.cyanskip.app.simulatedServiceRunning

@Composable
actual fun screenshotServiceRunning() = simulatedServiceRunning(SimulatedService.Screenshot)
actual fun nativeScreenshotAvailable() = false
actual fun UiHost.setScreenshotServiceEnabled(enabled: Boolean) {
    DesktopRuntime.requireCurrent().services.setEnabled(SimulatedService.Screenshot, enabled)
}
