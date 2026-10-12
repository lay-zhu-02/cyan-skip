package app.cyanskip.app.storage

actual fun logArchiveInputs() = LogArchiveInputs(
    mapOf("desktop.txt" to { "cyan-skip desktop development host\n" + System.getProperty("os.name") }),
)
