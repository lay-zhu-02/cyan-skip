package app.cyanskip.app.ui.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import app.cyanskip.app.model.ExcludeData
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.global_rule_missing
import app.cyanskip.app.rule.RuleGroupConfigService
import app.cyanskip.app.rule.RuleGroupTarget
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.rule.RuleSwitchRequest
import app.cyanskip.app.rule.RuleSwitchTarget
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.state.MutexState
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.navigation.SubsGlobalGroupExcludeRoute
import app.cyanskip.app.ui.option.AppSortOption
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.db.Db
import app.cyanskip.db.SubscriptionConfigSnapshot
import app.cyanskip.db.SubscriptionConfigStore

data class SubsGlobalGroupExcludeUiState(
    val subscription: RawSubscription,
    val group: RawSubscription.RawGlobalGroup,
    val configs: SubscriptionConfigSnapshot,
    val appActionOrder: Map<String, Int>,
) {
    private val config = configs.globalGroupConfigs.find {
        it.subsId == subscription.id && it.groupKey == group.key
    }
    val excludeData: ExcludeData = ExcludeData.parse(config?.exclude)
    val declaredAppIds: Set<String> = (group.apps.orEmpty().map { it.id } +
            group.rules.flatMap { it.apps.orEmpty().map { app -> app.id } } +
            excludeData.appIds.keys + excludeData.activityIds.map { it.first }).toSet()
}

class SubsGlobalGroupExcludeViewModel(
    val route: SubsGlobalGroupExcludeRoute,
) : BaseViewModel() {
    val draft: StateFlow<RuleExcludeEditorDraft?>
        field = MutableStateFlow(null)
    private var saveSession = EditorSaveSession<Boolean>()

    fun startEditing(state: SubsGlobalGroupExcludeUiState) {
        saveSession = EditorSaveSession()
        draft.value = RuleExcludeEditorDraft(
            state.subscription, state.excludeData, state.excludeData.stringify(),
        )
    }

    fun setDraftText(value: String) {
        draft.update { it?.copy(text = value) }
    }

    fun closeEditor() {
        draft.value = null
    }

    suspend fun saveEditor(value: RuleExcludeEditorDraft): Boolean = saveSession.save {
        saveRuleExclusions(
            RuleGroupTarget.Global(route.subsItemId, route.groupKey),
            value.subscription, value.expected, ExcludeData.parse(value.text),
        )
    }

    val query: StateFlow<String>
        field = MutableStateFlow("")

    fun setQuery(value: String) {
        query.value = value
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
            Db.actionLogDao.queryLatestUniqueAppIds(route.subsItemId, route.groupKey)
        ) { configs, ids ->
            SubsGlobalGroupExcludeUiState(
                subscription,
                subscription.globalGroups.find { it.key == route.groupKey }
                    ?: error(Res.string.global_rule_missing.getSync()),
                configs,
                ids.mapIndexed { i, id -> id to i }.toMap()
            )
        }
    }

    fun setSortType(option: AppSortOption) {
        SettingsRepository.updateSettings { it.copy(subsExcludeSort = option.value) }
    }

    fun setAppGroupType(value: Int) {
        SettingsRepository.updateSettings { it.copy(subsExcludeAppGroupType = value) }
    }

    fun toggleShowBlockApps() {
        SettingsRepository.updateSettings { it.copy(subsExcludeShowBlockApp = !it.subsExcludeShowBlockApp) }
    }

    fun prepareSwitches(
        state: SubsGlobalGroupExcludeUiState,
        appIds: Set<String>
    ): RuleSwitchRequest =
        RuleGroupConfigService.prepare(
            appIds.map { RuleSwitchTarget.GlobalApp(route.subsItemId, route.groupKey, it) },
            listOf(state.subscription), state.configs
        )

    suspend fun applySwitches(request: RuleSwitchRequest, setting: RuleSetting, confirmation: String? = null) =
        applyRuleSwitchWithConfirmation(request, setting, MainViewModel.requireCurrent().dialogRequests, RuleSwitchHost.GlobalRuleApps, confirmation)
}
