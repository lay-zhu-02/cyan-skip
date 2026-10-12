package app.cyanskip.app.storage

import app.cyanskip.app.data.LogMetadataSources
import app.cyanskip.app.util.AndroidStorage

actual fun logArchiveInputs() = LogArchiveInputs(
    LogMetadataSources.sources(),
    emptyList(),
)
