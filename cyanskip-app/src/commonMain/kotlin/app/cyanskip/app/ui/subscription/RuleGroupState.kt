package app.cyanskip.app.ui.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import app.cyanskip.app.model.ExcludeData
import app.cyanskip.db.SubsGlobalGroupConfig
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.app_rule_missing
import app.cyanskip.app.resources.delete_named_confirmation
import app.cyanskip.app.resources.delete_success
import app.cyanskip.app.resources.rule_delete
import app.cyanskip.app.resources.rule_missing
import app.cyanskip.app.rule.RuleControlState
import app.cyanskip.app.rule.RuleGroupConfigService
import app.cyanskip.app.rule.RuleGroupConfiguration
import app.cyanskip.app.rule.RuleGroupTarget
import app.cyanskip.app.rule.toSwitchTarget
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.subscription.edit
import app.cyanskip.app.ui.component.CsRetainedSheet
import app.cyanskip.app.ui.component.DialogRequests
import app.cyanskip.app.ui.component.SheetRequest
import app.cyanskip.app.ui.component.rememberRuleControlEnvironment
import app.cyanskip.app.ui.navigation.AppNavigator
import app.cyanskip.app.ui.navigation.ConfirmDeletion
import app.cyanskip.app.ui.navigation.RuleExcludeEditorRoute
import app.cyanskip.app.ui.navigation.SubsGlobalGroupExcludeRoute
import app.cyanskip.app.ui.navigation.UpsertRuleGroupRoute
import app.cyanskip.app.ui.share.DeletionTarget
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.ToastUtils

private data class RuleSheetSnapshot(
    val target: RuleGroupTarget,
    val subscription: RawSubscription,
    val group: RawSubscription.RawGroupProps,
    val configuration: RuleGroupConfiguration,
    val control: RuleControlState,
)

class RuleGroupState(
    private val dialogs: DialogRequests,
) {
    private val showGroupFlow = MutableStateFlow<SheetRequest<RuleGroupTarget>?>(null)
    private fun dismissGroupShow(request: SheetRequest<RuleGroupTarget>) {
        if (showGroupFlow.value === request) showGroupFlow.value = null
    }

    fun showGroup(state: RuleGroupTarget) {
        showGroupFlow.value = SheetRequest(state)
    }

    fun dismissForDeletion(targets: Set<DeletionTarget>) {
        val request = showGroupFlow.value ?: return
        val group = request.key
        if (DeletionTarget.Subscription(group.subsId) in targets ||
            DeletionTarget.Group(group.subsId, group.appId, group.groupKey) in targets ||
            (group.appId?.let { DeletionTarget.App(group.subsId, it) in targets } == true)
        ) {
            dismissGroupShow(request)
        }
    }

    private fun openExcludeEditor(
        state: RuleGroupTarget,
        dismiss: () -> Unit,
        navigator: AppNavigator,
    ) {
        dismiss()
        navigator.navigate(
            if (state.appId == null) SubsGlobalGroupExcludeRoute(state.subsId, state.groupKey)
            else RuleExcludeEditorRoute(state.subsId, state.groupKey, state.appId)
        )
    }

    private suspend fun deleteGroup(
        state: RuleGroupTarget,
    ) {
        SubscriptionRepository.update(state.subsId) { subscription ->
            when (state) {
                is RuleGroupTarget.Global -> subscription.edit {
                    if (removeGlobalGroups { it.key == state.groupKey }.isEmpty()) {
                        error(Res.string.rule_missing.getSync())
                    }
                }

                is RuleGroupTarget.App -> subscription.edit {
                    if (subscription.apps.none { it.id == state.appId }) {
                        error(Res.string.app_rule_missing.getSync())
                    }
                    if (removeAppGroups(state.appId) { it.key == state.groupKey }.isEmpty()) {
                        error(Res.string.rule_missing.getSync())
                    }
                }
            }
        }
    }

    @Composable
    fun Render(
        navigator: AppNavigator,
        openSubscription: (Long) -> Unit,
        ruleControl: RuleControlDialogState,
        confirmDelete: ConfirmDeletion,
    ) {
        val scope = rememberCoroutineScope()
        val environment = rememberRuleControlEnvironment()
        val requested by showGroupFlow.collectAsStateWithLifecycle()
        val target = requested?.key
        val subscriptions by SubscriptionRepository.snapshotFlow.collectAsStateWithLifecycle()
        val subscription = subscriptions.value?.subscriptions?.get(target?.subsId)
        val group =
            if (target?.appId == null) subscription?.globalGroups?.find { it.key == target?.groupKey } else subscription?.apps?.find { it.id == target.appId }?.groups?.find { it.key == target.groupKey }
        val configurationFlow = remember(target) {
            target?.let(RuleGroupConfigService::groupConfiguration) ?: flowOf(null)
        }
        val configuration =
            key(requested) { configurationFlow.collectAsStateWithLifecycle(null).value }
        val snapshot =
            if (target != null && subscription != null && group != null && configuration != null) {
                RuleSheetSnapshot(
                    target, subscription, group, configuration,
                    environment.resolve(
                        subscription,
                        group,
                        target.pageAppId,
                        configuration.snapshot
                    )
                )
            } else null
        CsRetainedSheet(
            request = requested,
            snapshot = snapshot,
            missing = target != null && subscriptions is Loadable.Ready && (subscription == null || group == null),
            onDismissRequest = ::dismissGroupShow,
        ) { _, displayed, sheetState, dismiss ->
            val showGroupState = displayed.target
            val showSubs = displayed.subscription
            val showGroup = displayed.group
            val configSnapshot = displayed.configuration
            val subsConfig = configSnapshot.group
            val excludeData = remember(subsConfig?.exclude) {
                ExcludeData.parse(subsConfig?.exclude)
            }
            CsRuleGroupDialog(
                navigator, openSubscription, ruleControl,
                sheetState = sheetState,
                subs = showSubs,
                group = showGroup,
                appId = showGroupState.pageAppId,
                excludeData = excludeData,
                excludeAppId = showGroupState.appId,
                onDismissRequest = dismiss,
                control = displayed.control,
                matchAnyApp = (subsConfig as? SubsGlobalGroupConfig)?.matchAnyApp,
                onMatchAnyAppChange = { value ->
                    scope.launchUi {
                        RuleGroupConfigService.setMatchAnyApp(
                            showSubs, RuleGroupTarget.Global(showSubs.id, showGroup.key), value,
                        )
                    }
                },
                onSettingChange = { setting ->
                    val change = RuleGroupConfigService.prepare(
                        listOf(showGroupState.toSwitchTarget()),
                        listOf(showSubs),
                        configSnapshot.snapshot
                    )
                    scope.launchUi {
                        applyRuleSwitchWithConfirmation(change, setting, dialogs, RuleSwitchHost.RuleDetail)
                            ?.failureMessage()
                            ?.let { ToastUtils.show(it) }
                    }
                },
                onClickEdit = {
                    dismiss()
                    navigator.navigate(
                        UpsertRuleGroupRoute(
                            subsId = showGroupState.subsId,
                            groupKey = showGroupState.groupKey,
                            appId = showGroupState.appId,
                        )
                    )
                },
                onClickEditExclude = {
                    openExcludeEditor(showGroupState, dismiss, navigator)
                },
                onClickDelete = {
                    confirmDelete(
                        Res.string.rule_delete.getSync(),
                        Res.string.delete_named_confirmation.getSync(showGroup.name),
                        {
                            setOf(
                                DeletionTarget.Group(
                                    showGroupState.subsId,
                                    showGroupState.appId,
                                    showGroupState.groupKey
                                )
                            )
                        },
                        dismiss
                    ) {
                        deleteGroup(showGroupState)
                        ToastUtils.show(Res.string.delete_success.getSync())
                    }
                }
            )
        }

    }
}
