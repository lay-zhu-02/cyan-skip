package app.cyanskip.app.ui.snapshot

import kotlinx.coroutines.CancellationException
import app.cyanskip.app.snapshot.SnapshotStore
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.app.util.LogUtils
import app.cyanskip.db.Db
import app.cyanskip.db.Snapshot

data class SnapshotDeleteResult(
    val deletedIds: Set<Long>,
    val failedCount: Int,
)

class SnapshotViewModel : BaseViewModel() {
    val uiState = Db.snapshotDao.query().stateLoadable()


    suspend fun deleteSnapshots(snapshots: List<Snapshot>): SnapshotDeleteResult {
        val deletedIds = mutableSetOf<Long>()
        var failedCount = 0
        snapshots.forEach { snapshot ->
            try {
                SnapshotStore.delete(snapshot)
                deletedIds.add(snapshot.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtils.d(e)
                failedCount++
            }
        }
        return SnapshotDeleteResult(deletedIds, failedCount)
    }

}
