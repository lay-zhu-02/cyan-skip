package app.cyanskip.app.storage

import app.cyanskip.app.util.AndroidStorage

actual fun appStorage(): AppStorageLayout = AndroidStorage.storage
