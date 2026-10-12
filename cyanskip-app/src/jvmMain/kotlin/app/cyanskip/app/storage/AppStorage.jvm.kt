package app.cyanskip.app.storage

import app.cyanskip.app.DesktopStorage

actual fun appStorage(): AppStorageLayout = DesktopStorage.layout
