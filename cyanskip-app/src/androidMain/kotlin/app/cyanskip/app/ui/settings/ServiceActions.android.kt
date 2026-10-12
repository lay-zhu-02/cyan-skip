package app.cyanskip.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cyanskip.app.permission.PermissionStates
import app.cyanskip.app.platform.service.ServiceController
import app.cyanskip.app.service.ButtonService
import app.cyanskip.app.ui.androidState
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.share.launchUi

actual fun UiHost.setSnapshotButtonEnabled(enabled: Boolean) {
    mainVm.scope.launchUi {
        if (!enabled || mainVm.androidState.permissionRequests.ensurePermissions(
                PermissionStates.foregroundServiceSpecialUse,
                PermissionStates.notification,
                PermissionStates.drawOverlays,
            )
        ) {
            ServiceController.setSnapshotButtonEnabled(enabled)
        }
    }
}

@Composable
actual fun snapshotButtonRunning() =
    ButtonService.isRunning.collectAsStateWithLifecycle().value
