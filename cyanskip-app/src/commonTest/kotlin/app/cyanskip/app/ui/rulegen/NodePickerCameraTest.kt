package app.cyanskip.app.ui.rulegen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodePickerCameraTest {
    // 截图 2000x1000, 视口 500x500 -> 完整显示时缩放 0.25, 画面 500x250 居中
    private val initial = NodePickerCamera.initial(2000f, 1000f, Size(500f, 500f))

    @Test
    fun initialShowsWholeImageCentered() {
        assertFalse(initial.isZoomed)
        assertClose(0.25f, initial.scale)
        assertClose(0f, initial.left)
        assertClose(125f, initial.top)
    }

    @Test
    fun zoomKeepsThePointUnderTheGestureFixed() {
        val anchor = Offset(320f, 300f)
        val imagePoint = initial.toImage(anchor)
        val zoomed = initial.zoomAt(3f, anchor)
        assertClose(3f, zoomed.zoomFactor)
        assertOffsetClose(imagePoint, zoomed.toImage(anchor))
    }

    @Test
    fun dragKeepsThePointUnderTheFingerFixed() {
        val zoomed = initial.zoomAt(4f, Offset(250f, 250f))
        val pan = Offset(40f, -30f)
        val startAnchor = Offset(220f, 260f)
        val imagePoint = zoomed.toImage(startAnchor - pan)
        val dragged = zoomed.transform(startAnchor, zoom = 1f, pan = pan)
        assertOffsetClose(imagePoint, dragged.toImage(startAnchor))
    }

    @Test
    fun zoomIsLimitedToTheConfiguredRange() {
        assertClose(NodePickerCamera.MAX_ZOOM, initial.zoomAt(99f, Offset(10f, 10f)).zoomFactor)
        assertClose(NodePickerCamera.MIN_ZOOM, initial.zoomAt(0.01f, Offset(10f, 10f)).zoomFactor)
    }

    @Test
    fun draggingStopsAtTheImageEdgesSoEdgeElementsReachTheMiddle() {
        // 放大到两个方向都超出视口, 才谈得上上下左右拖动
        val zoomed = initial.zoomAt(4f, Offset(250f, 250f))
        val dragged = zoomed.transform(
            centroid = Offset(250f, 250f),
            zoom = 1f,
            pan = Offset(5000f, 5000f),
        )
        // 贴边的元素可以拖到屏幕中间: 相机中心允许停在图片边缘
        assertClose(0f, dragged.centerX)
        assertClose(0f, dragged.centerY)
        // 但不会越过图片
        val other = zoomed.transform(
            centroid = Offset(250f, 250f),
            zoom = 1f,
            pan = Offset(-5000f, -5000f),
        )
        assertClose(2000f, other.centerX)
        assertClose(1000f, other.centerY)
    }

    @Test
    fun viewportChangeKeepsZoomAndCenter() {
        // 选中后下方出现选项面板, 视口变小: 画面不应跳回完整显示
        val zoomed = initial.zoomAt(2.5f, Offset(250f, 250f))
        val resized = zoomed.withViewport(500f, 300f)
        assertClose(2.5f, resized.zoomFactor)
        assertClose(zoomed.centerX, resized.centerX)
        assertClose(zoomed.centerY, resized.centerY)
    }

    @Test
    fun doubleTapTogglesBetweenFitAndZoom() {
        val anchor = Offset(200f, 200f)
        val zoomed = initial.toggleZoom(anchor)
        assertTrue(zoomed.isZoomed)
        assertClose(NodePickerCamera.DOUBLE_ZOOM, zoomed.zoomFactor)
        val restored = zoomed.toggleZoom(anchor)
        assertFalse(restored.isZoomed)
        assertClose(initial.left, restored.left)
        assertClose(initial.top, restored.top)
    }

    @Test
    fun viewportCenterMapsToTheCameraCenter() {
        val zoomed = initial.zoomAt(2f, Offset(100f, 400f))
        // 视口中心就是相机中心
        assertOffsetClose(Offset(zoomed.centerX, zoomed.centerY), zoomed.toImage(Offset(250f, 250f)))
        // 视口移动 10 像素, 对应的截图坐标按缩放倍数换算
        assertOffsetClose(
            Offset(zoomed.centerX + 10f / zoomed.scale, zoomed.centerY),
            zoomed.toImage(Offset(260f, 250f)),
        )
    }

    @Test
    fun focusingAnOffscreenNodeCentersItWithoutChangingZoom() {
        val zoomed = initial.zoomAt(4f, Offset(250f, 250f))
        val farAway = ImageBounds(left = 1400f, top = 400f, right = 1500f, bottom = 500f)
        val focused = zoomed.focusOn(farAway)
        assertClose(zoomed.zoomFactor, focused.zoomFactor)
        assertClose(1450f, focused.centerX)
        assertClose(450f, focused.centerY)
    }

    @Test
    fun focusingACornerElementCentersItExactly() {
        val zoomed = initial.zoomAt(4f, Offset(250f, 250f))
        val corner = ImageBounds(left = 1900f, top = 950f, right = 1990f, bottom = 990f)
        val focused = zoomed.focusOn(corner)
        assertClose(1945f, focused.centerX)
        assertClose(970f, focused.centerY)
    }

    @Test
    fun focusingAVisibleNodeChangesNothing() {
        val zoomed = initial.zoomAt(4f, Offset(250f, 250f))
        val visible = ImageBounds(
            left = zoomed.centerX - 10f,
            top = zoomed.centerY - 10f,
            right = zoomed.centerX + 10f,
            bottom = zoomed.centerY + 10f,
        )
        val focused = zoomed.focusOn(visible, padding = 1f)
        assertClose(zoomed.centerX, focused.centerX)
        assertClose(zoomed.centerY, focused.centerY)
    }

    @Test
    fun focusingWhileFitChangesNothing() {
        val bounds = ImageBounds(left = 1800f, top = 900f, right = 1900f, bottom = 960f)
        val focused = initial.focusOn(bounds)
        assertClose(initial.centerX, focused.centerX)
        assertClose(initial.centerY, focused.centerY)
    }

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected=$expected actual=$actual",
        )
    }

    private fun assertOffsetClose(expected: Offset, actual: Offset, tolerance: Float = 0.1f) {
        assertTrue(
            (expected - actual).getDistance() <= tolerance,
            "expected=$expected actual=$actual",
        )
    }
}
