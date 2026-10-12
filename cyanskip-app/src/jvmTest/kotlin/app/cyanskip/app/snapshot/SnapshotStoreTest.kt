package app.cyanskip.app.snapshot

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import app.cyanskip.app.storage.AppStorageLayout
import app.cyanskip.db.Db
import app.cyanskip.db.Snapshot
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnapshotStoreTest(private val layout: AppStorageLayout) {
    fun runAll() = runBlocking {
        val snapshot = Snapshot(1, "app.id", null, 1, 1, false)
        Db.snapshotDao.insert(snapshot)
        val directory = layout.snapshot.resolve("1").apply { mkdirs() }
        val screenshot = SnapshotFileLayout(layout.snapshot).committed(1).screenshotFile
            .apply {
                javax.imageio.ImageIO.write(
                    java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB),
                    "png", this,
                )
                check(setLastModified(10_000))
            }
        directory.resolve("1.json").writeText("{}")
        try {
            val first = SnapshotStore.createArchive(1, snapshot.appId)
            try {
                val second = SnapshotStore.createArchive(1, snapshot.appId)
                try {
                    assertEquals(first.name, second.name)
                    assertTrue(first.parentFile != second.parentFile)
                    assertTrue(first.isFile && second.isFile)
                    java.util.zip.ZipFile(second).use { zip ->
                        assertEquals("{}", zip.getInputStream(zip.getEntry("1.json")).reader().readText())
                    }
                } finally {
                    SnapshotStore.deleteArchive(second)
                }
                assertTrue(first.isFile)
            } finally {
                SnapshotStore.deleteArchive(first)
            }
            withSnapshotDbFailure(layout.database, "DELETE") {
                assertFails { SnapshotStore.delete(snapshot) }
            }
            assertTrue(screenshot.isFile)
            assertEquals(1, Db.snapshotDao.query().first().size)
            SnapshotStore.delete(snapshot)
            assertFalse(directory.exists())
            assertTrue(Db.snapshotDao.query().first().isEmpty())
            assertTrue(layout.snapshot.listFiles().orEmpty().isEmpty())
        } finally {
            Db.snapshotDao.query().first().forEach { SnapshotStore.delete(it) }
        }
    }
}

/** Fail a real database operation without a fake DAO or a production fault-injection hook. */
suspend fun withSnapshotDbFailure(database: File, event: String, block: suspend () -> Unit) {
    require(event in setOf("INSERT", "DELETE"))
    BundledSQLiteDriver().open(database.absolutePath).use { connection ->
        connection.prepare("CREATE TRIGGER test_snapshot_failure BEFORE $event ON snapshot BEGIN SELECT RAISE(FAIL, 'test database failure'); END")
            .use { it.step() }
        try {
            block()
        } finally {
            connection.prepare("DROP TRIGGER test_snapshot_failure").use { it.step() }
        }
    }
}
