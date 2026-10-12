package app.cyanskip.app.snapshot

import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.screenshot_frame_missing
import app.cyanskip.app.resources.screenshot_may_be_protected
import app.cyanskip.app.ui.text.getSync

fun SnapshotScreenshotStatus.detailText(): String? = when (this) {
    SnapshotScreenshotStatus.Captured -> null
    SnapshotScreenshotStatus.Unavailable -> Res.string.screenshot_frame_missing.getSync()
    SnapshotScreenshotStatus.LikelyProtected -> Res.string.screenshot_may_be_protected.getSync()
}
