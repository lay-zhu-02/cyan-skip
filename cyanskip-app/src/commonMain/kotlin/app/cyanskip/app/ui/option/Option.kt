package app.cyanskip.app.ui.option

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.cyanskip.app.app.AppGroupFlags
import app.cyanskip.app.app.AppSort
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_pause
import app.cyanskip.app.resources.app_sort_name
import app.cyanskip.app.resources.app_sort_recent_trigger
import app.cyanskip.app.resources.app_sort_recent_used
import app.cyanskip.app.resources.apps_not_installed
import app.cyanskip.app.resources.apps_system
import app.cyanskip.app.resources.apps_user
import app.cyanskip.app.resources.category_follow_subscription
import app.cyanskip.app.resources.rule_sort_default
import app.cyanskip.app.resources.rule_sort_name
import app.cyanskip.app.resources.rules_enable_all
import app.cyanskip.app.resources.settings_all_off
import app.cyanskip.app.resources.snapshot_view_app
import app.cyanskip.app.resources.snapshot_view_time
import app.cyanskip.app.resources.theme_auto
import app.cyanskip.app.resources.theme_dark
import app.cyanskip.app.resources.theme_light
import app.cyanskip.app.resources.update_interval_daily
import app.cyanskip.app.resources.update_interval_three_days
import app.cyanskip.app.resources.update_interval_weekly
import app.cyanskip.app.rule.RuleSort
import app.cyanskip.app.settings.SubscriptionUpdateInterval
import app.cyanskip.app.snapshot.SnapshotDisplayMode
import app.cyanskip.app.ui.component.CsIcons
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

sealed interface Option<T> {
    val value: T
    val labelKey: StringResource
    val label: String
        @Composable get() = stringResource(labelKey)
    val options: List<Option<T>>
}

sealed interface OptionIcon {
    val icon: ImageVector
}

fun <V, T : Option<V>> Iterable<T>.findOption(value: V): T {
    return find { it.value == value } ?: first()
}

sealed class SnapshotDisplayModeOption(
    override val value: Int,
    override val labelKey: StringResource
) : Option<Int> {
    override val options get() = objects

    data object ByTime :
        SnapshotDisplayModeOption(SnapshotDisplayMode.ByTime.value, Res.string.snapshot_view_time)

    data object ByApp :
        SnapshotDisplayModeOption(SnapshotDisplayMode.ByApp.value, Res.string.snapshot_view_app)

    companion object {
        val objects by lazy { listOf(ByTime, ByApp) }
    }
}

sealed class AppSortOption(override val value: Int, override val labelKey: StringResource) :
    Option<Int> {
    override val options get() = objects

    data object ByAppName : AppSortOption(AppSort.ByAppName.value, Res.string.app_sort_name)
    data object ByActionTime :
        AppSortOption(AppSort.ByActionTime.value, Res.string.app_sort_recent_trigger)

    data object ByUsedTime :
        AppSortOption(AppSort.ByUsedTime.value, Res.string.app_sort_recent_used)

    companion object {
        val objects by lazy { listOf(ByAppName, ByUsedTime, ByActionTime) }
    }
}

sealed class UpdateTimeOption(
    override val value: Long,
    override val labelKey: StringResource
) : Option<Long> {
    override val options get() = objects

    data object Pause : UpdateTimeOption(SubscriptionUpdateInterval.Paused, Res.string.action_pause)
    data object Everyday :
        UpdateTimeOption(SubscriptionUpdateInterval.Daily, Res.string.update_interval_daily)

    data object Every3Days : UpdateTimeOption(
        SubscriptionUpdateInterval.EveryThreeDays,
        Res.string.update_interval_three_days
    )

    data object Every7Days :
        UpdateTimeOption(SubscriptionUpdateInterval.Weekly, Res.string.update_interval_weekly)

    companion object {
        val objects by lazy { listOf(Pause, Everyday, Every3Days, Every7Days) }
    }
}

sealed class DarkThemeOption(
    override val value: Boolean?,
    override val labelKey: StringResource,
    override val icon: ImageVector
) : Option<Boolean?>, OptionIcon {
    override val options get() = objects

    data object FollowSystem : DarkThemeOption(null, Res.string.theme_auto, CsIcons.BrightnessAuto)
    data object AlwaysEnable : DarkThemeOption(true, Res.string.theme_dark, CsIcons.DarkMode)
    data object AlwaysDisable : DarkThemeOption(false, Res.string.theme_light, CsIcons.LightMode)

    companion object {
        val objects by lazy { listOf(FollowSystem, AlwaysDisable, AlwaysEnable) }
    }
}

sealed class EnableGroupOption(
    override val value: Boolean?,
    override val labelKey: StringResource
) : Option<Boolean?> {
    override val options get() = objects

    data object FollowSubs : EnableGroupOption(null, Res.string.category_follow_subscription)
    data object AllEnable : EnableGroupOption(true, Res.string.rules_enable_all)
    data object AllDisable : EnableGroupOption(false, Res.string.settings_all_off)

    companion object {
        val objects by lazy { listOf(FollowSubs, AllEnable, AllDisable) }
    }
}

sealed class RuleSortOption(override val value: Int, override val labelKey: StringResource) :
    Option<Int> {
    override val options get() = objects

    data object ByDefault : RuleSortOption(RuleSort.ByDefault.value, Res.string.rule_sort_default)
    data object ByActionTime :
        RuleSortOption(RuleSort.ByActionTime.value, Res.string.app_sort_recent_trigger)

    data object ByRuleName : RuleSortOption(RuleSort.ByRuleName.value, Res.string.rule_sort_name)

    companion object {
        val objects by lazy { listOf(ByDefault, ByActionTime, ByRuleName) }
    }
}

sealed interface BinaryOption : Option<Int> {
    fun include(flag: Int): Boolean = (value and flag) != 0
    fun invert(flag: Int): Int = value xor flag

    companion object {
        fun combine(options: Collection<BinaryOption>): Int {
            return options.fold(0) { a, b -> a or b.value }
        }
    }
}

sealed class AppGroupOption(
    override val value: Int,
    override val labelKey: StringResource
) : BinaryOption {
    override val options get() = allObjects

    data object SystemGroup : AppGroupOption(AppGroupFlags.System, Res.string.apps_system)
    data object UserGroup : AppGroupOption(AppGroupFlags.User, Res.string.apps_user)
    data object UnInstalledGroup :
        AppGroupOption(AppGroupFlags.Uninstalled, Res.string.apps_not_installed)

    companion object {
        val normalObjects by lazy { listOf(SystemGroup, UserGroup) }
        val allObjects by lazy { listOf(SystemGroup, UserGroup, UnInstalledGroup) }
    }
}
