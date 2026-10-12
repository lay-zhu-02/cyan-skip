package app.cyanskip.app.ui.snapshot

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cyanskip.app.app
import app.cyanskip.app.permission.PermissionStates
import app.cyanskip.app.service.ScreenshotService
import app.cyanskip.app.ui.androidState
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.util.AndroidTarget

actual fun UiHost.setScreenshotServiceEnabled(enabled: Boolean) {
    mainVm.scope.launchUi {
        if (!enabled) {
            ScreenshotService.stop()
            return@launchUi
        }
        if (!mainVm.androidState.permissionRequests.ensurePermissions(PermissionStates.notification)) {
            return@launchUi
        }
        val activityResult = mainVm.androidState.activityResults.startActivity(
            app.mediaProjectionManager.createScreenCaptureIntent(),
        )
        val intent = activityResult.data
        if (activityResult.resultCode == Activity.RESULT_OK && intent != null) {
            ScreenshotService.start(intent)
        }
    }
}

@Composable
actual fun screenshotServiceRunning() =
    ScreenshotService.isRunning.collectAsStateWithLifecycle().value

actual fun nativeScreenshotAvailable() = AndroidTarget.R
