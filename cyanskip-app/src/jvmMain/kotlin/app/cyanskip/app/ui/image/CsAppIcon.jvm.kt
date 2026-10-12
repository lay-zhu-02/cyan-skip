package app.cyanskip.app.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import app.cyanskip.app.DesktopRuntime
import app.cyanskip.app.ui.component.CsAppIcon

@Composable
actual fun CsAppIcon(
    appId: String,
    size: Dp,
) = CsAppIcon(DesktopRuntime.requireCurrent().appIcon(appId), size = size)
