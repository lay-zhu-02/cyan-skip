package app.cyanskip.app.ui.settings

import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.backup_import_skipped_config_count
import app.cyanskip.app.resources.import_success
import app.cyanskip.app.ui.text.getSync

object SettingsFeedback {
    fun backupImported(skipped: Int): String = if (skipped > 0) {
        Res.string.backup_import_skipped_config_count.getSync(skipped)
    } else Res.string.import_success.getSync()
}
