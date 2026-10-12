package app.cyanskip.app.util

import app.cyanskip.app.platform.writeClipboardText
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.copy_success
import app.cyanskip.app.ui.text.getSync

fun copyText(text: String) {
    writeClipboardText(text)
    ToastUtils.show(Res.string.copy_success.getSync())
}
