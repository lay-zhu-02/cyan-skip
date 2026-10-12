package app.cyanskip.app.rulegen

import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.SubscriptionEditor
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.db.Db
import app.cyanskip.db.LOCAL_SUBS_ID

/** 规则触发后执行的动作. */
enum class RuleDraftAction(val value: String) {
    Click("click"),
    LongClick("longClick"),
}

/** 待保存的规则; 来源是快照上验证过的选择器. */
data class SnapshotRuleDraft(
    val appId: String,
    val appName: String?,
    val activityId: String?,
    val scopeThisPage: Boolean,
    val action: RuleDraftAction,
    val source: String,
    val name: String,
)

/** 把快照生成的规则写入本地规则. */
object SnapshotRuleWriter {
    suspend fun save(draft: SnapshotRuleDraft) {
        SubscriptionRepository.update(LOCAL_SUBS_ID) { subscription ->
            val editor = SubscriptionEditor(subscription)
            val app = subscription.getApp(draft.appId, draft.appName)
            val groupName = availableGroupName(app.groups, draft.name)
            val group = RawSubscription.RawAppGroup(
                key = 0,
                name = groupName,
                desc = null,
                enable = null,
                scopeKeys = null,
                actionCdKey = null,
                actionMaximumKey = null,
                actionCd = null,
                actionDelay = null,
                fastQuery = null,
                matchRoot = null,
                actionMaximum = null,
                priorityTime = null,
                priorityActionMaximum = null,
                order = null,
                forcedTime = null,
                matchDelay = null,
                matchTime = null,
                resetMatch = null,
                snapshotUrls = null,
                excludeSnapshotUrls = null,
                exampleUrls = null,
                activityIds = draft.activityId
                    ?.takeIf { draft.scopeThisPage && it.isNotBlank() }
                    ?.let { listOf(it) },
                excludeActivityIds = null,
                rules = listOf(
                    RawSubscription.RawAppRule(
                        key = 0,
                        name = draft.name,
                        preKeys = null,
                        action = draft.action.value,
                        position = null,
                        swipeArg = null,
                        matches = listOf(draft.source),
                        excludeMatches = null,
                        excludeAllMatches = null,
                        anyMatches = null,
                        actionCdKey = null,
                        actionMaximumKey = null,
                        actionCd = null,
                        actionDelay = null,
                        fastQuery = null,
                        matchRoot = null,
                        actionMaximum = null,
                        priorityTime = null,
                        priorityActionMaximum = null,
                        order = null,
                        forcedTime = null,
                        matchDelay = null,
                        matchTime = null,
                        resetMatch = null,
                        snapshotUrls = null,
                        excludeSnapshotUrls = null,
                        exampleUrls = null,
                        activityIds = null,
                        excludeActivityIds = null,
                        versionCode = null,
                        versionName = null,
                    ),
                ),
                versionCode = null,
                versionName = null,
                ignoreGlobalGroupMatch = null,
            )
            editor.appendAppGroups(app, listOf(group))
            editor.build()
        }
    }

    suspend fun localSubscriptionEnabled(): Boolean =
        Db.subsItemDao.queryAll().any { it.id == LOCAL_SUBS_ID && it.enable }

    suspend fun enableLocalSubscription() {
        SubscriptionRepository.requestEnabled(LOCAL_SUBS_ID, true)
    }
}

private fun availableGroupName(
    groups: List<RawSubscription.RawAppGroup>,
    name: String,
): String {
    val used = groups.mapTo(mutableSetOf()) { it.name }
    if (used.add(name)) return name
    var index = 2
    while (!used.add("$name ($index)")) {
        index++
    }
    return "$name ($index)"
}
