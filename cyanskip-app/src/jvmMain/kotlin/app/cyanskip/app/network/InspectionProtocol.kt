package app.cyanskip.app.network

import kotlinx.serialization.Serializable
import app.cyanskip.app.model.AppInfo
import app.cyanskip.app.model.DeviceInfo

@Serializable
data class RpcOk(
    val message: String? = null,
)

@Serializable
data class ReqId(
    val id: Long,
)

@Serializable
data class ServerInfo(
    val device: DeviceInfo,
    val cyanskipAppInfo: AppInfo
)

