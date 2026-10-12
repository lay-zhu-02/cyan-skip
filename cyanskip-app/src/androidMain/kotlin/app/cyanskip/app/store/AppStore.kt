package app.cyanskip.app.store

import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.rule_matching_enable
import app.cyanskip.app.resources.rule_matching_pause
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.ToastUtils

object AppStore {

    fun toggleEnableMatch() {
        if (SettingsRepository.settings.value.enableMatch) {
            ToastUtils.show(Res.string.rule_matching_pause.getSync())
        } else {
            ToastUtils.show(Res.string.rule_matching_enable.getSync())
        }
        SettingsRepository.updateSettings { it.copy(enableMatch = !it.enableMatch) }
    }



}
