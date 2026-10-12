package app.cyanskip.app.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import app.cyanskip.app.app
import app.cyanskip.app.appScope
import app.cyanskip.app.notif.NotificationCatalog
import app.cyanskip.app.platform.lifecycle.RuntimeStateSynchronizer
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.execution_success
import app.cyanskip.app.resources.external_call_unknown
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.AndroidStorage
import app.cyanskip.app.util.LogUtils
import app.cyanskip.app.util.ThreadUtils
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.app.util.componentName

class ExposeService : Service() {
    override fun onBind(intent: Intent?): Binder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appScope.launchUi {
            try {
                handleIntent(intent)
            } finally {
                ThreadUtils.runMainOrPost(1000) { stopSelf() }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    suspend fun handleIntent(intent: Intent?) {
        val expose = intent?.getIntExtra("expose", ACTION_SYNC_RUNTIME) ?: ACTION_SYNC_RUNTIME
        val data = intent?.getStringExtra("data")
        LogUtils.d("ExposeService::handleIntent", expose, data)
        when (expose) {
            ACTION_SYNC_RUNTIME -> {
                ToastUtils.show(Res.string.execution_success.getSync(), forced = true)
                RuntimeStateSynchronizer.requestSync()
            }

            else -> {
                ToastUtils.show(
                    Res.string.external_call_unknown.getSync(expose, data),
                    forced = true
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationCatalog.expose().startForeground()
    }

    companion object {
        // Public expose.sh/Intent protocol: preserve these numeric values.
        const val ACTION_SYNC_RUNTIME = 1

        fun initCommandFile() {
            val commandText = template
                .replace("{service}", ExposeService::class.componentName.flattenToShortString())
            AndroidStorage.storage.sh.resolve("expose.sh").writeText(commandText)
        }

        fun exposeIntent(expose: Int, data: String? = null): Intent {
            return Intent(app, ExposeService::class.java).apply {
                putExtra("expose", expose)
                if (data != null) {
                    putExtra("data", data)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

private const val template = $$"""set -euo pipefail
echo '> start expose.sh'
p=''
if [ -n "${1:-}" ]; then
  p+=" --ei expose $1"
fi
if [ -n "${2:-}" ]; then
  p+=" --es data $2"
fi
am start-foreground-service -n {service} $p
echo '> expose.sh end'
"""
