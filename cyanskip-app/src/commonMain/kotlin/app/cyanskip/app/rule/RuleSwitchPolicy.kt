package app.cyanskip.app.rule

import app.cyanskip.app.model.ExcludeData
import app.cyanskip.db.SubsGroupConfig
import app.cyanskip.db.withEnable
import app.cyanskip.db.withExclude

object RuleSwitchPolicy {
    fun updateGroup(
        target: RuleSwitchTarget,
        current: SubsGroupConfig,
        setting: RuleSetting
    ): SubsGroupConfig =
        when (target) {
            is RuleSwitchTarget.App -> error("应用总开关不属于规则组配置")
            is RuleSwitchTarget.AppGroup, is RuleSwitchTarget.GlobalGroup -> current.withEnable(
                setting.value
            )

            is RuleSwitchTarget.GlobalApp -> {
                val exclude = ExcludeData.parse(current.exclude)
                current.withExclude(exclude.copy(appIds = exclude.appIds.toMutableMap().apply {
                    val value = setting.value
                    if (value == null) remove(target.appId) else set(target.appId, !value)
                }).stringify())
            }
        }
}
