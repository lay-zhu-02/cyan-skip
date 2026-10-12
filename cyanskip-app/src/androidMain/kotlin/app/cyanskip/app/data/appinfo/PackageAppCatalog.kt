package app.cyanskip.app.data.appinfo

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import app.cyanskip.app.app
import app.cyanskip.app.app.AppCatalogQuery
import app.cyanskip.app.app.AppCatalogSnapshot
import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.appScope
import app.cyanskip.app.data.toAppInfo
import app.cyanskip.app.model.AppInfo
import app.cyanskip.app.permission.PermissionStates
import app.cyanskip.app.platform.PlatformResult
import app.cyanskip.app.util.LogUtils
import app.cyanskip.app.util.pkgIcon
import java.util.concurrent.ConcurrentHashMap

/** Android package collection and resources only; all merge/refresh policy lives in shared. */
object PackageAppCatalog {
    val selfAppInfo by lazy {
        app.packageManager.getPackageInfo(app.packageName, 0).toAppInfo()
    }

    const val packageFlags = PackageManager.MATCH_UNINSTALLED_PACKAGES
    private val icons = ConcurrentHashMap<AppInfo, Drawable>()
    fun icon(info: AppInfo?): Drawable? = info?.let(icons::get)

    suspend fun open(): AppCatalogQuery {
        val allowed = PermissionStates.queryPackages.updateAndGet()
        return object : AppCatalogQuery {
            override val userId = android.os.Process.myUid() / 100000
            override val launcherAppId: String? = null
            override val canQueryPackages = allowed
            override fun committed(snapshot: AppCatalogSnapshot) {
                icons.keys.retainAll(snapshot.apps.values.toSet())
            }

            override fun isCurrent() = PermissionStates.queryPackages.value == allowed

            private fun convert(
                info: PackageInfo,
                user: Int = userId,
                hidden: Boolean? = null
            ): AppInfo {
                val result = info.toAppInfo(user, hidden)
                if (!result.hidden) {
                    try {
                        val icon = info.pkgIcon
                        if (icon == null) icons.remove(result) else icons[result] = icon
                    } catch (e: Exception) {
                        LogUtils.d("app icon query failed", e)
                    }
                }
                return result
            }

            override suspend fun primaryApps() =
                app.packageManager.getInstalledPackages(packageFlags).map { convert(it) }

            override suspend fun primaryApp(id: String) = app.getPkgInfo(id)?.let { convert(it) }
            override suspend fun visibleApps(): List<AppInfo> =
                arrayOf(Intent.ACTION_MAIN, Intent.ACTION_VIEW).asSequence().flatMap { action ->
                    app.packageManager.queryIntentActivities(
                        Intent(action),
                        PackageManager.MATCH_DISABLED_COMPONENTS
                    )
                }.map { it.activityInfo.packageName }.toSet().mapNotNull { app.getPkgInfo(it) }
                    .map { convert(it, hidden = false) }.toList()
        }
    }

    fun initialize() {
        AppChangeMonitor.register { AppInfoRepository.packagesChanged(setOf(it)) }
        AppInfoRepository.requestRefresh()

    }
}
