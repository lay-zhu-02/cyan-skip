package app.cyanskip.app.ui.platform

import androidx.compose.runtime.Composable


@Composable
expect fun ignoresBatteryOptimizations(): Boolean
@Composable
expect fun RefreshPermissions()
expect fun openAppDetails()
expect fun openRecents()
expect fun openA11ySettings()
expect fun dynamicColorAvailable(): Boolean
