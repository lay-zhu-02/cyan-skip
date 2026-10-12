package app.cyanskip.app.ui.settings

import app.cyanskip.app.DesktopProfile

actual fun appVersion() = AppVersion(
    "青跳·预览",
    DesktopProfile.versionName,
)
