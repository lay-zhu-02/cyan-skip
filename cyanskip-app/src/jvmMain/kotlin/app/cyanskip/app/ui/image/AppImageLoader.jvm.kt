package app.cyanskip.app.ui.image

import app.cyanskip.app.DesktopRuntime

actual fun appImageLoader() = DesktopRuntime.requireCurrent().imageLoader
