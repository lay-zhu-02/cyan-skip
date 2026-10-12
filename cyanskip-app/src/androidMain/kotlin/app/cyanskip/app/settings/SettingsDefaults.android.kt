package app.cyanskip.app.settings

import app.cyanskip.app.util.DefaultBlockedApps

actual fun defaultSettings() = SettingsStore(
    actionToast = "青跳❤",
)

actual fun defaultBlockMatchAppList(): Set<String> = DefaultBlockedApps.query()
