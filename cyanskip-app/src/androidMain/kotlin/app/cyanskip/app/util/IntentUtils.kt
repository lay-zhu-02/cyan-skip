package app.cyanskip.app.util

import android.app.Service
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import app.cyanskip.app.META
import app.cyanskip.app.app
import app.cyanskip.app.permission.PermissionStates
import app.cyanskip.app.platform.lifecycle.MainActivityVisibility
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.service_launch_failed
import app.cyanskip.app.resources.wechat_unavailable
import app.cyanskip.app.ui.text.getSync
import li.songe.codeorigin.CallSite
import kotlin.reflect.KClass

object IntentUtils {
    fun openWeChatScanner() {
        val intent = app.packageManager.getLaunchIntentForPackage("com.tencent.mm")?.apply {
            putExtra("LauncherUI.From.Scaner.Shortcut", true)
        }
        if (intent == null) {
            ToastUtils.show(Res.string.wechat_unavailable.getSync())
            return
        }
        app.tryStartActivity(intent)
    }

    fun openA11ySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        app.tryStartActivity(intent)
    }

    fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${app.packageName}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        app.tryStartActivity(intent)
    }

    fun <T : Service> stopService(clazz: KClass<T>) {
        val intent = Intent(app, clazz.java)
        app.stopService(intent)
    }

    fun <T : Service> startForegroundService(
        clazz: KClass<T>,
        @CallSite loc: String = "",
    ) {
        if (!PermissionStates.notification.checkOrToast(loc = loc)) return
        if (!PermissionStates.foregroundServiceSpecialUse.checkOrToast(loc = loc)) return
        val intent = Intent(app, clazz.java)
        try {
            app.startForegroundService(intent)
        } catch (e: Throwable) {
            LogUtils.d(e, loc = loc)
            val prefix = if (MainActivityVisibility.isVisible) "" else "${META.appName}: "
            ToastUtils.show(
                Res.string.service_launch_failed.getSync(prefix, e.message),
                forced = true,
                loc = loc
            )
        }
    }
}
