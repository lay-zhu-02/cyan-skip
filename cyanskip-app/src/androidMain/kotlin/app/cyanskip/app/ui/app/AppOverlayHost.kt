package app.cyanskip.app.ui.app

import androidx.compose.runtime.getValue
import app.cyanskip.app.util.copyText

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import app.cyanskip.app.service.A11yService
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.ui.component.CsPlatformWarningDialogs
import app.cyanskip.app.ui.component.CsTermsAcceptDialog
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.ToastUtils

@Composable
fun AppOverlayHost() {
    val activity = LocalActivity.current as app.cyanskip.app.MainActivity
    val mainVm = activity.mainVm
    if (!SettingsRepository.termsAccepted.collectAsStateWithLifecycle().value) {
        CsTermsAcceptDialog(
            onAccept = SettingsRepository::acceptTerms,
            onError = { ToastUtils.show(it.message ?: it.toString()) },
            onDisagree = { activity.finish() },
        )
    } else {
        // Sheet
        mainVm.subsSheet.Render()

        // Dialog
        PlatformWarnings()
        mainVm.dialogRequests.Render()
        mainVm.subsLinkDialog.Render()
        mainVm.ruleGroupState.Render(
            navigator = mainVm.navigator, openSubscription = mainVm.subsSheet::show,
            ruleControl = mainVm.ruleControlDialog, confirmDelete = mainVm::confirmDelete,
        )
        mainVm.ruleControlDialog.Render()
        mainVm.textDialog.Render(onCopy = ::copyText)
        mainVm.shareLog.Render(activity)
    }
}

private val accessRestrictedSettingsShowFlow = MutableStateFlow(false)

fun showAccessRestrictedSettingsDialog() {
    accessRestrictedSettingsShowFlow.value = true
}

private fun dismissAccessRestrictedSettingsDialog() {
    accessRestrictedSettingsShowFlow.value = false
}

@Composable
private fun PlatformWarnings() {
    val mainVm = (androidx.activity.compose.LocalActivity.current as app.cyanskip.app.MainActivity).mainVm
    val a11y by A11yService.isRunning.collectAsStateWithLifecycle()
    val restricted by accessRestrictedSettingsShowFlow.collectAsStateWithLifecycle()
    CsPlatformWarningDialogs(restricted, a11y,
        ::dismissAccessRestrictedSettingsDialog,
        app.cyanskip.app.util.IntentUtils::openAppDetailsSettings)
}
