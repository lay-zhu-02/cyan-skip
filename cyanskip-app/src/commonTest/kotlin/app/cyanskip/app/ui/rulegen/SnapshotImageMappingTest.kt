package app.cyanskip.app.ui.rulegen

import kotlin.test.Test
import kotlin.test.assertEquals
import app.cyanskip.app.rulegen.SnapshotNodeTree
import app.cyanskip.app.rulegen.adScreen

class SnapshotImageMappingTest {
    @Test
    fun fullSizeImageKeepsScreenCoordinates() {
        val mapping = SnapshotImageMapping(
            screenWidth = 1080,
            screenHeight = 2400,
            imageWidth = 1080,
            imageHeight = 2400,
        )
        assertEquals(1f, mapping.scale)
        val bounds = mapping.bounds(skipButton())
        assertEquals(860f, bounds.left)
        assertEquals(820f, bounds.top)
        assertEquals(960f, bounds.right)
        assertEquals(880f, bounds.bottom)
    }

    @Test
    fun croppedStatusBarShiftsNodeBoundsUp() {
        // 隐藏状态栏后截图比屏幕矮 120 像素, 节点坐标需要整体上移.
        val mapping = SnapshotImageMapping(
            screenWidth = 1080,
            screenHeight = 2400,
            imageWidth = 1080,
            imageHeight = 2280,
        )
        val bounds = mapping.bounds(skipButton())
        assertEquals(700f, bounds.top)
        assertEquals(760f, bounds.bottom)
    }

    @Test
    fun tapPositionRoundTripsThroughScreenCoordinates() {
        val mapping = SnapshotImageMapping(
            screenWidth = 1080,
            screenHeight = 2400,
            imageWidth = 540,
            imageHeight = 1140,
        )
        assertEquals(0.5f, mapping.scale)
        assertEquals(430, mapping.imageXToScreen(mapping.screenXToImage(430)))
        assertEquals(1500, mapping.imageYToScreen(mapping.screenYToImage(1500)))
        // 超出截图范围的位置被裁剪到截图内.
        assertEquals(0f, mapping.screenYToImage(-100))
        assertEquals(1140f, mapping.screenYToImage(5000))
    }

    private fun skipButton() = SnapshotNodeTree.build(adScreen()).node(3)!!
}
