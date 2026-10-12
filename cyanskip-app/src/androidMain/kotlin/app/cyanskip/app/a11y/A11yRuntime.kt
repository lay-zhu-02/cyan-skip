package app.cyanskip.app.a11y

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.cyanskip.app.data.ActionPerformer
import app.cyanskip.app.model.ActionResult
import app.cyanskip.app.model.CsAction
import app.cyanskip.app.model.RpcError
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.screen_nodes_unavailable
import app.cyanskip.app.resources.selector_invalid
import app.cyanskip.app.resources.selector_node_not_found
import app.cyanskip.app.resources.selector_type_error
import app.cyanskip.app.resources.service_not_connected
import app.cyanskip.app.service.A11yService
import app.cyanskip.selector.MatchOptions
import app.cyanskip.selector.Selector
import app.cyanskip.selector.SelectorCompileResult
import app.cyanskip.selector.SelectorTypeResult
import org.jetbrains.compose.resources.getString

object A11yRuntime {
    val service: app.cyanskip.app.service.A11yService?
        get() = A11yService.instance

    fun onA11yConnected(service: A11yService) {
        service.ruleEngine.onA11yConnected()
    }

    // Keep each read bound to its selected service and update that engine's root cache.
    fun getRoot(service: A11yService? = this.service): AccessibilityNodeInfo? =
        service?.ruleEngine?.safeActiveWindow

    fun compatWindows(): List<AccessibilityWindowInfo> {
        return try {
            service?.windowInfos
        } catch (_: Throwable) {
            null
        } ?: emptyList()
    }

    fun onScreenForcedActive() {
        service?.ruleEngine?.onScreenForcedActive()
    }

    fun performActionBack(): Boolean {
        return A11yService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) == true
    }

    suspend fun screenshot(): Bitmap? = service?.screenshot()

    suspend fun execAction(csAction: CsAction): ActionResult {
        val selectorResult = Selector.compile(csAction.selector)
        val selector = (selectorResult as? SelectorCompileResult.Success)?.value
            ?: throw RpcError(getString(Res.string.selector_invalid))
        val typeResult = selector.validateType(selectorTypeModel)
        if (typeResult is SelectorTypeResult.Failure) {
            throw RpcError(getString(Res.string.selector_type_error, typeResult.error.message))
        }
        val service = service ?: throw RpcError(getString(Res.string.service_not_connected))
        val root =
            getRoot(service) ?: throw RpcError(getString(Res.string.screen_nodes_unavailable))
        val targetNode = A11yContext(
            getRoot = { getRoot(service) },
            interruptable = false,
        ).querySelfOrSelector(
            root, selector, MatchOptions(fastQuery = csAction.fastQuery)
        ) ?: throw RpcError(getString(Res.string.selector_node_not_found))
        return withContext(Dispatchers.IO) {
            ActionPerformer
                .getAction(csAction.action ?: ActionPerformer.None.action)
                .perform(targetNode, csAction)
        }
    }
}
