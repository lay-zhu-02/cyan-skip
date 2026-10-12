package app.cyanskip.app

import androidx.lifecycle.ViewModelStore
import androidx.compose.runtime.snapshots.Snapshot
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import app.cyanskip.app.backup.BackupManager
import app.cyanskip.app.crash.assertCrashReports
import app.cyanskip.app.model.AppInfo
import app.cyanskip.app.model.AppInventory
import app.cyanskip.app.network.assertInspectionProtocol
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.settings.SettingsRepositoryChecks
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.storage.AppStorageLayout
import app.cyanskip.app.storage.FileSource
import app.cyanskip.app.storage.appStorage
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.SubscriptionJson
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.ToastState
import app.cyanskip.app.ui.home.BottomNavItem
import app.cyanskip.app.ui.home.HomeState
import app.cyanskip.app.ui.home.HomeViewModel
import app.cyanskip.app.ui.navigation.AboutRoute
import app.cyanskip.app.ui.navigation.AppConfigRoute
import app.cyanskip.app.ui.navigation.HomeRoute
import app.cyanskip.app.ui.navigation.SnapshotPageRoute
import app.cyanskip.app.ui.subscription.AppConfigViewModel
import app.cyanskip.app.util.LogUtils
import app.cyanskip.db.Db
import app.cyanskip.db.SubscriptionConfigStore
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationIntegrationTest {
    @Test
    fun applicationImportsProfileEditsDataAndRestoresBackup() = runBlocking {
        System.setProperty(
            "cyanskip.projectRoot",
            System.getProperty("cyanskip.projectRoot", File("..").canonicalPath)
        )
        DesktopStorage.initialize(test = true)
        val session = DesktopStorage.sessionDirectory
        try {
            val profileDir = DesktopStorage.profile.apply { mkdirs() }
            profileDir.resolve("seed.json")
                .writeText("""{"blocked":["example.blocked"],"subscriptions":{"92":false,"13":true}}""")
            profileDir.resolve("settings.json").writeText("""{"showBlockApp":false}""")
            profileDir.resolve("apps.json").writeText(
                Json.encodeToString(
                    AppInventory(
                        apps = listOf(
                            AppInfo("test.alpha", "Alpha", 1, "1", false, 0, false, 0),
                            AppInfo("test.beta", "Beta", 1, "1", false, 0, false, 0),
                        )
                    )
                )
            )
            profileDir.resolve("subscriptions").mkdirs()
            for (id in listOf(13, 92)) {
                profileDir.resolve("subscriptions/$id.json")
                    .writeText("""{"id":$id,"name":"fixture-$id","version":1}""")
            }
            // Current settings files are loaded exactly once at application startup.
            val store = DesktopStorage.data.resolve("files/store").apply { mkdirs() }
            store.resolve("store.json")
                .writeText("""{"appGroupType":7,"actionToast":"saved toast","enableDynamicColor":false}""")
            store.resolve("overlay_position.json").writeText("""{"snapshot-button":[12,34]}""")
            // An Android recovery file without a database row must keep its user-supplied name.
            val subscriptions = DesktopStorage.data.resolve("files/subscription").apply { mkdirs() }
            subscriptions.resolve("${app.cyanskip.db.LOCAL_SUBS_ID}.json.bak").writeText(
                """{"id":${app.cyanskip.db.LOCAL_SUBS_ID},"name":"My saved rules","version":7}"""
            )
            val profile = DesktopProfile
            assertEquals(setOf("example.blocked"), profile.blocked)
            // Process-lifetime singletons follow the real application lifecycle, exactly once.
            DesktopRuntime(
                SimulatorStore(
                    SimulatorSettings(
                        device = SimulatedDevice(
                            wifi = false,
                            mobileSignal = 0
                        )
                    )
                )
            ).use { runtime ->
                assertLogCallerValues()
                withTimeout(20_000) { runtime.subscriptionInitialization.join() }
                withTimeout(5_000) { runtime.crashInitialization.join() }
                val items = Db.subsItemDao.queryAll().associateBy { it.id }
                assertTrue(items.getValue(13).enable)
                assertFalse(items.getValue(92).enable)
                assertTrue(appStorage().subscription.resolve("92.json").isFile)
                assertPlatformInputs(runtime.simulator)
                assertSubscriptionCreationNames()
                app.cyanskip.app.subscription.assertUpdateErrorDismissal(runtime.simulator)
                app.cyanskip.app.subscription.assertSubscriptionDownloadStatus()
                assertHomeLifecycle()
                assertRootNavigationLifecycle(runtime)
                assertAppRuleParentVisibility()
                SettingsRepositoryChecks.run(appStorage().store)
                assertCrashReports()
                app.cyanskip.app.rule.RuleSwitchPolicyTest()
                    .requestRejectsChangedSwitchesButAcceptsConcurrentPageAndOtherAppEdits()
                app.cyanskip.app.snapshot.SnapshotCaptureRepositoryTest(appStorage()).runAll()
                app.cyanskip.app.snapshot.SnapshotStoreTest(appStorage()).runAll()
                DesktopAppCatalogTest().profileReloadUsesSharedSelectionAndRecoversFromCorruptFile(
                    profileDir
                )
                app.cyanskip.app.record.assertRuntimeRecords(appStorage().database)
                app.cyanskip.app.storage.LogArchiveChecks().apply {
                    archiveContainsMaterialsAndExpiredCacheIsCleanedByMaintenance()
                    materialFailurePreservesCauseAndLeavesFilesForCacheMaintenance()
                }
                assertBackupRoundTrip(appStorage())
            }
            assertFailsWith<IllegalStateException> { app.cyanskip.app.network.isNetworkAvailable() }
            assertFailsWith<IllegalStateException> { app.cyanskip.app.app.openAppCatalog() }
            val logs = appStorage().log.listFiles()!!.associate { it.name to it.readText() }
            LogUtils.d("log-after-close")
            LogUtils.flush()
            LogUtils.close()
            assertEquals(logs, appStorage().log.listFiles()!!.associate { it.name to it.readText() })
        } finally {
            session.deleteRecursively()
            DesktopStorage.initialize(test = false)
        }
    }

    private suspend fun assertPlatformInputs(simulator: SimulatorStore) {
        val previous = simulator.settings.value
        try {
            val beforeOfflineRefresh = SubscriptionRepository.snapshotFlow.value
            // The repository uses the active host's current network state on every operation.
            assertEquals(
                app.cyanskip.app.subscription.SubscriptionResult.Failure(
                    app.cyanskip.app.subscription.SubscriptionResult.FailureReason.NetworkUnavailable
                ),
                SubscriptionRepository.refresh(),
            )
            // An offline preflight must not replace subscription data or update errors.
            assertEquals(beforeOfflineRefresh, SubscriptionRepository.snapshotFlow.value)
            simulator.update { it.copy(device = it.device.copy(wifi = true)) }
            // These imported subscriptions have no update URL: success requires no external network.
            assertTrue(SubscriptionRepository.refresh() is app.cyanskip.app.subscription.SubscriptionResult.Success)
            simulator.update { it.copy(device = it.device.copy(wifi = false, mobileSignal = 0)) }
            assertEquals(
                app.cyanskip.app.subscription.SubscriptionResult.Failure(
                    app.cyanskip.app.subscription.SubscriptionResult.FailureReason.NetworkUnavailable
                ),
                SubscriptionRepository.refresh(),
            )
            val query = app.cyanskip.app.app.openAppCatalog()
            assertTrue(query.isCurrent())
            simulator.update {
                it.copy(permissions = it.permissions.copy(canQueryPackages = !query.canQueryPackages))
            }
            assertFalse(query.isCurrent())
            app.cyanskip.app.app.AppInfoRepository.refresh()
            assertEquals(!query.canQueryPackages, app.cyanskip.app.app.AppInfoRepository.snapshot!!.canQueryPackages)
        } finally {
            simulator.update {
                it.copy(
                    device = it.device.copy(wifi = previous.device.wifi, mobileSignal = previous.device.mobileSignal),
                    permissions = it.permissions.copy(canQueryPackages = previous.permissions.canQueryPackages),
                )
            }
            app.cyanskip.app.app.AppInfoRepository.refresh()
        }
    }

    private suspend fun assertSubscriptionCreationNames() {
        val localId = app.cyanskip.db.LOCAL_SUBS_ID
        val memoryId = app.cyanskip.db.LOCAL_HTTP_SUBS_ID
        assertEquals("My saved rules", SubscriptionRepository.awaitSnapshot().subscriptions.getValue(localId).name)
        val localFile = appStorage().subscription.resolve("$localId.json")
        val originalBytes = localFile.readBytes()
        // A retry with a different language creates only missing built-in data.
        Db.subsItemDao.upsert(app.cyanskip.db.SubsItem(memoryId, order = 0))
        SubscriptionRepository.initialize(app.cyanskip.app.subscription.SubscriptionDefaults("Local rules", "Memory rules"))
        assertTrue(originalBytes.contentEquals(localFile.readBytes()))
        val memoryFile = appStorage().subscription.resolve("$memoryId.json")
        assertEquals("Memory rules", RawSubscription.parse(memoryFile.readText()).name)
        val memoryBytes = memoryFile.readBytes()
        SubscriptionRepository.initialize(app.cyanskip.app.subscription.SubscriptionDefaults("本地订阅", "内存订阅"))
        assertTrue(originalBytes.contentEquals(localFile.readBytes()))
        assertTrue(memoryBytes.contentEquals(memoryFile.readBytes()))
        // A failed built-in file creation stays local to that subscription and can be retried.
        assertTrue(memoryFile.delete())
        assertTrue(memoryFile.mkdir())
        val obstruction = memoryFile.resolve("occupied").apply { writeText("blocked") }
        SubscriptionRepository.initialize(app.cyanskip.app.subscription.SubscriptionDefaults("Other", "Retry memory"))
        assertTrue(memoryId in SubscriptionRepository.awaitSnapshot().loadErrors)
        assertTrue(originalBytes.contentEquals(localFile.readBytes()))
        assertTrue(obstruction.delete())
        assertTrue(memoryFile.delete())
        SubscriptionRepository.initialize(app.cyanskip.app.subscription.SubscriptionDefaults("Other", "Retry memory"))
        assertFalse(memoryId in SubscriptionRepository.awaitSnapshot().loadErrors)
        assertEquals("Retry memory", RawSubscription.parse(memoryFile.readText()).name)
        // First local creation uses the operation's name; subsequent retries leave it untouched.
        Db.subsItemDao.deleteById(localId)
        assertTrue(localFile.delete())
        SubscriptionRepository.initialize(app.cyanskip.app.subscription.SubscriptionDefaults("Fresh local", "Other memory"))
        assertEquals("Fresh local", RawSubscription.parse(localFile.readText()).name)
        SubscriptionRepository.initialize(app.cyanskip.app.subscription.SubscriptionDefaults("Another locale", "Other memory"))
        assertEquals("Fresh local", SubscriptionRepository.awaitSnapshot().subscriptions.getValue(localId).name)
    }

    private fun assertLogCallerValues() {
        var value = "log-capture-before"
        val mutable = object {
            override fun toString() = value
        }
        val caller = Thread.currentThread().name
        LogUtils.d(mutable)
        value = "log-capture-after"
        LogUtils.flush()
        val text = appStorage().log.listFiles()!!.joinToString("\n") { it.readText() }
        assertTrue(text.contains("\nlog-capture-before\n\n"))
        assertFalse(text.contains("log-capture-after"))
        assertTrue(text.contains(" ApplicationIntegrationTest, $caller, "), text)
        assertTrue(text.contains("assertLogCallerValues"), text)
    }

    private suspend fun assertAppRuleParentVisibility() {
        val id = -991L
        val subscription = RawSubscription.parse(
            """{
            id: -991, name: 'Parent visibility', version: 1,
            globalGroups: [
                {key: 1, name: 'Default on', rules: [{matches: '[text="Ad"]'}]},
                {key: 2, name: 'Default off', enable: false, rules: [{matches: '[text="Ad"]'}]}
            ],
            apps: [{id: 'test.alpha', groups: [{key: 101, name: 'App rule', rules: [{matches: '[text="Ad"]'}]}]}]
        }"""
        )
        val owner = ViewModelStore()
        val originalShowDisabled = SettingsRepository.settings.value.showDisabledRule
        try {
            SubscriptionRepository.saveWithItem(
                subscription,
                app.cyanskip.db.SubsItem(id, order = 0, enable = true)
            )
            val vm = AppConfigViewModel(
                AppConfigRoute("test.alpha")
            ).also { owner.put("app-rules", it) }

            suspend fun expectGroups(vararg keys: Int) {
                withTimeout(5_000) {
                    vm.uiState.first { loaded ->
                        loaded is Loadable.Ready && loaded.value.subsPairs
                            .filter { it.first.subsItem.id == id }
                            .flatMap { it.second }.map { it.key }.toSet() == keys.toSet()
                    }
                }
            }
            expectGroups(1, 101)
            // A per-app allow override must not bypass the global group's parent switch.
            SubscriptionConfigStore.updateGlobalGroupConfig(id, 1) {
                it.copy(enable = false, exclude = "!test.alpha")
            }
            expectGroups(101)
            vm.setShowDisabledRules(true)
            SettingsRepository.awaitPersistence()
            expectGroups(101)
            SubscriptionConfigStore.updateGlobalGroupConfig(id, 2) { it.copy(enable = true) }
            expectGroups(2, 101)
            SubscriptionConfigStore.setAppEnabled(id, "test.alpha", false)
            expectGroups(2)
            SubscriptionConfigStore.updateGlobalGroupConfig(id, 2) { it.copy(enable = null) }
            expectGroups()
            SubscriptionConfigStore.updateGlobalGroupConfig(id, 1) { it.copy(enable = null) }
            expectGroups(1)
            val saved =
                SubscriptionConfigStore.capture().globalGroupConfigs.single { it.subsId == id && it.groupKey == 1 }
            assertEquals("!test.alpha", saved.exclude)
            assertEquals(subscription, SubscriptionRepository.awaitSnapshot().subscriptions[id])
        } finally {
            owner.clear()
            SettingsRepository.updateSettings { it.copy(showDisabledRule = originalShowDisabled) }
            SettingsRepository.awaitPersistence()
            SubscriptionRepository.delete(id)
        }
    }

    // Scenario reload replaces page state but must retain root tasks; closing the host cancels them.
    private suspend fun assertRootNavigationLifecycle(runtime: DesktopRuntime) =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
            assertFailsWith<IllegalStateException> { MainViewModel.requireCurrent() }
            val state = DesktopState(runtime.toast, isolated = true, simulator = runtime.simulator)
            val vm = state.mainVm
            val rootJob = vm.scope.coroutineContext[kotlinx.coroutines.Job]!!
            try {
                kotlin.test.assertSame(vm, MainViewModel.requireCurrent())
                // Rebinding the same retained root must not create or replace its instance.
                vm.registerCurrent()
                kotlin.test.assertSame(vm, MainViewModel.requireCurrent())
                val retrieved = androidx.lifecycle.ViewModelProvider.create(state)[MainViewModel::class]
                kotlin.test.assertSame(vm, retrieved)
                vm.navigator.navigate(AboutRoute)
                vm.navigator.navigate(SnapshotPageRoute, replaced = true)
                assertEquals(listOf(HomeRoute, SnapshotPageRoute), vm.navigator.backStack.map { it.route })
                vm.navigator.pop()
                assertEquals(HomeRoute, vm.navigator.topRoute)
                val previousHome = HomeState()
                vm.homeNavigation.bind(previousHome)
                state.load(ScenarioRequest("settings"))
                vm.homeNavigation.unbind(previousHome)
                val nextHome = HomeState()
                vm.homeNavigation.bind(nextHome)
                assertEquals(BottomNavItem.Settings, nextHome.selectedTab.value)
                kotlin.test.assertSame(vm, state.mainVm)
                kotlin.test.assertSame(vm, MainViewModel.requireCurrent())
                assertTrue(rootJob.isActive)
                vm.homeNavigation.unbind(nextHome)
            } finally {
                state.close()
            }
            assertTrue(rootJob.isCancelled)
            assertFailsWith<IllegalStateException> { MainViewModel.requireCurrent() }
        }

    private suspend fun assertHomeLifecycle() {
        val owner = ViewModelStore()
        val first = app.cyanskip.db.SubsItem(id = 900001, order = 100, enable = true)
        val second = app.cyanskip.db.SubsItem(id = 900002, order = 101)
        try {
            val vm = HomeViewModel().also { owner.put("home", it) }
            withTimeout(5_000) { vm.appsState.first { it.value?.apps?.size == 2 } }
            withTimeout(5_000) { vm.latestState.first { it is app.cyanskip.app.state.Loadable.Ready } }
            withTimeout(5_000) { vm.subscriptionsState.first { it is app.cyanskip.app.state.Loadable.Ready } }
            Db.subsItemDao.upsert(first, second)
            vm.requestSubscriptionEnabled(second, true)
            assertTrue(Db.subsItemDao.queryAll().single { it.id == second.id }.enable)
            vm.requestSubscriptionEnabled(second, false)
            assertFalse(Db.subsItemDao.queryAll().single { it.id == second.id }.enable)
            assertTrue(vm.scope.coroutineContext[kotlinx.coroutines.Job]!!.isActive)
            owner.clear()
            assertTrue(vm.scope.coroutineContext[kotlinx.coroutines.Job]!!.isCancelled)
        } finally {
            owner.clear()
            Db.subsItemDao.deleteById(first.id, second.id)
        }
    }

    private suspend fun assertBackupRoundTrip(layout: AppStorageLayout) {
        val repository = SubscriptionRepository
        val settings = SettingsRepository
        val manager =
            BackupManager
        settings.updateSettings { it.copy(actionToast = "archive") }
        settings.awaitPersistence()
        val original = repository.awaitSnapshot().subscriptions.getValue(-2)
        val archive = manager.exportData()
        ZipFile(archive).use { zip ->
            assertNotNull(zip.getEntry("store/store.json"))
            assertNotNull(zip.getEntry("subscription/-2.json"))
            assertNotNull(zip.getEntry("db.json"))
        }
        settings.updateSettings { it.copy(actionToast = "later") }
        settings.awaitPersistence()
        androidx.sqlite.driver.bundled.BundledSQLiteDriver().open(layout.database.absolutePath)
            .use { connection ->
                connection.prepare("CREATE TRIGGER test_backup_failure BEFORE INSERT ON subs_item BEGIN SELECT RAISE(FAIL, 'backup failure'); END")
                    .use { it.step() }
                try {
                    assertFails { manager.importData(FileSource.Local(archive)) }
                } finally {
                    connection.prepare("DROP TRIGGER test_backup_failure").use { it.step() }
                }
            }
        assertEquals("later", settings.settings.value.actionToast)
        assertEquals(
            SubscriptionJson.json.encodeToString(original),
            SubscriptionJson.json.encodeToString(
                repository.awaitSnapshot().subscriptions.getValue(-2)
            )
        )
        assertFailsWith<CancellationException> {
            manager.importData { throw CancellationException("cancelled selection") }
        }
        val malformedArchive = layout.sharedCache.resolve("invalid-subscription-directory.zip")
        try {
            ZipOutputStream(malformedArchive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("subscription"))
                zip.write("not a directory".toByteArray())
                zip.closeEntry()
            }
            assertFailsWith<IOException> {
                manager.importData(FileSource.Local(malformedArchive))
            }
            assertEquals(
                SubscriptionJson.json.encodeToString(original),
                SubscriptionJson.json.encodeToString(
                    repository.awaitSnapshot().subscriptions.getValue(-2)
                ),
            )
        } finally {
            malformedArchive.delete()
        }
        assertEquals("later", settings.settings.value.actionToast)
        manager.importData(FileSource.Uri(archive.toURI().toString()))
        assertEquals("archive", settings.settings.value.actionToast)
        assertEquals(
            SubscriptionJson.json.encodeToString(original.copy(version = original.version + 1)),
            SubscriptionJson.json.encodeToString(
                repository.awaitSnapshot().subscriptions.getValue(-2)
            )
        )
        assertTrue(layout.tempCache.listFiles().orEmpty().isEmpty())
        assertInspectionProtocol(layout, repository)
    }
}
