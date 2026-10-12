package app.cyanskip.app.ui.settings

import app.cyanskip.app.META

actual fun appVersion() = AppVersion(
    META.appName,
    META.versionName,
)
