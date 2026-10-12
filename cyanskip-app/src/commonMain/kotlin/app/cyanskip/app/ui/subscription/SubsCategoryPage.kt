package app.cyanskip.app.ui.subscription

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_close
import app.cyanskip.app.resources.action_delete
import app.cyanskip.app.resources.action_turn_on
import app.cyanskip.app.resources.categories_delete_confirmation
import app.cyanskip.app.resources.categories_empty
import app.cyanskip.app.resources.category_add
import app.cyanskip.app.resources.category_delete
import app.cyanskip.app.resources.category_follow_subscription
import app.cyanskip.app.resources.category_help
import app.cyanskip.app.resources.category_help_description
import app.cyanskip.app.resources.category_use_group_default
import app.cyanskip.app.resources.delete_success
import app.cyanskip.app.resources.rule_categories
import app.cyanskip.app.resources.update_success
import app.cyanskip.app.rule.CategorySetting
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsAnimatedFloatingActionButton
import app.cyanskip.app.ui.component.CsBatchActionMenuItem
import app.cyanskip.app.ui.component.CsEmptyState
import app.cyanskip.app.ui.component.CsIcon
import app.cyanskip.app.ui.component.CsIconButton
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsMultiSelectionActions
import app.cyanskip.app.ui.component.CsMultiSelectionTopAppBar
import app.cyanskip.app.ui.component.CsPageBottomSpace
import app.cyanskip.app.ui.component.CsRuleListItem
import app.cyanskip.app.ui.component.CsRuleStats
import app.cyanskip.app.ui.component.CsRuleStatsData
import app.cyanskip.app.ui.component.CsScaffold
import app.cyanskip.app.ui.component.CsSubscriptionPageContent
import app.cyanskip.app.ui.component.CsTooltipIconButtonBox
import app.cyanskip.app.ui.component.CsTwoLineText
import app.cyanskip.app.ui.component.rememberListScrollState
import app.cyanskip.app.ui.component.rememberMultiSelectionState
import app.cyanskip.app.ui.navigation.CategoryEditorRoute
import app.cyanskip.app.ui.navigation.SubsCategoryGroupRoute
import app.cyanskip.app.ui.navigation.SubsCategoryRoute
import app.cyanskip.app.ui.platform.CsBackHandler
import app.cyanskip.app.ui.share.ListPlaceholder
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.util.ToastUtils
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubsCategoryPage(
    route: SubsCategoryRoute,
) {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { SubsCategoryViewModel(route) }
    val busy by vm.busyFlow.collectAsStateWithLifecycle()
    val selection = rememberMultiSelectionState<Int>()
    CsBackHandler(selection.active) { selection.clear() }
    CsSubscriptionPageContent(vm.uiState, mainVm.navigator::pop) { state ->
        val subs = state.subscription
        val scroll = rememberListScrollState()
        val keys =
            remember(state.categories) { state.categories.mapTo(mutableSetOf()) { it.category.key } }
        val selected = selection.selectedKeys intersect keys
        LaunchedEffect(keys) { selection.retain(keys) }
        fun setSelected(setting: CategorySetting) {
            val selectedKeys = selected
            vm.scope.launchUi {
                vm.runAction {
                    vm.setSettings(state, selectedKeys, setting)
                    ToastUtils.show(getString(Res.string.update_success))
                }
            }
        }
        CsScaffold(
            modifier = Modifier.nestedScroll(scroll.scrollBehavior.nestedScrollConnection),
            topBar = {
                CsMultiSelectionTopAppBar(
                    selectedMode = selection.active,
                    selectedCount = selected.size,
                    onExitSelection = selection::clear,
                    onNavigateBack = mainVm.navigator::pop,
                    onTitleClick = scroll::resetScroll,
                    scrollBehavior = scroll.scrollBehavior,
                    title = {
                        CsTwoLineText(
                            title = subs.name,
                            subtitle = stringResource(Res.string.rule_categories),
                        )
                    },
                    actions = { selectedMode ->
                        if (selectedMode) {
                            CsMultiSelectionActions(selection, keys, enabled = !busy) { dismiss ->
                                CsBatchActionMenuItem(
                                    stringResource(Res.string.category_follow_subscription),
                                    dismiss,
                                    { setSelected(CategorySetting.FollowSubscription) })
                                CsBatchActionMenuItem(
                                    stringResource(Res.string.action_turn_on), dismiss,
                                    { setSelected(CategorySetting.Enabled) })
                                CsBatchActionMenuItem(
                                    stringResource(Res.string.action_close), dismiss,
                                    { setSelected(CategorySetting.Disabled) })
                                CsBatchActionMenuItem(
                                    stringResource(Res.string.category_use_group_default), dismiss,
                                    { setSelected(CategorySetting.GroupDefault) })
                                if (subs.isLocal) {
                                    CsBatchActionMenuItem(
                                        stringResource(Res.string.action_delete),
                                        dismiss,
                                        {
                                            val selectedKeys = selected
                                            vm.scope.launchUi {
                                                vm.runAction {
                                                    if (!mainVm.dialogRequests.confirm(
                                                            title = getString(Res.string.category_delete),
                                                            text = getString(
                                                                Res.string.categories_delete_confirmation,
                                                                selectedKeys.size.toString()
                                                            ),
                                                            error = true,
                                                        )
                                                    ) return@runAction
                                                    vm.deleteCategories(state, selectedKeys)
                                                    selection.removeDeleted(selectedKeys)
                                                    ToastUtils.show(getString(Res.string.delete_success))
                                                }
                                            }
                                        })
                                }
                            }
                        } else {
                            CsIconButton(
                                imageVector = CsIcons.Info,
                                contentDescription = stringResource(Res.string.category_help),
                                onClick = {
                                    vm.scope.launchUi {
                                        mainVm.dialogRequests.showMessage(
                                            title = getString(Res.string.category_help),
                                            text = getString(Res.string.category_help_description),
                                        )
                                    }
                                })
                        }
                    },
                )
            },
            floatingActionButton = {
                if (subs.isLocal) {
                    CsAnimatedFloatingActionButton(
                        visible = !selection.active,
                        onClick = { mainVm.navigator.navigate(CategoryEditorRoute(subs.id)) },
                        imageVector = CsIcons.Add,
                        contentDescription = stringResource(Res.string.category_add),
                    )
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.scaffoldPadding(padding),
                state = scroll.listState,
            ) {
                items(state.categories, key = { it.category.key }) { summary ->
                    CategoryItemCard(
                        summary = summary,
                        selectedMode = selection.active,
                        selected = summary.category.key in selected,
                        selectionEnabled = !busy,
                        onLongClick = { selection.select(summary.category.key) },
                        onSelect = { selection.toggle(summary.category.key) },
                        onOpen = {
                            mainVm.navigator.navigate(
                                SubsCategoryGroupRoute(
                                    subs.id,
                                    summary.category.key
                                )
                            )
                        },
                    )
                }
                item(ListPlaceholder.KEY, ListPlaceholder.TYPE) {
                    if (state.categories.isEmpty()) CsEmptyState(stringResource(Res.string.categories_empty))
                    CsPageBottomSpace()
                }
            }
        }

    }
}

@Composable
private fun CategoryItemCard(
    summary: CategorySummary,
    selectedMode: Boolean,
    selected: Boolean,
    selectionEnabled: Boolean,
    onLongClick: () -> Unit,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
) {
    CsRuleListItem(
        onClick = onOpen,
        selectedMode = selectedMode,
        selected = selected,
        selectionEnabled = selectionEnabled,
        onLongClick = onLongClick,
        onSelect = onSelect,
        trailing = {
            CsTooltipIconButtonBox(contentDescription = summary.setting.label) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CsIcon(
                        imageVector = summary.setting.icon,
                        modifier = Modifier.size(20.dp),
                        contentDescription = summary.setting.label,
                        tint = if (summary.setting == CategorySetting.FollowSubscription)
                            MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }) {
        Text(
            summary.category.name, style = MaterialTheme.typography.bodyLarge,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        summary.category.desc?.trim()
            ?.takeIf { it.isNotEmpty() && it != summary.category.name.trim() }?.let { description ->
                Text(
                    description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
        CsRuleStats(
            CsRuleStatsData(
                apps = summary.appCount, appGroups = summary.groupCount,
                enabledAppGroups = summary.enabledGroupCount, showZeroCounts = true
            )
        )
    }
}
