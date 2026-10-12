package app.cyanskip.app.data

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay
import app.cyanskip.app.a11y.A11yRuntime
import app.cyanskip.app.model.ActionResult
import app.cyanskip.app.util.toHidden
import app.cyanskip.app.service.A11yService
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.util.ScreenUtils
import kotlin.time.Duration.Companion.milliseconds

sealed class ActionPerformer(val action: String) {
    abstract suspend fun perform(
        node: AccessibilityNodeInfo,
        locationProps: RawSubscription.LocationProps,
    ): ActionResult

    data object ClickNode : ActionPerformer("clickNode") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
        ): ActionResult {
            return ActionResult(
                action = action,
                result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            )
        }
    }

    data object ClickCenter : ActionPerformer("clickCenter") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
        ): ActionResult {
            val rect = node.toHidden.boundsInScreen
            val p = locationProps.position?.calc(rect)
            val x = p?.first ?: ((rect.right + rect.left) / 2f)
            val y = p?.second ?: ((rect.bottom + rect.top) / 2f)
            if (!ScreenUtils.contains(x, y)) {
                return ActionResult(
                    action = action,
                    result = false,
                    position = x to y,
                )
            }
            return ActionResult(
                action = action,
                result = run {
                    val gestureDescription = GestureDescription.Builder()
                    val path = Path()
                    path.moveTo(x, y)
                    gestureDescription.addStroke(
                        GestureDescription.StrokeDescription(
                            path, 0, ViewConfiguration.getTapTimeout().toLong()
                        )
                    )
                    A11yService.instance?.dispatchGesture(
                        gestureDescription.build(), null, null
                    ) == true
                },
                position = x to y
            )
        }
    }

    data object Click : ActionPerformer("click") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
        ): ActionResult {
            if (node.isClickable) {
                val result = ClickNode.perform(node, locationProps)
                if (result.result) {
                    return result
                }
            }
            return ClickCenter.perform(node, locationProps)
        }
    }

    data object LongClickNode : ActionPerformer("longClickNode") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
        ): ActionResult {
            return ActionResult(
                action = action,
                result = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK).apply {
                    if (this) {
                        delay(LongClickCenter.LONG_DURATION.milliseconds)
                    }
                }
            )
        }
    }

    data object LongClickCenter : ActionPerformer("longClickCenter") {
        const val LONG_DURATION = 500L
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
        ): ActionResult {
            val rect = node.toHidden.boundsInScreen
            val p = locationProps.position?.calc(rect)
            val x = p?.first ?: ((rect.right + rect.left) / 2f)
            val y = p?.second ?: ((rect.bottom + rect.top) / 2f)
            // 某些系统的 ViewConfiguration.getLongPressTimeout() 返回 300 , 这将导致触发普通的 click 事件
            if (!ScreenUtils.contains(x, y)) {
                return ActionResult(
                    action = action,
                    result = false,
                    position = x to y,
                )
            }
            return ActionResult(
                action = action,
                result = run {
                    val gestureDescription = GestureDescription.Builder()
                    val path = Path()
                    path.moveTo(x, y)
                    gestureDescription.addStroke(
                        GestureDescription.StrokeDescription(
                            path, 0, LONG_DURATION
                        )
                    )
                    (A11yService.instance?.dispatchGesture(
                        gestureDescription.build(), null, null
                    ) == true).apply {
                        if (this) {
                            delay(LONG_DURATION.milliseconds)
                        }
                    }
                },
                position = x to y
            )
        }
    }

    data object LongClick : ActionPerformer("longClick") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
        ): ActionResult {
            if (node.isLongClickable) {
                val result = LongClickNode.perform(node, locationProps)
                if (result.result) {
                    return result
                }
            }
            return LongClickCenter.perform(node, locationProps)
        }
    }

    data object Back : ActionPerformer("back") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
        ): ActionResult {
            return ActionResult(
                action = action,
                result = A11yRuntime.performActionBack()
            )
        }
    }

    data object None : ActionPerformer("none") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
        ): ActionResult {
            return ActionResult(
                action = action,
                result = true
            )
        }
    }

    data object Swipe : ActionPerformer("swipe") {
        override suspend fun perform(
            node: AccessibilityNodeInfo,
            locationProps: RawSubscription.LocationProps,
        ): ActionResult {
            val rect = node.toHidden.boundsInScreen
            val swipeArg = locationProps.swipeArg ?: return ActionResult(
                action = action,
                result = false,
            )
            val startP = swipeArg.start.calc(rect)
            val endP = swipeArg.end?.calc(rect) ?: startP
            if (startP == null || endP == null) {
                return ActionResult(
                    action = action,
                    result = false,
                )
            }
            val startX = startP.first
            val startY = startP.second
            val endX = endP.first
            val endY = endP.second
            if (!(ScreenUtils.contains(startX, startY) && ScreenUtils.contains(endX, endY))) {
                return ActionResult(
                    action = action,
                    result = false,
                    position = endX to endY,
                )
            }
            return run {
                val gestureDescription = GestureDescription.Builder()
                val path = Path()
                path.moveTo(startX, startY)
                path.lineTo(endX, endY)
                gestureDescription.addStroke(
                    GestureDescription.StrokeDescription(
                        path, 0, swipeArg.duration
                    )
                )
                ActionResult(
                    action = action,
                    result = (A11yService.instance?.dispatchGesture(
                        gestureDescription.build(), null, null
                    ) == true).apply {
                        if (this) {
                            delay(swipeArg.duration.milliseconds)
                        }
                    },
                    position = endX to endY,
                )
            }
        }
    }

    companion object {
        private val allSubObjects by lazy {
            arrayOf(
                ClickNode,
                ClickCenter,
                Click,
                LongClickNode,
                LongClickCenter,
                LongClick,
                Back,
                None,
                Swipe,
            )
        }

        fun getAction(action: String?): ActionPerformer {
            return allSubObjects.find { it.action == action } ?: Click
        }
    }
}
