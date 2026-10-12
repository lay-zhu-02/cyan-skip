package app.cyanskip.app.ui.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.category_removed_back_notice
import app.cyanskip.app.resources.category_selected_rules_changed
import app.cyanskip.app.rule.CategoryPolicy
import app.cyanskip.app.rule.CategorySetting
import app.cyanskip.app.rule.RuleGroupConfigService
import app.cyanskip.app.rule.RuleGroupTarget
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.rule.RuleSwitchRequest
import app.cyanskip.app.rule.toSwitchTarget
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.state.MutexState
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.ui.navigation.SubsCategoryGroupRoute
import app.cyanskip.app.ui.option.AppSortOption
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.db.Db
import app.cyanskip.db.SubsAppGroupConfig
import app.cyanskip.db.SubsCategoryConfig
import app.cyanskip.db.SubscriptionConfigSnapshot
import app.cyanskip.db.SubscriptionConfigStore

data class SubsCategoryGroupUiState(
    val subscription: RawSubscription,
    val category: RawSubscription.RawCategory,
    val apps: List<RawSubscription.RawApp>,
    val groupConfigs: Map<RuleGroupTarget.App, SubsAppGroupConfig>,
    val categoryConfig: SubsCategoryConfig?,
    val appActionOrder: Map<String, Int>,
    val configs: SubscriptionConfigSnapshot,
) {
    val targets: Set<RuleGroupTarget.App> = apps.flatMap { app ->
        app.groups.map { RuleGroupTarget.App(subscription.id, app.id, it.key) }
    }.toSet()
    val overrideTargets: Set<RuleGroupTarget.App> = targets.filterTo(mutableSetOf()) {
        groupConfigs[it]?.enable != null
    }
    val setting: CategorySetting = CategoryPolicy.setting(categoryConfig)
}

class SubsCategoryGroupViewModel(
    private val route: SubsCategoryGroupRoute,
) : BaseViewModel() {
    val showActions: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setShowActions(value: Boolean) {
        showActions.value = value
    }

    val showFilter: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setShowFilter(value: Boolean) {
        showFilter.value = value
    }

    private val mutation = MutexState()
    val busyFlow: StateFlow<Boolean> get() = mutation.state

    suspend fun runAction(action: suspend () -> Unit) {
        mutation.tryWithStateLock(action)
    }

    val uiState = RequiredSubscription(route.subsId, scope).buildUiState { subscription ->
        combine(
            SubscriptionConfigStore.observe(),
            Db.actionLogDao.queryLatestUniqueAppIds(route.subsId),
        ) { snapshot, actionAppIds ->
            val disabledAppIds = snapshot.appConfigs.filter {
                it.subsId == subscription.id && it.enable == false
            }.mapTo(mutableSetOf()) { it.appId }
            SubsCategoryGroupUiState(
                configs = snapshot,
                subscription = subscription,
                category = subscription.categories.find { it.key == route.categoryKey }
                    ?: error(Res.string.category_removed_back_notice.getSync()),
                apps = subscription.getCategoryApps(route.categoryKey)
                    .filter { it.id !in disabledAppIds },
                groupConfigs = snapshot.appGroupConfigs.filter { it.subsId == subscription.id }
                    .associateBy { RuleGroupTarget.App(it.subsId, it.appId, it.groupKey) },
                categoryConfig = snapshot.categoryConfigs.find {
                    it.subsId == subscription.id && it.categoryKey == route.categoryKey
                },
                appActionOrder = actionAppIds.mapIndexed { index, id -> id to index }.toMap(),
            )
        }
    }

    fun setSortType(option: AppSortOption) {
        SettingsRepository.updateSettings { it.copy(subsCategorySort = option.value) }
    }

    fun setAppGroupType(value: Int) {
        SettingsRepository.updateSettings { it.copy(subsCategoryGroupType = value) }
    }

    fun toggleShowBlockApps() {
        SettingsRepository.updateSettings { it.copy(subsCategoryShowBlock = !it.subsCategoryShowBlock) }
    }

    suspend fun setCategorySetting(
        subscription: RawSubscription,
        setting: CategorySetting,
        expected: CategorySetting
    ) {
        RuleGroupConfigService.setCategorySetting(
            subscription,
            route.categoryKey,
            setting,
            expected
        )
    }

    fun prepareSwitches(
        state: SubsCategoryGroupUiState,
        targets: Set<RuleGroupTarget.App>
    ): RuleSwitchRequest {
        check(state.targets.containsAll(targets)) { Res.string.category_selected_rules_changed.getSync() }
        return RuleGroupConfigService.prepare(
            targets.map { it.toSwitchTarget() },
            listOf(state.subscription),
            state.configs
        )
    }

    suspend fun applySwitches(request: RuleSwitchRequest, setting: RuleSetting) =
        RuleGroupConfigService.apply(request, setting)

    suspend fun deleteCategory(subscription: RawSubscription) {
        SubscriptionRepository.deleteCategory(subscription, route.categoryKey)
    }
}
