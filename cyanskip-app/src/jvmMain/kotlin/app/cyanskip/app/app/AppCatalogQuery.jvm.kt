package app.cyanskip.app.app

import app.cyanskip.app.DesktopRuntime

actual suspend fun openAppCatalog(): AppCatalogQuery = DesktopRuntime.requireCurrent().appCatalog.open()
