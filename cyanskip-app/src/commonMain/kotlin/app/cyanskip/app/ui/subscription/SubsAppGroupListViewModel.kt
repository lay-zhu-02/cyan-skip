package app.cyanskip.app.ui.subscription

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.remote_rule_delete_unsupported
import app.cyanskip.app.resources.selected_rules_no_copyable
import app.cyanskip.app.resources.selected_rules_reselect
import app.cyanskip.app.resources.subscription_app_not_loaded
import app.cyanskip.app.rule.RuleGroupConfigService
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.rule.RuleSwitchRequest
import app.cyanskip.app.rule.RuleSwitchTarget
import app.cyanskip.app.rule.toRuleGroupTarget
import app.cyanskip.app.rule.toSwitchTarget
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.state.MutexState
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.SubscriptionJson.toJson5String
import app.cyanskip.app.subscription.edit
import app.cyanskip.app.ui.navigation.SubsAppGroupListRoute
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.db.SubscriptionConfigSnapshot
import app.cyanskip.db.SubscriptionConfigStore

data class SubsAppGroupListUiState(
    val subscription: RawSubscription,
    val app: RawSubscription.RawApp,
    val configs: SubscriptionConfigSnapshot,
)

class SubsAppGroupListViewModel(
    val route: SubsAppGroupListRoute,
) : BaseViewModel() {
    val showAppSetting: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setShowAppSetting(value: Boolean) {
        showAppSetting.value = value
    }

    private val batchMutex = MutexState()
    fun removeFromWhitelist() {
        SettingsRepository.updateBlockMatchAppList { it - route.appId }
    }



    val batchBusyFlow: StateFlow<Boolean> get() = batchMutex.state

    suspend fun runBatchAction(action: suspend () -> Unit) {
        batchMutex.tryWithStateLock(action)
    }

    private val subscription = RequiredSubscription(route.subsItemId, scope)

    val uiState = subscription.buildUiState { rawSubscription ->
        SubscriptionConfigStore.observe().map { configs ->
            SubsAppGroupListUiState(
                subscription = rawSubscription,
                app = rawSubscription.getApp(
                    route.appId,
                    AppInfoRepository.snapshot?.apps.orEmpty()[route.appId]?.name
                ),
                configs = configs,
            )
        }
    }

    suspend fun buildSelectedGroupsText(selectedKeys: Set<Int>): String =
        withContext(Dispatchers.Default) {
            val app = uiState.value.value?.app
                ?: error(Res.string.subscription_app_not_loaded.getSync())
            val groups = app.groups.filter { it.key in selectedKeys }
            check(groups.isNotEmpty()) { Res.string.selected_rules_no_copyable.getSync() }
            toJson5String(
                app.copy(
                    groups = groups,
                ),
            )
        }

    fun prepareAppSwitch(state: SubsAppGroupListUiState): RuleSwitchRequest =
        RuleGroupConfigService.prepare(
            listOf(RuleSwitchTarget.App(route.subsItemId, route.appId)),
            listOf(state.subscription), state.configs
        )

    fun prepareSwitches(state: SubsAppGroupListUiState, keys: Set<Int>): RuleSwitchRequest {
        val targets = state.app.groups.filter { it.key in keys }
            .map { it.toRuleGroupTarget(route.subsItemId, route.appId).toSwitchTarget() }
        check(targets.size == keys.size) { Res.string.selected_rules_reselect.getSync() }
        return RuleGroupConfigService.prepare(targets, listOf(state.subscription), state.configs)
    }

    suspend fun applySwitches(request: RuleSwitchRequest, setting: RuleSetting) =
        RuleGroupConfigService.apply(request, setting)

    suspend fun deleteSelectedGroups(selectedKeys: Set<Int>): Int {
        check(route.subsItemId < 0) { Res.string.remote_rule_delete_unsupported.getSync() }
        var deletedSize = 0
        subscription.update { current ->
            current.edit {
                deletedSize = removeAppGroups(
                    appId = route.appId,
                    removeAppIfEmpty = true,
                ) { it.key in selectedKeys }.size
            }
        }
        return deletedSize
    }
}
