package app.cyanskip.app.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.cyanskip.app.a11y.useA11yServiceEnabledFlow
import app.cyanskip.app.app
import app.cyanskip.app.MainActivity
import app.cyanskip.app.service.A11yService

@Composable
actual fun dashboardPlatformState(): DashboardPlatformState {
    val lifecycle = (LocalActivity.current as MainActivity).lifecycle
    // Refresh on host resume when returning from system settings.
    val a11yEnabled by produceState(app.getSecureA11yServices().contains(A11yService.a11yCn), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            useA11yServiceEnabledFlow(this).collect { value = it }
        }
    }
    return DashboardPlatformState(a11yEnabled)
}
