package app.cyanskip.app.rule

import app.cyanskip.app.model.ExcludeData
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.db.SubsGroupConfig
import app.cyanskip.db.SubsGlobalGroupConfig
import app.cyanskip.db.SubsItem

sealed class ResolvedGroup(
    open val group: RawSubscription.RawGroupProps,
    val subscription: RawSubscription,
    val subsItem: SubsItem,
    val config: SubsGroupConfig?,
) {
    val excludeData by lazy { ExcludeData.parse(config?.exclude) }

    abstract val appId: String?
}

class ResolvedAppGroup(
    override val group: RawSubscription.RawAppGroup,
    subscription: RawSubscription,
    subsItem: SubsItem,
    config: SubsGroupConfig?,
    val app: RawSubscription.RawApp,
    val enable: Boolean,
) : ResolvedGroup(group, subscription, subsItem, config) {
    override val appId: String?
        get() = app.id
}

class ResolvedGlobalGroup(
    override val group: RawSubscription.RawGlobalGroup,
    subscription: RawSubscription,
    subsItem: SubsItem,
    config: SubsGroupConfig?,
) : ResolvedGroup(group, subscription, subsItem, config) {
    override val appId: String?
        get() = null

    val matchAnyApp = (config as? SubsGlobalGroupConfig)?.matchAnyApp

    val groupExcludeAppIds
        get() = subscription.globalGroupAppGroupNameDisableMap[group.key] ?: emptySet()
}
