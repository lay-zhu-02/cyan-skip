package app.cyanskip.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.rule_format_error
import app.cyanskip.app.resources.rule_invalid_position
import app.cyanskip.app.resources.selector_invalid_detail
import app.cyanskip.app.rule.RuleControlState
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.rule.toRuleGroupTarget
import app.cyanskip.app.rule.toSwitchTarget
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.RuleValidationError
import org.jetbrains.compose.resources.stringResource

@Composable
fun CsRuleGroupCard(
    modifier: Modifier = Modifier,
    subs: RawSubscription,
    appId: String?,
    group: RawSubscription.RawGroupProps,
    control: RuleControlState,
    onOpen: () -> Unit,
    onSettingChange: (RuleSetting) -> Unit,
    highlighted: Boolean = false,
    hideCategoryPrefix: Boolean = false,
    isSelectedMode: Boolean = false,
    isSelected: Boolean = false,
    selectionEnabled: Boolean = true,
    onLongClick: () -> Unit = {},
    onSelectedChange: () -> Unit = {},
) {
    CsRuleListItem(
        modifier = modifier, onClick = onOpen,
        selectedMode = isSelectedMode, selected = isSelected, highlighted = highlighted,
        selectable = control.canEnable,
        selectionEnabled = selectionEnabled,
        onLongClick = onLongClick, onSelect = onSelectedChange,
        trailing = {
            CsRuleEnableControl(
                control, onSettingChange, modifier = it,
                identity = group.toRuleGroupTarget(subs.id, appId).toSwitchTarget()
            )
        },
    ) {
        val description = if (!group.valid) {
            when (val error = group.validationError) {
                is RuleValidationError.SelectorError -> stringResource(
                    Res.string.selector_invalid_detail, error.source, error.detail,
                )

                is RuleValidationError.InvalidPosition -> stringResource(
                    Res.string.rule_invalid_position,
                    error.value
                )

                null -> stringResource(Res.string.rule_format_error)
            }
        } else group.desc?.takeIf { it.isNotBlank() }

        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CsGroupNameText(
                text = group.name, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                isGlobal = group is RawSubscription.RawGlobalGroup, maxLines = 2,
                categoryName = if (group is RawSubscription.RawAppGroup) subs.getCategory(group.name)?.name else null,
                hideCategoryPrefix = hideCategoryPrefix,
                overflow = TextOverflow.Ellipsis
            )
            if (description == null) CsRulePropertyIndicators(control)
        }
        if (description != null) {
            CsRuleSupportingContent(
                control, text = description,
                textColor = if (group.valid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
        }
    }
}
