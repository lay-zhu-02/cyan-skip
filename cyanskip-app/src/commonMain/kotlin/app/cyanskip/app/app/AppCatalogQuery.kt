package app.cyanskip.app.app

import app.cyanskip.app.model.AppInfo
import app.cyanskip.app.model.AppUser
import app.cyanskip.app.platform.PlatformResult

/** A query captures one host environment; it must keep permission inputs consistent. */
interface AppCatalogQuery {
    val userId: Int
    val launcherAppId: String?
    val canQueryPackages: Boolean
    val detectIncompleteList: Boolean get() = true
    val queryPackagesAbnormal: Boolean get() = false
    fun isCurrent(): Boolean = true

    /** Release obsolete host resources after a successful query. Must not throw. */
    fun committed(snapshot: AppCatalogSnapshot) = Unit
    suspend fun primaryApps(): List<AppInfo>

    /** null means confirmed absence, never a failed query. */
    suspend fun primaryApp(id: String): AppInfo?
    suspend fun visibleApps(): List<AppInfo>
}

/** Opens a platform query session; aggregation and publication stay in AppInfoRepository. */
expect suspend fun openAppCatalog(): AppCatalogQuery
