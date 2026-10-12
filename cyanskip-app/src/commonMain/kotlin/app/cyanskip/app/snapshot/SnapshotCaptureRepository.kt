package app.cyanskip.app.snapshot

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import app.cyanskip.app.model.AppInfo
import app.cyanskip.app.model.ComplexSnapshot
import app.cyanskip.app.model.DeviceInfo
import app.cyanskip.app.model.NodeInfo

enum class SnapshotScreenshotStatus { Captured, Unavailable, LikelyProtected }

/** Platform resources must be released before returning this value, including on cancellation. */
class SnapshotCaptureInput(
    val appId: String,
    val activityId: String?,
    val screenHeight: Int,
    val screenWidth: Int,
    val isLandscape: Boolean,
    val nodes: List<NodeInfo>,
    val appInfo: AppInfo?,
    val cyanskipAppInfo: AppInfo?,
    val device: DeviceInfo,
    val screenshot: ByteArray,
    val screenshotStatus: SnapshotScreenshotStatus,
)

data class SnapshotCaptureResult(
    val snapshot: ComplexSnapshot,
    val screenshotStatus: SnapshotScreenshotStatus,
)

class SnapshotCaptureBusyException :
    IllegalStateException("Snapshot capture is already in progress")

/** Application-owned orchestration. Callers own coroutines and user feedback. */
object SnapshotCaptureRepository {
    private val mutex = Mutex()
    val isCapturing: Boolean get() = mutex.isLocked

    /** Cancellation propagates; a snapshot already committed by the store remains saved. */
    suspend fun capture(collect: suspend () -> SnapshotCaptureInput): SnapshotCaptureResult {
        if (!mutex.tryLock()) throw SnapshotCaptureBusyException()
        try {
            currentCoroutineContext().ensureActive()
            val id = System.currentTimeMillis()
            val input = collect()
            val snapshot = ComplexSnapshot(
                id, input.appId, input.activityId, input.screenHeight, input.screenWidth,
                input.isLandscape, input.appInfo, input.cyanskipAppInfo, input.device, input.nodes,
            )
            SnapshotStore.save(snapshot, input.screenshot)
            return SnapshotCaptureResult(snapshot, input.screenshotStatus)
        } finally {
            mutex.unlock()
        }
    }
}
