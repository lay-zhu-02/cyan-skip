package app.cyanskip.app.platform.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import app.cyanskip.app.util.LogUtils

/** Keeps a one-pixel accessibility overlay attached so the service process survives background cleanup. */
object KeepAliveOverlayCoordinator {
    val accessibilityAttached: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private var attachedOverlay: AttachedOverlay? = null
    private var overlayOwner: Any? = null

    @Synchronized
    fun acquire(
        owner: Any,
        context: Context,
        windowType: Int,
    ): Boolean {
        if (overlayOwner === owner) return true
        val overlay = runCatching { KeepAliveOverlay(context, windowType) }
            .onFailure(LogUtils::d)
            .getOrNull() ?: return false
        attachedOverlay?.overlay?.close()
        attachedOverlay = AttachedOverlay(owner, overlay)
        overlayOwner = owner
        accessibilityAttached.value = true
        return true
    }

    @Synchronized
    fun release(owner: Any) {
        val current = attachedOverlay
        if (current?.owner !== owner) return
        attachedOverlay = null
        overlayOwner = null
        accessibilityAttached.value = false
        current.overlay.close()
    }

    private data class AttachedOverlay(
        val owner: Any,
        val overlay: KeepAliveOverlay,
    )

    private class KeepAliveOverlay(
        context: Context,
        windowType: Int,
    ) : AutoCloseable {
        private val windowManager = context.getSystemService(WindowManager::class.java)
        private val view = View(context)
        private var attached = false

        init {
            val layoutParams = WindowManager.LayoutParams().apply {
                type = windowType
                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                gravity = Gravity.START or Gravity.TOP
                width = 1
                height = 1
                packageName = context.packageName
            }
            windowManager.addView(view, layoutParams)
            attached = true
        }

        @Synchronized
        override fun close() {
            if (!attached) return
            attached = false
            runCatching { windowManager.removeView(view) }
                .onFailure(LogUtils::d)
        }
    }

}
