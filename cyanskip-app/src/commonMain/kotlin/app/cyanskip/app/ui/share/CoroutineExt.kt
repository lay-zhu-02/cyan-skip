package app.cyanskip.app.ui.share

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import app.cyanskip.app.model.RpcError
import app.cyanskip.app.ui.text.subscriptionMessage
import app.cyanskip.app.util.LogUtils
import app.cyanskip.app.util.ToastUtils
import li.songe.codeorigin.CallSite

fun CoroutineScope.launchUi(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    @CallSite loc: String = "",
    block: suspend CoroutineScope.() -> Unit,
): Job = launch(context, start) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LogUtils.d(e, loc = loc)
        ToastUtils.show(
            e.subscriptionMessage(),
            forced = e is RpcError,
            loc = "",
        )
    }
}

fun CoroutineScope.launchUiAction(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    @CallSite loc: String = "",
    block: suspend CoroutineScope.() -> Unit,
): () -> Unit = {
    launchUi(context = context, start = start, loc = loc, block = block)
}

fun <T> CoroutineScope.launchUiAction(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    @CallSite loc: String = "",
    block: suspend CoroutineScope.(T) -> Unit,
): (T) -> Unit = { value ->
    launchUi(context = context, start = start, loc = loc) { block(value) }
}
