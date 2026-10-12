package app.cyanskip.app.service

import kotlinx.coroutines.flow.map
import app.cyanskip.app.settings.SettingsRepository.settings
import app.cyanskip.app.store.AppStore.toggleEnableMatch

class MatchTileService : BaseTileService() {
    override val activeFlow = settings.map { it.enableMatch }

    override fun onTileClick() = toggleEnableMatch()
}
