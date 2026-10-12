package app.cyanskip.app.ui.share

import app.cyanskip.app.ui.component.DialogRequests
import app.cyanskip.app.ui.navigation.AppNavigator
import app.cyanskip.app.ui.subscription.RuleControlDialogState
import app.cyanskip.app.ui.subscription.RuleGroupState
import app.cyanskip.app.ui.subscription.SubsSheetState

suspend fun confirmDelete(
    dialogs: DialogRequests,
    navigator: AppNavigator,
    subsSheet: SubsSheetState,
    rules: RuleGroupState,
    ruleControl: RuleControlDialogState,
    title: String,
    text: String,
    targets: () -> Set<DeletionTarget>,
    dismiss: () -> Unit,
    delete: suspend () -> Unit,
) {
    if (!dialogs.confirm(title, text, error = true)) return
    val deleted = targets()
    dismiss()
    subsSheet.dismissForDeletion(deleted)
    rules.dismissForDeletion(deleted)
    ruleControl.dismissForDeletion(deleted)
    navigator.popFromFirst { route -> deleted.any { it.owns(route) } }.join()
    delete()
}
