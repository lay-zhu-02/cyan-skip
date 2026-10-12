package app.cyanskip.app.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import app.cyanskip.app.ui.component.CsAppIcon as AndroidAppIcon

@Composable
actual fun CsAppIcon(
    appId: String,
    size: Dp,
) = AndroidAppIcon(appId = appId, size = size)
