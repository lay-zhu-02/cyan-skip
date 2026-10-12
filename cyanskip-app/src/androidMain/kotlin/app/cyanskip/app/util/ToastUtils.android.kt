package app.cyanskip.app.util

import com.hjq.toast.Toaster
import app.cyanskip.app.platform.lifecycle.MainActivityVisibility
import app.cyanskip.app.service.OverlayWindowService

actual fun showPlatformToast(text: CharSequence, forced: Boolean) {
    if (forced || MainActivityVisibility.isVisible || OverlayWindowService.isAnyAlive) {
        Toaster.show(text)
    }
}
