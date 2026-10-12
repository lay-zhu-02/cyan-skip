package app.cyanskip.app.snapshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.set
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import app.cyanskip.app.a11y.A11yRuntime
import app.cyanskip.app.a11y.currentTopActivity
import app.cyanskip.app.app.ActivityNames.getShowActivityId
import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.data.appinfo.PackageAppCatalog.selfAppInfo
import app.cyanskip.app.data.captureNodeInfoList
import app.cyanskip.app.data.currentDeviceInfo
import app.cyanskip.app.model.ComplexSnapshot
import app.cyanskip.app.model.RpcError
import app.cyanskip.app.notif.NotificationCatalog
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.screenshot_frame_missing
import app.cyanskip.app.resources.screenshot_replace_manually
import app.cyanskip.app.resources.service_unavailable_authorize
import app.cyanskip.app.resources.snapshot_app_a11y_missing
import app.cyanskip.app.resources.snapshot_save_in_progress
import app.cyanskip.app.resources.snapshot_saved
import app.cyanskip.app.resources.snapshot_saved_warning
import app.cyanskip.app.service.ScreenshotService
import app.cyanskip.app.settings.SettingsRepository.settings
import app.cyanskip.app.snapshot.platform.encodeSnapshotScreenshot
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.AndroidTarget
import app.cyanskip.app.util.BarUtils
import app.cyanskip.app.util.LogUtils
import app.cyanskip.app.util.ScreenUtils
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.app.util.px
import org.jetbrains.compose.resources.getString
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

object SnapshotCaptureHost {
    val isCapturing: Boolean get() = SnapshotCaptureRepository.isCapturing

    private class ScreenResult(
        val bytes: ByteArray,
        val status: SnapshotScreenshotStatus,
    )

    private fun createMissingScreenshotBitmap(): Bitmap {
        val bitmap = createBitmap(ScreenUtils.getWidth(), ScreenUtils.getHeight())
        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 32.sp.px
                color = Color.BLUE
                textAlign = Paint.Align.CENTER
            }
            val canvas = Canvas(bitmap)
            val lines = listOf(
                Res.string.screenshot_frame_missing.getSync(),
                Res.string.screenshot_replace_manually.getSync()
            )
            lines.forEachIndexed { index, line ->
                canvas.drawText(
                    line,
                    bitmap.width / 2f,
                    (bitmap.height / 2f) +
                            (index - lines.size / 2f) * (paint.textSize + 4.sp.px),
                    paint,
                )
            }
            return bitmap
        } catch (e: Throwable) {
            bitmap.recycle()
            throw e
        }
    }



    private fun cropStatusBar(bitmap: Bitmap): Bitmap {
        val mutableBitmap = bitmap.run {
            if (!isMutable || config == Bitmap.Config.HARDWARE) {
                copy(Bitmap.Config.ARGB_8888, true)
            } else {
                this
            }
        }
        try {
            val barHeight = min(BarUtils.getStatusHeight(), mutableBitmap.height)
            for (x in 0 until mutableBitmap.width) {
                for (y in 0 until barHeight) {
                    mutableBitmap[x, y] = 0
                }
            }
            return mutableBitmap
        } catch (e: Throwable) {
            if (mutableBitmap !== bitmap) mutableBitmap.recycle()
            throw e
        }
    }

    private fun looksLikeBlankScreenshot(bitmap: Bitmap): Boolean {
        fun Bitmap.recycleIfTemporary() {
            if (this !== bitmap) recycle()
        }

        val size = 64
        val scaled = bitmap.scale(size, size, false)
        val softwareBitmap = if (scaled.config == Bitmap.Config.HARDWARE) {
            val copy = try {
                scaled.copy(Bitmap.Config.ARGB_8888, false)
            } finally {
                scaled.recycleIfTemporary()
            }
            copy ?: return false
        } else {
            scaled
        }
        val pixels = try {
            IntArray(size * size).also {
                softwareBitmap.getPixels(it, 0, size, 0, 0, size, size)
            }
        } finally {
            softwareBitmap.recycleIfTemporary()
        }
        val ignoredEdge = (size * 0.08).toInt()
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        var nearBlackCount = 0
        val step = 2
        for (y in ignoredEdge until size - ignoredEdge step step) {
            for (x in ignoredEdge until size - ignoredEdge step step) {
                val pixel = pixels[y * size + x]
                val red = (pixel shr 16) and 0xff
                val green = (pixel shr 8) and 0xff
                val blue = pixel and 0xff
                val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
                sum += luminance
                sumSq += luminance * luminance
                count++
                if (luminance < 10) nearBlackCount++
            }
        }
        if (count == 0) return false
        val mean = sum / count
        val variance = sumSq / count - mean * mean
        val blackRatio = nearBlackCount.toDouble() / count
        return variance < 15.0 && blackRatio > 0.85 && mean < 15.0
    }

    private suspend fun resolveActivityId(appId: String): String? {
        var topActivity = currentTopActivity
        var waited = 0L
        while (topActivity.appId != appId && waited < 2000) {
            delay(100.milliseconds)
            topActivity = currentTopActivity
            waited += 100
        }
        return topActivity.activityId.takeIf { topActivity.appId == appId }
    }



    private suspend fun captureScreen(
        appId: String,
        forcedCropStatusBar: Boolean,
        hideStatusBar: Boolean,
    ): ScreenResult {
        val a11yScreenshot = try {
            A11yRuntime.screenshot()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogUtils.d("无障碍截图失败", e)
            null
        }
        val rawPicture = a11yScreenshot ?: ScreenshotService.screenshot()
        var ownedBitmap = rawPicture
        try {
            val (bitmap, status) = when {

                rawPicture == null -> {
                    createMissingScreenshotBitmap() to SnapshotScreenshotStatus.Unavailable
                }

                looksLikeBlankScreenshot(rawPicture) -> {
                    val status = SnapshotScreenshotStatus.LikelyProtected
                    rawPicture to status
                }

                else -> {
                    rawPicture to SnapshotScreenshotStatus.Captured
                }
            }
            ownedBitmap = bitmap
            val processedBitmap = if (
                status == SnapshotScreenshotStatus.Captured &&
                hideStatusBar &&
                (forcedCropStatusBar || BarUtils.isStatusVisible() == true)
            ) {
                cropStatusBar(bitmap).also { cropped ->
                    if (cropped !== bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }
            ownedBitmap = processedBitmap
            return ScreenResult(processedBitmap.encodeSnapshotScreenshot(), status)
        } finally {
            ownedBitmap?.recycle()
        }
    }

    suspend fun capture(forcedCropStatusBar: Boolean = false): ComplexSnapshot {
        val service = A11yRuntime.service
            ?: throw RpcError(getString(Res.string.service_unavailable_authorize))
        val settings = settings.value
        val captured = try {
            SnapshotCaptureRepository.capture {
                val rootNode = A11yRuntime.getRoot(service)
                    ?: throw RpcError(getString(Res.string.snapshot_app_a11y_missing))
                val appId = rootNode.packageName.toString()
                val screenHeight = ScreenUtils.getHeight()
                val screenWidth = ScreenUtils.getWidth()
                val isLandscape = ScreenUtils.isLandscape()
                coroutineScope {
                    val nodes = async(Dispatchers.IO) { captureNodeInfoList(rootNode) }
                    val activityId = async(Dispatchers.IO) { resolveActivityId(appId) }
                    val screenshot = async(Dispatchers.Default) {
                        captureScreen(
                            appId,
                            forcedCropStatusBar,
                            settings.hideSnapshotStatusBar,
                        )
                    }
                    val screen = screenshot.await()
                    SnapshotCaptureInput(
                        appId = appId,
                        activityId = activityId.await(),
                        screenHeight = screenHeight,
                        screenWidth = screenWidth,
                        isLandscape = isLandscape,
                        nodes = nodes.await(),
                        appInfo = AppInfoRepository.snapshot?.apps?.get(appId),
                        cyanskipAppInfo = selfAppInfo,
                        device = currentDeviceInfo(),
                        screenshot = screen.bytes,
                        screenshotStatus = screen.status,
                    )
                }
            }
        } catch (_: SnapshotCaptureBusyException) {
            throw RpcError(getString(Res.string.snapshot_save_in_progress))
        }
        val snapshot = captured.snapshot
        val detail = listOfNotNull(captured.screenshotStatus.detailText())
            .joinToString(" · ").takeIf { it.isNotEmpty() }
        // Notification delivery does not change the already committed capture / HTTP result.
        try {
            NotificationCatalog.snapshotSaved(
                appName = snapshot.appInfo?.name ?: snapshot.appId,
                activityId = getShowActivityId(snapshot.appId, snapshot.activityId),
                screenshotStatus = captured.screenshotStatus,
            ).post()
            ToastUtils.show(
                if (detail == null) getString(Res.string.snapshot_saved)
                else getString(Res.string.snapshot_saved_warning, detail), forced = true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogUtils.d("快照已保存，发送提示失败", e)
        }
        return snapshot
    }
}
