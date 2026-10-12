package app.cyanskip.app.ui.subscription

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.app_rules
import app.cyanskip.app.resources.app_search_hint
import app.cyanskip.app.resources.rules_empty
import app.cyanskip.app.resources.search_clear
import app.cyanskip.app.resources.search_close
import app.cyanskip.app.resources.search_no_results
import app.cyanskip.app.resources.search_no_results_filter_hint
import app.cyanskip.app.resources.search_open
import app.cyanskip.app.resources.subscription_app_switch_batch_confirmation
import app.cyanskip.app.resources.subscription_app_switch_set
import app.cyanskip.app.rule.RuleConfigIndex
import app.cyanskip.app.rule.RuleSetting
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsAppBarTextField
import app.cyanskip.app.ui.component.CsAppFilterContent
import app.cyanskip.app.ui.component.CsAppNameText
import app.cyanskip.app.ui.component.CsEmptyState
import app.cyanskip.app.ui.component.CsFilterIconButton
import app.cyanskip.app.ui.component.CsIcon
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsMultiSelectionActions
import app.cyanskip.app.ui.component.CsMultiSelectionTopAppBar
import app.cyanskip.app.ui.component.CsPageBottomSpace
import app.cyanskip.app.ui.component.CsPageBottomSpaceDefaults
import app.cyanskip.app.ui.component.CsRuleBatchMenuItems
import app.cyanskip.app.ui.component.CsScaffold
import app.cyanskip.app.ui.component.CsSubsAppCard
import app.cyanskip.app.ui.component.CsTwoLineText
import app.cyanskip.app.ui.component.autoFocus
import app.cyanskip.app.ui.component.rememberListScrollState
import app.cyanskip.app.ui.component.rememberMultiSelectionState
import app.cyanskip.app.ui.component.rememberRuleControlEnvironment
import app.cyanskip.app.ui.home.rememberVisitOrder
import app.cyanskip.app.ui.icon.CsSearchCloseIconButton
import app.cyanskip.app.ui.image.CsAppIcon
import app.cyanskip.app.ui.navigation.SubsAppGroupListRoute
import app.cyanskip.app.ui.navigation.SubsAppListRoute
import app.cyanskip.app.ui.navigation.UpsertRuleGroupRoute
import app.cyanskip.app.ui.option.AppSortOption
import app.cyanskip.app.ui.option.findOption
import app.cyanskip.app.ui.platform.CsBackHandler
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.platform.hideIme
import app.cyanskip.app.ui.share.ListPlaceholder
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.ui.text.subscriptionMessageResource
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.db.LOCAL_SUBS_IDS
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubsAppListPage(
    host: UiHost,
    route: SubsAppListRoute,
) {
    val mainVm = MainViewModel.requireCurrent()
    val subsItemId = route.subsItemId

    val vm = viewModel { SubsAppListViewModel(route) }
    val scope = vm.scope
    val busy by vm.busyFlow.collectAsStateWithLifecycle()
    val subscriptionSnapshot by SubscriptionRepository.snapshotFlow.collectAsStateWithLifecycle()
    val subscription = subscriptionSnapshot.value?.subscriptions?.get(subsItemId)

    val loadableState by vm.uiState.collectAsStateWithLifecycle()
    val environment = rememberRuleControlEnvironment()
    val appInfoMap = environment.apps
    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val visits = rememberVisitOrder()
    val searchStr by vm.searchStr.collectAsStateWithLifecycle()
    val showSearchBar by vm.showSearchBar.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    fun closeSearch() {
        vm.setShowSearchBar(false)
        vm.setSearchStr("")
        focusManager.clearFocus()
        host.hideIme()
    }

    val state = loadableState.value
    val configIndex = remember(state?.configs) { state?.configs?.let(::RuleConfigIndex) }
    val firstLoading = loadableState is Loadable.Loading
    val loadError = (loadableState as? Loadable.Failure)?.cause
    val sortedApps = remember(state, environment, store, visits) {
        filterSubsApps(
            state?.subscription?.apps.orEmpty(),
            environment.apps,
            store,
            state?.appActionOrder.orEmpty(),
            visits.value.orEmpty(),
            environment.blockedApps,
            { it.subsAppGroupType },
            { AppSortOption.objects.findOption(it.subsAppSort) },
            { it.subsAppShowBlock },
        )
    }
    val apps = remember(sortedApps, searchStr, appInfoMap) {
        sortedApps.filter { app ->
            app.id.contains(searchStr, true) || (appInfoMap[app.id]?.name ?: app.name).orEmpty()
                .contains(searchStr, true)
        }
    }
    val selection = rememberMultiSelectionState<String>()
    val visibleTargets = apps.mapTo(mutableSetOf()) { it.id }
    val selected = selection.selectedKeys intersect visibleTargets
    LaunchedEffect(visibleTargets) { selection.retain(visibleTargets) }
    CsBackHandler(showSearchBar && !selection.active) {
        if (!host.hideIme()) closeSearch()
    }
    CsBackHandler(selection.active) { if (!busy) selection.clear() }
    val updateSelected: (RuleSetting) -> Unit = { setting ->
        val request = vm.prepareSwitches(checkNotNull(state), selected)
        scope.launchUi {
            vm.runAction {
                if (mainVm.dialogRequests.confirm(
                        getString(Res.string.subscription_app_switch_set),
                        getString(
                            Res.string.subscription_app_switch_batch_confirmation,
                            selected.size.toString(),
                            setting.labelText()
                        )
                    )
                ) {
                    ToastUtils.show(vm.applySwitches(request, setting).description())
                }
            }
        }
    }
    val showAllApps = apps.size == state?.subscription?.apps?.size
    val pageScrollState = rememberListScrollState()
    val scrollBehavior = pageScrollState.scrollBehavior
    val listState = pageScrollState.listState
    pageScrollState.ResetOnListChange(apps, key = { it.id })
    var expanded by remember { mutableStateOf(false) }

    CsScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CsMultiSelectionTopAppBar(
                selectedMode = selection.active, selectedCount = selected.size,
                onExitSelection = selection::clear,
                onNavigateBack = { if (showSearchBar) closeSearch() else mainVm.navigator.pop() },
                onTitleClick = if (showSearchBar) null else pageScrollState::resetScroll,
                scrollBehavior = scrollBehavior, title = {
                    val firstShowSearchBar = remember { showSearchBar }
                    if (showSearchBar) {
                        CsAppBarTextField(
                            value = searchStr,
                            onValueChange = {
                                // 关闭时失焦可能回传旧文本，不能恢复已清空的搜索条件。
                                if (showSearchBar) vm.setSearchStr(it)
                            },
                            hint = stringResource(Res.string.app_search_hint),
                            modifier = if (firstShowSearchBar) Modifier else Modifier.autoFocus(),
                        )
                    } else {
                        CsTwoLineText(
                            title = subscription?.name ?: subsItemId.toString(),
                            subtitle = stringResource(Res.string.app_rules)
                        )
                    }
                }, actions = { selectedMode ->
                    if (selectedMode) {
                        CsMultiSelectionActions(selection, visibleTargets, !busy) { dismiss ->
                            CsRuleBatchMenuItems(!busy, dismiss, updateSelected)
                        }
                    } else {
                        CsSearchCloseIconButton(
                            isSearchOpen = showSearchBar,
                            contentDescription = if (!showSearchBar) stringResource(Res.string.search_open) else if (searchStr.isEmpty()) stringResource(
                                Res.string.search_close
                            ) else stringResource(Res.string.search_clear),
                            onClick = {
                                if (!showSearchBar) vm.setShowSearchBar(true)
                                else if (searchStr.isNotEmpty()) vm.setSearchStr("")
                                else closeSearch()
                            },
                        )
                        CsFilterIconButton(
                            filtered = sortedApps.size < state?.subscription?.apps.orEmpty().size,
                            onClick = {
                                expanded = true
                            },
                        )
                        Box(
                            modifier = Modifier.wrapContentSize(Alignment.TopStart)
                        ) {
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                CsAppFilterContent(
                                    sort = store.subsAppSort,
                                    groupType = store.subsAppGroupType,
                                    showBlockApps = store.subsAppShowBlock,
                                    onSort = vm::setSortType,
                                    onAppGroup = vm::setAppGroupType,
                                    onToggleBlock = vm::toggleShowBlockApps,
                                )
                            }
                        }
                    }
                })
        },
        floatingActionButton = {
            if (LOCAL_SUBS_IDS.contains(subsItemId) && !selection.active) {
                FloatingActionButton(onClick = {
                    mainVm.navigator.navigate(UpsertRuleGroupRoute(subsId = subsItemId, groupKey = null, appId = "", forward = true))
                }) {
                    CsIcon(
                        imageVector = CsIcons.Add,
                    )
                }
            }
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.scaffoldPadding(contentPadding),
            state = listState
        ) {
            items(apps, { it.id }) { app ->
                val enabledGroupCount = remember(app, state, environment) {
                    val current = checkNotNull(state)
                    app.groups.count { group ->
                        // Match the child list's switch positions, independent of parent availability.
                        environment.resolve(
                            current.subscription, group, app.id, current.configs,
                            checkNotNull(configIndex)
                        ).configuredEnabled
                    }
                }
                CsSubsAppCard(
                    appIcon = { CsAppIcon(app.id, 32.dp) },
                    appName = { CsAppNameText(app.id, app.name) },
                    subsId = subsItemId,
                    rawApp = app,
                    enabledGroupCount = enabledGroupCount,
                    appInfo = appInfoMap[app.id],
                    control = environment.app(
                        subsItemId, app.id, checkNotNull(state).configs,
                        checkNotNull(configIndex)
                    ),
                    selectionEnabled = !busy,
                    selectedMode = selection.active,
                    selected = app.id in selected,
                    onLongClick = {
                        if (!busy) {
                            focusManager.clearFocus()
                            host.hideIme()
                            selection.select(app.id)
                        }
                    },
                    onSelect = { selection.toggle(app.id) },
                    onClick = {
                        host.hideIme()
                        mainVm.navigator.navigate(SubsAppGroupListRoute(subsItemId, app.id))
                    },
                    onSettingChange = { setting ->
                        val request = vm.prepareSwitches(checkNotNull(state), setOf(app.id))
                        scope.launchUi {
                            vm.applySwitches(request, setting).failureMessage()
                                ?.let { ToastUtils.show(it) }
                        }
                    },
                )
            }
            item(ListPlaceholder.KEY, ListPlaceholder.TYPE) {
                if (apps.isEmpty() && !firstLoading) {
                    CsEmptyState(
                        text = if (loadError != null) {
                            loadError.subscriptionMessageResource()
                        } else if (searchStr.isNotEmpty()) {
                            if (showAllApps) stringResource(Res.string.search_no_results) else stringResource(
                                Res.string.search_no_results_filter_hint
                            )
                        } else {
                            stringResource(Res.string.rules_empty)
                        }
                    )
                    CsPageBottomSpace(height = CsPageBottomSpaceDefaults.CompactHeight)
                } else {
                    CsPageBottomSpace()
                }
            }
        }
    }
}
