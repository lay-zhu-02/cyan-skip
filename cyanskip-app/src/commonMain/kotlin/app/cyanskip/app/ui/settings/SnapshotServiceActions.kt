package app.cyanskip.app.ui.settings

import androidx.compose.runtime.Composable
import app.cyanskip.app.ui.platform.UiHost

expect fun UiHost.setSnapshotButtonEnabled(enabled: Boolean)
@Composable expect fun snapshotButtonRunning(): Boolean
