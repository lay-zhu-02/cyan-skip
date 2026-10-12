package app.cyanskip.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cyanskip.app.model.AppInfo
import app.cyanskip.app.rule.RuleControlState
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.rule.RuleSwitchTarget
import app.cyanskip.app.subscription.RawSubscription

@Composable
fun CsSubsAppCard(
    subsId: Long,
    rawApp: RawSubscription.RawApp,
    appInfo: AppInfo?,
    control: RuleControlState,
    enabledGroupCount: Int,
    onClick: (() -> Unit),
    onSettingChange: (RuleSetting) -> Unit,
    selectedMode: Boolean = false,
    selected: Boolean = false,
    selectionEnabled: Boolean = true,
    onLongClick: () -> Unit = {},
    onSelect: () -> Unit = {},
    appIcon: @Composable () -> Unit,
    appName: @Composable () -> Unit,
) {
    CsRuleListItem(
        onClick = onClick, selectedMode = selectedMode, selected = selected,
        selectable = control.canEnable,
        selectionEnabled = selectionEnabled,
        onLongClick = onLongClick, onSelect = onSelect,
        leading = appIcon,
        trailing = {
            CsRuleEnableControl(
                control, onSettingChange, modifier = it,
                identity = RuleSwitchTarget.App(subsId, rawApp.id)
            )
        },
    ) {
        appName()
        Row(
            Modifier.fillMaxWidth().heightIn(min = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CsRuleStats(
                CsRuleStatsData(
                    appGroups = rawApp.groups.size,
                    enabledAppGroups = enabledGroupCount
                ), Modifier.weight(1f),
                emptyText = rawApp.id
            )
            CsRulePropertyIndicators(control)
        }
    }
}
