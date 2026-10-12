package app.cyanskip.app.ui.home

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import app.cyanskip.app.state.Loadable
import app.cyanskip.db.ActionLog
import app.cyanskip.db.Db

data class LatestActionSnapshot(val record: ActionLog?)

object HomeDataSources {
    fun observeLatest(): Flow<Loadable<LatestActionSnapshot>> = Db.actionLogDao.queryLatest()
        .map<ActionLog?, Loadable<LatestActionSnapshot>> { Loadable.Ready(LatestActionSnapshot(it)) }
        .catch { emit(Loadable.Failure(it)) }

}
