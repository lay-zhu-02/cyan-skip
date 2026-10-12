package app.cyanskip.app.util

import app.cyanskip.app.META
import app.cyanskip.app.app
import java.io.File

object ApkUtils {
    fun createShareFile(): File =
        AndroidStorage.storage.sharedCache.resolve("cyanskip-v${META.versionName}.apk").apply {
            File(app.packageCodePath).copyTo(this, overwrite = true)
        }
}
