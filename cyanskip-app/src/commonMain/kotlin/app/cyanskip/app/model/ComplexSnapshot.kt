package app.cyanskip.app.model

import kotlinx.serialization.Serializable
import app.cyanskip.db.BaseSnapshot
import app.cyanskip.db.Snapshot

@Serializable
data class ComplexSnapshot(
    override val id: Long,
    override val appId: String,
    override val activityId: String?,
    override val screenHeight: Int,
    override val screenWidth: Int,
    override val isLandscape: Boolean,
    val appInfo: AppInfo?,
    val cyanskipAppInfo: AppInfo?,
    val device: DeviceInfo,
    val nodes: List<NodeInfo>,
) : BaseSnapshot {
    fun toSnapshot(): Snapshot {
        return Snapshot(
            id = id,
            appId = appId,
            activityId = activityId,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            isLandscape = isLandscape,
        )
    }
}
