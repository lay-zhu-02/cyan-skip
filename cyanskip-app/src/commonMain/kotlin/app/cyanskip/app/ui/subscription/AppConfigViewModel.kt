package app.cyanskip.app.ui.subscription

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.selected_app_rules_no_copyable
import app.cyanskip.app.rule.RuleGroupConfigService
import app.cyanskip.app.rule.RuleGroupTarget
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.rule.RuleSwitchRequest
import app.cyanskip.app.rule.toSwitchTarget
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.state.MutexState
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.subscription.SubscriptionJson.toJson5String
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.ui.navigation.AppConfigRoute
import app.cyanskip.app.ui.option.RuleSortOption
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.db.ActionLog
import app.cyanskip.db.Db
import app.cyanskip.db.RuleGroupType
import app.cyanskip.db.SubsItem
import app.cyanskip.db.SubscriptionConfigSnapshot
import app.cyanskip.db.SubscriptionConfigStore

data class AppRuleSubscription(val subsItem: SubsItem, val subscription: RawSubscription)

data class AppConfigUiState(
    val configs: SubscriptionConfigSnapshot,
    val subsPairs: List<Pair<AppRuleSubscription, List<RawSubscription.RawGroupProps>>>,
    val latestLogs: List<ActionLog>,
)

class AppConfigViewModel(
    val route: AppConfigRoute,
) : BaseViewModel() {
    val revealDisabledRules: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setRevealDisabledRules(value: Boolean) {
        revealDisabledRules.value = value
    }

    private val batchMutex = MutexState()
    fun removeFromWhitelist() {
        SettingsRepository.updateBlockMatchAppList { it - route.appId }
    }



    val batchBusyFlow: StateFlow<Boolean> get() = batchMutex.state

    suspend fun runBatchAction(action: suspend () -> Unit) {
        batchMutex.tryWithStateLock(action)
    }

    fun setRuleSortType(option: RuleSortOption) {
        SettingsRepository.updateSettings { it.copy(appRuleSort = option.value) }
    }

    fun setShowDisabledRules(show: Boolean) {
        SettingsRepository.updateSettings { it.copy(showDisabledRule = show) }
    }

    val uiState: StateFlow<Loadable<AppConfigUiState>> =
        SubscriptionRepository.snapshotFlow.flatMapLatest { source ->
            when (source) {
                Loadable.Loading -> flowOf(Loadable.Loading)
                is Loadable.Failure -> flowOf(source)
                is Loadable.Ready -> combine<SubscriptionConfigSnapshot, List<ActionLog>, Loadable<AppConfigUiState>>(
                    SubscriptionConfigStore.observe(),
                    Db.actionLogDao.queryLatestByAppId(route.appId),
                ) { configs, logs ->
                    val globalConfigs =
                        configs.globalGroupConfigs.associateBy { it.subsId to it.groupKey }
                    Loadable.Ready(
                        AppConfigUiState(
                            configs = configs,
                            subsPairs = configs.subsItems.filter { it.enable }.sortedBy { it.order }
                                .mapNotNull { item ->
                                    val sub = source.value.subscriptions[item.id]
                                        ?: return@mapNotNull null
                                    val appEnabled = configs.appConfigs.find {
                                        it.subsId == item.id && it.appId == route.appId
                                    }?.enable != false
                                    // Parent switches define this summary's scope, even when disabled rules are shown.
                                    val globalGroups = sub.globalGroups.filter { group ->
                                        globalConfigs[item.id to group.key]?.enable ?: group.enable
                                        ?: true
                                    }
                                    val groups =
                                        globalGroups + if (appEnabled) sub.getAppGroups(route.appId) else emptyList()
                                    (AppRuleSubscription(
                                        item,
                                        sub
                                    ) to groups).takeIf { groups.isNotEmpty() }
                                },
                            latestLogs = logs,
                        )
                    )
                }.catch { emit(Loadable.Failure(it)) }
            }
        }.stateIn(scope, SharingStarted.Eagerly, Loadable.Loading)

    fun prepareSwitches(state: AppConfigUiState, targets: Set<RuleGroupTarget>): RuleSwitchRequest =
        RuleGroupConfigService.prepare(
            targets.map { it.toSwitchTarget() },
            state.subsPairs.map { it.first.subscription }, state.configs
        )

    suspend fun applySwitches(request: RuleSwitchRequest, setting: RuleSetting, confirmation: String? = null) =
        applyRuleSwitchWithConfirmation(request, setting, MainViewModel.requireCurrent().dialogRequests, RuleSwitchHost.AppRules, confirmation)

    suspend fun buildSelectedGroupsText(selectedGroups: Set<RuleGroupTarget>): String =
        withContext(Dispatchers.Default) {
            val selectedKeys = selectedGroups.mapTo(mutableSetOf()) {
                Triple(it.subsId, it.groupType, it.groupKey)
            }
            val subsPairs = uiState.value.value?.subsPairs.orEmpty()
            val groups = subsPairs.flatMap { (entry, groups) ->
                groups.filterIsInstance<RawSubscription.RawAppGroup>().filter { group ->
                    Triple(entry.subsItem.id, RuleGroupType.App, group.key) in selectedKeys
                }
            }
            check(groups.isNotEmpty()) { Res.string.selected_app_rules_no_copyable.getSync() }
            toJson5String(
                RawSubscription.RawApp(
                    id = route.appId,
                    name = AppInfoRepository.snapshot?.apps.orEmpty()[route.appId]?.name,
                    groups = groups,
                )
            )
        }
}
