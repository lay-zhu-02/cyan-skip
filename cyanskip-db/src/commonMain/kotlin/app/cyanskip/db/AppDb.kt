package app.cyanskip.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    version = 1,
    entities = [
        SubsItem::class,
        Snapshot::class,
        SubsAppGroupConfig::class,
        SubsGlobalGroupConfig::class,
        SubsCategoryConfig::class,
        ActionLog::class,
        SubsAppConfig::class,
        AppLastVisit::class,
    ],
)
@ConstructedBy(AppDbConstructor::class)
abstract class AppDb : RoomDatabase() {
    abstract fun subsItemDao(): SubsItem.SubsItemDao
    abstract fun snapshotDao(): Snapshot.SnapshotDao
    abstract fun subsAppGroupConfigDao(): SubsAppGroupConfig.SubsAppGroupConfigDao
    abstract fun subsGlobalGroupConfigDao(): SubsGlobalGroupConfig.SubsGlobalGroupConfigDao
    abstract fun subsAppConfigDao(): SubsAppConfig.SubsAppConfigDao
    abstract fun subsCategoryConfigDao(): SubsCategoryConfig.SubsCategoryConfigDao
    abstract fun actionLogDao(): ActionLog.ActionLogDao
    abstract fun appLastVisitDao(): AppLastVisit.AppLastVisitDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object AppDbConstructor : RoomDatabaseConstructor<AppDb> {
    override fun initialize(): AppDb
}
