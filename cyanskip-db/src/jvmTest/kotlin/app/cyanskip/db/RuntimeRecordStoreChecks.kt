package app.cyanskip.db

import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class RuntimeRecordStoreChecks {
    private fun database(block: suspend (AppDb, RuntimeRecordStore, (String) -> Unit) -> Unit) = runBlocking {
        DatabaseStoreTestSession.clear()
        block(Db.database, RuntimeRecordStore, DatabaseStoreTestSession::execute)
    }

    private fun action(time: Long) = ActionLog(
        ctime = time, appId = "app", subsId = 7, subsVersion = 1,
        groupKey = 2, groupType = RuleGroupType.App, ruleIndex = 0,
    )

    fun secondVisitFailureRollsBackFirstVisitAndRetryKeepsNewestValue() = database { db, store, sql ->
        store.writeVisits(listOf(AppLastVisit("old", 1)), false)
        sql("CREATE TRIGGER fail_visit BEFORE INSERT ON app_last_visit WHEN NEW.app_id = 'new' BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        assertFails { store.writeVisits(listOf(AppLastVisit("old", 10), AppLastVisit("new", 11)), false) }
        store.writeVisits(listOf(AppLastVisit("middle", 5)), false)
        assertEquals(listOf("middle", "old"), db.appLastVisitDao().query().first())
        sql("DROP TRIGGER fail_visit")
        store.writeVisits(listOf(AppLastVisit("old", 10), AppLastVisit("new", 11)), false)
        assertEquals(listOf("new", "old", "middle"), db.appLastVisitDao().query().first())
        store.writeVisits(listOf(AppLastVisit("new", 20), AppLastVisit("new", 21)), false)
        assertEquals(listOf("new", "old", "middle"), db.appLastVisitDao().query().first())
    }

    fun actionTrimFailureRollsBackInsertAndRetryDoesNotDuplicate() = database { db, store, sql ->
        db.actionLogDao().insert(*(0L until 501).map(::action).toTypedArray())
        sql("CREATE TRIGGER fail_trim BEFORE DELETE ON action_log BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        assertFails { store.writeAction(action(999), true) }
        assertEquals(501, db.actionLogDao().count().first())
        assertEquals(500, db.actionLogDao().queryLatest().first()!!.ctime)
        sql("DROP TRIGGER fail_trim")
        store.writeAction(action(999), true)
        assertEquals(500, db.actionLogDao().count().first())
        assertEquals(1, db.actionLogDao().query().first().count { it.ctime == 999L })
    }

    fun timestampRetentionPreservesExistingTieBoundaryWhileActionsUseIds() = database { db, store, _ ->
        // Persisted log retention uses <= the 501st timestamp, so ties at that boundary are removed.
        store.writeVisits(List(501) { AppLastVisit("app.$it", 10) }, true)
        assertEquals(emptyList(), db.appLastVisitDao().query().first())
        db.actionLogDao().insert(*List(501) { action(10) }.toTypedArray())
        store.writeAction(action(10), true)
        assertEquals(500, db.actionLogDao().count().first())
    }
}
