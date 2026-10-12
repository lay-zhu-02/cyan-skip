package app.cyanskip.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.data.appinfo.PackageAppCatalog
import app.cyanskip.app.ui.component.CsAppIcon as SharedGkAppIcon

@Composable
fun CsAppIcon(
    modifier: Modifier = Modifier,
    appId: String,
    size: Dp = 32.dp,
) {
    val catalog = AppInfoRepository.state.collectAsStateWithLifecycle().value.snapshot
    val icon = PackageAppCatalog.icon(catalog?.apps?.get(appId))
    SharedGkAppIcon(
        painter = icon?.let { rememberDrawablePainter(it) },
        modifier = modifier,
        size = size,
    )
}
