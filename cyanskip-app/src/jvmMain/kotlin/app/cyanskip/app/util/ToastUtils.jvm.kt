package app.cyanskip.app.util

import app.cyanskip.app.DesktopRuntime

actual fun showPlatformToast(text: CharSequence, forced: Boolean) {
    DesktopRuntime.requireCurrent().toast.show(text.toString())
}
