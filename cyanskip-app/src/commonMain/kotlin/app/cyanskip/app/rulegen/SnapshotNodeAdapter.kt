package app.cyanskip.app.rulegen

import app.cyanskip.selector.NodeAdapter

/**
 * 让快照节点参与选择器匹配.
 *
 * 属性名与无障碍实现 `A11yContext` 保持一致, 这样在快照上验证通过的选择器
 * 在设备上得到相同结果.
 */
class SnapshotNodeAdapter(private val tree: SnapshotNodeTree) : NodeAdapter<SnapshotNode>() {
    override fun getAttr(target: Any, name: String): Any? =
        (target as? SnapshotNode)?.attrValue(name)

    override fun getInvoke(target: Any, name: String, args: List<Any>): Any? {
        val node = target as? SnapshotNode ?: return null
        return when (name) {
            "getChild" -> node.children.getOrNull(args.firstOrNull() as? Int ?: -1)
            else -> null
        }
    }

    override fun getName(node: SnapshotNode): String? = node.className

    override fun getChildCount(node: SnapshotNode): Int = node.children.size

    override fun getChild(node: SnapshotNode, index: Int): SnapshotNode? =
        node.children.getOrNull(index)

    override fun getParent(node: SnapshotNode): SnapshotNode? = node.parent

    override fun getNodeKey(node: SnapshotNode): Any = node.id

    override fun getRoot(node: SnapshotNode): SnapshotNode? = tree.root
}

/** 与 A11yContext.getCacheAttr 一致的无障碍属性表. */
private fun SnapshotNode.attrValue(name: String): Any? = when (name) {
    "id" -> attr.id
    "vid" -> attr.vid
    "name" -> attr.name
    "text" -> attr.text
    "desc" -> attr.desc

    "clickable" -> attr.clickable
    "focusable" -> attr.focusable
    "checkable" -> attr.checkable
    "checked" -> attr.checked
    "editable" -> attr.editable
    "longClickable" -> attr.longClickable
    "visibleToUser" -> attr.visibleToUser

    "left" -> attr.left
    "top" -> attr.top
    "right" -> attr.right
    "bottom" -> attr.bottom
    "width" -> attr.width
    "height" -> attr.height

    "index" -> attr.index
    "depth" -> attr.depth
    "childCount" -> attr.childCount.coerceAtMost(CHILD_COUNT_LIMIT)

    "parent" -> parent

    else -> null
}

/** 与无障碍节点的子节点遍历上限一致(见 A11yExt.MAX_CHILD_SIZE). */
private const val CHILD_COUNT_LIMIT = 512
