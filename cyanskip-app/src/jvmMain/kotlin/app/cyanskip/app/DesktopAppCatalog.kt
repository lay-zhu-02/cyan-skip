package app.cyanskip.app

import kotlinx.serialization.json.Json
import app.cyanskip.app.app.AppCatalogQuery
import app.cyanskip.app.model.AppInfo
import app.cyanskip.app.model.AppInventory
import app.cyanskip.app.platform.PlatformResult
import java.io.File

/** Profile files are platform input. Selection and refresh semantics belong to the shared repository. */
class DesktopAppCatalog(
    private val simulator: SimulatorStore,
    private val directory: File = DesktopStorage.profile,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun open(): AppCatalogQuery {
        val inventoryFile = directory.resolve("apps.json")
        val inventory = if (inventoryFile.isFile) {
            json.decodeFromString<AppInventory>(inventoryFile.readText())
        } else {
            AppInventory(
                apps = listOf(
                    AppInfo("app.cyanskip.android", "青跳", 1, "1.0", false, 0, false, 0),
                    AppInfo("com.android.settings", "设置", 1, "1.0", true, 0, false, 0),
                )
            )
        }
        val permissions = simulator.settings.value.permissions
        return object : AppCatalogQuery {
            override val userId = inventory.userId
            override val launcherAppId get() = DesktopProfile.launcherAppId
            override val canQueryPackages = permissions.canQueryPackages
            override val detectIncompleteList = false
            override val queryPackagesAbnormal = permissions.queryPackagesAbnormal
            override fun isCurrent() = simulator.settings.value.permissions.let {
                it.canQueryPackages == canQueryPackages && it.queryPackagesAbnormal == queryPackagesAbnormal
            }

            override suspend fun primaryApps() = inventory.apps
            override suspend fun primaryApp(id: String) = inventory.apps.find { it.id == id }

            override suspend fun visibleApps() = emptyList<AppInfo>()
        }
    }
}
