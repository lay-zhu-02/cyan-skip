package app.cyanskip.app.ui.rulegen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.max
import kotlin.math.min

/**
 * 快照查看的缩放与平移状态.
 *
 * 状态是"缩放倍数 + 视口中心对应的截图像素坐标", 因此视口尺寸变化(例如下方选项面板出现)
 * 时画面不会跳回原样, 截图与叠加层也始终使用同一套变换.
 */
data class NodePickerCamera(
    val imageWidth: Float,
    val imageHeight: Float,
    val viewportWidth: Float,
    val viewportHeight: Float,
    /** 相对"整张图完整显示"的倍数, 1 表示完整显示. */
    val zoomFactor: Float,
    /** 视口中心对应的截图坐标. */
    val centerX: Float,
    val centerY: Float,
) {
    val fitScale: Float
        get() = if (imageWidth <= 0f || imageHeight <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) {
            1f
        } else {
            min(viewportWidth / imageWidth, viewportHeight / imageHeight)
        }

    val scale: Float get() = fitScale * zoomFactor

    /** 截图左上角在视口中的位置. */
    val left: Float get() = viewportWidth / 2f - centerX * scale

    val top: Float get() = viewportHeight / 2f - centerY * scale

    val isZoomed: Boolean get() = zoomFactor > MIN_ZOOM + 0.01f

    fun withViewport(width: Float, height: Float): NodePickerCamera =
        copy(viewportWidth = width, viewportHeight = height).clamped()

    fun toImage(point: Offset): Offset = Offset(
        x = centerX + (point.x - viewportWidth / 2f) / scale,
        y = centerY + (point.y - viewportHeight / 2f) / scale,
    )

    /** 双指缩放与拖动; 手指下的截图像素保持不动. */
    fun transform(centroid: Offset, zoom: Float, pan: Offset): NodePickerCamera =
        zoomAt(
            factor = zoomFactor * zoom,
            anchor = centroid,
            pan = pan,
        )

    fun zoomAt(factor: Float, anchor: Offset, pan: Offset = Offset.Zero): NodePickerCamera {
        val target = factor.coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (target == zoomFactor && pan == Offset.Zero) return this
        // pan 是手势期间手指移动的距离, 手势开始时手指下的截图坐标要落在当前手指位置.
        val imagePoint = toImage(anchor - pan)
        val newScale = fitScale * target
        val next = copy(
            zoomFactor = target,
            centerX = imagePoint.x - (anchor.x - viewportWidth / 2f) / newScale,
            centerY = imagePoint.y - (anchor.y - viewportHeight / 2f) / newScale,
        )
        return next.clamped()
    }

    /** 双击在完整显示与放大之间切换. */
    fun toggleZoom(anchor: Offset): NodePickerCamera =
        if (isZoomed) reset() else zoomAt(DOUBLE_ZOOM, anchor)

    fun reset(): NodePickerCamera = copy(
        zoomFactor = MIN_ZOOM,
        centerX = imageWidth / 2f,
        centerY = imageHeight / 2f,
    ).clamped()

    /** 让某个截图区域可见: 已经完整可见就不动, 否则把它的中心移到视口中心. */
    fun focusOn(bounds: ImageBounds, padding: Float = 0f): NodePickerCamera {
        if (zoomFactor <= MIN_ZOOM + 0.001f) return this
        val visibleLeft = (0f - left) / scale
        val visibleTop = (0f - top) / scale
        val visibleRight = (viewportWidth - left) / scale
        val visibleBottom = (viewportHeight - top) / scale
        val fullyVisible = bounds.left >= visibleLeft + padding &&
                bounds.top >= visibleTop + padding &&
                bounds.right <= visibleRight - padding &&
                bounds.bottom <= visibleBottom - padding
        if (fullyVisible) return this
        return copy(
            centerX = (bounds.left + bounds.right) / 2f,
            centerY = (bounds.top + bounds.bottom) / 2f,
        ).clamped()
    }

    /** 保证画面不露出空白: 小于视口的方向居中, 大于视口的方向夹在边界内. */
    fun clamped(): NodePickerCamera {
        val contentWidth = imageWidth * scale
        val contentHeight = imageHeight * scale
        // 放大后允许相机中心走到图片边缘, 这样贴边的元素也能拖到屏幕中间选中;
        // 完整显示时(没放大)始终居中, 不出现位移。
        val x = if (zoomFactor <= MIN_ZOOM + 0.001f || contentWidth <= viewportWidth) {
            imageWidth / 2f
        } else {
            centerX.coerceIn(0f, imageWidth)
        }
        val y = if (zoomFactor <= MIN_ZOOM + 0.001f || contentHeight <= viewportHeight) {
            imageHeight / 2f
        } else {
            centerY.coerceIn(0f, imageHeight)
        }
        return if (x == centerX && y == centerY) this else copy(centerX = x, centerY = y)
    }

    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 8f
        const val DOUBLE_ZOOM = 2.5f

        fun initial(
            imageWidth: Float,
            imageHeight: Float,
            viewport: Size,
        ): NodePickerCamera = NodePickerCamera(
            imageWidth = max(imageWidth, 1f),
            imageHeight = max(imageHeight, 1f),
            viewportWidth = viewport.width,
            viewportHeight = viewport.height,
            zoomFactor = MIN_ZOOM,
            centerX = max(imageWidth, 1f) / 2f,
            centerY = max(imageHeight, 1f) / 2f,
        ).clamped()
    }
}
