package app.cyanskip.app.ui

import androidx.compose.runtime.Composable
import app.cyanskip.app.ui.navigation.WebViewRoute
import app.cyanskip.app.ui.platform.UiHost

@Composable
expect fun WebViewPage(
    route: WebViewRoute,
    host: UiHost,
)
