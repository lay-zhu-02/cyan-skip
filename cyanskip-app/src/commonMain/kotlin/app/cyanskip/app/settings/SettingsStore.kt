package app.cyanskip.app.settings

import kotlinx.serialization.Serializable
import app.cyanskip.app.app.AppGroupFlags
import app.cyanskip.app.app.AppSort
import app.cyanskip.app.rule.RuleSort
import app.cyanskip.app.snapshot.SnapshotDisplayMode

@Serializable
data class SettingsStore(
    val enableMatch: Boolean = true,
    val excludeFromRecents: Boolean = false,
    val httpServerPort: Int = 8888,
    val updateSubsInterval: Long = SubscriptionUpdateInterval.Daily,
    val toastWhenClick: Boolean = true,
    val actionToast: String = "青跳❤",
    val autoClearMemorySubs: Boolean = false,
    val hideSnapshotStatusBar: Boolean = false,
    val enableDarkTheme: Boolean? = null,
    val enableDynamicColor: Boolean = true,
    val useSystemToast: Boolean = false,
    val appSort: Int = AppSort.ByUsedTime.value,
    val showBlockApp: Boolean = true,
    val appRuleSort: Int = RuleSort.ByDefault.value,
    val subsAppSort: Int = AppSort.ByUsedTime.value,
    val subsCategorySort: Int = AppSort.ByUsedTime.value,
    val subsAppGroupType: Int = AppGroupFlags.Installed,
    val subsCategoryGroupType: Int = AppGroupFlags.Installed,
    val subsAppShowBlock: Boolean = false,
    val subsCategoryShowBlock: Boolean = false,
    val subsExcludeSort: Int = AppSort.ByUsedTime.value,
    val subsExcludeShowBlockApp: Boolean = true,
    val appGroupType: Int = AppGroupFlags.Installed,
    val subsExcludeAppGroupType: Int = AppGroupFlags.Installed,
    val showDisabledRule: Boolean = true,
    val snapshotDisplayMode: Int = SnapshotDisplayMode.ByTime.value,
)
