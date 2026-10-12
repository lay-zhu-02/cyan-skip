package app.cyanskip.app.ui.subscription

import kotlinx.coroutines.CancellationException
import app.cyanskip.app.model.ExcludeData
import app.cyanskip.app.rule.RuleGroupConfigService
import app.cyanskip.app.rule.RuleGroupTarget
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.rule.RuleSwitchTarget
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.db.SubscriptionConfigStore

data class RuleExcludeEditorDraft(
    val subscription: RawSubscription,
    val expected: ExcludeData,
    val text: String,
)

suspend fun saveRuleExclusions(
    target: RuleGroupTarget,
    subscription: RawSubscription,
    expected: ExcludeData,
    value: ExcludeData,
): Boolean {
    if (value == expected) return false
    if (target is RuleGroupTarget.Global) {
        val enabledApps = value.appIds.filter { (id, excluded) ->
            !excluded && expected.appIds[id] != false
        }.keys
        if (enabledApps.isNotEmpty()) {
            val request = RuleGroupConfigService.prepare(
                enabledApps.map { RuleSwitchTarget.GlobalApp(target.subsId, target.groupKey, it) },
                listOf(subscription), SubscriptionConfigStore.capture(),
            )
            if (!confirmRuleSwitch(request, RuleSetting.Enabled, MainViewModel.requireCurrent().dialogRequests, RuleSwitchHost.GlobalRuleApps)) {
                // Keep the draft and allow retry; EditorSaveSession must not record a completed save.
                throw CancellationException("Rule enable cancelled")
            }
        }
    }
    RuleGroupConfigService.replaceExclude(target, expected, value, subscription)
    return true
}
