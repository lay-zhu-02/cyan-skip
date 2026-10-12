package app.cyanskip.app.ui.subscription

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.rule_focus_missing
import app.cyanskip.app.resources.action_close
import app.cyanskip.app.resources.action_delete
import app.cyanskip.app.resources.action_enable
import app.cyanskip.app.resources.action_notice
import app.cyanskip.app.resources.global_rule_batch_setting_confirmation
import app.cyanskip.app.resources.global_rules
import app.cyanskip.app.resources.rule_add
import app.cyanskip.app.resources.rule_delete
import app.cyanskip.app.resources.rule_groups_delete_confirmation
import app.cyanskip.app.resources.rule_groups_deleted_count
import app.cyanskip.app.resources.rules_empty
import app.cyanskip.app.resources.selected_rules_changed
import app.cyanskip.app.resources.setting_follow_default
import app.cyanskip.app.rule.RuleConfigIndex
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsAnimatedFloatingActionButton
import app.cyanskip.app.ui.component.CsBatchActionMenuItem
import app.cyanskip.app.ui.component.CsEmptyState
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsMultiSelectionActions
import app.cyanskip.app.ui.component.CsMultiSelectionTopAppBar
import app.cyanskip.app.ui.component.CsPageBottomSpace
import app.cyanskip.app.ui.component.CsRuleBatchMenuItems
import app.cyanskip.app.ui.component.CsRuleGroupCard
import app.cyanskip.app.ui.component.CsScaffold
import app.cyanskip.app.ui.component.CsSubscriptionPageContent
import app.cyanskip.app.ui.component.CsTwoLineText
import app.cyanskip.app.ui.component.animateListItem
import app.cyanskip.app.ui.component.rememberListScrollState
import app.cyanskip.app.ui.component.rememberMultiSelectionState
import app.cyanskip.app.ui.component.rememberRuleControlEnvironment
import app.cyanskip.app.ui.component.rememberRuleListFocus
import app.cyanskip.app.ui.navigation.SubsGlobalGroupListRoute
import app.cyanskip.app.ui.navigation.UpsertRuleGroupRoute
import app.cyanskip.app.ui.platform.CsBackHandler
import app.cyanskip.app.ui.share.ListPlaceholder
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.util.ToastUtils
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubsGlobalGroupListPage(
    route: SubsGlobalGroupListRoute,
) {
    val mainVm = MainViewModel.requireCurrent()
    val subsItemId = route.subsItemId
    val focusGroupKey = route.focusGroupKey

    val vm = viewModel { SubsGlobalGroupListViewModel(route) }
    val scope = vm.scope
    val environment = rememberRuleControlEnvironment()
    val batchBusy by vm.batchBusyFlow.collectAsStateWithLifecycle()

    CsSubscriptionPageContent(vm.uiState, mainVm.navigator::pop) { state ->
        val subs = state.subscription
        val configIndex = remember(state.configs) { RuleConfigIndex(state.configs) }
        val editable = subsItemId < 0
        val controls = remember(state, environment) {
            subs.globalGroups.associate { group ->
                group.key to environment.resolve(subs, group, null, state.configs, configIndex)
            }
        }
        val globalGroups = subs.globalGroups

        val selectionState = rememberMultiSelectionState<Int>()
        val selectableKeys = remember(controls) { controls.filterValues { it.canEnable }.keys }
        val selectedKeys = selectionState.selectedKeys intersect selectableKeys
        val isSelectedMode = selectionState.active
        LaunchedEffect(selectableKeys) {
            selectionState.retain(selectableKeys)
        }
        CsBackHandler(isSelectedMode) {
            selectionState.clear()
        }

        val updateSelected: (RuleSetting) -> Unit = { setting ->
            val enabled = setting.value
            val keysToUpdate = selectedKeys
            if (keysToUpdate.isNotEmpty()) {
                val request = vm.prepareSwitches(state, keysToUpdate)
                scope.launchUi {
                    vm.runBatchAction {
                        val action = when (enabled) {
                            false -> getString(Res.string.action_close)
                            true -> getString(Res.string.action_enable)
                            null -> getString(Res.string.setting_follow_default)
                        }
                        if (!mainVm.dialogRequests.confirm(
                                title = getString(Res.string.action_notice),
                                text = getString(
                                    Res.string.global_rule_batch_setting_confirmation,
                                    keysToUpdate.size.toString(),
                                    action
                                ),
                            )
                        ) return@runBatchAction
                        ToastUtils.show(
                            vm.applySwitches(request, RuleSetting.from(enabled))
                                .description()
                        )
                    }
                }
            }
        }
        val pageScrollState = rememberListScrollState()
        val scrollBehavior = pageScrollState.scrollBehavior
        val listState = pageScrollState.listState
        val itemKeys = remember(globalGroups) { globalGroups.map { it.key } }
        val focus = rememberRuleListFocus(
            requestKey = focusGroupKey,
            onTargetMissing = { ToastUtils.show(getString(Res.string.rule_focus_missing)) },
            scrollState = pageScrollState,
            itemKeys = itemKeys,
            targetExists = subs.globalGroups.any { it.key == focusGroupKey },
        )
        pageScrollState.ResetOnChange(globalGroups.isEmpty(), enabled = !focus.pending)
        CsScaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CsMultiSelectionTopAppBar(
                    selectedMode = isSelectedMode,
                    selectedCount = selectedKeys.size,
                    onExitSelection = selectionState::clear,
                    scrollBehavior = scrollBehavior,
                    onNavigateBack = { mainVm.navigator.pop() },
                    onTitleClick = pageScrollState::resetScroll,
                    title = {
                        CsTwoLineText(
                            title = subs.name,
                            subtitle = stringResource(Res.string.global_rules),
                        )
                    },
                    actions = { selectedMode ->
                        if (selectedMode) {
                            CsMultiSelectionActions(
                                selectionState = selectionState,
                                keys = selectableKeys,
                                enabled = !batchBusy,
                            ) { dismiss ->
                                CsRuleBatchMenuItems(
                                    enabled = !batchBusy,
                                    onDismiss = dismiss,
                                    onUpdate = updateSelected,
                                )
                                if (editable) {
                                    CsBatchActionMenuItem(
                                        text = stringResource(Res.string.action_delete),
                                        onDismiss = dismiss,
                                        onClick = {
                                            val keysToDelete = selectedKeys
                                            scope.launchUi {
                                                vm.runBatchAction {
                                                    if (!mainVm.dialogRequests.confirm(
                                                            title = getString(Res.string.rule_delete),
                                                            text = getString(
                                                                Res.string.rule_groups_delete_confirmation,
                                                                keysToDelete.size.toString()
                                                            ),
                                                            error = true,
                                                        )
                                                    ) return@runBatchAction
                                                    val deletedSize =
                                                        vm.deleteSelectedGroups(keysToDelete)
                                                    selectionState.removeDeleted(keysToDelete)
                                                    ToastUtils.show(
                                                        if (deletedSize > 0) getString(
                                                            Res.string.rule_groups_deleted_count,
                                                            deletedSize.toString()
                                                        ) else getString(Res.string.selected_rules_changed)
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                if (editable) {
                    CsAnimatedFloatingActionButton(
                        visible = !isSelectedMode,
                        onClick = {
                            mainVm.navigator.navigate(
                                UpsertRuleGroupRoute(
                                    subsId = subsItemId,
                                    groupKey = null,
                                    appId = null,
                                )
                            )
                        },
                        imageVector = CsIcons.Add,
                        contentDescription = stringResource(Res.string.rule_add)
                    )
                }
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.scaffoldPadding(paddingValues).fillMaxSize(),
                state = listState,
            ) {
                items(globalGroups, { g -> g.key }) { group ->
                    CsRuleGroupCard(
                        modifier = Modifier.animateListItem(),
                        subs = subs,
                        appId = null,
                        group = group,
                        highlighted = !isSelectedMode && focus.highlightedKey == group.key,
                        control = controls.getValue(group.key),
                        onOpen = {
                            mainVm.showRuleGroup(subs.id, null, group, null)
                        },
                        onSettingChange = { setting ->
                            val request = vm.prepareSwitches(state, setOf(group.key))
                            scope.launchUi {
                                vm.applySwitches(request, setting)
                                    .failureMessage()?.let { ToastUtils.show(it) }
                            }
                        },
                        isSelectedMode = isSelectedMode,
                        selectionEnabled = !batchBusy,
                        isSelected = group.key in selectedKeys,
                        onLongClick = {
                            if (!batchBusy) {
                                selectionState.select(group.key)
                            }
                        },
                        onSelectedChange = {
                            selectionState.toggle(group.key)
                        }
                    )
                }
                item(ListPlaceholder.KEY, ListPlaceholder.TYPE) {
                    if (globalGroups.isEmpty()) {
                        CsEmptyState(
                            text = stringResource(Res.string.rules_empty)
                        )
                    } else {
                        CsPageBottomSpace()
                    }
                }
            }
        }
    }
}
