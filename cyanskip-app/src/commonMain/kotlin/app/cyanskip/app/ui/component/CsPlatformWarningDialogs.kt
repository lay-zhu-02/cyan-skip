package app.cyanskip.app.ui.component
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun CsPlatformWarningDialogs(
    restrictedWarning: Boolean,
    a11yRunning: Boolean,
    dismissRestricted: () -> Unit,
    openSettings: () -> Unit,
) {
    LaunchedEffect(restrictedWarning, a11yRunning) {
        if (restrictedWarning && a11yRunning) dismissRestricted()
    }
    if (restrictedWarning && !a11yRunning) {
        CsRestrictedSettingsDialog({ dismissRestricted(); openSettings() }, dismissRestricted)
    }
}
