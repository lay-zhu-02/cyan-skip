package app.cyanskip.app.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.cyanskip.app.crash.CrashData
import app.cyanskip.app.permission.PermissionRequester
import app.cyanskip.app.rule.RuleGroupTarget
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.ui.component.DialogRequests
import app.cyanskip.app.ui.component.ShareLogState
import app.cyanskip.app.ui.component.TextDialogState
import app.cyanskip.app.ui.home.HomeNavigation
import app.cyanskip.app.ui.navigation.AppNavigator
import app.cyanskip.app.ui.navigation.CrashReportRoute
import app.cyanskip.app.ui.share.DeletionTarget
import app.cyanskip.app.ui.share.confirmDelete as confirmRuleDeletion
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.app.ui.subscription.RuleControlDialogState
import app.cyanskip.app.ui.subscription.RuleGroupState
import app.cyanskip.app.ui.subscription.SubsLinkDialogState
import app.cyanskip.app.ui.subscription.SubsSheetState

class MainViewModel(val permissions: PermissionRequester) : BaseViewModel() {
    companion object {
        private var current: MainViewModel? = null

        fun requireCurrent(): MainViewModel =
            checkNotNull(current) { "Main UI is not initialized" }
    }

    fun registerCurrent() {
        check(current == null || current === this) { "Main UI is already registered" }
        current = this
    }

    init {
        addCloseable {
            if (current === this) current = null
        }
    }

    val navigator = AppNavigator(scope)

    val dialogRequests = DialogRequests()

    fun confirmDelete(
        title: String,
        text: String,
        targets: () -> Set<DeletionTarget> = { emptySet() },
        dismiss: () -> Unit = {},
        delete: suspend () -> Unit,
    ) = scope.launchUi {
        confirmRuleDeletion(
            dialogRequests, navigator, subsSheet, ruleGroupState, ruleControlDialog,
            title, text, targets, dismiss, delete,
        )
    }

    val shareLog = ShareLogState(scope)

    val subsLinkDialog = SubsLinkDialogState(
        navigator = navigator,
        permissions = permissions,
    )

    val textDialog = TextDialogState()

    val subsSheet = SubsSheetState(
        navigator = navigator,
        openUrl = textDialog::showUrl,
        requestUrl = subsLinkDialog::request,
        confirmDelete = ::confirmDelete,
    )

    val ruleGroupState = RuleGroupState(dialogRequests)
    fun showRuleGroup(
        subscriptionId: Long,
        appId: String?,
        group: RawSubscription.RawGroupProps,
        pageAppId: String?,
    ) = scope.launch {
        withContext(Dispatchers.Default) { group.cacheStr }
        ruleGroupState.showGroup(
            if (group is RawSubscription.RawGlobalGroup) {
                RuleGroupTarget.Global(subscriptionId, group.key, pageAppId)
            } else {
                RuleGroupTarget.App(subscriptionId, requireNotNull(appId), group.key)
            },
        )
    }

    val ruleControlDialog = RuleControlDialogState()

    val homeNavigation = HomeNavigation()

    private var pendingCrashes = emptyList<CrashData>()

    fun showCrashReports(records: List<CrashData>) {
        pendingCrashes = records
        if (records.isNotEmpty()) navigator.navigate(CrashReportRoute)
    }

    fun takeCrashDataList(): List<CrashData> = pendingCrashes.also { pendingCrashes = emptyList() }
}
