package app.cyanskip.app.ui.subscription

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_close
import app.cyanskip.app.resources.action_turn_on
import app.cyanskip.app.resources.app_sort_filter
import app.cyanskip.app.resources.app_view_all_rules
import app.cyanskip.app.resources.category_all_scope
import app.cyanskip.app.resources.category_clear_settings_confirmation
import app.cyanskip.app.resources.category_delete
import app.cyanskip.app.resources.category_delete_confirmation
import app.cyanskip.app.resources.category_groups_empty
import app.cyanskip.app.resources.category_scope_includes_hidden
import app.cyanskip.app.resources.category_scope_selected_only
import app.cyanskip.app.resources.delete_success
import app.cyanskip.app.resources.more_actions
import app.cyanskip.app.resources.rule_clear_custom_settings
import app.cyanskip.app.resources.rule_groups_no_filter_matches
import app.cyanskip.app.resources.rule_switch_expected_changed_count
import app.cyanskip.app.resources.rule_switch_expected_counts
import app.cyanskip.app.resources.rule_switch_restricted_count_suffix
import app.cyanskip.app.resources.selection_scope
import app.cyanskip.app.resources.settings_reset_default
import app.cyanskip.app.resources.update_success
import app.cyanskip.app.rule.RuleConfigIndex
import app.cyanskip.app.rule.RuleGroupTarget
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsAppFilterContent
import app.cyanskip.app.ui.component.CsAppNameText
import app.cyanskip.app.ui.component.CsBatchActionMenuItem
import app.cyanskip.app.ui.component.CsEmptyState
import app.cyanskip.app.ui.component.CsFilterIconButton
import app.cyanskip.app.ui.component.CsIconButton
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsMultiSelectionActions
import app.cyanskip.app.ui.component.CsMultiSelectionTopAppBar
import app.cyanskip.app.ui.component.CsPageBottomSpace
import app.cyanskip.app.ui.component.CsRuleGroupCard
import app.cyanskip.app.ui.component.CsRuleListHeader
import app.cyanskip.app.ui.component.CsScaffold
import app.cyanskip.app.ui.component.CsSubscriptionPageContent
import app.cyanskip.app.ui.component.CsTwoLineText
import app.cyanskip.app.ui.component.rememberListScrollState
import app.cyanskip.app.ui.component.rememberMultiSelectionState
import app.cyanskip.app.ui.component.rememberRuleControlEnvironment
import app.cyanskip.app.ui.home.rememberVisitOrder
import app.cyanskip.app.ui.navigation.CategoryEditorRoute
import app.cyanskip.app.ui.navigation.SubsAppGroupListRoute
import app.cyanskip.app.ui.navigation.SubsCategoryGroupRoute
import app.cyanskip.app.ui.option.AppSortOption
import app.cyanskip.app.ui.option.findOption
import app.cyanskip.app.ui.platform.CsBackHandler
import app.cyanskip.app.ui.share.DeletionTarget
import app.cyanskip.app.ui.share.ListPlaceholder
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.ToastUtils
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubsCategoryGroupPage(
    route: SubsCategoryGroupRoute,
) {
    val mainVm = MainViewModel.requireCurrent()
    val environment = rememberRuleControlEnvironment()
    val vm = viewModel { SubsCategoryGroupViewModel(route) }
    val busy by vm.busyFlow.collectAsStateWithLifecycle()
    val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
    val appMap = rememberRuleControlEnvironment().apps
    val visitOrder = rememberVisitOrder()
    val blockApps by SettingsRepository.blockMatchAppList.collectAsStateWithLifecycle()
    val showActions by vm.showActions.collectAsStateWithLifecycle()
    val showFilter by vm.showFilter.collectAsStateWithLifecycle()
    var retainForRemoval by remember { mutableStateOf(false) }
    CsSubscriptionPageContent(
        vm.uiState,
        onBack = mainVm.navigator::pop,
        retainContent = retainForRemoval
    ) { state ->
        val subs = state.subscription
        val configIndex = remember(state.configs) { RuleConfigIndex(state.configs) }
        val category = state.category
        val sortedApps =
            remember(state.apps, appMap, settings, state.appActionOrder, visitOrder, blockApps) {
                filterSubsApps(
                    apps = state.apps,
                    appMap = appMap,
                    settings = settings,
                    appActionOrderMap = state.appActionOrder,
                    appVisitOrderMap = visitOrder.value.orEmpty(),
                    blockSet = blockApps,
                    appGroupType = { it.subsCategoryGroupType },
                    sortType = { AppSortOption.objects.findOption(it.subsCategorySort) },
                    showBlockApps = { it.subsCategoryShowBlock },
                )
            }
        val controls = remember(state, environment) {
            state.apps.flatMap { app ->
                app.groups.map { group ->
                    (app.id to group.key) to environment.resolve(
                        subs,
                        group,
                        app.id,
                        state.configs,
                        configIndex
                    )
                }
            }.toMap()
        }
        val apps = sortedApps
        val visibleTargets = remember(apps, subs.id) {
            apps.flatMap { app -> app.groups.map { RuleGroupTarget.App(subs.id, app.id, it.key) } }
                .toSet()
        }
        val selectableTargets = remember(visibleTargets, controls) {
            visibleTargets.filterTo(mutableSetOf()) { controls.getValue(it.appId to it.groupKey).canEnable }
        }
        val selection = rememberMultiSelectionState<RuleGroupTarget.App>()
        val selected = selection.selectedKeys intersect selectableTargets
        LaunchedEffect(selectableTargets) { selection.retain(selectableTargets) }
        CsBackHandler(selection.active) { selection.clear() }
        val scroll = rememberListScrollState()
        fun updateGroups(
            targets: Set<RuleGroupTarget.App>,
            enabled: Boolean?,
            entireCategory: Boolean = false
        ) {
            val request = vm.prepareSwitches(state, targets)
            vm.scope.launchUi {
                vm.runAction {
                    if (enabled == null) {
                        val scopeLabel =
                            if (entireCategory) getString(Res.string.category_all_scope) else getString(
                                Res.string.selection_scope
                            )
                        val changedCount =
                            request.expected.values.count { it != RuleSetting.FollowDefault }
                        val unchangedCount = request.expected.size - changedCount
                        if (!mainVm.dialogRequests.confirm(
                                title = getString(Res.string.rule_clear_custom_settings),
                                text = getString(
                                    Res.string.category_clear_settings_confirmation,
                                    scopeLabel,
                                    targets.size.toString(),
                                    if (entireCategory) getString(Res.string.category_scope_includes_hidden) else getString(
                                        Res.string.category_scope_selected_only
                                    )
                                ) +
                                        "\n\n" + if (unchangedCount > 0)
                                    getString(
                                        Res.string.rule_switch_expected_counts,
                                        changedCount.toString(),
                                        unchangedCount.toString()
                                    )
                                else getString(
                                    Res.string.rule_switch_expected_changed_count,
                                    changedCount.toString()
                                ),
                            )
                        ) return@runAction
                    }
                    val result = vm.applySwitches(request, RuleSetting.from(enabled))
                    if (enabled == null) {
                        ToastUtils.show(
                            result.failureMessage()
                                ?: if (result.restricted > 0)
                                    getString(
                                        Res.string.rule_switch_restricted_count_suffix,
                                        result.restricted.toString()
                                    ).trimStart('；')
                                else getString(Res.string.update_success)
                        )
                    } else {
                        ToastUtils.show(result.description())
                    }
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
                    scrollBehavior = scroll.scrollBehavior,
                    onTitleClick = scroll::resetScroll,
                    title = { CsTwoLineText(title = subs.name, subtitle = category.name) },
                    actions = { selectedMode ->
                        if (selectedMode) {
                            CsMultiSelectionActions(
                                selection,
                                selectableTargets,
                                enabled = !busy
                            ) { dismiss ->
                                CsBatchActionMenuItem(
                                    stringResource(Res.string.action_turn_on),
                                    dismiss,
                                    { updateGroups(selected, true) },
                                    !busy
                                )
                                CsBatchActionMenuItem(
                                    stringResource(Res.string.action_close),
                                    dismiss,
                                    { updateGroups(selected, false) },
                                    !busy
                                )
                                CsBatchActionMenuItem(
                                    stringResource(Res.string.settings_reset_default), dismiss,
                                    {
                                        updateGroups(
                                            selected intersect state.overrideTargets,
                                            null
                                        )
                                    },
                                    !busy && selected.any { it in state.overrideTargets },
                                )
                            }
                        } else {
                            Box {
                                CsFilterIconButton(
                                    filtered = visibleTargets.size < state.targets.size,
                                    contentDescription = stringResource(Res.string.app_sort_filter),
                                    onClick = { vm.setShowFilter(true) })
                                DropdownMenu(
                                    expanded = showFilter,
                                    onDismissRequest = { vm.setShowFilter(false) }) {
                                    CsAppFilterContent(
                                        sort = settings.subsCategorySort,
                                        groupType = settings.subsCategoryGroupType,
                                        showBlockApps = settings.subsCategoryShowBlock,
                                        onSort = vm::setSortType,
                                        onAppGroup = vm::setAppGroupType,
                                        onToggleBlock = vm::toggleShowBlockApps,
                                    )
                                }
                            }
                            CsIconButton(
                                imageVector = CsIcons.MoreVert,
                                contentDescription = stringResource(Res.string.more_actions),
                                onClick = { vm.setShowActions(true) })
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.scaffoldPadding(padding),
                state = scroll.listState,
            ) {
                apps.forEach { app ->
                    stickyHeader("app:${app.id}") {
                        CsRuleListHeader(
                            enabled = !selection.active,
                            onClickLabel = stringResource(Res.string.app_view_all_rules),
                            onClick = {
                                mainVm.navigator.navigate(
                                    SubsAppGroupListRoute(
                                        subs.id,
                                        app.id
                                    )
                                )
                            },
                        ) {
                            CsAppNameText(
                                modifier = Modifier.weight(1f),
                                appId = app.id,
                                fallbackName = app.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    items(app.groups, key = { "group:${app.id}:${it.key}" }) { group ->
                        val target = RuleGroupTarget.App(subs.id, app.id, group.key)
                        CsRuleGroupCard(
                            subs = subs,
                            appId = app.id,
                            group = group,
                            control = controls.getValue(app.id to group.key),
                            hideCategoryPrefix = true,
                            onOpen = { mainVm.showRuleGroup(subs.id, app.id, group, app.id) },
                            onSettingChange = { setting ->
                                val request = vm.prepareSwitches(state, setOf(target))
                                vm.scope.launchUi {
                                    vm.applySwitches(request, setting)
                                        .failureMessage()?.let { ToastUtils.show(it) }
                                }
                            },
                            isSelectedMode = selection.active,
                            isSelected = target in selected,
                            selectionEnabled = !busy,
                            onLongClick = { if (!busy) selection.select(target) },
                            onSelectedChange = { if (!busy) selection.toggle(target) },
                        )
                    }
                }
                item(ListPlaceholder.KEY, ListPlaceholder.TYPE) {
                    if (apps.isEmpty()) {
                        CsEmptyState(
                            text = if (state.targets.isEmpty()) stringResource(Res.string.category_groups_empty) else stringResource(
                                Res.string.rule_groups_no_filter_matches
                            ),
                        )
                    }
                    CsPageBottomSpace()
                }
            }
        }
        if (showActions) {
            CsCategoryActionsSheet(
                category = category,
                setting = state.setting,
                overrideCount = state.overrideTargets.size,
                editable = subs.isLocal,
                busy = busy,
                onDismissRequest = { vm.setShowActions(false) },
                onSetting = { setting ->
                    vm.scope.launchUi {
                        vm.setCategorySetting(
                            subs,
                            setting,
                            state.setting
                        )
                    }
                },
                onClearOverrides = {
                    vm.setShowActions(false)
                    updateGroups(state.overrideTargets, null, entireCategory = true)
                },
                onEdit = {
                    vm.setShowActions(false); mainVm.navigator.navigate(
                    CategoryEditorRoute(
                        subs.id,
                        category.key
                    )
                )
                },
                onDelete = {
                    mainVm.confirmDelete(
                        Res.string.category_delete.getSync(),
                        Res.string.category_delete_confirmation.getSync(
                            category.name,
                            state.targets.size.toString()
                        ),
                        { setOf(DeletionTarget.Category(subs.id, category.key)) },
                        {
                            vm.setShowActions(false)
                            retainForRemoval = true
                        }) {
                        vm.deleteCategory(subs)
                        ToastUtils.show(getString(Res.string.delete_success))
                    }
                },
            )
        }

    }
}
