package app.cyanskip.app.ui.rule

import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.rule_status_action_delay
import app.cyanskip.app.resources.rule_status_cooldown
import app.cyanskip.app.resources.rule_status_match_delay
import app.cyanskip.app.resources.rule_status_match_timeout
import app.cyanskip.app.resources.rule_status_maximum_actions
import app.cyanskip.app.resources.rule_status_prerequisite
import app.cyanskip.app.rule.ResolvedRule
import app.cyanskip.app.rule.RuleStatus
import app.cyanskip.app.ui.text.getSync

fun ResolvedRule.statusText(): String {
    val description = when (status) {
        RuleStatus.Ready -> "ok"
        RuleStatus.ActionLimitReached -> Res.string.rule_status_maximum_actions.getSync()
        RuleStatus.PrerequisitePending -> Res.string.rule_status_prerequisite.getSync()
        RuleStatus.MatchDelay -> Res.string.rule_status_match_delay.getSync()
        RuleStatus.MatchTimeout -> Res.string.rule_status_match_timeout.getSync()
        RuleStatus.Cooldown -> Res.string.rule_status_cooldown.getSync()
        RuleStatus.ActionDelay -> Res.string.rule_status_action_delay.getSync()
    }
    return "id:${subsItem.id}, v:${rawSubs.version}, type:${type}, gKey=${g.group.key}, gName:${g.group.name}, index:${index}, key:${key}, status:$description"
}
