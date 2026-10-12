package app.cyanskip.app.app

import app.cyanskip.app.data.appinfo.PackageAppCatalog

actual suspend fun openAppCatalog(): AppCatalogQuery = PackageAppCatalog.open()
