package app.cyanskip.app.ui.rulegen

import app.cyanskip.app.rulegen.SnapshotNode

/** 元素列表里的一行. */
data class NodeTreeRow(
    val node: SnapshotNode,
    val depth: Int,
    val hasChildren: Boolean,
    val expanded: Boolean,
) {
    val id: Int get() = node.id
}

/** 默认展开"从根到当前选中元素"的路径, 让列表一打开就能看到它. */
fun nodeTreeDefaultExpanded(root: SnapshotNode?, selected: SnapshotNode?): Set<Int> {
    val expanded = mutableSetOf<Int>()
    if (root == null) return expanded
    if (selected == null) {
        expanded.add(root.id)
        return expanded
    }
    // 展开从根到选中节点的路径(不含它自己), 一打开列表就能看到它
    selected.ancestors().forEach { ancestor -> expanded.add(ancestor.id) }
    return expanded
}

/**
 * 把节点树摊平成列表行.
 *
 * [onlyClickable] 打开时只保留可点击或带文字/说明的节点及其祖先, 并全部展开.
 */
fun nodeTreeRows(
    root: SnapshotNode?,
    expanded: Set<Int>,
    onlyClickable: Boolean,
): List<NodeTreeRow> {
    if (root == null) return emptyList()
    val rows = ArrayList<NodeTreeRow>()
    val kept = if (onlyClickable) keptIds(root) else null

    fun visit(node: SnapshotNode, depth: Int) {
        if (depth > MAX_DEPTH) return
        kept?.let { if (node.id !in it) return }
        val children = node.children.filter { child -> kept?.contains(child.id) ?: true }
        val isExpanded = if (kept != null) true else node.id in expanded
        rows.add(
            NodeTreeRow(
                node = node,
                depth = depth,
                hasChildren = children.isNotEmpty(),
                expanded = isExpanded,
            )
        )
        if (!isExpanded) return
        children.forEach { child -> visit(child, depth + 1) }
    }
    visit(root, 0)
    return rows
}

private const val MAX_DEPTH = 64

/**
 * 列表缩进层数: 层级太深时不再继续缩进, 否则文字会被挤出屏幕.
 * 超过上限的行会额外显示真实层级编号.
 */
internal const val MAX_INDENT_LEVELS = 10

internal fun cappedIndentLevels(depth: Int): Int = depth.coerceIn(0, MAX_INDENT_LEVELS)

private fun keptIds(root: SnapshotNode): Set<Int> {
    val kept = HashSet<Int>()
    collectKept(root, kept)
    return kept
}

private fun collectKept(node: SnapshotNode, kept: MutableSet<Int>): Boolean {
    var keep = node.attr.clickable || node.attr.longClickable || node.label != null
    node.children.forEach { child ->
        if (collectKept(child, kept)) {
            keep = true
        }
    }
    if (keep) kept.add(node.id)
    return keep
}
