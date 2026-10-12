package app.cyanskip.app.ui

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.cyanskip.app.crash.FileCrashStorage
import app.cyanskip.app.entry.EntryActivity
import app.cyanskip.app.entry.OpenFileActivity
import app.cyanskip.app.permission.PermissionRequests
import app.cyanskip.app.permission.PermissionStates
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.backup_import_confirmation
import app.cyanskip.app.resources.backup_import_label
import app.cyanskip.app.resources.uri_unknown
import app.cyanskip.app.service.A11yService
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.storage.FileSource
import app.cyanskip.app.storage.StorageMaintenance
import app.cyanskip.app.ui.home.BottomNavItem
import app.cyanskip.app.ui.navigation.CsLink
import app.cyanskip.app.ui.share.ActivityResultRequests
import app.cyanskip.app.ui.share.importBackup as importBackupArchive
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.AndroidStorage
import app.cyanskip.app.util.IntentUtils
import app.cyanskip.app.util.LogUtils
import app.cyanskip.app.util.ThreadUtils
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.app.util.launchLogged

fun createMainViewModel(): MainViewModel {
    lateinit var vm: MainViewModel
    val permissions = PermissionRequests()
    vm = MainViewModel(permissions)
    vm.addCloseable("android", AndroidMainState(vm, permissions))
    return vm
}

val MainViewModel.androidState: AndroidMainState
    get() = requireNotNull(getCloseable<AndroidMainState>("android"))

class AndroidMainState(
    private val mainVm: MainViewModel,
    val permissionRequests: PermissionRequests,
) : AutoCloseable {
    val activityResults = ActivityResultRequests()

    fun handleDeepLink(uri: Uri) {
        val notFoundToast = { ToastUtils.show(Res.string.uri_unknown.getSync(uri)) }
        when (val link = CsLink.parse(uri.toString())) {
            is CsLink.Home -> ThreadUtils.runMainOrPost {
                val tab = link.tab
                if (tab != null && BottomNavItem.allSubObjects.any { it.key == tab }) {
                    mainVm.homeNavigation.selectTab(BottomNavItem.allSubObjects.first { it.key == tab })
                }
                // MainActivity 被复用时，也需要返回首页。
                mainVm.navigator.popToHome()
            }

            is CsLink.Page -> mainVm.navigator.navigate(link.route)
            CsLink.WeChatScanner -> IntentUtils.openWeChatScanner()
            null -> notFoundToast()
        }
    }

    fun handleIntent(intent: Intent) = mainVm.scope.launchUi {
        LogUtils.d(intent)
        val uri = intent.data?.normalizeScheme()
        val source = intent.getStringExtra(EntryActivity.activityNavSourceName)
        if (uri?.scheme == "cyanskip") {
            handleDeepLink(uri)
        } else if (source == OpenFileActivity::class.java.name && uri != null) {
            if (!mainVm.dialogRequests.confirm(
                    title = Res.string.backup_import_label.getSync(),
                    text = Res.string.backup_import_confirmation.getSync(),
                )
            ) {
                return@launchUi
            }
            importBackupArchive(FileSource.Uri(uri.toString()))
        }
    }

    override fun close() {}

    init {
        mainVm.scope.launchLogged(Dispatchers.IO) {
            // 每次进入删除缓存
            StorageMaintenance.clearExpired(AndroidStorage.storage)
        }

        mainVm.scope.launchLogged(Dispatchers.IO) {
            val list = FileCrashStorage.takePending()
            withContext(Dispatchers.Main.immediate) { mainVm.showCrashReports(list) }
        }

    }
}
