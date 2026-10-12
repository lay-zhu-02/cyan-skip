package app.cyanskip.app.storage

import kotlinx.coroutines.test.runTest
import app.cyanskip.app.settings.FileSettingsStorage
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppStorageLayoutTest {
    @Test
    fun readsAndroidDirectoryCopyAndWritesBackWithoutFlatteningOrExposingPrivateData() = runTest {
        val root = Files.createTempDirectory("cyanskip-storage-layout").toFile()
        try {
            // Android external storage paths owned by this app.
            val external = root.resolve("external")
            val original = external.resolve("files/store/store.json")
            original.parentFile.mkdirs()
            original.writeText("{\"enableDarkTheme\":true}")
            val layout = AppStorageLayout(
                external.resolve("files"),
                external.resolve("cache"),
                root.resolve("private/files")
            )
            val settings = FileSettingsStorage(layout.store)
            assertEquals(original.readText(), settings.read("store.json"))
            settings.writeAtomically("store.json", "{\"enableDarkTheme\":false}")
            assertEquals("{\"enableDarkTheme\":false}", original.readText())
            layout.database.writeText("database fixture")
            assertTrue(external.resolve("files/db/cyanskip.db").isFile)
            layout.subscription.resolve("-2.json").writeText("{}")
            assertTrue(external.resolve("files/subscription/-2.json").isFile)
            FileSettingsStorage(layout.privateStore).writeAtomically(
                "github_cookie.txt",
                "test-only"
            )
            assertFalse(external.walkTopDown().any { it.name == "github_cookie.txt" })
            assertFalse(external.resolve("store.json").exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
