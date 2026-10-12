package app.cyanskip.app.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import app.cyanskip.app.ui.androidState
import app.cyanskip.app.ui.navigation.CsAppNavigation
import app.cyanskip.app.ui.style.AppTheme

@Composable
fun AppRoot() {
    val activity = androidx.activity.compose.LocalActivity.current as app.cyanskip.app.MainActivity
    val mainVm = activity.mainVm
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            CsAppNavigation(host = activity)
            AppOverlayHost()
            mainVm.androidState.permissionRequests.Render(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f),
            )
        }
    }
}
