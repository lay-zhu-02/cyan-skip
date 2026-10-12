package app.cyanskip.app.settings

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import app.cyanskip.app.subscription.SubscriptionJson.json
import app.cyanskip.app.ui.settings.AppIdListEditor
import app.cyanskip.app.ui.settings.AppWhitelistViewModel
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Invoked by ApplicationIntegrationTest after its one real application startup. */
object SettingsRepositoryChecks {
    suspend fun run(directory: File) {
        val repo = SettingsRepository
        assertEquals("saved toast", repo.settings.value.actionToast)
        assertFalse(repo.settings.value.enableDynamicColor)
        assertEquals(listOf(12, 34), repo.overlayPositions.value["snapshot-button"])
        val original = repo.exportBackupEntries()
        fun diskSettings() =
            json.decodeFromString<SettingsStore>(directory.resolve("store.json").readText())

        val owner = ViewModelStore()
        try {
            repo.updateSettings {
                it.copy(
                    enableMatch = false,
                    httpServerPort = 9123,
                    snapshotDisplayMode = 2
                )
            }
            repo.awaitPersistence()
            assertEquals(repo.settings.value, diskSettings())
            val beforeCount = repo.actionCount.value
            coroutineScope { List(200) { async(Dispatchers.Default) { repo.incrementActionCount() } }.awaitAll() }
            repo.awaitPersistence()
            assertEquals(beforeCount + 200, repo.actionCount.value)
            assertEquals(
                repo.actionCount.value.toString(),
                directory.resolve("action_count.txt").readText()
            )

            repo.updateSettings { it.copy(enableMatch = true, httpServerPort = 8000) }
            repo.replaceBlockMatchAppList(setOf("local.app"))
            repo.awaitPersistence()
            val localCount = repo.actionCount.value
            assertFailsWith<IOException> {
                repo.withBackupRestore(
                    mapOf(
                        "store.json" to json.encodeToString(
                            repo.settings.value.copy(
                                enableMatch = false,
                                httpServerPort = 9000
                            )
                        ),
                        "action_count.txt" to "100", "block_match_app_list.txt" to "imported.app",
                    )
                ) {
                    coroutineScope {
                        launch(Dispatchers.Default) {
                            repo.updateSettings { it.copy(httpServerPort = 8123) }
                            repo.updateBlockMatchAppList { it + "later.app" }
                        }
                        repeat(20) { launch(Dispatchers.Default) { repo.incrementActionCount() } }
                    }
                    throw IOException("database commit failed")
                }
            }
            assertTrue(repo.settings.value.enableMatch)
            assertEquals(8123, repo.settings.value.httpServerPort)
            assertEquals(localCount + 20, repo.actionCount.value)
            assertEquals(setOf("local.app", "later.app"), repo.blockMatchAppList.value)
            assertEquals(repo.settings.value, diskSettings())
            assertEquals("committed", repo.withBackupRestore(mapOf("action_count.txt" to "100")) {
                repo.incrementActionCount(); "committed"
            })
            repo.awaitPersistence()
            assertEquals("101", directory.resolve("action_count.txt").readText())
            repo.withBackupRestore(mapOf("action_count.txt" to "200")) {}
            coroutineScope {
                val entered = CompletableDeferred<Unit>()
                val job = launch {
                    repo.withBackupRestore(mapOf("action_count.txt" to "100")) {
                        entered.complete(
                            Unit
                        ); awaitCancellation()
                    }
                }
                withTimeout(5_000) { entered.await() }
                repo.incrementActionCount()
                withTimeout(5_000) { job.cancelAndJoin() }
            }
            assertEquals(201L, repo.actionCount.value)
            assertEquals("201", directory.resolve("action_count.txt").readText())
            var databaseStarted = false
            withBlockedWrite(directory, "action_count.txt") {
                val error = assertFailsWith<SettingsWriteException> {
                    repo.withBackupRestore(mapOf("action_count.txt" to "100")) {
                        databaseStarted = true
                    }
                }
                assertIs<IOException>(error.cause)
            }
            assertFalse(databaseStarted)
            assertEquals(201L, repo.actionCount.value)
            repo.incrementActionCount()
            repo.awaitPersistence()
            assertEquals("202", directory.resolve("action_count.txt").readText())

            assertEquals(8123, repo.settings.value.httpServerPort)
            coroutineScope {
                repeat(30) { i ->
                    launch(Dispatchers.Default) {
                        repo.updateOverlayPosition(
                            "overlay-$i",
                            i,
                            -i
                        )
                    }
                }
            }
            repo.awaitPersistence()
            val positions = json.decodeFromString<Map<String, List<Int>>>(
                directory.resolve("overlay_position.json").readText()
            )
            repeat(30) { i -> assertEquals(listOf(i, -i), positions["overlay-$i"]) }
            assertEquals(listOf(12, 34), positions["snapshot-button"])
            val oldPositions = directory.resolve("overlay_position.json").readText()
            withBlockedWrite(directory, "overlay_position.json") {
                repo.updateOverlayPosition("overlay", 3, 4)
                assertEquals(
                    "overlay_position.json",
                    assertFailsWith<SettingsWriteException> { repo.awaitPersistence() }.filename
                )
                assertEquals(oldPositions, directory.resolve("overlay_position.json").readText())
            }
            repo.updateOverlayPosition("overlay", 3, 4)
            repo.awaitPersistence()
            assertFailsWith<IOException> {
                repo.withBackupRestore(mapOf("store.json" to """{"enableMatch":false}""")) {
                    repo.updateOverlayPosition("overlay", 5, 6)
                    throw IOException("restore failed")
                }
            }
            repo.awaitPersistence()
            assertTrue(repo.settings.value.enableMatch)
            assertEquals(listOf(5, 6), repo.overlayPositions.value["overlay"])

            val settings = SettingsRepository
            val beforeSave = directory.resolve("store.json").readText()
            withBlockedWrite(directory, "store.json") {
                assertFailsWith<SettingsWriteException> {
                    settings.saveActionToast(
                        true,
                        false,
                        "My draft"
                    )
                }
                assertEquals(beforeSave, directory.resolve("store.json").readText())
            }
            settings.saveActionToast(true, false, "My draft")
            assertEquals("My draft", diskSettings().actionToast)
            assertFalse(diskSettings().enableDynamicColor)
            val beforeInvalid = repo.settings.value
            assertFalse(repo.saveHttpServerPort("65536"))
            assertFalse(repo.saveHttpServerPort("text"))
            assertEquals(beforeInvalid, repo.settings.value)
            assertTrue(repo.saveHttpServerPort("17333"))
            repo.updateSettings {
                it.copy(hideSnapshotStatusBar = true)
            }
            repo.awaitPersistence()
            assertEquals(17333, diskSettings().httpServerPort)
            assertTrue(diskSettings().hideSnapshotStatusBar)

            val whitelist = AppWhitelistViewModel().also { owner.put("whitelist", it) }
            withBlockedWrite(directory, "block_match_app_list.txt") {
                assertFailsWith<SettingsWriteException> { whitelist.saveChanges("test.alpha\n# Alpha\ntest.alpha\ninvalid") }
                assertTrue(whitelist.hasChanges("test.alpha"))
            }
            assertTrue(whitelist.saveChanges("test.alpha"))
            assertFalse(whitelist.hasChanges("test.alpha"))
            assertFalse(whitelist.saveChanges("test.alpha"))
            assertEquals("test.alpha", directory.resolve("block_match_app_list.txt").readText())
            assertFalse(repo.termsAccepted.value)
            withBlockedWrite(directory, "terms_accepted.txt") {
                assertFailsWith<IOException> { repo.acceptTerms() }
                assertFalse(repo.termsAccepted.value)
            }
            repo.acceptTerms()
            repo.acceptTerms()
            assertTrue(repo.termsAccepted.value)
            assertEquals("true", directory.resolve("terms_accepted.txt").readText())
        } finally {
            owner.clear()
            repo.withBackupRestore(original) {}
            repo.awaitPersistence()
        }
    }

    // A real nonempty directory prevents atomic writes; production needs no fault-injection API.
    private suspend fun withBlockedWrite(
        directory: File,
        filename: String,
        block: suspend () -> Unit
    ) {
        SettingsRepository.awaitPersistence()
        val obstruction = directory.resolve("$filename.tmp")
        check(!obstruction.exists())
        check(obstruction.mkdir())
        val marker = obstruction.resolve("blocker").apply { writeText("test") }
        try {
            block()
        } finally {
            check(marker.delete()); check(obstruction.delete())
        }
    }
}
