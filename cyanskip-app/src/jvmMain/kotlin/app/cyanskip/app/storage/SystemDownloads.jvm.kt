package app.cyanskip.app.storage

import app.cyanskip.app.platform.PlatformResult

import java.io.File

/** Desktop uses an explicit save-as target; there is no portable Downloads collection contract. */
actual suspend fun saveToDownloads(source: File): PlatformResult<String> =
    PlatformResult.Unsupported
