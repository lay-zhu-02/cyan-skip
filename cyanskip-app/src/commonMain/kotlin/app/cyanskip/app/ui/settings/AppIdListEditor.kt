package app.cyanskip.app.ui.settings

import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.settings.SettingsAppIds
import app.cyanskip.app.settings.SettingsRepository

/** Prepares and persists explicit app ID edits; the owning page ViewModel manages the draft. */
class AppIdListEditor(
    private val currentIds: () -> Set<String>,
    private val replace: (Set<String>) -> Unit,
) {
    fun initialText(): String {
        val appNames = AppInfoRepository.snapshot?.apps.orEmpty().mapValues { it.value.name }
        return currentIds().sorted().sortedBy { if (it in appNames) 0 else 1 }
            .joinToString("\n\n", postfix = "\n\n") { id ->
                appNames[id]?.let { "$id\n# $it" } ?: id
            }
    }

    fun hasChanges(text: String) = currentIds() != SettingsAppIds.decode(text)

    suspend fun save(text: String): Boolean {
        val ids = SettingsAppIds.decode(text)
        val changed = currentIds() != ids
        replace(ids)
        SettingsRepository.awaitPersistence()
        return changed
    }
}
