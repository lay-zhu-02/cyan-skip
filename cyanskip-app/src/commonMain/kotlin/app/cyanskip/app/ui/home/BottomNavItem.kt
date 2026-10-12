package app.cyanskip.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.apps_title
import app.cyanskip.app.resources.home_title
import app.cyanskip.app.resources.settings_title
import app.cyanskip.app.resources.lite_rules
import app.cyanskip.app.resources.lite_snapshots
import app.cyanskip.app.ui.component.CsIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

sealed class BottomNavItem(
    val key: Int,
    val labelKey: StringResource,
    val icon: ImageVector,
) {
    val label: String @Composable get() = stringResource(labelKey)

    object Dashboard : BottomNavItem(
        key = 0,
        labelKey = Res.string.home_title,
        icon = CsIcons.Home,
    )

    object SubsManage : BottomNavItem(
        key = 1,
        labelKey = Res.string.lite_rules,
        icon = CsIcons.StackedDocuments,
    )

    object AppList : BottomNavItem(
        key = 2,
        labelKey = Res.string.apps_title,
        icon = CsIcons.Android,
    )

    object Settings : BottomNavItem(
        key = 3,
        labelKey = Res.string.settings_title,
        icon = CsIcons.Settings,
    )

    object Snapshots : BottomNavItem(
        key = 4,
        labelKey = Res.string.lite_snapshots,
        icon = CsIcons.Image,
    )

    companion object {
        val allSubObjects by lazy { arrayOf(Dashboard, SubsManage, AppList, Settings, Snapshots) }
        val primaryItems by lazy { listOf(Dashboard, SubsManage, Snapshots) }
    }
}

