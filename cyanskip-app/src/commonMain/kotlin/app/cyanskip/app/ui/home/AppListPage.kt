package app.cyanskip.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.model.AppInfo
import app.cyanskip.app.permission.AppPermission
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.app_list_permission_error_description
import app.cyanskip.app.resources.app_list_search
import app.cyanskip.app.resources.app_list_update_success
import app.cyanskip.app.resources.app_name_id_input_hint
import app.cyanskip.app.resources.app_whitelist
import app.cyanskip.app.resources.app_whitelist_state_description
import app.cyanskip.app.resources.data_load_failed
import app.cyanskip.app.resources.edit_enter
import app.cyanskip.app.resources.edit_exit
import app.cyanskip.app.resources.filter_title
import app.cyanskip.app.resources.group_title
import app.cyanskip.app.resources.loading_progress
import app.cyanskip.app.resources.permission_error
import app.cyanskip.app.resources.permission_query_apps
import app.cyanskip.app.resources.rule_summary_open
import app.cyanskip.app.resources.search_close
import app.cyanskip.app.resources.search_no_results
import app.cyanskip.app.resources.search_no_results_filter_hint
import app.cyanskip.app.resources.sort_filter
import app.cyanskip.app.resources.sort_title
import app.cyanskip.app.resources.whitelist_add
import app.cyanskip.app.resources.whitelist_edit
import app.cyanskip.app.resources.whitelist_edit_mode_toggle
import app.cyanskip.app.resources.whitelist_member
import app.cyanskip.app.resources.whitelist_not_member
import app.cyanskip.app.resources.whitelist_remove
import app.cyanskip.app.resources.whitelist_title
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsAnimatedFloatingActionButton
import app.cyanskip.app.ui.component.CsAppBarTextField
import app.cyanskip.app.ui.component.CsAppNameText
import app.cyanskip.app.ui.component.CsCheckbox
import app.cyanskip.app.ui.component.CsDesktopKeyHandler
import app.cyanskip.app.ui.component.CsEmptyState
import app.cyanskip.app.ui.component.CsFilterIconButton
import app.cyanskip.app.ui.component.CsIcon
import app.cyanskip.app.ui.component.CsIconButton
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsMenuGroupCard
import app.cyanskip.app.ui.component.CsMenuItemCheckbox
import app.cyanskip.app.ui.component.CsMenuItemRadioButton
import app.cyanskip.app.ui.component.CsPageBottomSpace
import app.cyanskip.app.ui.component.CsPageBottomSpaceDefaults
import app.cyanskip.app.ui.component.CsQueryPkgAuthCard
import app.cyanskip.app.ui.component.CsRuleStats
import app.cyanskip.app.ui.component.CsRuleStatsData
import app.cyanskip.app.ui.component.CsTopAppBar
import app.cyanskip.app.ui.component.LocalOverlayBackHandler
import app.cyanskip.app.ui.component.autoFocus
import app.cyanskip.app.ui.component.rememberListScrollState
import app.cyanskip.app.ui.icon.CsBlockCloseIconButton
import app.cyanskip.app.ui.icon.CsSearchCloseIconButton
import app.cyanskip.app.ui.image.CsAppIcon
import app.cyanskip.app.ui.navigation.AppConfigRoute
import app.cyanskip.app.ui.navigation.EditBlockAppListRoute
import app.cyanskip.app.ui.option.AppGroupOption
import app.cyanskip.app.ui.option.AppSortOption
import app.cyanskip.app.ui.option.findOption
import app.cyanskip.app.ui.platform.CsBackHandler
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.platform.hideIme
import app.cyanskip.app.ui.share.launchUiAction
import app.cyanskip.app.ui.style.getUpDownTransform
import app.cyanskip.app.ui.style.itemPadding
import app.cyanskip.app.ui.text.displayMessage
import app.cyanskip.app.util.ToastUtils
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

data class AppListContentState(
    val appInfos: List<AppInfo>,
    val listState: Loadable<Unit> = Loadable.Ready(Unit),
    val ruleStatsFailed: Boolean = false,
    val searchText: String = "",
    val showSearchBar: Boolean = false,
    val editWhiteListMode: Boolean = false,
    val showAllApps: Boolean = true,
    val ruleStats: Map<String, CsRuleStatsData> = emptyMap(),
    val whiteListAppIds: Set<String> = emptySet(),
    val canQueryPackages: Boolean = true,
    val queryPackagesAbnormal: Boolean = false,
    val refreshing: Boolean = false,
    val sort: Int = 0,
    val group: Int = 3,
    val showBlocked: Boolean = true,
)

@Composable
fun appListPage(
    host: UiHost,
    vm: HomeViewModel,
): ScaffoldExt {
    val mainVm = MainViewModel.requireCurrent()
    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val appState by vm.appsState.collectAsStateWithLifecycle()
    val blocked by SettingsRepository.blockMatchAppList.collectAsStateWithLifecycle()
    val controls by vm.appListState.collectAsStateWithLifecycle()
    val state = controls.content(appState, store, blocked)

    val scope = rememberCoroutineScope()
    fun refresh() {
        scope.launch {
            try {
                AppInfoRepository.refresh()
                ToastUtils.show(getString(Res.string.app_list_update_success))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ToastUtils.show(e.displayMessage())
            }
        }
    }
    CsDesktopKeyHandler(Key.F5, onKey = ::refresh)
    val appInfos = state.appInfos
    val searchStr = state.searchText

    val showSearchBar = state.showSearchBar
    val refreshing = state.refreshing
    val pullToRefreshState = rememberPullToRefreshState()
    val editWhiteListMode = state.editWhiteListMode
    val pageScrollState = rememberListScrollState()
    val scrollBehavior = pageScrollState.scrollBehavior
    val listState = pageScrollState.listState
    pageScrollState.ResetOnListChange(
        appInfos,
        key = { it.id },
        leadingItemKey = if (state.canQueryPackages) null else 1,
    )
    ResetPageScrollOnRequest(
        vm.homeState,
        BottomNavItem.AppList,
        pageScrollState::resetScrollAndAwait,
    )
    return ScaffoldExt(
        navItem = BottomNavItem.AppList,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DisposableEffect(null) {
                onDispose {
                    vm.leaveAppList()
                }
            }
            CsTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    val firstShowSearchBar = remember { showSearchBar }
                    if (showSearchBar) {
                        CsBackHandler { if (!host.hideIme()) vm.closeAppSearch() }
                        CsAppBarTextField(
                            value = searchStr,
                            onValueChange = vm::setAppSearchText,
                            hint = stringResource(Res.string.app_name_id_input_hint),
                            modifier = if (firstShowSearchBar) Modifier else Modifier.autoFocus(),
                        )
                    } else {
                        val titleModifier =
                            Modifier.clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = {
                                    pageScrollState.resetScroll()
                                },
                            )
                        if (editWhiteListMode) {
                            CsBackHandler(onBack = vm::closeAppListEdit)
                        }
                        AnimatedContent(
                            targetState = editWhiteListMode,
                            transitionSpec = { getUpDownTransform() },
                        ) { localEditWhiteListMode ->
                            if (localEditWhiteListMode) {
                                Text(
                                    modifier = titleModifier,
                                    text = stringResource(Res.string.app_whitelist),
                                )
                            } else {
                                Text(
                                    modifier = titleModifier,
                                    text = BottomNavItem.AppList.label,
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (state.queryPackagesAbnormal) {
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.error
                        ) {
                            CsIconButton(
                                imageVector = CsIcons.WarningAmber,
                                contentDescription = stringResource(Res.string.permission_error),
                                onClick = {
                                    scope.launch {
                                        mainVm.dialogRequests.showMessage(
                                            getString(Res.string.permission_error),
                                            getString(
                                                Res.string.app_list_permission_error_description,
                                                getString(Res.string.permission_query_apps),
                                            ),
                                        )
                                    }
                                },
                            )
                        }
                    }
                    CsSearchCloseIconButton(
                        onClick = vm::toggleAppSearch,
                        isSearchOpen = showSearchBar,
                        contentDescription =
                            if (showSearchBar) stringResource(Res.string.search_close)
                            else stringResource(Res.string.app_list_search),
                    )
                    var expanded by remember { mutableStateOf(false) }
                    if (expanded) LocalOverlayBackHandler.current { expanded = false }
                    CsFilterIconButton(
                        filtered = !state.showAllApps,
                        contentDescription = stringResource(Res.string.sort_filter),
                        onClick = {
                            expanded = true
                        },
                    )
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            CsMenuGroupCard(
                                inTop = true,
                                title = stringResource(Res.string.sort_title),
                            ) {
                                AppSortOption.objects.forEach { option ->
                                    CsMenuItemRadioButton(
                                        text = option.label,
                                        selected =
                                            AppSortOption.objects.findOption(state.sort) == option,
                                        onClick = { SettingsRepository.updateSettings { it.copy(appSort = option.value) } },
                                    )
                                }
                            }
                            CsMenuGroupCard(title = stringResource(Res.string.group_title)) {
                                AppGroupOption.normalObjects.forEach { option ->
                                    val newValue = option.invert(state.group)
                                    CsMenuItemCheckbox(
                                        enabled = newValue != 0,
                                        text = option.label,
                                        checked = option.include(state.group),
                                        onClick = { SettingsRepository.updateSettings { it.copy(appGroupType = newValue) } },
                                    )
                                }
                            }
                            CsMenuGroupCard(title = stringResource(Res.string.filter_title)) {
                                CsMenuItemCheckbox(
                                    text = stringResource(Res.string.whitelist_title),
                                    checked = state.showBlocked,
                                    onClick = {
                                        SettingsRepository.updateSettings { it.copy(showBlockApp = !state.showBlocked) }
                                    },
                                )
                            }
                        }
                    }
                    CsBlockCloseIconButton(
                        isClose = editWhiteListMode,
                        contentDescription = stringResource(Res.string.whitelist_edit_mode_toggle),
                        onClickLabel =
                            if (editWhiteListMode) stringResource(Res.string.edit_exit)
                            else stringResource(Res.string.edit_enter),
                        onClick = { vm.toggleAppListEdit(blocked) },
                    )
                },
            )
        },
        floatingActionButton = {
            CsAnimatedFloatingActionButton(
                visible = editWhiteListMode,
                contentDescription = stringResource(Res.string.whitelist_edit),
                onClick = {
                    mainVm.navigator.navigate(EditBlockAppListRoute)
                },
                imageVector = CsIcons.Edit,
            )
        },
    ) { contentPadding ->
        PullToRefreshBox(
            modifier = Modifier.padding(contentPadding),
            state = pullToRefreshState,
            isRefreshing = refreshing,
            onRefresh = ::refresh,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
            ) {
                if (!state.canQueryPackages) {
                    item(key = 1, contentType = 1) {
                        CsQueryPkgAuthCard(state.refreshing, mainVm.scope.launchUiAction {
                            mainVm.permissions.ensurePermissions(AppPermission.QueryPackages)
                        })
                    }
                }
                if (state.listState !is Loadable.Ready || state.ruleStatsFailed) {
                    item("load-state") {
                        Text(
                            stringResource(
                                if (state.listState is Loadable.Loading) Res.string.loading_progress
                                else Res.string.data_load_failed
                            ),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(appInfos, { it.id }) { appInfo ->
                    val stats = if (editWhiteListMode) null else state.ruleStats[appInfo.id]
                    CsAppListItem(
                        appInfo = appInfo,
                        icon = { CsAppIcon(appInfo.id, 32.dp) },
                        name = { CsAppNameText(appInfo = appInfo) },
                        stats = stats,
                        editWhiteListMode = editWhiteListMode,
                        inWhiteList = appInfo.id in state.whiteListAppIds,
                        onClick = {
                            if (editWhiteListMode) {
                                SettingsRepository.updateBlockMatchAppList {
                                    if (appInfo.id in it) it - appInfo.id else it + appInfo.id
                                }
                            } else {
                                host.hideIme()
                                mainVm.navigator.navigate(AppConfigRoute(appInfo.id))
                            }
                        },
                    )
                }
                item("placeholder", "placeholder") {
                    if (
                        state.listState is Loadable.Ready &&
                            appInfos.isEmpty() &&
                            searchStr.isNotEmpty()
                    ) {
                        CsEmptyState(
                            text =
                                if (state.showAllApps) stringResource(Res.string.search_no_results)
                                else stringResource(Res.string.search_no_results_filter_hint)
                        )
                        CsPageBottomSpace(height = CsPageBottomSpaceDefaults.CompactHeight)
                    } else {
                        CsPageBottomSpace()
                    }
                }
            }
        }
    }
}

@Composable
fun CsAppListItem(
    appInfo: AppInfo,
    icon: @Composable () -> Unit,
    name: @Composable () -> Unit,
    stats: CsRuleStatsData?,
    editWhiteListMode: Boolean,
    inWhiteList: Boolean,
    onClick: () -> Unit,
) {
    val statsDescription = stats?.takeIf { it.hasRules }?.description
    val description =
        if (editWhiteListMode) appInfo.name
        else
            stringResource(
                Res.string.app_whitelist_state_description,
                appInfo.name,
                statsDescription ?: appInfo.id,
            )
    val member = stringResource(Res.string.whitelist_member)
    val notMember = stringResource(Res.string.whitelist_not_member)
    val clickLabel =
        if (editWhiteListMode)
            stringResource(
                if (inWhiteList) Res.string.whitelist_remove else Res.string.whitelist_add
            )
        else stringResource(Res.string.rule_summary_open)

    Row(
        modifier =
            Modifier.clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = description
                    if (inWhiteList) {
                        stateDescription = member
                    } else if (editWhiteListMode) {
                        stateDescription = notMember
                    }
                    onClick(
                        label = clickLabel,
                        action = null,
                    )
                }
                .itemPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            name()
            if (stats != null) {
                CsRuleStats(stats, emptyText = appInfo.id)
            } else {
                Text(
                    text = appInfo.id,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }
        if (editWhiteListMode) {
            CsCheckbox(
                key = appInfo.id,
                checked = inWhiteList,
            )
        } else if (inWhiteList) {
            CsIcon(
                modifier = Modifier.padding(2.dp).size(20.dp),
                imageVector = CsIcons.Block,
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
