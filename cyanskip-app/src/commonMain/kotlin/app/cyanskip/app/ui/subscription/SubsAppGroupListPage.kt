package app.cyanskip.app.ui.subscription

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.rule_focus_missing
import app.cyanskip.app.resources.action_close
import app.cyanskip.app.resources.action_copy
import app.cyanskip.app.resources.action_delete
import app.cyanskip.app.resources.action_enable
import app.cyanskip.app.resources.action_notice
import app.cyanskip.app.resources.app_rule_restriction_remove
import app.cyanskip.app.resources.app_rule_whitelist_remove_confirm
import app.cyanskip.app.resources.rule_add
import app.cyanskip.app.resources.rule_batch_setting_confirmation_prefix
import app.cyanskip.app.resources.rule_clear_custom_settings
import app.cyanskip.app.resources.rule_clear_custom_settings_description
import app.cyanskip.app.resources.rule_custom_settings_description
import app.cyanskip.app.resources.rule_delete
import app.cyanskip.app.resources.rule_enable_in_app
import app.cyanskip.app.resources.rule_groups_delete_confirmation
import app.cyanskip.app.resources.rule_groups_deleted_count
import app.cyanskip.app.resources.rules_empty
import app.cyanskip.app.resources.rules_no_filter_matches
import app.cyanskip.app.resources.selected_rules_changed
import app.cyanskip.app.resources.whitelist_remove
import app.cyanskip.app.rule.RuleConfigIndex
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.rule.RuleSwitchTarget
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsAnimatedFloatingActionButton
import app.cyanskip.app.ui.component.CsAppNameText
import app.cyanskip.app.ui.component.CsAppRuleRestrictionCard
import app.cyanskip.app.ui.component.CsBatchActionMenuItem
import app.cyanskip.app.ui.component.CsEmptyState
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsMultiSelectionActions
import app.cyanskip.app.ui.component.CsMultiSelectionTopAppBar
import app.cyanskip.app.ui.component.CsPageBottomSpace
import app.cyanskip.app.ui.component.CsRuleBatchMenuItems
import app.cyanskip.app.ui.component.CsRuleEnableControl
import app.cyanskip.app.ui.component.CsRuleGroupCard
import app.cyanskip.app.ui.component.CsRuleListItem
import app.cyanskip.app.ui.component.CsRuleSettingsContent
import app.cyanskip.app.ui.component.CsRuleSettingsSheet
import app.cyanskip.app.ui.component.CsScaffold
import app.cyanskip.app.ui.component.CsSubscriptionPageContent
import app.cyanskip.app.ui.component.CsTwoLineText
import app.cyanskip.app.ui.component.RulePropertyText
import app.cyanskip.app.ui.component.animateListItem
import app.cyanskip.app.ui.component.rememberListScrollState
import app.cyanskip.app.ui.component.rememberMultiSelectionState
import app.cyanskip.app.ui.component.rememberRuleControlEnvironment
import app.cyanskip.app.ui.component.rememberRuleListFocus
import app.cyanskip.app.ui.navigation.SubsAppGroupListRoute
import app.cyanskip.app.ui.navigation.UpsertRuleGroupRoute
import app.cyanskip.app.ui.platform.CsBackHandler
import app.cyanskip.app.ui.share.ListPlaceholder
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.app.util.copyText
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubsAppGroupListPage(
    route: SubsAppGroupListRoute,
) {
    val mainVm = MainViewModel.requireCurrent()
    val subsItemId = route.subsItemId
    val appId = route.appId
    val focusGroupKey = route.focusGroupKey

    val vm = viewModel { SubsAppGroupListViewModel(route) }
    val scope = vm.scope
    val environment = rememberRuleControlEnvironment()
    val whitelist by SettingsRepository.blockMatchAppList.collectAsStateWithLifecycle()
    val restrictionSettings by SettingsRepository.settings.collectAsStateWithLifecycle()
    val whitelisted = appId in whitelist
    val showAppRestriction = whitelisted
    val showAppSetting by vm.showAppSetting.collectAsStateWithLifecycle()
    val batchBusy by vm.batchBusyFlow.collectAsStateWithLifecycle()
    CsSubscriptionPageContent(vm.uiState, mainVm.navigator::pop) { state ->
        val subs = state.subscription
        val configIndex = remember(state.configs) { RuleConfigIndex(state.configs) }
        val app = state.app
        val appExists = subs.apps.any { it.id == appId }
        val appControl = environment.app(subs.id, appId, state.configs, configIndex)
        val setApp: (RuleSetting) -> Unit = { setting ->
            val request = vm.prepareAppSwitch(state)
            scope.launchUi {
                vm.applySwitches(request, setting).failureMessage()
                    ?.let { ToastUtils.show(it) }
            }
        }
        if (showAppSetting && appExists) {
            CsRuleSettingsSheet(
                title = app.name ?: appId, subtitle = appControl.scope.label,
                onDismissRequest = { vm.setShowAppSetting(false) }) {
                CsRuleSettingsContent(
                    appControl,
                    setApp,
                    title = stringResource(Res.string.rule_enable_in_app)
                )
            }
        }
        val controls = remember(state, environment) {
            app.groups.associate { group ->
                group.key to environment.resolve(subs, group, appId, state.configs, configIndex)
            }
        }
        val groups = app.groups
        val editable = subsItemId < 0
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
                            null -> getString(Res.string.rule_clear_custom_settings)
                        }
                        if (!mainVm.dialogRequests.confirm(
                                title = getString(Res.string.action_notice),
                                text = getString(
                                    Res.string.rule_batch_setting_confirmation_prefix,
                                    keysToUpdate.size.toString(),
                                    action
                                ) +
                                        if (enabled == null) getString(Res.string.rule_clear_custom_settings_description)
                                        else getString(Res.string.rule_custom_settings_description),
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
        val itemKeys = remember(groups, appExists, showAppRestriction) {
            buildList {
                if (showAppRestriction) add("app-restrictions")
                if (appExists) add("app-switch")
                addAll(groups.map { it.key })
            }
        }
        val focus = rememberRuleListFocus(
            requestKey = focusGroupKey,
            onTargetMissing = { ToastUtils.show(getString(Res.string.rule_focus_missing)) },
            scrollState = pageScrollState,
            itemKeys = itemKeys,
            targetExists = app.groups.any { it.key == focusGroupKey },
        )
        pageScrollState.ResetOnChange(app.groups.isEmpty(), enabled = !focus.pending)
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
                            subtitle = appId,
                            showApp = true,
                            appContent = { id, name -> CsAppNameText(id, name) },
                            appFallbackName = app.name,
                        )
                    },
                    actions = { selectedMode ->
                        if (selectedMode) {
                            CsMultiSelectionActions(
                                selectionState = selectionState,
                                keys = selectableKeys,
                                enabled = !batchBusy,
                            ) { dismiss ->
                                CsBatchActionMenuItem(
                                    text = stringResource(Res.string.action_copy),
                                    onDismiss = dismiss,
                                    onClick = {
                                        val keysToCopy = selectedKeys
                                        scope.launchUi {
                                            vm.runBatchAction {
                                                copyText(vm.buildSelectedGroupsText(keysToCopy))
                                            }
                                        }
                                    },
                                )
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
                        } else {
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
                                    appId = appId
                                )
                            )
                        },
                        contentDescription = stringResource(Res.string.rule_add),
                        imageVector = CsIcons.Add,
                    )
                }
            }) { contentPadding ->
            LazyColumn(
                modifier = Modifier.scaffoldPadding(contentPadding).fillMaxSize(),
                state = listState,
            ) {
                if (showAppRestriction) {
                    item("app-restrictions") {
                        CsAppRuleRestrictionCard(
                            whitelisted = whitelisted,


                            onRemoveWhitelist = {
                                scope.launchUi {
                                    if (mainVm.dialogRequests.confirm(
                                            title = getString(Res.string.whitelist_remove),
                                            text = getString(Res.string.app_rule_whitelist_remove_confirm),
                                            confirmText = getString(Res.string.app_rule_restriction_remove),
                                            dismissOnRequest = true,
                                        )
                                    ) vm.removeFromWhitelist()
                                }
                            },

                        )
                    }
                }
                if (appExists) {
                    item("app-switch") {
                        CsRuleListItem(
                            onClick = { if (!isSelectedMode) vm.setShowAppSetting(true) },
                            trailing = {
                                if (!isSelectedMode) CsRuleEnableControl(
                                    appControl, setApp, modifier = it,
                                    identity = RuleSwitchTarget.App(
                                        subsItemId,
                                        appId
                                    )
                                )
                            }) {
                            Text(
                                stringResource(Res.string.rule_enable_in_app),
                                style = MaterialTheme.typography.titleSmall
                            )
                            val restriction =
                                RulePropertyText.restrictionSummary(appControl)
                            if (restriction != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    restriction, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                items(groups, { it.key }) { group ->
                    CsRuleGroupCard(
                        modifier = Modifier.animateListItem(),
                        subs = subs,
                        appId = appId,
                        group = group,
                        control = controls.getValue(group.key),
                        onOpen = {
                            mainVm.showRuleGroup(subs.id, appId, group, appId)
                        },
                        onSettingChange = { setting ->
                            val request = vm.prepareSwitches(state, setOf(group.key))
                            scope.launchUi {
                                vm.applySwitches(request, setting)
                                    .failureMessage()?.let { ToastUtils.show(it) }
                            }
                        },
                        highlighted = !isSelectedMode && focus.highlightedKey == group.key,
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
                    if (groups.isEmpty()) {
                        CsEmptyState(
                            text = if (app.groups.isEmpty()) stringResource(Res.string.rules_empty) else stringResource(
                                Res.string.rules_no_filter_matches
                            )
                        )
                    } else {
                        CsPageBottomSpace()
                    }
                }
            }
        }
    }
}
