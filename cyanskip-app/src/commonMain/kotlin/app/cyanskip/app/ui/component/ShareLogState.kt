package app.cyanskip.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.logs_share_file
import app.cyanskip.app.storage.createLogArchive
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.share.saveArchive
import app.cyanskip.app.ui.share.shareArchive
import org.jetbrains.compose.resources.getString

class ShareLogState(
    private val scope: CoroutineScope,
) {
    private var visible by mutableStateOf(false)

    fun show() { visible = true }
    private fun dismiss() { visible = false }

    private fun share(host: UiHost) {
        dismiss()
        scope.launchUi {
            host.shareArchive(getString(Res.string.logs_share_file), ::createLogArchive)
        }
    }

    private fun save(host: UiHost) {
        dismiss()
        scope.launchUi { host.saveArchive(::createLogArchive) }
    }

    @Composable
    fun Render(host: UiHost) {
        if (visible) CsShareLogDialog(::dismiss, { share(host) }, { save(host) })
    }
}
