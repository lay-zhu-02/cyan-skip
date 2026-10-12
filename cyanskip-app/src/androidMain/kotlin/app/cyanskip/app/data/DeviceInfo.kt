package app.cyanskip.app.data

import android.os.Build
import app.cyanskip.app.model.DeviceInfo

fun currentDeviceInfo() = DeviceInfo(
    Build.DEVICE,
    Build.MODEL,
    Build.MANUFACTURER,
    Build.BRAND,
    Build.VERSION.SDK_INT,
    Build.VERSION.RELEASE
)
