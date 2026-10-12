package app.cyanskip.app.ui.settings

data class AppVersion(
    val appName: String,
    val versionName: String,
)

expect fun appVersion(): AppVersion
