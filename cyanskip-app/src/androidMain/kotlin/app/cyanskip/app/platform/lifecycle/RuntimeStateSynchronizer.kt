package app.cyanskip.app.platform.lifecycle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.cyanskip.app.a11y.updateSystemDefaultAppId
import app.cyanskip.app.appScope
import app.cyanskip.app.permission.PermissionStates
import app.cyanskip.app.util.LogUtils
import li.songe.codeorigin.CallSite

object RuntimeStateSynchronizer {
    private val requests = Channel<String>(Channel.CONFLATED)

    init {
        appScope.launch(Dispatchers.IO) {
            for (initialLoc in requests) {
                delay(COALESCE_DELAY_MILLIS)
                var loc = initialLoc
                while (true) {
                    loc = requests.tryReceive().getOrNull() ?: break
                }
                try {
                    updateSystemDefaultAppId()
                    PermissionStates.refreshAll()
                } catch (e: Exception) {
                    LogUtils.d(e, loc = loc)
                }
            }
        }
    }

    fun requestSync(@CallSite loc: String = "") {
        check(requests.trySend(loc).isSuccess) { "运行时状态同步队列已关闭" }
    }

    private const val COALESCE_DELAY_MILLIS = 50L
}
