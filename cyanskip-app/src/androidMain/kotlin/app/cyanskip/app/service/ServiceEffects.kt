package app.cyanskip.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import app.cyanskip.app.notif.StopServiceReceiver
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.service_started
import app.cyanskip.app.resources.service_stopped
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.ToastUtils
import li.songe.codeorigin.CallSite

fun LifecycleHookService.useServicePresence(
    stateFlow: MutableStateFlow<Boolean>,
    name: String,
    startToastDelayMillis: Long = 0L,
    @CallSite loc: String = "",
) {
    onCreated {
        stateFlow.value = true
        ToastUtils.show(
            Res.string.service_started.getSync(name),
            delayMillis = startToastDelayMillis,
            loc = loc
        )
    }
    onDestroyed(loc = loc) {
        stateFlow.value = false
        ToastUtils.show(Res.string.service_stopped.getSync(name), loc = loc)
    }
}

fun LifecycleHookService.useStopServiceReceiver(
    @CallSite loc: String = "",
) {
    val receiver = StopServiceReceiver(this)
    onCreated { receiver.register() }
    onDestroyed(loc = loc) { receiver.close() }
}
