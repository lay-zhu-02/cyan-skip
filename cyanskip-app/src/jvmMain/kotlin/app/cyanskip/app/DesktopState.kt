package app.cyanskip.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.Serializable
import app.cyanskip.app.network.AppLinks
import app.cyanskip.app.platform.writeClipboardText
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.uri_unknown
import app.cyanskip.app.permission.SimulatorPermissionRequester
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.ToastState
import app.cyanskip.app.ui.home.BottomNavItem
import app.cyanskip.app.ui.navigation.AboutRoute
import app.cyanskip.app.ui.navigation.ActionLogRoute
import app.cyanskip.app.ui.navigation.ActionToastRoute
import app.cyanskip.app.ui.navigation.AppConfigRoute
import app.cyanskip.app.ui.navigation.AppRoute
import app.cyanskip.app.ui.navigation.CategoryEditorRoute
import app.cyanskip.app.ui.navigation.CrashReportRoute
import app.cyanskip.app.ui.navigation.EditBlockAppListRoute
import app.cyanskip.app.ui.navigation.CsLink
import app.cyanskip.app.ui.navigation.HomeRoute
import app.cyanskip.app.ui.navigation.ImagePreviewRoute
import app.cyanskip.app.ui.navigation.RuleExcludeEditorRoute
import app.cyanskip.app.ui.navigation.SnapshotPageRoute
import app.cyanskip.app.ui.navigation.SnapshotPreviewRoute
import app.cyanskip.app.ui.navigation.SnapshotRuleRoute
import app.cyanskip.app.ui.navigation.SnapshotSettingsRoute
import app.cyanskip.app.ui.navigation.SubsAppGroupListRoute
import app.cyanskip.app.ui.navigation.SubsAppListRoute
import app.cyanskip.app.ui.navigation.SubsCategoryGroupRoute
import app.cyanskip.app.ui.navigation.SubsCategoryRoute
import app.cyanskip.app.ui.navigation.SubsGlobalGroupExcludeRoute
import app.cyanskip.app.ui.navigation.SubsGlobalGroupListRoute
import app.cyanskip.app.ui.navigation.UpsertRuleGroupRoute
import app.cyanskip.app.ui.navigation.WebViewRoute
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.db.LOCAL_SUBS_ID

@Serializable
data class DesktopEnvironment(
    val width: Int = defaultSimulatedDevice.width,
    val height: Int = defaultSimulatedDevice.height,
    val density: Float = defaultSimulatedDevice.density,
    val fontScale: Float = defaultSimulatedDevice.fontScale,
    val dark: Boolean = false,
    val locale: String = defaultSimulatedDevice.locale,
    val android: AndroidWindow = AndroidWindow(),
) {
    fun validate() {
        require(width in 240..1600 && height in 320..2400) { "Invalid viewport" }
        require(density in 0.5f..3f && fontScale in 0.5f..3f) { "Invalid density or fontScale" }
        require(locale in setOf("zh-CN", "en")) { "Unsupported development locale" }
        android.validate(width, height)
    }
}

@Serializable
data class ScenarioRequest(
    val page: String = "dashboard",
    val variant: String = "normal",
    val route: AppRoute? = null,
    val overlay: String? = null
)

@Serializable
data class DesktopSnapshot(
    val page: String,
    val variant: String,
    val revision: Int,
    val environment: DesktopEnvironment,
    val lastEvent: String?,
    val isolated: Boolean = false,
    val simulator: SimulatorSettings,
)

class DesktopState(
    val toast: ToastState,
    initialEnvironment: DesktopEnvironment = DesktopEnvironment(),
    private val isolated: Boolean = false,
    val simulator: SimulatorStore = SimulatorStore(
        SimulatorSettings().withEnvironment(
            initialEnvironment
        )
    ),
) : ViewModelStoreOwner {
    var overlay by mutableStateOf<String?>(null)
    fun unsupported() {
        toast.show("当前平台不支持")
    }

    val environment: DesktopEnvironment get() = simulator.settings.value.environment()
    private var requestedScenario by mutableStateOf(ScenarioRequest())
    override val viewModelStore = ViewModelStore()
    val mainVm = ViewModelProvider.create(this, viewModelFactory {
        initializer {
            MainViewModel(SimulatorPermissionRequester(simulator))
        }
    })[MainViewModel::class]

    var showingComponentCatalog by mutableStateOf(false)
        private set

    fun closeComponentCatalog() { showingComponentCatalog = false }

    val scenario: ScenarioRequest
        get() = if (showingComponentCatalog) requestedScenario.copy(page = "components") else if (mainVm.navigator.topRoute == HomeRoute) {
            requestedScenario.copy(page = homePages.getValue(mainVm.homeNavigation.selectedTab.key))
        } else requestedScenario.copy(page = routeName(mainVm.navigator.topRoute))
    var revision by mutableStateOf(0)
        private set
    var lastEvent by mutableStateOf<String?>(null)
        private set

    fun load(request: ScenarioRequest) {
        require(request.route != null || request.page in pages) { "Unknown page: ${request.page}" }
        require(request.variant in variants) { "Unknown variant: ${request.variant}" }
        overlay = request.overlay
        requestedScenario = request
        showingComponentCatalog = request.route == null && request.page == "components"
        mainVm.navigator.reset(
            request.route ?: if (!showingComponentCatalog && request.page !in homePages.values) {
                routeFor(request.page)
            } else null,
        )
        selectHomeTab(if (request.page == "edit-whitelist") "apps" else if (request.page in homePages.values) request.page else "settings")
        lastEvent = null
        revision++
    }

    fun navigate(page: String) {
        require(page in pages)
        showingComponentCatalog = page == "components"
        if (showingComponentCatalog) return
        if (page in homePages.values) {
            mainVm.navigator.popToHome()
            selectHomeTab(page)
        } else {
            mainVm.navigator.navigate(routeFor(page))
        }
    }

    fun dismissIme(): Boolean {
        if (!environment.android.imeVisible) return false
        simulator.updateAndroid { it.copy(imeVisible = false) }
        return true
    }

    fun handleDeepLink(uri: String) {
        when (val link = CsLink.parse(uri)) {
            is CsLink.Home -> {
                BottomNavItem.allSubObjects.firstOrNull { it.key == link.tab }
                    ?.let(mainVm.homeNavigation::selectTab)
                mainVm.navigator.popToHome()
            }

            is CsLink.Page -> mainVm.navigator.navigate(link.route)
            CsLink.WeChatScanner -> unsupported()
            null -> toast.show(Res.string.uri_unknown.getSync(uri))
        }
    }

    private fun selectHomeTab(page: String) {
        homePages.entries.firstOrNull { it.value == page }?.let { entry ->
            mainVm.homeNavigation.selectTab(BottomNavItem.allSubObjects.first { it.key == entry.key })
        }
    }

    fun close() {
        viewModelStore.clear()
    }

    fun simulatorCommand(action: () -> Unit) {
        try {
            action()
        } catch (e: IllegalArgumentException) {
            toast.show("模拟设置无效：${e.message ?: e}")
        } catch (e: Exception) {
            toast.show("模拟设置更新失败：${e.javaClass.simpleName}: ${e.message ?: e}")
        }
    }

    fun record(event: String) {
        lastEvent = event
    }

    fun snapshot(): DesktopSnapshot {
        val settings = simulator.settings.value
        return DesktopSnapshot(
            scenario.page,
            scenario.variant,
            revision,
            settings.environment(),
            lastEvent,
            isolated,
            settings
        )
    }

    init {
        mainVm.registerCurrent()
    }

    companion object {
        private val homePages by lazy {
            mapOf(
                BottomNavItem.Dashboard.key to "dashboard",
                BottomNavItem.SubsManage.key to "subscriptions",
                BottomNavItem.AppList.key to "apps",
                BottomNavItem.Settings.key to "settings",
                BottomNavItem.Snapshots.key to "snapshots",
            )
        }
        val pages by lazy { homePages.values + (desktopRoutes.keys - "home") + "components" }
        val variants = listOf("normal", "empty")
    }
}

private val desktopRoutes by lazy {
    mapOf(
        "home" to HomeRoute,
        "lite-settings" to app.cyanskip.app.ui.navigation.LiteSettingsRoute,
        "action-toast" to ActionToastRoute,
        "edit-whitelist" to EditBlockAppListRoute,
        "about" to AboutRoute,
        "snapshot-settings" to SnapshotSettingsRoute,
        "crash-reports" to CrashReportRoute,
        "rule-editor" to UpsertRuleGroupRoute(LOCAL_SUBS_ID),
        "category-editor" to CategoryEditorRoute(LOCAL_SUBS_ID),
        "subs-global-groups" to SubsGlobalGroupListRoute(LOCAL_SUBS_ID),
        "subs-apps" to SubsAppListRoute(LOCAL_SUBS_ID),
        "subs-app-groups" to SubsAppGroupListRoute(LOCAL_SUBS_ID, "app.cyanskip"),
        "subs-categories" to SubsCategoryRoute(LOCAL_SUBS_ID),
        "subs-category-groups" to SubsCategoryGroupRoute(LOCAL_SUBS_ID, 0),
        "subs-global-exclude" to SubsGlobalGroupExcludeRoute(LOCAL_SUBS_ID, 0),
        "rule-exclude-editor" to RuleExcludeEditorRoute(LOCAL_SUBS_ID, 0),
        "app-config" to AppConfigRoute("app.cyanskip"),
        "action-log" to ActionLogRoute(),
        "image-preview" to ImagePreviewRoute(),
        "snapshots" to SnapshotPageRoute,
        "snapshot-preview" to SnapshotPreviewRoute(0, emptyList()),
    "snapshot-rule" to SnapshotRuleRoute(0),
        "web-view" to WebViewRoute(AppLinks.Home),
    )
}

fun routeFor(page: String): AppRoute = desktopRoutes.getValue(page)
fun routeName(route: AppRoute): String = when (route) {
    is SubsGlobalGroupListRoute -> "subs-global-groups"
    is SubsAppListRoute -> "subs-apps"
    is SubsAppGroupListRoute -> "subs-app-groups"
    is SubsCategoryRoute -> "subs-categories"
    is SubsCategoryGroupRoute -> "subs-category-groups"
    is SubsGlobalGroupExcludeRoute -> "subs-global-exclude"
    is RuleExcludeEditorRoute -> "rule-exclude-editor"
    is WebViewRoute -> "web-view"
    is SnapshotPreviewRoute -> "snapshot-preview"
    is SnapshotRuleRoute -> "snapshot-rule"
    is ImagePreviewRoute -> "image-preview"
    is AppConfigRoute -> "app-config"
    is ActionLogRoute -> "action-log"
    is UpsertRuleGroupRoute -> "rule-editor"
    is CategoryEditorRoute -> "category-editor"
    else -> desktopRoutes.entries.first { it.value == route }.key
}
