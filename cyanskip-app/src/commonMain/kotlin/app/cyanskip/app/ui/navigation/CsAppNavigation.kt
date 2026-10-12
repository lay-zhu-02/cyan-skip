package app.cyanskip.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import app.cyanskip.app.ui.ImagePreviewPage
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.WebViewPage
import app.cyanskip.app.ui.crash.CrashReportPage
import app.cyanskip.app.ui.home.ActionToastPage
import app.cyanskip.app.ui.home.HomePage
import app.cyanskip.app.ui.home.LiteSettingsPage
import app.cyanskip.app.ui.home.LiteAboutPage
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.rulegen.RuleGeneratorPage
import app.cyanskip.app.ui.settings.AppWhitelistPage
import app.cyanskip.app.ui.snapshot.SnapshotPage
import app.cyanskip.app.ui.snapshot.SnapshotPreviewPage
import app.cyanskip.app.ui.snapshot.SnapshotSettingsPage
import app.cyanskip.app.ui.subscription.ActionLogPage
import app.cyanskip.app.ui.subscription.AppConfigPage
import app.cyanskip.app.ui.subscription.CategoryEditorPage
import app.cyanskip.app.ui.subscription.RuleExcludeEditorPage
import app.cyanskip.app.ui.subscription.SubsAppGroupListPage
import app.cyanskip.app.ui.subscription.SubsAppListPage
import app.cyanskip.app.ui.subscription.SubsCategoryGroupPage
import app.cyanskip.app.ui.subscription.SubsCategoryPage
import app.cyanskip.app.ui.subscription.SubsGlobalGroupExcludePage
import app.cyanskip.app.ui.subscription.SubsGlobalGroupListPage
import app.cyanskip.app.ui.subscription.UpsertRuleGroupPage

/** Shared production route registry for both hosts. */
@Composable
fun CsAppNavigation(
    host: UiHost,
) {
    val mainVm = MainViewModel.requireCurrent()
    CsNavigation(mainVm.navigator.backStack, mainVm.navigator::pop) { entry ->
        val route = entry.route
        NavEntry(
            entry,
            contentKey = entry.id,
            metadata = route.transition.toMetadata(),
        ) {
            when (route) {
                is SubsAppListRoute -> SubsAppListPage(host, route)
                is SubsAppGroupListRoute -> SubsAppGroupListPage(route)
                is SubsGlobalGroupListRoute -> SubsGlobalGroupListPage(route)
                is SubsGlobalGroupExcludeRoute -> SubsGlobalGroupExcludePage(host, route)
                is SubsCategoryRoute -> SubsCategoryPage(route)
                is SubsCategoryGroupRoute -> SubsCategoryGroupPage(route)
                is RuleExcludeEditorRoute -> RuleExcludeEditorPage(host, route)
                is AppConfigRoute -> AppConfigPage(route)
                is ActionLogRoute -> ActionLogPage(route)
                is UpsertRuleGroupRoute -> UpsertRuleGroupPage(host, route)
                is CategoryEditorRoute -> CategoryEditorPage(host, route)
                is SnapshotPreviewRoute -> SnapshotPreviewPage(host, route)
                is SnapshotRuleRoute -> RuleGeneratorPage(route)
                is WebViewRoute -> WebViewPage(route, host)
                is ImagePreviewRoute -> ImagePreviewPage(host, route)

                EditBlockAppListRoute -> AppWhitelistPage(host)
                SnapshotPageRoute -> SnapshotPage(host)
                HomeRoute -> HomePage(host)
                LiteSettingsRoute -> LiteSettingsPage(host)
                ActionToastRoute -> ActionToastPage(host)
                AboutRoute -> LiteAboutPage()
                SnapshotSettingsRoute -> SnapshotSettingsPage(host)
                CrashReportRoute -> CrashReportPage()
            }
        }
    }
}
