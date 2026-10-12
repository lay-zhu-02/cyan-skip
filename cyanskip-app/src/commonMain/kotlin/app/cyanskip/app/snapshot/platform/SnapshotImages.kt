package app.cyanskip.app.snapshot.platform

import app.cyanskip.app.platform.PlatformResult

import java.io.File

/** Returns false for undecodable input or different dimensions; throws on write failure. */
expect fun prepareSnapshotReplacement(original: File, bytes: ByteArray, output: File): Boolean

/** The caller obtains any required permission before invoking this operation. */
expect suspend fun saveImageToAlbum(image: File): PlatformResult<Unit>
