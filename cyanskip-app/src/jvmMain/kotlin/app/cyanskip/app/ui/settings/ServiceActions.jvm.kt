package app.cyanskip.app.ui.settings

import androidx.compose.runtime.Composable
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.DesktopRuntime
import app.cyanskip.app.SimulatedService
import app.cyanskip.app.simulatedServiceRunning

actual fun UiHost.setSnapshotButtonEnabled(enabled: Boolean) = DesktopRuntime.requireCurrent().services.setEnabled(SimulatedService.Button, enabled)

@Composable
actual fun snapshotButtonRunning() = simulatedServiceRunning(SimulatedService.Button)
