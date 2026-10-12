package app.cyanskip.db

import androidx.room3.withWriteTransaction
import app.cyanskip.db.Db.database

/** Each write includes its retention step in the same database transaction. */
object RuntimeRecordStore {
    suspend fun writeVisits(records: List<AppLastVisit>, trim: Boolean) {
        database.withWriteTransaction {
            database.appLastVisitDao().insert(*records.toTypedArray())
            if (trim) database.appLastVisitDao().deleteKeepLatest()
        }
    }

    suspend fun writeAction(record: ActionLog, trim: Boolean) {
        database.withWriteTransaction {
            database.actionLogDao().insert(record)
            if (trim) database.actionLogDao().deleteKeepLatest()
        }
    }
}
