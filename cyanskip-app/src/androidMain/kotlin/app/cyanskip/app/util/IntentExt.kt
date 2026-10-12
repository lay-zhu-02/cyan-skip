package app.cyanskip.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.intent_launch_failed_prefix
import app.cyanskip.app.ui.text.getSync
import li.songe.codeorigin.CallSite

fun Context.tryStartActivity(
    intent: Intent,
    @CallSite loc: String = "",
) {
    try {
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        LogUtils.d("tryStartActivity", e, loc = loc)
        ToastUtils.show(
            Res.string.intent_launch_failed_prefix.getSync() + (e.message
                ?: e.stackTraceToString()), loc = loc
        )
    }
}

val Intent.extraCptName: ComponentName?
    get() = if (AndroidTarget.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(Intent.EXTRA_COMPONENT_NAME) as? ComponentName?
    }
