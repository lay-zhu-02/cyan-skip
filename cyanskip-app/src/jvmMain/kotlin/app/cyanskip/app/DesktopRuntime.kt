package app.cyanskip.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.app.applicationScope
import app.cyanskip.app.crash.CrashMetadata
import app.cyanskip.app.crash.CrashRecorder
import app.cyanskip.app.crash.CrashData
import app.cyanskip.app.crash.FileCrashStorage
import app.cyanskip.app.network.NetworkClients
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.storage.StorageMaintenance
import app.cyanskip.app.storage.appStorage
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.ui.image.ImageLoaders
import app.cyanskip.app.ui.component.ToastState
import app.cyanskip.app.ui.text.subscriptionDefaults
import app.cyanskip.app.util.LogUtils
import app.cyanskip.db.Db
import app.cyanskip.db.LOCAL_SUBS_ID
import app.cyanskip.db.SubsItem
import app.cyanskip.db.initialize

/** Application-owned repositories outlive routes and explicit UI scenario reloads. */
class DesktopRuntime(
    val simulator: SimulatorStore
) : AutoCloseable {
    private val scope = applicationScope()
    val appCatalog = DesktopAppCatalog(simulator)
    val toast = ToastState()
    val httpService = DesktopHttpService(simulator, scope, toast::show)
    val services = DesktopServiceSimulation(simulator, httpService::setEnabled)
    private var pendingCrashData = emptyList<CrashData>()
    val hasPendingCrashReports get() = pendingCrashData.isNotEmpty()
    val crashInitialization = scope.launch(Dispatchers.IO) {
        pendingCrashData = FileCrashStorage.takePending()
    }

    fun takeCrashDataList(): List<CrashData> = pendingCrashData.also { pendingCrashData = emptyList() }

    companion object {
        @Volatile
        private var current: DesktopRuntime? = null

        fun requireCurrent(): DesktopRuntime =
            checkNotNull(current) { "Desktop runtime is not active" }
    }

    @Composable
    fun appIcon(id: String): BitmapPainter? {
        val catalog by AppInfoRepository.state.collectAsStateWithLifecycle()
        return remember(id, catalog.snapshot, catalog.refreshing) { DesktopProfile.readIcon(id) }
    }

    init {
        LogUtils.d("Desktop runtime started")
    }

    private val pendingProfile = DesktopStorage.sessionDirectory.resolve("profile-import-pending")
    private val importProfile = !appStorage().database.isFile || pendingProfile.isFile

    init {
        if (importProfile) pendingProfile.writeText("1")
    }

    init {
        Db.initialize(appStorage().database.absolutePath)
    }

    private val sharedImageLoader = lazy {
        ImageLoaders.create(PlatformContext.INSTANCE, appStorage().coilCache)
    }
    val imageLoader: ImageLoader by sharedImageLoader

    private val crashRecorder = CrashRecorder(
        CrashMetadata(
            device = "Desktop " + System.getProperty("os.name"),
            osCode = 0,
            osName = System.getProperty("os.version"),
            versionCode = DesktopProfile.versionCode.toIntOrNull() ?: 0,
            versionName = DesktopProfile.versionName
        )
    )
    private val previousCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val crashHandler = Thread.UncaughtExceptionHandler { thread, error ->
        LogUtils.d("UncaughtExceptionHandler", thread, error)
        runCatching { crashRecorder.record(thread, error) }.onFailure { it.printStackTrace() }
        runCatching { LogUtils.flush() }.onFailure { it.printStackTrace() }
        previousCrashHandler?.uncaughtException(thread, error) ?: error.printStackTrace()
    }

    init {
        synchronized(Companion) {
            check(current == null) { "Desktop runtime is already active" }
            current = this
        }
        AppInfoRepository.initialize()
        simulator.onServiceRunningChanged = { service, running ->
            toast.show(serviceLifecycleToast(service, running))
        }
        simulator.onAppCatalogChanged = { AppInfoRepository.requestRefresh() }
        AppInfoRepository.requestRefresh()
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
    }

    init {
        scope.launch { StorageMaintenance.clearExpired(appStorage()) }
    }

    val subscriptionInitialization = scope.launch {
        SubscriptionRepository.initialize(subscriptionDefaults())
        val imported = DesktopStorage.sessionDirectory.resolve("profile-imported")
        if (!imported.exists()) {
            val existing = Db.subsItemDao.queryAll().mapTo(mutableSetOf()) { it.id }

            DesktopProfile.directory.resolve("subscriptions").takeIf { importProfile }?.listFiles()
                .orEmpty().filter { it.extension == "json" }.forEachIndexed { index, file ->
                    val value =
                        RawSubscription.parse(file.readText(), json5 = false)
                    if (value.id in existing && value.id != LOCAL_SUBS_ID) return@forEachIndexed
                    val enabled = DesktopProfile.subscriptionEnabled[value.id] ?: true
                    SubscriptionRepository.saveWithItem(
                        value,
                        SubsItem(
                            id = value.id,
                            order = index,
                            enable = enabled,
                            updateUrl = value.updateUrl
                        )
                    )
                }
            imported.writeText("1")
            check(!pendingProfile.exists() || pendingProfile.delete()) { "Cannot clear profile import marker" }
        }
        DesktopProfile.directory.resolve("fixtures.json").takeIf { it.isFile }?.let {
            importDesktopFixtures(
                it,
                appStorage().snapshot,
                DesktopStorage.sessionDirectory.resolve("fixtures-imported")
            )
        }
    }

    override fun close() {
        simulator.onServiceRunningChanged = null
        simulator.onAppCatalogChanged = null
        if (Thread.getDefaultUncaughtExceptionHandler() === crashHandler) Thread.setDefaultUncaughtExceptionHandler(
            previousCrashHandler
        )
        try {
            httpService.close()
            services.close()
            runBlocking {
                SettingsRepository.awaitPersistence()
            }
        } catch (e: Exception) {
            LogUtils.d("Desktop closed with unsaved data", e)
        } finally {
            try {
                runBlocking { scope.coroutineContext[Job]!!.cancelAndJoin() }
                if (sharedImageLoader.isInitialized()) imageLoader.shutdown()
                NetworkClients.close()
                Db.close()
            } finally {
                synchronized(Companion) {
                    if (current === this) current = null
                }
                LogUtils.d("Desktop runtime closed")
                LogUtils.close()
            }
        }
    }
}
