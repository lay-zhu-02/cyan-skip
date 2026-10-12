package app.cyanskip.app.rulegen

import app.cyanskip.app.model.NodeInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnapshotRuleGeneratorTest {
    @Test
    fun treeRebuildsParentChildLinksFromPid() {
        val tree = SnapshotNodeTree.build(adScreen())
        val root = assertNotNull(tree.root)
        assertEquals(0, root.id)
        assertEquals(6, tree.nodes.size)
        val dialog = assertNotNull(tree.node(1))
        assertEquals(listOf(2, 3, 4), dialog.children.map { it.id })
        assertEquals(listOf(1, 0), tree.node(3)!!.ancestors().map { it.id }.toList())
        assertEquals(1, tree.node(2)!!.parent?.id)
        assertEquals(0, dialog.parent?.id)
        assertEquals(0, tree.node(5)!!.parent?.id)
    }

    @Test
    fun adapterMirrorsAccessibilityAttributes() {
        val tree = SnapshotNodeTree.build(adScreen())
        val adapter = SnapshotNodeAdapter(tree)
        val skip = assertNotNull(tree.node(3))
        assertEquals("跳过", adapter.getAttr(skip, "text"))
        assertEquals(true, adapter.getAttr(skip, "clickable"))
        assertEquals(true, adapter.getAttr(skip, "visibleToUser"))
        assertEquals(1, adapter.getAttr(skip, "index"))
        assertEquals(3, adapter.getAttr(skip, "depth"))
        assertEquals(0, adapter.getAttr(skip, "childCount"))
        assertEquals("android.widget.TextView", adapter.getName(skip))
        assertEquals(3, adapter.getNodeKey(skip))
        val dialog = assertNotNull(tree.node(1))
        assertEquals(3, adapter.getChildCount(dialog))
        assertEquals(3, adapter.getChild(dialog, 1)?.id)
        assertNull(adapter.getChild(dialog, 3))
        assertEquals(1, adapter.getParent(skip)?.id)
        assertEquals(0, adapter.getRoot(skip)?.id)
    }

    @Test
    fun identicalTextUsesAUniqueSingleUnitSelectorWhenOneExists() {
        val tree = SnapshotNodeTree.build(adScreen())
        val options = assertNotNull(SnapshotRuleGenerator(tree).draftOptions(tree.node(3)!!))
        // 快照上还有第二个"跳过", 因此最稳的写法会命中两处.
        assertEquals("[text=\"跳过\"]", options.similar.source)
        assertEquals(2, options.similar.matchCount)
        assertEquals(RuleStrength.Strong, options.similar.strength)
        // 只点选中的一个时, 优先使用不依赖节点关系、同样命中唯一目标的写法.
        val unique = assertNotNull(options.unique)
        assertTrue(unique.isUnique)
        assertFalse(unique.relationBased)
        assertEquals(listOf(3), unique.matches.map { it.id })
    }

    @Test
    fun uniqueTextProducesExactTextSelector() {
        val tree = SnapshotNodeTree.build(adScreen().filter { it.id != 5 })
        val options = assertNotNull(SnapshotRuleGenerator(tree).draftOptions(tree.node(3)!!))
        assertEquals("[text=\"跳过\"]", options.similar.source)
        assertEquals(options.similar, options.unique)
        assertEquals(1, options.similar.matchCount)
        assertEquals(RuleKeyKind.Text, options.similar.keyKind)
        assertEquals("跳过", options.similar.keyValue)
    }

    @Test
    fun countdownTextIsAlsoOfferedAsAPrefixMatch() {
        val nodes = adScreen().map { info ->
            if (info.id == 3) info.copy(attr = info.attr.copy(text = "跳过 3s")) else info
        }.filter { it.id != 5 }
        val tree = SnapshotNodeTree.build(nodes)
        val options = assertNotNull(SnapshotRuleGenerator(tree).draftOptions(tree.node(3)!!))
        assertEquals("[text=\"跳过 3s\"]", options.similar.source)
        val suggestions = SnapshotRuleGenerator(tree).suggestions(tree.node(3)!!)
        assertTrue(suggestions.any { it.source == "[text*=\"跳过\"]" })
    }

    @Test
    fun pictureTargetIsAnchoredToNeighbouringText() {
        val tree = SnapshotNodeTree.build(adScreen())
        val options = assertNotNull(SnapshotRuleGenerator(tree).draftOptions(tree.node(2)!!))
        val suggestion = assertNotNull(options.unique)
        assertTrue(suggestion.relationBased)
        assertEquals(listOf(2), suggestion.matches.map { it.id })
        assertTrue(suggestion.source.endsWith("ImageView"), suggestion.source)
    }

    @Test
    fun invalidSelectorIsRejectedByVerification() {
        val tree = SnapshotNodeTree.build(adScreen())
        val matcher = SnapshotSelectorMatcher(tree)
        assertNull(matcher.verify("[text="))
        assertNull(matcher.verify("[text=1]"))
        assertNotNull(matcher.verify("[text=\"跳过\"]"))
    }

    @Test
    fun tapPicksInnermostNodeAndOffersOuterCandidates() {
        val tree = SnapshotNodeTree.build(adScreen())
        val chain = tree.pickCandidates(900, 850)
        // 弹窗图片与"跳过"按钮重叠, 因此都会成为候选, 由内向外按面积排列.
        assertEquals(listOf(3, 2, 1, 0), chain.map { it.id })
        assertEquals(3, chain.first { it.attr.clickable }.id)
        assertTrue(tree.outlineNodes().map { it.id }.containsAll(listOf(3, 4, 5)))
    }

    @Test
    fun overlappingBannerDrawnLaterIsPickedInsteadOfTheCardBehindIt() {
        // 真机淘宝快照: 信息流卡片与底部横幅重叠, 用户点的是画在上层的横幅.
        val tree = SnapshotNodeTree.build(overlayScreen())
        val chain = tree.pickCandidates(1000, 2100)
        assertEquals(listOf(4, 3, 1, 2, 0).sorted(), chain.map { it.id }.sorted())
        assertEquals(4, chain.first().id)
        val options = assertNotNull(SnapshotRuleGenerator(tree).draftOptions(tree.node(4)!!))
        val primary = options.unique ?: options.similar
        // 横幅自身没有文字/编号, 唯一的稳健写法是"弹出层(按控件编号定位)里的那个元素"
        assertTrue(primary.source.startsWith("[vid=\"pop_view\"] "), primary.source)
        assertEquals(1, primary.matchCount)
        assertEquals(RuleKeyKind.Id, primary.keyKind)
        assertEquals("pop_view", primary.keyValue)
    }

    @Test
    fun cardBehindTheBannerIsStillSelectableOutsideTheOverlap() {
        val tree = SnapshotNodeTree.build(overlayScreen())
        assertEquals(2, tree.pickCandidates(1500, 2000).first().id)
    }

    @Test
    fun tappingBlankAreaHasNoTarget() {
        val tree = SnapshotNodeTree.build(overlayScreen())
        // 只有普通容器覆盖的位置, 视为空白: 不选中任何元素(界面据此取消选中)
        assertEquals(2, tree.pickTargets(1500, 2000).firstOrNull()?.id)
        assertNull(tree.pickTargets(20, 300).firstOrNull())
        val clickable = SnapshotNodeTree.build(adScreen())
        assertEquals(3, clickable.pickTargets(900, 850).firstOrNull()?.id)
    }

    @Test
    fun overlappingClickablesAreAllOfferedTopFirst() {
        val tree = SnapshotNodeTree.build(overlayScreen())
        // 横幅与卡片都能点, 两个都要给出来让用户挑
        assertEquals(listOf(4, 2), tree.pickTargets(1000, 2100).map { it.id })
        assertEquals(4, tree.pickTargets(1000, 2100).first().id)
        // 只压在卡片上的位置只有卡片一个候选
        assertEquals(listOf(2), tree.pickTargets(1500, 2000).map { it.id })
    }
}

/**
 * 底部弹出横幅压在信息流卡片上: 横幅在更晚绘制的一侧, 点在重叠区域时应选中横幅.
 */
private fun overlayScreen(): List<NodeInfo> = listOf(
    fixtureNode(
        id = 0, pid = -1, className = "android.widget.FrameLayout",
        right = 1080, bottom = 2400, childCount = 2,
    ),
    fixtureNode(
        id = 1, pid = 0, className = "android.widget.FrameLayout",
        right = 1080, bottom = 2400, index = 0, depth = 1, childCount = 1,
    ),
    fixtureNode(
        id = 2, pid = 1, className = "android.widget.LinearLayout",
        left = 900, top = 1900, right = 1660, bottom = 2200,
        index = 0, depth = 2, clickable = true, childCount = 4,
    ),
    fixtureNode(
        id = 3, pid = 0, className = "android.widget.FrameLayout",
        right = 1080, bottom = 2400, index = 1, depth = 1, childCount = 1,
        vid = "pop_view", viewId = "com.example.ad:id/pop_view",
    ),
    fixtureNode(
        id = 4, pid = 3, className = "android.widget.FrameLayout",
        left = 0, top = 2050, right = 1080, bottom = 2160,
        index = 0, depth = 2, clickable = true, childCount = 4,
    ),
)
