package app.cyanskip.app


import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.simulation_authorization_title
import app.cyanskip.app.resources.simulation_authorization_message
import app.cyanskip.app.resources.action_agree
import app.cyanskip.app.resources.action_cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.jetbrains.compose.resources.stringResource
import app.cyanskip.app.ui.component.CsPlatformWarningDialogs
import app.cyanskip.app.ui.component.CsTermsAcceptDialog
import app.cyanskip.app.ui.text.getSync

@Composable
fun DesktopOverlays(state: DesktopState, session: DesktopSession) {
    session.state.mainVm.shareLog.Render(session)
    val simulatorSettings by state.simulator.settings.collectAsStateWithLifecycle()
    val awaiting = SimulatedService.entries.firstOrNull {
        simulatorSettings.services.state(it).phase == ServicePhase.AwaitingAuthorization
    }
    if (awaiting != null) {
        val attempt = simulatorSettings.services.state(awaiting).attempt
        fun respond(type: ServiceEventType) = state.simulatorCommand {
            DesktopRuntime.requireCurrent().services.event(ServiceEvent(awaiting, type, attempt))
        }
        AlertDialog(
            onDismissRequest = { respond(ServiceEventType.Cancelled) },
            title = { Text(stringResource(Res.string.simulation_authorization_title, stringResource(awaiting.label()))) },
            text = { Text(stringResource(Res.string.simulation_authorization_message)) },
            confirmButton = { TextButton(onClick = { respond(ServiceEventType.Authorized) }) { Text(stringResource(Res.string.action_agree)) } },
            dismissButton = { TextButton(onClick = { respond(ServiceEventType.Cancelled) }) { Text(stringResource(Res.string.action_cancel)) } },
        )
    }
    val device = simulatorSettings.environment().android
    CsPlatformWarningDialogs(
        device.restrictedWarning || state.overlay == "restricted",
        device.serviceEnabled,
        { state.overlay = null; state.simulator.updateAndroid { it.copy(restrictedWarning = false) } },
        { state.unsupported() },
    )
    when (state.overlay) {
        "terms" -> CsTermsAcceptDialog(
            { state.overlay = null },
            { state.toast.show(it.message ?: it.toString()) },
            { state.overlay = null })
    }
}
