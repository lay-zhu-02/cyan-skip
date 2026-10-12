package app.cyanskip.app.util

import app.cyanskip.app.app
import app.cyanskip.app.storage.AppStorageLayout
import app.cyanskip.app.storage.StorageLocation
import app.cyanskip.app.storage.DirectorySelectionRecord
import app.cyanskip.app.storage.StorageSelectionReason
import app.cyanskip.app.storage.recordStartupLog
import java.io.File

object AndroidStorage {
    private data class StorageInitialization(val filesDir: File, val directorySelectionRecord: DirectorySelectionRecord)

    private val initialization: StorageInitialization by lazy {
        val internalFiles = app.filesDir
        val markFile = internalFiles.resolve(".cyanskip")
        val marked = markFile.isFile
        val external = if (marked) null else app.getExternalFilesDir(null)
        // fix #1333: once external storage is unavailable, keep using internal storage.
        val createMarker = !marked && external == null
        val markerCreated = if (createMarker) markFile.createNewFile() else null
        val selected = external ?: internalFiles
        val record = DirectorySelectionRecord(
            markerBefore = marked,
            externalResult = external?.absolutePath,
            reason = when {
                marked -> StorageSelectionReason.InternalMarkerExists
                external != null -> StorageSelectionReason.ExternalAvailable
                else -> StorageSelectionReason.ExternalNullFallback
            },
            markerCreated = markerCreated,
            selectedPath = selected.absolutePath,
            storageLocation = if (external != null) StorageLocation.External else StorageLocation.Internal,
        )
        recordStartupLog(record, internalFiles, external)
        StorageInitialization(selected, record)
    }

    val filesDir get() = initialization.filesDir
    val directorySelectionRecord get() = initialization.directorySelectionRecord
    private val cacheDir by lazy { app.externalCacheDir ?: app.cacheDir }

    val storage by lazy { AppStorageLayout(filesDir, cacheDir, app.filesDir) }
}
