package app.cyanskip.app.ui.image

import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import app.cyanskip.app.app
import app.cyanskip.app.util.AndroidStorage
import app.cyanskip.app.util.AndroidTarget

actual fun appImageLoader() = sharedImageLoader

private val sharedImageLoader by lazy {
    ImageLoaders.create(app, AndroidStorage.storage.coilCache) {
        if (AndroidTarget.P) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())
    }
}
