package app.cyanskip.app

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopSimulatorStorageTest {


    @Test
    fun configurationSurvivesRestartWithoutRestoringImeOrOverwritingInvalidInput() {
        val directory = Files.createTempDirectory("cyanskip-simulator-test").toFile()
        try {
            val file = directory.resolve("simulator.json")
            val storage = DesktopSimulatorStorage(file)
            val original = SimulatorSettings().withEnvironment(
                DesktopEnvironment(
                    fontScale = 1.5f,
                    android = AndroidWindow(imeVisible = true, batteryPercent = 42)
                )
            )
            storage.save(original)
            val reopened = DesktopSimulatorStorage(file).load(DesktopEnvironment())
            assertEquals(1.5f, reopened.device.fontScale)
            assertEquals(100, reopened.device.batteryPercent)
            assertFalse(reopened.device.imeVisible)
            assertFailsWith<IllegalArgumentException> {
                storage.save(
                    original.copy(
                        device = original.device.copy(
                            width = 0
                        )
                    )
                )
            }
            assertEquals(reopened, storage.load(DesktopEnvironment()))
            file.writeText("invalid")
            assertFails { storage.load(DesktopEnvironment()) }
            assertEquals("invalid", file.readText())
        } finally {
            directory.deleteRecursively()
        }
    }
}
