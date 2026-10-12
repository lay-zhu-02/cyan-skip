package app.cyanskip.app

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.Application
import android.app.KeyguardManager
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process.killProcess
import android.os.Process.myPid
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import app.cyanskip.app.a11y.initA11yFeat
import app.cyanskip.app.app.AppInfoRepository
import app.cyanskip.app.crash.CrashMetadata
import app.cyanskip.app.crash.CrashRecorder
import app.cyanskip.app.data.AndroidLogMetadata
import app.cyanskip.app.data.appinfo.PackageAppCatalog
import app.cyanskip.app.data.appinfo.PackageAppCatalog.selfAppInfo
import app.cyanskip.app.network.AppLinks
import app.cyanskip.app.notif.NotificationChannels
import app.cyanskip.app.platform.lifecycle.MainActivityVisibility
import app.cyanskip.app.platform.lifecycle.RuntimeStateSynchronizer
import app.cyanskip.app.service.ExposeService
import app.cyanskip.app.subscription.SubscriptionRepository
import app.cyanskip.app.ui.text.subscriptionDefaults
import app.cyanskip.app.util.AndroidStorage
import app.cyanskip.app.util.AndroidTarget
import app.cyanskip.app.util.LogUtils
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.app.util.AndroidToastUtils
import app.cyanskip.app.util.launchLogged
import app.cyanskip.db.Db
import app.cyanskip.db.initialize
import org.lsposed.hiddenapibypass.HiddenApiBypass

val appScope by lazy { MainScope() }

private lateinit var innerApp: App
val app: App
    get() = innerApp

// https://github.com/android-cs/16/blob/main/packages/SettingsLib/src/com/android/settingslib/accessibility/AccessibilityUtils.java#L41
private const val ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR = ':'

@Serializable
data class AppMeta(
    val channel: String = app.getMetaString("channel"),
    val buildKey: String = app.getMetaString("buildKey"),
    val commitId: String = app.getMetaString("commitId"),
    val commitTime: Long = app.getMetaString("commitTime").toLong(),
    val tagName: String? = app.getMetaString("tagName").takeIf { it.isNotEmpty() },
    val debuggable: Boolean = app.applicationInfoWithMetadata.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
    val versionCode: Int = selfAppInfo.versionCode,
    val versionName: String = selfAppInfo.versionName!!,
    val appId: String = app.packageName!!,
    val appName: String = app.applicationInfoWithMetadata.loadLabel(app.packageManager).toString()
) {
}

val META by lazy { AppMeta() }

class App : Application() {
    init {
        innerApp = this
    }

    val applicationInfoWithMetadata: ApplicationInfo by lazy {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    }

    val notificationSmallIcon: Int by lazy {
        getMetaInt("notificationSmallIcon")
    }

    val splashScreenLightTheme: Int by lazy {
        getMetaInt("splashScreenLightTheme")
    }

    val splashScreenNightTheme: Int by lazy {
        getMetaInt("splashScreenNightTheme")
    }

    fun getMetaString(key: String): String {
        return applicationInfoWithMetadata.metaData?.getString(key)
            ?: error("Missing meta-data: $key")
    }

    fun getMetaInt(key: String): Int {
        val resourceId = applicationInfoWithMetadata.metaData?.getInt(key) ?: 0
        check(resourceId != 0) { "Missing resource meta-data: $key" }
        return resourceId
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (AndroidTarget.P) {
            HiddenApiBypass.addHiddenApiExemptions("L")
        }
    }

    fun registerObserver(
        uri: Uri,
        observer: ContentObserver
    ) {
        contentResolver.registerContentObserver(uri, false, observer)
    }

    fun unregisterObserver(observer: ContentObserver) {
        contentResolver.unregisterContentObserver(observer)
    }

    fun getSecureString(name: String): String? = Settings.Secure.getString(contentResolver, name)




    fun getSecureA11yServices(): MutableSet<ComponentName> {
        val value = getSecureString(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (value.isNullOrEmpty()) return mutableSetOf()
        return value.split(
            ENABLED_ACCESSIBILITY_SERVICES_SEPARATOR
        ).mapNotNull { ComponentName.unflattenFromString(it) }.toHashSet()
    }



    fun resolveAppId(intent: Intent): String? {
        return intent.resolveActivity(packageManager)?.packageName
    }

    fun getPkgInfo(appId: String): PackageInfo? = try {
        packageManager.getPackageInfo(appId, PackageAppCatalog.packageFlags)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    fun resolveAppId(action: String, category: String? = null): String? {
        val intent = Intent(action)
        if (category != null) {
            intent.addCategory(category)
        }
        return resolveAppId(intent)
    }

    fun startLaunchActivity() {
        val intent = packageManager.getLaunchIntentForPackage(META.appId)!!
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        startActivity(intent)
    }

    fun checkGrantedPermission(permission: String) = ContextCompat.checkSelfPermission(
        this,
        permission,
    ) == PackageManager.PERMISSION_GRANTED

    val startTime = System.currentTimeMillis()


    val activityManager by lazy { app.getSystemService(ACTIVITY_SERVICE) as ActivityManager }
    val appOpsManager by lazy { app.getSystemService(APP_OPS_SERVICE) as AppOpsManager }
    val inputMethodManager by lazy { app.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager }
    val inputManager by lazy { app.getSystemService(INPUT_SERVICE) as InputManager }
    val mediaProjectionManager by lazy { getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    val windowManager by lazy { app.getSystemService(WINDOW_SERVICE) as WindowManager }
    val displayManager by lazy { app.getSystemService(DISPLAY_SERVICE) as DisplayManager }
    val keyguardManager by lazy { app.getSystemService(KEYGUARD_SERVICE) as KeyguardManager }
    val clipboardManager by lazy { app.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager }
    val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }
    val a11yManager by lazy { getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager }
    val launcherApps by lazy { getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps }

    val compatDisplay: Display
        get() = if (AndroidTarget.R) {
            displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }

    override fun onCreate() {
        super.onCreate()
        AndroidToastUtils.init()
        installCrashHandler()
        Db.initialize(this, AndroidStorage.storage.database.absolutePath)
        AppInfoRepository.initialize()
        LogUtils.d()
        initializeRuntimeComponents()
    }

    private fun installCrashHandler() {
        val recorder = CrashRecorder(
            CrashMetadata(
                AndroidLogMetadata.deviceDescription,
                Build.VERSION.SDK_INT,
                Build.VERSION.RELEASE,
                META.versionCode,
                META.versionName
            )
        )
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            ToastUtils.show(e.message ?: e.toString())
            LogUtils.d("UncaughtExceptionHandler", t, e)
            appScope.launchLogged(Dispatchers.IO) {
                try {
                    recorder.record(t, e)
                    delay(1500.milliseconds)
                    if (MainActivityVisibility.isVisible) {
                        startLaunchActivity()
                    }
                } finally {
                    try {
                        withContext(NonCancellable + Dispatchers.IO) { LogUtils.close() }
                    } finally {
                        killProcess(myPid())
                    }
                }
            }
        }
    }

    private fun initializeRuntimeComponents() {
        appScope.launchLogged(Dispatchers.IO) {
            ExposeService.initCommandFile()
        }
        NotificationChannels.initialize()
        PackageAppCatalog.initialize()
        initA11yFeat()

        appScope.launchLogged(Dispatchers.IO) {
            SubscriptionRepository.initialize(subscriptionDefaults())
        }
        RuntimeStateSynchronizer.requestSync()
    }
}
