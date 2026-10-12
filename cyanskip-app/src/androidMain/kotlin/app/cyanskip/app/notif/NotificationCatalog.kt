package app.cyanskip.app.notif

import android.app.Service
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.external_call_notification_description
import app.cyanskip.app.resources.external_call_processing
import app.cyanskip.app.resources.screenshot_capture_enabled
import app.cyanskip.app.resources.screenshot_capture_notification_description
import app.cyanskip.app.resources.snapshot_button_enabled
import app.cyanskip.app.resources.snapshot_button_notification_description
import app.cyanskip.app.resources.snapshot_saved_app
import app.cyanskip.app.service.ButtonService
import app.cyanskip.app.service.ScreenshotService
import app.cyanskip.app.snapshot.SnapshotScreenshotStatus
import app.cyanskip.app.snapshot.detailText
import app.cyanskip.app.ui.text.getSync
import kotlin.reflect.KClass

enum class ForegroundNotificationKey(
    val id: Int,
    val channel: AppNotificationChannel = AppNotificationChannel.Service,
) {
    Screenshot(id = 101),
    Button(id = 102),
    Expose(id = 104),
}

enum class PostedNotificationKey(
    val id: Int,
    val channel: AppNotificationChannel,
) {
    SnapshotSaved(id = 105, channel = AppNotificationChannel.Snapshot),
}

sealed interface AppNotificationSpec {
    val id: Int
    val channel: AppNotificationChannel
    val title: String
    val text: String?
    val uri: String?
    val ongoing: Boolean
    val autoCancel: Boolean
    val stopService: KClass<out Service>?
}

data class ForegroundNotification(
    val key: ForegroundNotificationKey,
    override val title: String,
    override val text: String? = null,
    override val uri: String? = null,
    override val stopService: KClass<out Service>? = null,
) : AppNotificationSpec {
    override val id: Int
        get() = key.id
    override val channel: AppNotificationChannel
        get() = key.channel
    override val ongoing = true
    override val autoCancel = false

    context(service: Service)
    fun startForeground() = NotificationDispatcher.startForeground(service, this)
}

data class PostedNotification(
    val key: PostedNotificationKey,
    override val title: String,
    override val text: String? = null,
    override val uri: String? = null,
    override val ongoing: Boolean = false,
    override val autoCancel: Boolean = true,
) : AppNotificationSpec {
    override val id: Int
        get() = key.id
    override val channel: AppNotificationChannel
        get() = key.channel
    override val stopService: KClass<out Service>? = null

    fun post() {
        NotificationDispatcher.post(this)
    }
}

object NotificationCatalog {
    fun screenshot() = ForegroundNotification(
        key = ForegroundNotificationKey.Screenshot,
        title = Res.string.screenshot_capture_enabled.getSync(),
        text = Res.string.screenshot_capture_notification_description.getSync(),
        uri = "cyanskip://page/1",
        stopService = ScreenshotService::class,
    )

    fun button() = ForegroundNotification(
        key = ForegroundNotificationKey.Button,
        title = Res.string.snapshot_button_enabled.getSync(),
        text = Res.string.snapshot_button_notification_description.getSync(),
        uri = "cyanskip://page/1",
        stopService = ButtonService::class,
    )

    fun expose() = ForegroundNotification(
        key = ForegroundNotificationKey.Expose,
        title = Res.string.external_call_processing.getSync(),
        text = Res.string.external_call_notification_description.getSync(),
    )

    fun snapshotSaved(
        appName: String,
        activityId: String?,
        screenshotStatus: SnapshotScreenshotStatus,
    ) = PostedNotification(
        key = PostedNotificationKey.SnapshotSaved,
        title = Res.string.snapshot_saved_app.getSync(appName),
        text = buildList {
            activityId?.let(::add)
            screenshotStatus.detailText()?.let(::add)
        }.joinToString(separator = " · ").takeIf { it.isNotEmpty() },
        uri = "cyanskip://page/2",
    )

}
