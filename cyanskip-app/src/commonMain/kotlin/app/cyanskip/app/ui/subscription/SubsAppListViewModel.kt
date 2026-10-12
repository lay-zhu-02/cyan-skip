package app.cyanskip.app.ui.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import app.cyanskip.app.rule.RuleGroupConfigService
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.rule.RuleSwitchRequest
import app.cyanskip.app.rule.RuleSwitchTarget
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.state.MutexState
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.ui.navigation.SubsAppListRoute
import app.cyanskip.app.ui.option.AppSortOption
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.db.Db
import app.cyanskip.db.SubscriptionConfigSnapshot
import app.cyanskip.db.SubscriptionConfigStore

data class SubsAppListUiState(
    val subscription: RawSubscription,
    val configs: SubscriptionConfigSnapshot,
    val appActionOrder: Map<String, Int>,
)

class SubsAppListViewModel(
    val route: SubsAppListRoute,
) : BaseViewModel() {
    val searchStr: StateFlow<String>
        field = MutableStateFlow("")

    fun setSearchStr(value: String) {
        searchStr.value = value
    }

    val showSearchBar: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setShowSearchBar(value: Boolean) {
        showSearchBar.value = value
    }

    private val mutation = MutexState()
    val busyFlow: StateFlow<Boolean> get() = mutation.state
    suspend fun runAction(action: suspend () -> Unit) {
        mutation.tryWithStateLock(action)
    }

    val uiState = RequiredSubscription(route.subsItemId, scope).buildUiState { subscription ->
        combine(
            SubscriptionConfigStore.observe(),
            Db.actionLogDao.queryLatestUniqueAppIds(route.subsItemId)
        ) { configs, ids ->
            SubsAppListUiState(subscription, configs, ids.mapIndexed { i, id -> id to i }.toMap())
        }
    }

    fun setSortType(value: AppSortOption) {
        SettingsRepository.updateSettings { it.copy(subsAppSort = value.value) }
    }

    fun setAppGroupType(value: Int) {
        SettingsRepository.updateSettings { it.copy(subsAppGroupType = value) }
    }

    fun toggleShowBlockApps() {
        SettingsRepository.updateSettings { it.copy(subsAppShowBlock = !it.subsAppShowBlock) }
    }

    fun prepareSwitches(state: SubsAppListUiState, appIds: Set<String>): RuleSwitchRequest =
        RuleGroupConfigService.prepare(
            appIds.map { RuleSwitchTarget.App(route.subsItemId, it) },
            listOf(state.subscription), state.configs
        )

    suspend fun applySwitches(request: RuleSwitchRequest, setting: RuleSetting) =
        RuleGroupConfigService.apply(request, setting)
}
