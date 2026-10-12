package app.cyanskip.app.storage

import androidx.core.net.toUri
import app.cyanskip.app.app
import java.io.FileNotFoundException
import java.io.InputStream

actual fun openFileSource(source: FileSource): InputStream = when (source) {
    is FileSource.Local -> source.file.inputStream()
    is FileSource.Uri -> app.contentResolver.openInputStream(source.value.toUri())
        ?: throw FileNotFoundException("Cannot open selected document")
}
