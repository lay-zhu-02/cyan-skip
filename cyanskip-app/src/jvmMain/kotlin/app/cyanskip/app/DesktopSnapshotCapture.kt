package app.cyanskip.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import app.cyanskip.app.model.ComplexSnapshot
import app.cyanskip.app.snapshot.SnapshotCaptureInput
import app.cyanskip.app.snapshot.SnapshotScreenshotStatus
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.util.Base64

/** Explicit development input. The source id is ignored; capture assigns a new timestamp. */
@Serializable
data class DesktopSnapshotCaptureRequest(
    val source: ComplexSnapshot,
    val screenshotBase64: String,
)

@Serializable
data class DesktopSnapshotCaptureResult(
    val snapshot: ComplexSnapshot,
)

suspend fun DesktopRuntime.replaySnapshot(request: DesktopSnapshotCaptureRequest): DesktopSnapshotCaptureResult {
    require(DesktopStorage.isolated) { "Snapshot capture replay requires --test" }
    val result = app.cyanskip.app.snapshot.SnapshotCaptureRepository.capture {
        withContext(Dispatchers.Default) {
            val source = request.source
            val screenshot =
                Image.makeFromEncoded(Base64.getDecoder().decode(request.screenshotBase64))
                    .use { image ->
                        require(image.width == source.screenWidth && image.height == source.screenHeight)
                        image.encodeToData(EncodedImageFormat.WEBP, 85)?.use { it.bytes }
                            ?: error("Cannot encode replay screenshot")
                    }
            SnapshotCaptureInput(
                source.appId, source.activityId, source.screenHeight, source.screenWidth,
                source.isLandscape, source.nodes, source.appInfo, source.cyanskipAppInfo, source.device,
                screenshot, SnapshotScreenshotStatus.Captured,
            )
        }
    }
    return DesktopSnapshotCaptureResult(result.snapshot)
}
