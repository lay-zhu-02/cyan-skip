package app.cyanskip.app.ui.rulegen

import kotlin.math.roundToInt
import app.cyanskip.app.rulegen.SnapshotNode

/** 截图上的一个矩形, 单位是截图像素. */
data class ImageBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * 截图与屏幕坐标的换算.
 *
 * 节点坐标来自屏幕, 而截图可能因为隐藏状态栏而被顶部裁剪, 因此纵向需要整体平移.
 */
data class SnapshotImageMapping(
    val screenWidth: Int,
    val screenHeight: Int,
    val imageWidth: Int,
    val imageHeight: Int,
) {
    /** 截图像素 / 屏幕像素. */
    val scale: Float =
        if (screenWidth > 0 && imageWidth > 0) imageWidth.toFloat() / screenWidth else 1f

    /** 截图相对屏幕的纵向偏移, 顶部被裁剪时为负数. */
    private val verticalShift: Int = imageHeight - screenHeight

    fun screenXToImage(x: Int): Float = (x * scale).coerceIn(0f, imageWidth.toFloat())

    fun screenYToImage(y: Int): Float =
        (y + verticalShift).toFloat().coerceIn(0f, imageHeight.toFloat())

    fun imageXToScreen(x: Float): Int = (x / scale).roundToInt()

    fun imageYToScreen(y: Float): Int = (y - verticalShift).roundToInt()

    fun bounds(node: SnapshotNode): ImageBounds = ImageBounds(
        left = screenXToImage(node.screenLeft),
        top = screenYToImage(node.screenTop),
        right = screenXToImage(node.screenRight),
        bottom = screenYToImage(node.screenBottom),
    )
}
