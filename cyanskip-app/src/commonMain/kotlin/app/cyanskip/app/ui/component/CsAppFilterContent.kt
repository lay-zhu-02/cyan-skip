package app.cyanskip.app.ui.component

import androidx.compose.runtime.Composable
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.filter_title
import app.cyanskip.app.resources.group_title
import app.cyanskip.app.resources.sort_title
import app.cyanskip.app.resources.whitelist_title
import app.cyanskip.app.ui.option.AppGroupOption
import app.cyanskip.app.ui.option.AppSortOption
import app.cyanskip.app.ui.option.findOption
import org.jetbrains.compose.resources.stringResource

@Composable
fun CsAppFilterContent(
    sort: Int,
    groupType: Int,
    showBlockApps: Boolean,
    onSort: (AppSortOption) -> Unit,
    onAppGroup: (Int) -> Unit,
    onToggleBlock: () -> Unit,
    includeUninstalled: Boolean = true,
    allowEmptyGroups: Boolean = false,
) {
    CsMenuGroupCard(inTop = true, title = stringResource(Res.string.sort_title)) {
        AppSortOption.objects.forEach { option ->
            CsMenuItemRadioButton(
                text = option.label,
                selected = AppSortOption.objects.findOption(sort) == option,
                onClick = { onSort(option) })
        }
    }
    CsMenuGroupCard(title = stringResource(Res.string.group_title)) {
        val options =
            if (includeUninstalled) AppGroupOption.allObjects else AppGroupOption.normalObjects
        options.forEach { option ->
            val next = option.invert(groupType)
            CsMenuItemCheckbox(
                text = option.label, checked = option.include(groupType),
                enabled = allowEmptyGroups || next != 0, onClick = { onAppGroup(next) })
        }
    }
    CsMenuGroupCard(title = stringResource(Res.string.filter_title)) {
        CsMenuItemCheckbox(
            text = stringResource(Res.string.whitelist_title), checked = showBlockApps,
            onClick = onToggleBlock
        )
    }
}
