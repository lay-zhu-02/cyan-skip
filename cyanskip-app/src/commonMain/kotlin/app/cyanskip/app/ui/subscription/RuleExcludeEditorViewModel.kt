package app.cyanskip.app.ui.subscription

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import app.cyanskip.app.model.ExcludeData
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.rule_missing
import app.cyanskip.app.rule.RuleGroupConfigService
import app.cyanskip.app.rule.RuleGroupTarget
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.ui.navigation.RuleExcludeEditorRoute
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.app.ui.text.getSync

data class RuleExcludeEditorUiState(
    val subscription: RawSubscription,
    val group: RawSubscription.RawGroupProps,
    val exclude: ExcludeData,
)

class RuleExcludeEditorViewModel(
    private val route: RuleExcludeEditorRoute,
) : BaseViewModel() {
    val draft: StateFlow<RuleExcludeEditorDraft?>
        field = MutableStateFlow(null)

    fun setText(value: String) {
        draft.update { it?.copy(text = value) }
    }

    private val target = route.appId?.let { RuleGroupTarget.App(route.subsId, it, route.groupKey) }
        ?: RuleGroupTarget.Global(route.subsId, route.groupKey)
    private val saveSession = EditorSaveSession<Boolean>()

    val uiState = RequiredSubscription(route.subsId, scope).buildUiState { subscription ->
        val group = (route.appId?.let(subscription::getAppGroups) ?: subscription.globalGroups)
            .find { it.key == route.groupKey }
            ?: error(Res.string.rule_missing.getSync())
        RuleGroupConfigService.groupConfiguration(target).map { configuration ->
            val exclude = ExcludeData.parse(configuration.group?.exclude)
            if (draft.value == null) {
                draft.value = RuleExcludeEditorDraft(subscription, exclude, exclude.stringify(route.appId))
            }
            RuleExcludeEditorUiState(
                subscription,
                group,
                exclude
            )
        }
    }

    suspend fun save(
        subscription: RawSubscription,
        expected: ExcludeData,
        value: ExcludeData
    ): Boolean =
        saveSession.save {
            saveRuleExclusions(target, subscription, expected, value)
        }
}
