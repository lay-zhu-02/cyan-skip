package app.cyanskip.app.ui.subscription

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.remote_rule_delete_unsupported
import app.cyanskip.app.resources.selected_rules_reselect
import app.cyanskip.app.rule.RuleGroupConfigService
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.rule.RuleSwitchRequest
import app.cyanskip.app.rule.toRuleGroupTarget
import app.cyanskip.app.rule.toSwitchTarget
import app.cyanskip.app.state.MutexState
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.edit
import app.cyanskip.app.ui.navigation.SubsGlobalGroupListRoute
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.db.SubscriptionConfigSnapshot
import app.cyanskip.db.SubscriptionConfigStore

data class SubsGlobalGroupListUiState(
    val subscription: RawSubscription,
    val configs: SubscriptionConfigSnapshot,
)

class SubsGlobalGroupListViewModel(
    val route: SubsGlobalGroupListRoute,
) : BaseViewModel() {
    private val batchMutex = MutexState()
    val batchBusyFlow: StateFlow<Boolean> get() = batchMutex.state

    suspend fun runBatchAction(action: suspend () -> Unit) {
        batchMutex.tryWithStateLock(action)
    }

    private val subscription = RequiredSubscription(route.subsItemId, scope)

    val uiState = subscription.buildUiState { rawSubscription ->
        SubscriptionConfigStore.observe().map { configs ->
            SubsGlobalGroupListUiState(
                subscription = rawSubscription,
                configs = configs,
            )
        }
    }

    fun prepareSwitches(state: SubsGlobalGroupListUiState, keys: Set<Int>): RuleSwitchRequest {
        val targets = state.subscription.globalGroups.filter { it.key in keys }
            .map { it.toRuleGroupTarget(route.subsItemId).toSwitchTarget() }
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
                deletedSize = removeGlobalGroups { it.key in selectedKeys }.size
            }
        }
        return deletedSize
    }
}
