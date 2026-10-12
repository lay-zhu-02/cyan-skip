package app.cyanskip.app.ui.style

import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import app.cyanskip.app.app
import app.cyanskip.app.ui.share.LocalDarkTheme
import app.cyanskip.app.ui.share.LocalIsTalkbackEnabled
import app.cyanskip.app.ui.theme.CsTheme
import app.cyanskip.app.ui.theme.cyanskipColors
import app.cyanskip.app.ui.theme.rememberAppearance


@Composable
fun AppTheme(
    invertedTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val appearance by rememberAppearance()
    val darkTheme = appearance.isDark(isSystemInDarkTheme(), invertedTheme)
    val colorScheme = cyanskipColors(darkTheme)

    val activity = LocalActivity.current
    if (activity != null) {
        LaunchedEffect(darkTheme) {
            // https://github.com/gkd-kit/gkd/pull/421
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
        }
        val bg = colorScheme.background.toArgb()
        LaunchedEffect(darkTheme, bg) {
            activity.window.decorView.setBackgroundColor(bg)
        }
    }

    var isTalkbackEnabled by remember { mutableStateOf(app.a11yManager.isTouchExplorationEnabled) }
    DisposableEffect(null) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener {
            isTalkbackEnabled = it
        }
        app.a11yManager.addTouchExplorationStateChangeListener(listener)
        onDispose {
            app.a11yManager.removeTouchExplorationStateChangeListener(listener)
        }
    }
    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalIsTalkbackEnabled provides isTalkbackEnabled
    ) {
        CsTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
