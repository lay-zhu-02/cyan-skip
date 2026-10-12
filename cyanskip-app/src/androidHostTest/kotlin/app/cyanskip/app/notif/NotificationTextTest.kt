package app.cyanskip.app.notif

import android.app.Application
import app.cyanskip.app.snapshot.SnapshotScreenshotStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class NotificationTextTest {
    @Test
    fun snapshotNotificationKeepsSavedTitleWithoutDetails() {
        val notification = NotificationCatalog.snapshotSaved(
            appName = "Example",
            activityId = null,
            screenshotStatus = SnapshotScreenshotStatus.Captured,
        )
        assertEquals("快照已保存 · Example", notification.title)
        assertNull(notification.text)
    }

    @Test
    fun snapshotNotificationPreservesAppNameAndActivity() {
        val name = "50% %1\$s <App>"
        val notification = NotificationCatalog.snapshotSaved(
            appName = name,
            activityId = "example.MainActivity",
            screenshotStatus = SnapshotScreenshotStatus.Captured,
        )
        assertEquals("快照已保存 · $name", notification.title)
        assertEquals("example.MainActivity", notification.text)
    }
}
