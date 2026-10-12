package app.cyanskip.app.ui.rulegen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.cyanskip.app.rulegen.SnapshotNode

/**
 * 在快照截图上点选节点.
 *
 * 截图与叠加框由同一块画布、同一套变换绘制, 因此缩放后两者不会错开; 单指拖动、双指缩放、
 * 双击放大/还原, 单击立即选中(不等待双击判定).
 */
@Composable
fun CsSnapshotNodePicker(
    painter: Painter,
    mapping: SnapshotImageMapping,
    outlines: List<SnapshotNode>,
    matches: List<SnapshotNode>,
    selected: SnapshotNode?,
    showOutlines: Boolean,
    /** 从元素列表选中时递增, 用于把该元素移到视野内. */
    focusToken: Int = 0,
    focusNode: SnapshotNode? = null,
    onPick: (screenX: Int, screenY: Int) -> Unit,
    /** 点到截图以外的空白处, 用于取消选中. */
    onClearSelection: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val imageWidth = mapping.imageWidth.toFloat().coerceAtLeast(1f)
    val imageHeight = mapping.imageHeight.toFloat().coerceAtLeast(1f)
    val selectionColor = MaterialTheme.colorScheme.primary
    val matchColor = Color(0xFFFFB300)
    val outlineColor = MaterialTheme.colorScheme.primary
    val selectionHalo = Color.Black.copy(alpha = 0.45f)
    var camera by remember(imageWidth, imageHeight) {
        mutableStateOf<NodePickerCamera?>(null)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(Color.Black).clipToBounds(),
    ) {
        val viewport = with(density) {
            Size(maxWidth.toPx(), maxHeight.toPx())
        }
        val active = remember(camera, viewport, imageWidth, imageHeight) {
            (camera ?: NodePickerCamera.initial(imageWidth, imageHeight, viewport))
                .withViewport(viewport.width, viewport.height)
        }
        val lastTap = remember { TapTracker() }

        LaunchedEffect(focusToken) {
            val node = focusNode ?: return@LaunchedEffect
            if (focusToken <= 0) return@LaunchedEffect
            val base = (camera ?: active).withViewport(viewport.width, viewport.height)
            camera = base.focusOn(mapping.bounds(node))
        }

        Box(
            modifier = Modifier.fillMaxSize()
                .pointerInput(imageWidth, imageHeight, viewport) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var moved = false
                        var zoomed = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.count { it.pressed }
                            if (pressed == 0) break
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (pressed > 1 || zoom != 1f) zoomed = true
                            if (pan.getDistance() > 0f || zoom != 1f) {
                                val current = camera ?: active
                                camera = current
                                    .withViewport(viewport.width, viewport.height)
                                    .transform(centroid, zoom, pan)
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) change.consume()
                                }
                            }
                            if ((event.changes.first().position - down.position).getDistance() >
                                viewConfiguration.touchSlop
                            ) {
                                moved = true
                            }
                        }
                        if (moved || zoomed) return@awaitEachGesture
                        val position = down.position
                        val tapTime = down.uptimeMillis
                        val isDoubleTap = lastTap.matches(position, tapTime)
                        lastTap.record(position, tapTime)
                        if (isDoubleTap) {
                            camera = active.toggleZoom(position)
                        } else {
                            val image = active.toImage(position)
                            val insideImage = image.x >= 0f && image.y >= 0f &&
                                    image.x <= imageWidth && image.y <= imageHeight
                            if (insideImage) {
                                onPick(
                                    mapping.imageXToScreen(image.x),
                                    mapping.imageYToScreen(image.y),
                                )
                            } else {
                                // 截图以外的空白区域: 取消选中
                                onClearSelection()
                            }
                        }
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                clipRect(0f, 0f, size.width, size.height) {
                    translate(active.left, active.top) {
                        scale(active.scale, active.scale, pivot = Offset.Zero) {
                            with(painter) {
                                draw(Size(imageWidth, imageHeight))
                            }
                            fun drawBounds(node: SnapshotNode, color: Color, stroke: Float, fill: Float) {
                                val bounds = mapping.bounds(node)
                                if (bounds.width <= 0f || bounds.height <= 0f) return
                                val topLeft = Offset(bounds.left, bounds.top)
                                val size = Size(bounds.width, bounds.height)
                                if (fill > 0f) {
                                    drawRect(
                                        color = color.copy(alpha = fill),
                                        topLeft = topLeft,
                                        size = size,
                                    )
                                }
                                drawRect(
                                    color = color,
                                    topLeft = topLeft,
                                    size = size,
                                    style = Stroke(width = stroke / active.scale),
                                )
                            }
                            if (showOutlines) {
                                // 已选中时把其它可选位置画淡一些, 避免盖过选中框
                                val outlineFill = if (selected == null) 0.12f else 0f
                                val outlineStroke =
                                    if (selected == null) 1.5.dp.toPx() else 1.dp.toPx()
                                val outlineAlpha = if (selected == null) 1f else 0.45f
                                outlines.forEach { node ->
                                    if (node.id != selected?.id) {
                                        drawBounds(
                                            node,
                                            outlineColor.copy(alpha = outlineAlpha),
                                            outlineStroke,
                                            outlineFill,
                                        )
                                    }
                                }
                            }
                            matches.forEach { node ->
                                if (node.id != selected?.id) {
                                    drawBounds(node, matchColor, 2.dp.toPx(), 0.18f)
                                }
                            }
                            // 选中框: 深色外圈 + 白色内圈, 在任何背景上都看得见
                            selected?.let { node ->
                                drawBounds(node, selectionHalo, 4.dp.toPx(), 0f)
                                drawBounds(node, Color.White, 2.5.dp.toPx(), 0f)
                                drawBounds(node, selectionColor, 1.dp.toPx(), 0.22f)
                            }
                        }
                    }
                }
            }
        }
    }
}

private class TapTracker {
    private var position: Offset = Offset.Zero
    private var uptime: Long = 0L

    fun matches(position: Offset, uptime: Long): Boolean =
        this.uptime != 0L && uptime - this.uptime <= DOUBLE_TAP_TIMEOUT &&
                (this.position - position).getDistance() <= DOUBLE_TAP_SLOP

    fun record(position: Offset, uptime: Long) {
        this.position = position
        this.uptime = uptime
    }

    companion object {
        private const val DOUBLE_TAP_TIMEOUT = 300L
        private const val DOUBLE_TAP_SLOP = 48f
    }
}
