package app.cyanskip.app.platform.service

import app.cyanskip.app.service.ButtonService

object ServiceController {
    fun setSnapshotButtonEnabled(enabled: Boolean) = setEnabled(
        enabled = enabled,
        start = ButtonService::start,
        stop = ButtonService::stop,
    )

    private fun setEnabled(
        enabled: Boolean,
        start: () -> Unit,
        stop: () -> Unit,
    ) {
        if (enabled) start() else stop()
    }
}
