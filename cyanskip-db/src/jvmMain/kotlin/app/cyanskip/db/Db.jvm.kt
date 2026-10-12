package app.cyanskip.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun Db.initialize(databasePath: String) {
    initialize {
        Room.databaseBuilder<AppDb>(databasePath, factory = AppDbConstructor::initialize)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
