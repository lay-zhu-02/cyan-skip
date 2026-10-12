package app.cyanskip.app.ui.home

import app.cyanskip.app.DesktopRuntime

actual fun previewActionToast(text: String, system: Boolean) {
    DesktopRuntime.requireCurrent().toast.show(text)
}
