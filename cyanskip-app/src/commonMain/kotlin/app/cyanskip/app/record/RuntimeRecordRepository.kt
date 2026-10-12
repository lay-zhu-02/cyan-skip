package app.cyanskip.app.record

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import app.cyanskip.app.app.applicationId
import app.cyanskip.app.app.launcherAppIdFlow
import app.cyanskip.app.util.Constants
import app.cyanskip.db.ActionLog
import app.cyanskip.db.AppLastVisit
import app.cyanskip.db.RuntimeRecordStore

/** An application transition captured at the event boundary. */
@Serializable
data class AppVisitInput(
    val oldAppId: String,
    val newAppId: String,
    val time: Long,
)

@Serializable
data class ActionRecordInput(
    val appId: String,
    val activityId: String?,
    val subsId: Long,
    val subsVersion: Int,
    val groupKey: Int,
    val groupType: Int,
    val ruleIndex: Int,
    val ruleKey: Int?,
    val time: Long,
)

/** Application-owned writer for visits and rule actions. */
object RuntimeRecordRepository {
    private const val TRIM_INTERVAL = 100L

    private val writer = Mutex()
    private var visitCount = 0L
    private var actionCount = 0L

    suspend fun recordVisit(input: AppVisitInput) {
        val currentLauncherAppId = launcherAppIdFlow.value
        fun weighted(appId: String, time: Long) = when (appId) {
            applicationId -> time - 120_000
            currentLauncherAppId, Constants.systemUiAppId -> time - 60_000
            else -> time
        }

        val records = listOf(
            AppLastVisit(input.oldAppId, weighted(input.oldAppId, input.time - 1)),
            AppLastVisit(input.newAppId, weighted(input.newAppId, input.time)),
        )
        writer.withLock {
            withContext(NonCancellable) {
                RuntimeRecordStore.writeVisits(records, visitCount % TRIM_INTERVAL == 0L)
                visitCount++
            }
        }
    }

    suspend fun recordAction(input: ActionRecordInput) {
        val record = ActionLog(
            appId = input.appId, activityId = input.activityId,
            subsId = input.subsId, subsVersion = input.subsVersion,
            groupKey = input.groupKey, groupType = input.groupType,
            ruleIndex = input.ruleIndex, ruleKey = input.ruleKey, ctime = input.time,
        )
        writer.withLock {
            withContext(NonCancellable) {
                RuntimeRecordStore.writeAction(record, (actionCount + 1) % TRIM_INTERVAL == 0L)
                actionCount++
            }
        }
    }
}
