package app.cyanskip.app.ui.home
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cyanskip.app.DesktopRuntime

@Composable
actual fun dashboardPlatformState(): DashboardPlatformState {
    val settings by DesktopRuntime.requireCurrent().simulator.settings.collectAsStateWithLifecycle()
    return DashboardPlatformState(settings.services.a11yEnabled)
}
