package app.cyanskip.app.ui.rulegen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import app.cyanskip.app.model.NodeInfo
import app.cyanskip.app.rulegen.SnapshotNodeTree
import app.cyanskip.app.rulegen.fixtureNode

class NodeTreeRowsTest {
    @Test
    fun onlyExpandedBranchesAreListed() {
        val tree = tree()
        val rows = nodeTreeRows(tree.root, expanded = setOf(0), onlyClickable = false)
        assertEquals(listOf(0, 1, 4), rows.map { it.id })
        assertEquals(listOf(0, 1, 1), rows.map { it.depth })
        assertTrue(rows.first().expanded)
        assertTrue(rows[1].hasChildren)
        assertFalse(rows[1].expanded)
        assertFalse(rows.last().hasChildren)
    }

    @Test
    fun expandingABranchShowsItsChildren() {
        val tree = tree()
        val rows = nodeTreeRows(tree.root, expanded = setOf(0, 1, 2), onlyClickable = false)
        assertEquals(listOf(0, 1, 2, 3, 4), rows.map { it.id })
        assertEquals(listOf(0, 1, 2, 3, 1), rows.map { it.depth })
    }

    @Test
    fun defaultExpansionRevealsTheSelectedNodePath() {
        val tree = tree()
        val expanded = nodeTreeDefaultExpanded(tree.root, tree.node(3)!!)
        val rows = nodeTreeRows(tree.root, expanded, onlyClickable = false)
        assertEquals(listOf(0, 1, 2, 3, 4), rows.map { it.id })
    }

    @Test
    fun clickableOnlyKeepsMatchesAndTheirAncestors() {
        val tree = tree()
        val rows = nodeTreeRows(tree.root, expanded = emptySet(), onlyClickable = true)
        // 只有叶子是可点击的, 它上面的容器作为上下文保留
        assertEquals(listOf(0, 1, 2, 3), rows.map { it.id })
        assertTrue(rows.last().node.attr.clickable)
    }

    @Test
    fun indentationStopsAtTheCapSoDeepRowsStayReadable() {
        assertEquals(0, cappedIndentLevels(0))
        assertEquals(3, cappedIndentLevels(3))
        assertEquals(MAX_INDENT_LEVELS, cappedIndentLevels(MAX_INDENT_LEVELS))
        assertEquals(MAX_INDENT_LEVELS, cappedIndentLevels(24))
    }

    /**
     * 0 根
     * ├─ 1 容器
     * │   └─ 2 容器
     * │       └─ 3 可点击文字
     * └─ 4 图片
     */
    private fun tree(): SnapshotNodeTree = SnapshotNodeTree.build(nodes)

    private val nodes: List<NodeInfo> = listOf(
        fixtureNode(
            id = 0, pid = -1, className = "android.widget.FrameLayout",
            right = 1080, bottom = 2400, childCount = 2,
        ),
        fixtureNode(
            id = 1, pid = 0, className = "android.widget.LinearLayout",
            left = 40, top = 100, right = 1040, bottom = 600,
            index = 0, depth = 1, childCount = 1,
        ),
        fixtureNode(
            id = 2, pid = 1, className = "android.widget.FrameLayout",
            left = 40, top = 100, right = 1040, bottom = 300,
            index = 0, depth = 2, childCount = 1,
        ),
        fixtureNode(
            id = 3, pid = 2, className = "android.widget.TextView",
            left = 60, top = 120, right = 300, bottom = 180,
            index = 0, depth = 3, text = "跳过", clickable = true,
        ),
        fixtureNode(
            id = 4, pid = 0, className = "android.widget.ImageView",
            left = 40, top = 700, right = 1040, bottom = 900,
            index = 1, depth = 1,
        ),
    )
}
