package app.cyanskip.app.ui.home

actual fun previewActionToast(text: String, system: Boolean) =
    app.cyanskip.app.util.AndroidToastUtils.previewAction(text, system)
