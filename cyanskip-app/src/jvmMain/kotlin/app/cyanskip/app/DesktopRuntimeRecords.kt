package app.cyanskip.app

import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import app.cyanskip.app.record.ActionRecordInput
import app.cyanskip.app.record.AppVisitInput
import app.cyanskip.db.ActionLog
import app.cyanskip.db.Db

/** Explicit synthetic event input, never an Accessibility action or fixture import. */
@Serializable
data class DesktopRuntimeRecordRequest(
    val visits: List<AppVisitInput> = emptyList(),
    val actions: List<ActionRecordInput> = emptyList(),
)

@Serializable
data class DesktopRuntimeRecordSnapshot(
    val actionCount: Int,
    val visits: List<String>,
    val latest: ActionLog?,
)

suspend fun DesktopRuntime.replayRecords(request: DesktopRuntimeRecordRequest): DesktopRuntimeRecordSnapshot {
    require(DesktopStorage.isolated) { "Runtime record replay requires --test" }
    require(request.visits.size + request.actions.size <= 2000)
    request.visits.forEach { app.cyanskip.app.record.RuntimeRecordRepository.recordVisit(it) }
    request.actions.forEach { app.cyanskip.app.record.RuntimeRecordRepository.recordAction(it) }
    return DesktopRuntimeRecordSnapshot(
        Db.actionLogDao.count().first(),
        Db.appLastVisitDao.query().first(), Db.actionLogDao.queryLatest().first(),
    )
}
