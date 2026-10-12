package app.cyanskip.app.ui.subscription

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.rule_focus_missing
import app.cyanskip.app.resources.action_close
import app.cyanskip.app.resources.action_copy
import app.cyanskip.app.resources.action_enable
import app.cyanskip.app.resources.app_rule_restriction_remove
import app.cyanskip.app.resources.app_rule_whitelist_remove_confirm
import app.cyanskip.app.resources.app_rules_batch_setting_confirmation
import app.cyanskip.app.resources.data_empty
import app.cyanskip.app.resources.data_empty_filter_hint
import app.cyanskip.app.resources.filter_title
import app.cyanskip.app.resources.not_enabled
import app.cyanskip.app.resources.rule_add
import app.cyanskip.app.resources.setting_follow_default
import app.cyanskip.app.resources.sort_title
import app.cyanskip.app.resources.whitelist_remove
import app.cyanskip.app.rule.RuleConfigIndex
import app.cyanskip.app.rule.RuleGroupTarget
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.rule.toRuleGroupTarget
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsAnimatedFloatingActionButton
import app.cyanskip.app.ui.component.CsAppNameText
import app.cyanskip.app.ui.component.CsAppRuleRestrictionCard
import app.cyanskip.app.ui.component.CsBatchActionMenuItem
import app.cyanskip.app.ui.component.CsEmptyState
import app.cyanskip.app.ui.component.CsFilterIconButton
import app.cyanskip.app.ui.component.CsIconButton
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsMenuGroupCard
import app.cyanskip.app.ui.component.CsMenuItemCheckbox
import app.cyanskip.app.ui.component.CsMenuItemRadioButton
import app.cyanskip.app.ui.component.CsMultiSelectionActions
import app.cyanskip.app.ui.component.CsMultiSelectionTopAppBar
import app.cyanskip.app.ui.component.CsPageBottomSpace
import app.cyanskip.app.ui.component.CsRuleBatchMenuItems
import app.cyanskip.app.ui.component.CsRuleGroupCard
import app.cyanskip.app.ui.component.CsRuleListHeader
import app.cyanskip.app.ui.component.CsScaffold
import app.cyanskip.app.ui.component.animateListItem
import app.cyanskip.app.ui.component.rememberListScrollState
import app.cyanskip.app.ui.component.rememberMultiSelectionState
import app.cyanskip.app.ui.component.rememberRuleControlEnvironment
import app.cyanskip.app.ui.component.rememberRuleListFocus
import app.cyanskip.app.ui.navigation.ActionLogRoute
import app.cyanskip.app.ui.navigation.AppConfigRoute
import app.cyanskip.app.ui.navigation.SubsAppGroupListRoute
import app.cyanskip.app.ui.navigation.SubsGlobalGroupListRoute
import app.cyanskip.app.ui.navigation.UpsertRuleGroupRoute
import app.cyanskip.app.ui.option.RuleSortOption
import app.cyanskip.app.ui.option.findOption
import app.cyanskip.app.ui.platform.CsBackHandler
import app.cyanskip.app.ui.share.ListPlaceholder
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.ui.text.subscriptionMessageResource
import app.cyanskip.app.util.SortUtils
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.app.util.copyText
import app.cyanskip.db.LOCAL_SUBS_ID
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppConfigPage(
    route: AppConfigRoute,
) {
    val mainVm = MainViewModel.requireCurrent()
    val appId = route.appId
    val focusLog = route.focusLog
    val vm = viewModel { AppConfigViewModel(route) }
    val scope = vm.scope
    val batchBusy by vm.batchBusyFlow.collectAsStateWithLifecycle()

    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val loadableState by vm.uiState.collectAsStateWithLifecycle()
    val state = loadableState.value
    val configIndex = remember(state?.configs) { state?.configs?.let(::RuleConfigIndex) }
    val firstLoading = loadableState is Loadable.Loading
    val loadError = (loadableState as? Loadable.Failure)?.cause
    val environment = rememberRuleControlEnvironment()
    val whitelist by SettingsRepository.blockMatchAppList.collectAsStateWithLifecycle()
    val whitelisted = appId in whitelist
    val showAppRestriction = whitelisted
    val revealDisabledRules by vm.revealDisabledRules.collectAsStateWithLifecycle()
    val showDisabledRules = store.showDisabledRule || revealDisabledRules
    val controls = remember(state, environment) {
        state?.subsPairs.orEmpty().flatMap { (entry, groups) ->
            groups.map { group ->
                group.toRuleGroupTarget(entry.subsItem.id, appId) to
                        environment.resolve(
                            entry.subscription, group, appId, checkNotNull(state).configs,
                            checkNotNull(configIndex)
                        )
            }
        }.toMap()
    }
    val subsPairs = remember(state, showDisabledRules, controls, store.appRuleSort) {
        state?.subsPairs.orEmpty().mapNotNull { (entry, groups) ->
            val visible = groups.filter { group ->
                val control = controls.getValue(group.toRuleGroupTarget(entry.subsItem.id, appId))
                // App-wide whitelist membership does not turn the rule's own switch off.
                showDisabledRules || (control.configuredEnabled && control.canEnable && control.restrictions.isEmpty())
            }
            val sorted = when (RuleSortOption.objects.findOption(store.appRuleSort)) {
                RuleSortOption.ByDefault -> visible
                RuleSortOption.ByRuleName -> visible.sortedWith { a, b ->
                    SortUtils.collator.compare(
                        a.name,
                        b.name
                    )
                }

                RuleSortOption.ByActionTime -> visible.sortedByDescending { group ->
                    state?.latestLogs?.find {
                        it.subsId == entry.subsItem.id && it.groupType == group.groupType && it.groupKey == group.key
                    }?.id ?: 0
                }
            }
            (entry to sorted).takeIf { sorted.isNotEmpty() }
        }
    }
    val groupSize = subsPairs.sumOf { it.second.size }

    val selectableTargets = remember(subsPairs, appId, controls) {
        subsPairs.flatMap { (entry, groups) ->
            groups.map { group -> group.toRuleGroupTarget(entry.subsItem.id, appId) }
        }.filterTo(mutableSetOf()) { controls.getValue(it).canEnable }
    }
    val selectionState = rememberMultiSelectionState<RuleGroupTarget>()
    val selectedDataSet = selectionState.selectedKeys intersect selectableTargets
    val isSelectedMode = selectionState.active
    LaunchedEffect(selectableTargets, loadableState) {
        if (loadableState is Loadable.Ready) selectionState.retain(selectableTargets)
    }
    CsBackHandler(isSelectedMode) {
        selectionState.clear()
    }

    val updateSelected: (RuleSetting) -> Unit = { setting ->
        val enabled = setting.value
        if (selectedDataSet.isNotEmpty() && state != null) {
            val request = vm.prepareSwitches(state, selectedDataSet)
            scope.launchUi {
                vm.runBatchAction {
                    val action = when (enabled) {
                        false -> getString(Res.string.action_close)
                        true -> getString(Res.string.action_enable)
                        null -> getString(Res.string.setting_follow_default)
                    }
                    vm.applySwitches(
                        request, setting,
                        confirmation = getString(
                            Res.string.app_rules_batch_setting_confirmation,
                            selectedDataSet.size.toString(),
                            action
                        ),
                    )?.let { ToastUtils.show(it.description()) }
                }
            }
        }
    }
    val pageScrollState = rememberListScrollState()
    val scrollBehavior = pageScrollState.scrollBehavior
    val listState = pageScrollState.listState
    val focusKey =
        remember(focusLog) { focusLog?.let { Triple(it.subsId, it.groupType, it.groupKey) } }
    val itemKeys = remember(subsPairs, showAppRestriction) {
        buildList {
            if (showAppRestriction) add("app-restrictions")
            subsPairs.forEach { (entry, groups) ->
                add(entry.subsItem.id)
                groups.forEach { add(Triple(entry.subsItem.id, it.groupType, it.key)) }
            }
        }
    }
    val focus = rememberRuleListFocus(
        requestKey = focusKey,
        onTargetMissing = { ToastUtils.show(getString(Res.string.rule_focus_missing)) },
        scrollState = pageScrollState,
        itemKeys = itemKeys,
        ready = loadableState is Loadable.Ready,
        targetExists = state?.subsPairs.orEmpty().any { (entry, groups) ->
            groups.any { Triple(entry.subsItem.id, it.groupType, it.key) == focusKey }
        },
        stickyHeaderKeys = remember(subsPairs) { subsPairs.mapTo(mutableSetOf()) { it.first.subsItem.id } },
        onRevealTarget = { vm.setRevealDisabledRules(true) },
    )
    pageScrollState.ResetOnChange(
        groupSize > 0,
        firstLoading,
        enabled = !focus.pending,
    )

    CsScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CsMultiSelectionTopAppBar(
                selectedMode = isSelectedMode,
                selectedCount = selectedDataSet.size,
                onExitSelection = selectionState::clear,
                scrollBehavior = scrollBehavior,
                onNavigateBack = { mainVm.navigator.pop() },
                onTitleClick = pageScrollState::resetScroll,
                title = {
                    CsAppNameText(appId = appId)
                },
                actions = { selectedMode ->
                    if (selectedMode) {
                        CsMultiSelectionActions(
                            selectionState = selectionState,
                            keys = selectableTargets,
                            enabled = !batchBusy && loadableState is Loadable.Ready,
                        ) { dismiss ->
                            CsBatchActionMenuItem(
                                text = stringResource(Res.string.action_copy),
                                enabled = selectedDataSet.any { it is RuleGroupTarget.App },
                                onDismiss = dismiss,
                                onClick = {
                                    val targets =
                                        selectedDataSet.filterIsInstance<RuleGroupTarget.App>()
                                            .toSet()
                                    scope.launchUi {
                                        vm.runBatchAction {
                                            copyText(vm.buildSelectedGroupsText(targets))
                                        }
                                    }
                                },
                            )
                            CsRuleBatchMenuItems(
                                enabled = true,
                                onDismiss = dismiss,
                                onUpdate = updateSelected,
                            )
                        }
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            CsFilterIconButton(
                                filtered = state != null && groupSize < state.subsPairs.sumOf { it.second.size },
                                onClick = { expanded = true },
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                CsMenuGroupCard(
                                    inTop = true,
                                    title = stringResource(Res.string.sort_title)
                                ) {
                                    val handleItem: (RuleSortOption) -> Unit = vm::setRuleSortType
                                    RuleSortOption.objects.forEach { option ->
                                        CsMenuItemRadioButton(
                                            text = option.label,
                                            selected = RuleSortOption.objects.findOption(store.appRuleSort) == option,
                                            onClick = { handleItem(option) },
                                        )
                                    }
                                }
                                CsMenuGroupCard(title = stringResource(Res.string.filter_title)) {
                                    CsMenuItemCheckbox(
                                        text = stringResource(Res.string.not_enabled),
                                        checked = showDisabledRules,
                                        onClick = {
                                            vm.setShowDisabledRules(!showDisabledRules)
                                            vm.setRevealDisabledRules(false)
                                        },
                                    )
                                }
                            }
                        }
                        CsIconButton(
                            imageVector = CsIcons.History,
                            onClick = {
                                mainVm.navigator.navigate(ActionLogRoute(appId = appId))
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            CsAnimatedFloatingActionButton(
                visible = !isSelectedMode,
                onClick = {
                    mainVm.navigator.navigate(
                        UpsertRuleGroupRoute(
                            subsId = LOCAL_SUBS_ID,
                            groupKey = null,
                            appId = appId
                        )
                    )
                },
                imageVector = CsIcons.Add,
                contentDescription = stringResource(Res.string.rule_add)
            )
        },
    ) { contentPadding ->
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
            subsPairs.forEach { (entry, groups) ->
                val subsId = entry.subsItem.id
                stickyHeader(entry.subsItem.id) {
                    CsRuleListHeader(
                        onClick = {
                            mainVm.navigator.navigate(
                                if (entry.subscription.apps.any { it.id == appId })
                                    SubsAppGroupListRoute(
                                        subsId,
                                        appId
                                    ) else SubsGlobalGroupListRoute(
                                    subsId
                                )
                            )
                        },
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = entry.subscription.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                items(groups, { Triple(subsId, it.groupType, it.key) }) { group ->
                    val isSelected = selectedDataSet.any {
                        it.subsId == subsId && it.groupType == group.groupType && it.groupKey == group.key
                    }
                    val onLongClick = {
                        if (!batchBusy) {
                            selectionState.select(
                                group.toRuleGroupTarget(
                                    subsId = subsId,
                                    appId = appId,
                                ),
                            )
                        }
                    }
                    val onSelectedChange = {
                        selectionState.toggle(
                            group.toRuleGroupTarget(
                                subsId = subsId,
                                appId = appId,
                            )
                        )
                    }
                    CsRuleGroupCard(
                        modifier = Modifier.animateListItem(),
                        subs = entry.subscription,
                        appId = appId,
                        group = group,
                        control = controls.getValue(group.toRuleGroupTarget(subsId, appId)),
                        onOpen = {
                            mainVm.showRuleGroup(subsId, appId, group, appId)
                        },
                        onSettingChange = { setting ->
                            val request = vm.prepareSwitches(
                                checkNotNull(state),
                                setOf(group.toRuleGroupTarget(subsId, appId))
                            )
                            scope.launchUi {
                                vm.applySwitches(request, setting)
                                    ?.failureMessage()?.let { ToastUtils.show(it) }
                            }
                        },
                        onLongClick = onLongClick,
                        isSelectedMode = isSelectedMode,
                        selectionEnabled = !batchBusy,
                        isSelected = isSelected,
                        onSelectedChange = onSelectedChange,
                        highlighted = !isSelectedMode && focus.highlightedKey == Triple(
                            subsId,
                            group.groupType,
                            group.key
                        ),
                    )
                }
            }
            item(ListPlaceholder.KEY, ListPlaceholder.TYPE) {
                if (groupSize == 0 && !firstLoading) {
                    CsEmptyState(
                        text = if (loadError != null) {
                            loadError.subscriptionMessageResource()
                        } else if (showDisabledRules) {
                            stringResource(Res.string.data_empty)
                        } else {
                            stringResource(Res.string.data_empty_filter_hint)
                        }
                    )
                } else {
                    CsPageBottomSpace()
                }
            }
        }
    }
}
