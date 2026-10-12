package app.cyanskip.app.ui.subscription

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.global_control_group
import app.cyanskip.app.resources.global_rule_default_apps
import app.cyanskip.app.resources.global_rule_default_apps_follow
import app.cyanskip.app.resources.global_control_app
import app.cyanskip.app.resources.global_control_default_app
import app.cyanskip.app.resources.global_control_default_group
import app.cyanskip.app.resources.global_control_name_off
import app.cyanskip.app.resources.global_control_launcher_off
import app.cyanskip.app.resources.global_control_system_off
import app.cyanskip.app.resources.global_control_local_off
import app.cyanskip.app.resources.global_control_subscription_off
import app.cyanskip.app.resources.global_control_app_off
import app.cyanskip.app.resources.global_control_group_hint
import app.cyanskip.app.resources.global_control_pages_hint
import app.cyanskip.app.resources.global_control_partial
import app.cyanskip.app.resources.global_control_enabled
import app.cyanskip.app.resources.action_close
import app.cyanskip.app.resources.action_turn_on
import app.cyanskip.app.resources.category_settings
import app.cyanskip.app.resources.category_use_group_default
import app.cyanskip.app.resources.category_use_group_default_description
import app.cyanskip.app.resources.dialog_close
import app.cyanskip.app.resources.rule_control_allowed
import app.cyanskip.app.resources.rule_control_app
import app.cyanskip.app.resources.rule_control_category
import app.cyanskip.app.resources.rule_control_disabled
import app.cyanskip.app.resources.rule_control_own
import app.cyanskip.app.resources.rule_control_restricted
import app.cyanskip.app.resources.rule_control_subscription
import app.cyanskip.app.resources.rule_control_title
import app.cyanskip.app.resources.rule_control_used
import app.cyanskip.app.resources.rule_current_restrictions
import app.cyanskip.app.resources.rule_group_default
import app.cyanskip.app.rule.RuleControlExplanation
import app.cyanskip.app.rule.RuleControlStep
import app.cyanskip.app.rule.RuleControlStepKind
import app.cyanskip.app.rule.RuleControlNote
import app.cyanskip.app.resources.setting_follow_default
import app.cyanskip.app.resources.setting_manual_disabled
import app.cyanskip.app.rule.RuleControlState
import app.cyanskip.app.rule.GlobalAppDefaultOffReason
import app.cyanskip.app.rule.RuleLimitationKind
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.ui.component.CsAppNameText
import app.cyanskip.app.ui.component.CsGroupNameText
import app.cyanskip.app.ui.component.CsIcon
import app.cyanskip.app.ui.component.CsIconButton
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsScaffold
import app.cyanskip.app.ui.component.CsPageBottomSpace
import app.cyanskip.app.ui.component.CsTopAppBar
import app.cyanskip.app.ui.platform.CsFullscreenDialog
import org.jetbrains.compose.resources.stringResource

@Composable
fun CsRuleControlDialog(
    subscription: RawSubscription,
    group: RawSubscription.RawGroupProps,
    appId: String?,
    control: RuleControlState,
    explanation: RuleControlExplanation,
    onDismissRequest: () -> Unit,
) {

    val global = group is RawSubscription.RawGlobalGroup
    val inApp = global && appId != null
    val category = if (global) null else subscription.getCategory(group.name)
    val appName = subscription.apps.find { it.id == appId }?.name ?: appId.orEmpty()
    val result = stringResource(when {
        explanation.enabled -> if (global) Res.string.global_control_enabled else Res.string.rule_control_allowed
        explanation.restricted -> Res.string.rule_control_restricted
        else -> Res.string.rule_control_disabled
    })
    CsFullscreenDialog(onDismissRequest) {
        CsScaffold(
            topBar = {
                Column {
                    CsTopAppBar(
                        title = {
                            Text(
                                stringResource(Res.string.rule_control_title),
                                maxLines = 1
                            )
                        },
                        actions = {
                            CsIconButton(
                                CsIcons.Close, onClick = onDismissRequest,
                                contentDescription = stringResource(Res.string.dialog_close)
                            )
                        },
                    )
                    Column(
                        Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CsGroupNameText(
                                text = group.name,
                                isGlobal = global,
                                categoryName = category?.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            CsIcon(
                                imageVector = if (explanation.enabled) CsIcons.ToggleOn else CsIcons.ToggleOff,
                                modifier = Modifier.size(24.dp),
                                contentDescription = result,
                                tint = if (explanation.enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                            )
                        }
                        if (inApp) {
                            CsAppNameText(appId = checkNotNull(appId), fallbackName = appName)
                        }
                    }
                }
            },
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                explanation.steps.forEachIndexed { index, step ->
                    ControlTimelineStep(
                        step = step,
                        title = stepTitle(step.kind, global),
                        description = stepDescription(step, subscription.name),
                        appId = appId.takeIf { step.kind == RuleControlStepKind.App },
                        appName = appName,
                        active = index <= explanation.decidingIndex,
                        decisive = index == explanation.decidingIndex,
                        first = index == 0,
                        last = index == explanation.steps.lastIndex,
                    )
                }
                if (global) {
                    if (!inApp) Text(
                        stringResource(Res.string.global_control_group_hint),
                        modifier = Modifier.padding(12.dp),
                    )
                    if (inApp && control.limitations.blockedRules in 1 until control.limitations.ruleCount) {
                        Text(stringResource(Res.string.global_control_partial), Modifier.padding(12.dp))
                    }
                    if ((control.limitations.builtIn + control.limitations.personal).any {
                            it.kind == RuleLimitationKind.ExcludedPagePrefix || it.kind == RuleLimitationKind.ExcludedPageExact
                        }) {
                        Text(stringResource(Res.string.global_control_pages_hint), Modifier.padding(12.dp))
                    }
                }
                CsPageBottomSpace()
            }
        }
    }
}


@Composable
private fun stepTitle(kind: RuleControlStepKind, global: Boolean): String = stringResource(when (kind) {
    RuleControlStepKind.Subscription -> Res.string.rule_control_subscription
    RuleControlStepKind.App -> Res.string.rule_control_app
    RuleControlStepKind.Restrictions -> Res.string.rule_current_restrictions
    RuleControlStepKind.Own -> Res.string.rule_control_own
    RuleControlStepKind.Category -> Res.string.rule_control_category
    RuleControlStepKind.GroupDefault -> if (global) Res.string.global_control_default_group else Res.string.rule_group_default
    RuleControlStepKind.GlobalGroup -> Res.string.global_control_group
    RuleControlStepKind.GlobalApp -> Res.string.global_control_app
    RuleControlStepKind.DefaultApps -> Res.string.global_rule_default_apps
    RuleControlStepKind.GlobalDefault -> Res.string.global_control_default_app
})

@Composable
private fun stepDescription(step: RuleControlStep, subscriptionName: String): String? {
    if (step.kind == RuleControlStepKind.Subscription) return subscriptionName
    if (step.kind == RuleControlStepKind.Restrictions) return step.restrictions.map { it.label }
        .joinToString("\n").ifEmpty { stringResource(Res.string.rule_control_restricted) }
    if (step.defaultOffReasons.isNotEmpty()) return step.defaultOffReasons.map { reason ->
        stringResource(when (reason) {
            GlobalAppDefaultOffReason.Launcher -> Res.string.global_control_launcher_off
            GlobalAppDefaultOffReason.SystemApp -> Res.string.global_control_system_off
            GlobalAppDefaultOffReason.LocalDefault -> Res.string.global_control_local_off
            GlobalAppDefaultOffReason.SubscriptionDefault -> Res.string.global_control_subscription_off
            GlobalAppDefaultOffReason.SubscriptionApp -> Res.string.global_control_app_off
            GlobalAppDefaultOffReason.SameNameAppGroup -> Res.string.global_control_name_off
        })
    }.joinToString("\n")
    return step.note?.let { note -> stringResource(when (note) {
        RuleControlNote.FollowDefault -> Res.string.setting_follow_default
        RuleControlNote.ManualDisabled -> Res.string.setting_manual_disabled
        RuleControlNote.FollowSubscription -> Res.string.global_rule_default_apps_follow
        RuleControlNote.CategorySettings -> Res.string.category_settings
        RuleControlNote.IgnoreCategory -> Res.string.category_use_group_default_description
        RuleControlNote.FollowGroup -> Res.string.category_use_group_default
    }) }
}

@Composable
private fun switchLabel(enabled: Boolean) =
    if (enabled) stringResource(Res.string.action_turn_on) else stringResource(Res.string.action_close)

@Composable
private fun ControlTimelineStep(
    step: RuleControlStep,
    title: String,
    description: String?,
    appId: String?,
    appName: String,
    active: Boolean,
    decisive: Boolean,
    first: Boolean,
    last: Boolean,
) {
    val decidingLabel = stringResource(Res.string.rule_control_used)
    val statusLabel = step.enabled?.let { switchLabel(it) }
    val color = when {
        !active -> MaterialTheme.colorScheme.outline
        decisive && (step.enabled == false) -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val itemSpacing = if (last) 0.dp else 12.dp
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Canvas(Modifier.width(32.dp).fillMaxHeight()) {
            val x = 12.dp.toPx()
            val y = (size.height - itemSpacing.toPx()) / 2f
            val radius = if (decisive) 6.dp.toPx() else 4.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
            if (!first) drawLine(
                if (active) color else lineColor,
                Offset(x, 0f),
                Offset(x, y - radius),
                2.dp.toPx(),
                pathEffect = if (active) null else dash
            )
            if (!last) drawLine(
                if (active && !decisive) color else lineColor,
                Offset(x, y + radius), Offset(x, size.height), 2.dp.toPx(),
                pathEffect = if (active && !decisive) null else dash
            )
            if (active) drawCircle(color, radius, Offset(x, y))
            else drawCircle(color, radius, Offset(x, y), style = Stroke(1.5.dp.toPx()))
        }
        Surface(
            color = when {
                decisive && (step.enabled == false) -> MaterialTheme.colorScheme.errorContainer
                decisive -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f).padding(bottom = itemSpacing)
                .semantics(mergeDescendants = true) {
                    val description = listOfNotNull(statusLabel, decidingLabel.takeIf { decisive })
                    if (description.isNotEmpty()) stateDescription = description.joinToString("，")
                },
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (appId != null) {
                        CsAppNameText(
                            appId = appId,
                            fallbackName = appName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (!description.isNullOrBlank()) {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (active) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                step.enabled?.let { enabled ->
                    CsIcon(
                        imageVector = if (enabled) CsIcons.ToggleOn else CsIcons.ToggleOff,
                        modifier = Modifier.padding(start = 12.dp).size(24.dp),
                        contentDescription = null,
                        tint = color,
                    )
                }
            }
        }
    }
}
