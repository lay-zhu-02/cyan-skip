package app.cyanskip.app.ui.home

data class DashboardPlatformState(
    val a11yEnabled: Boolean,
)

@androidx.compose.runtime.Composable
expect fun dashboardPlatformState(): DashboardPlatformState
