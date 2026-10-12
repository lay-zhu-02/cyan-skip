package app.cyanskip.app.data

import android.os.Build
import com.hjq.device.compat.DeviceBrand
import com.hjq.device.compat.DeviceMarketName
import com.hjq.device.compat.DeviceOs
import app.cyanskip.app.META
import app.cyanskip.app.app
import app.cyanskip.app.logging.LogMetadata

object AndroidLogMetadata {
    val deviceDescription by lazy {
        listOf(
            Build.MANUFACTURER,
            Build.MODEL,
            DeviceBrand.getBrandName(),
            DeviceOs.getOsName() + DeviceOs.getOsVersionName() + DeviceOs.getOsBigVersionCode(),
            DeviceMarketName.getMarketName(app)
        ).joinToString("/")
    }

    fun create() = LogMetadata(
        systemName = "Android",
        systemVersion = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
        device = deviceDescription,
        app = "${META.versionName} (${META.versionCode})",
    )
}
