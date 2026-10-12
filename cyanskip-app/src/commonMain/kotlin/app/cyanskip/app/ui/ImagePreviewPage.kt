package app.cyanskip.app.ui

import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.cyanskip.app.ui.component.CsIconButton
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.image.appImageLoader
import app.cyanskip.app.ui.navigation.ImagePreviewRoute
import app.cyanskip.app.ui.page.CsImagePreviewContent
import app.cyanskip.app.ui.platform.CsSystemBars
import app.cyanskip.app.ui.platform.UiHost

@Composable
fun ImagePreviewPage(
    host: UiHost,
    route: ImagePreviewRoute,
) {
    val mainVm = MainViewModel.requireCurrent()
    val imageLoader = appImageLoader()
    CsImagePreviewContent(
        route = route,
        onBack = mainVm.navigator::pop,
        imageLoader = imageLoader,
        systemBars = { host.CsSystemBars(it) },
        actionContent = { uri, _ ->
            if (uri != null && (uri.startsWith("https://", true) || uri.startsWith(
                    "http://",
                    true
                ))
            ) {
                CsIconButton(
                    CsIcons.OpenInNew, onClick = { mainVm.textDialog.showUrl(uri) },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                )
            }
        },
    )
}
