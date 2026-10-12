package app.cyanskip.app.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.google.android.accessibility.selecttospeak.SelectToSpeakService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import app.cyanskip.app.a11y.A11yRuleEngine
import app.cyanskip.app.a11y.A11yRuntime
import app.cyanskip.app.a11y.A11yState
import app.cyanskip.app.a11y.currentTopActivity
import app.cyanskip.app.a11y.updateTopActivity
import app.cyanskip.app.appScope
import app.cyanskip.app.platform.lifecycle.LifecycleHooks
import app.cyanskip.app.platform.overlay.KeepAliveOverlayCoordinator
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.a11y_keep_alive_failed
import app.cyanskip.app.resources.a11y_start_timeout
import app.cyanskip.app.resources.a11y_started
import app.cyanskip.app.resources.a11y_stopped
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.AndroidTarget
import app.cyanskip.app.util.LogUtils
import app.cyanskip.app.util.ToastUtils
import app.cyanskip.app.util.componentName
import kotlin.coroutines.resume

@SuppressLint("AccessibilityPolicy")
abstract class A11yService : AccessibilityService() {
    private val lifecycleHooks = LifecycleHooks()
    val scope = MainScope()
    val windowNodeInfo: AccessibilityNodeInfo? get() = rootInActiveWindow
    val windowInfos: List<AccessibilityWindowInfo> get() = windows
    suspend fun screenshot(): Bitmap? = suspendCancellableCoroutine { cont ->
        if (AndroidTarget.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                application.mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onFailure(errorCode: Int) {
                        if (cont.isActive) {
                            cont.resume(null)
                        }
                    }

                    override fun onSuccess(screenshot: ScreenshotResult) {
                        try {
                            if (cont.isActive) {
                                cont.resume(
                                    Bitmap.wrapHardwareBuffer(
                                        screenshot.hardwareBuffer, screenshot.colorSpace
                                    )
                                )
                            }
                        } finally {
                            screenshot.hardwareBuffer.close()
                        }
                    }
                }
            )
        } else {
            cont.resume(null)
        }
    }

    val ruleEngine by lazy { A11yRuleEngine(this) }

    override fun onInterrupt() {}
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = ruleEngine.onA11yEvent(event)

    private val startTime = System.currentTimeMillis()
    var justStarted: Boolean = true
        get() {
            if (field) {
                field = System.currentTimeMillis() - startTime < 3_000
            }
            return field
        }

    private var destroyed = false
    private var connected = false

    val wm by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    init {
        lifecycleHooks.useLogLifecycle(this)
        lifecycleHooks.onCreated {
            scope.launch {
                delay(3000)
                if (!(destroyed || connected)) {
                    ToastUtils.show(Res.string.a11y_start_timeout.getSync(), forced = true)
                }
            }
        }
        lifecycleHooks.onDestroyed {
            scope.cancel()
            if (instance === this) {
                instance = null
            }
            isRunning.value = false
            KeepAliveOverlayCoordinator.release(
                owner = this,
            )
            ToastUtils.show(Res.string.a11y_stopped.getSync())
            destroyed = true
        }
    }

    private fun attachKeepAliveOverlay() {
        if (!KeepAliveOverlayCoordinator.acquire(
                owner = this,
                context = this,
                windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            )
        ) {
            ToastUtils.show(Res.string.a11y_keep_alive_failed.getSync())
        }
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleHooks.dispatchCreated()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        LogUtils.d("onA11yConnected -> ${this::class.simpleName}")
        instance = this
        attachKeepAliveOverlay()
        connected = true
        ToastUtils.show(Res.string.a11y_started.getSync())
        isRunning.value = true
        A11yRuntime.onA11yConnected(this)
    }

    override fun onDestroy() {
        lifecycleHooks.dispatchDestroyed()
        super.onDestroy()
    }

    companion object {
        val a11yCn by lazy { SelectToSpeakService::class.componentName }
        val isRunning: StateFlow<Boolean>
            field = MutableStateFlow(false)

        @Volatile
        var instance: A11yService? = null
            private set
    }
}
