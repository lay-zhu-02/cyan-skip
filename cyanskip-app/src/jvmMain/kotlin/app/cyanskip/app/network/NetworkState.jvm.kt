package app.cyanskip.app.network

import app.cyanskip.app.DesktopRuntime

actual suspend fun isNetworkAvailable(): Boolean =
    DesktopRuntime.requireCurrent().simulator.settings.value.networkAvailable
