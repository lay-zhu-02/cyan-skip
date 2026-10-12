package app.cyanskip.app.snapshot

import app.cyanskip.app.testing.TestFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SnapshotFileLayoutTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder(TestFiles.root)

    @Test
    fun incompleteOrCorruptScreenshotDoesNotMakeSnapshotComplete() {
        val files = SnapshotFileLayout(temporaryFolder.root).committed(123)
        files.directory.mkdirs()
        files.snapshotFile.writeText("{}")
        assertEquals(files.webpFile, files.screenshotFile)
        assertFalse(files.hasCompleteFiles)
        files.webpFile.writeText("invalid")
        assertFalse(files.hasCompleteFiles)
        files.webpFile.writeBytes(byteArrayOf(
            0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50,
        ))
        assertTrue(files.hasCompleteFiles)
        files.snapshotFile.delete()
        assertFalse(files.hasCompleteFiles)
    }
}
