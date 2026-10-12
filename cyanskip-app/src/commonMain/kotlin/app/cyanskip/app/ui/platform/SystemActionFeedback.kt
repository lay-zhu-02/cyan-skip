package app.cyanskip.app.ui.platform

import app.cyanskip.app.platform.PlatformResult
import app.cyanskip.app.platform.openExternalUri
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.intent_launch_failed_prefix
import app.cyanskip.app.resources.platform_action_unsupported
import app.cyanskip.app.ui.text.displayMessage
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.ToastUtils

object SystemActionFeedback {
    fun openExternal(uri: String) {
        try {
            if (openExternalUri(uri) == PlatformResult.Unsupported) ToastUtils.show(Res.string.platform_action_unsupported.getSync())
        } catch (e: Exception) {
            ToastUtils.show(Res.string.intent_launch_failed_prefix.getSync() + (e.displayMessage()))
        }
    }
}
