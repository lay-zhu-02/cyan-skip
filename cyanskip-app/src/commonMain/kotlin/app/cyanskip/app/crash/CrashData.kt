package app.cyanskip.app.crash

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import app.cyanskip.app.time.format

@Serializable
data class CrashData(
    val id: Long,
    val mtime: Long,
    val device: String,
    val androidVersionCode: Int,
    val androidVersionName: String,
    val versionCode: Int,
    val versionName: String,
    val name: String,
    val message: String?,
    val thread: String,
    val stackTrace: String,
    @Transient
    val filename: String = "cyanskip-crash-" + mtime.format("yyyyMMddHHmmss") + ".json",
)
