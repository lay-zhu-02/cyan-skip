package app.cyanskip.app.ui.component

import androidx.compose.runtime.Composable
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_close
import app.cyanskip.app.resources.action_turn_on
import app.cyanskip.app.resources.settings_reset_default
import app.cyanskip.app.rule.RuleSetting
import org.jetbrains.compose.resources.stringResource

@Composable
fun CsRuleBatchMenuItems(
    enabled: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (RuleSetting) -> Unit,
) {
    CsBatchActionMenuItem(
        stringResource(Res.string.action_turn_on),
        onDismiss,
        { onUpdate(RuleSetting.Enabled) },
        enabled
    )
    CsBatchActionMenuItem(
        stringResource(Res.string.action_close),
        onDismiss,
        { onUpdate(RuleSetting.Disabled) },
        enabled
    )
    CsBatchActionMenuItem(
        stringResource(Res.string.settings_reset_default),
        onDismiss,
        { onUpdate(RuleSetting.FollowDefault) },
        enabled
    )
}
