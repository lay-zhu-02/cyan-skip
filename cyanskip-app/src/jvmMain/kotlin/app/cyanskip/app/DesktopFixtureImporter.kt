package app.cyanskip.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import app.cyanskip.app.snapshot.SnapshotFileLayout
import app.cyanskip.db.ActionLog
import app.cyanskip.db.Db
import app.cyanskip.db.Snapshot
import java.io.File

/** Explicit development import; fixture ids must be stable so reopening is idempotent. */
@Serializable
data class DesktopFixtures(
    val actionLogs: List<ActionLog> = emptyList(),
    val snapshots: List<Snapshot> = emptyList(),
)

private val fixtureJson = Json { ignoreUnknownKeys = true }

suspend fun importDesktopFixtures(source: File, snapshotDirectory: File, marker: File) {
    if (marker.exists()) return
    val fixtures =
        fixtureJson.decodeFromString<DesktopFixtures>(source.readText())
    val imagesRoot = source.parentFile.resolve("snapshot").canonicalFile
    val layout = SnapshotFileLayout(snapshotDirectory)
    // Copy complete per-snapshot directories before committing indices; retry can replace these files.
    fixtures.snapshots.forEach { snapshot ->
        val sourceDir = imagesRoot.resolve(snapshot.id.toString()).canonicalFile
        require(
            sourceDir.toPath().startsWith(imagesRoot.toPath()) && sourceDir.isDirectory
        ) { "Missing snapshot ${snapshot.id}" }
        sourceDir.copyRecursively(layout.committed(snapshot.id).directory, overwrite = true)
    }
    Db.withTransaction {
        if (fixtures.actionLogs.isNotEmpty()) Db.actionLogDao.insert(*fixtures.actionLogs.toTypedArray())
        if (fixtures.snapshots.isNotEmpty()) Db.snapshotDao.insert(*fixtures.snapshots.toTypedArray())
    }
    marker.writeText(source.canonicalPath)
}
