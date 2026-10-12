package app.cyanskip.app.rulegen

import app.cyanskip.app.model.AttrInfo
import app.cyanskip.app.model.NodeInfo

/** 节点在界面上的大致角色, 只用于界面文案, 不参与规则匹配. */
enum class NodeRole {
    Button,
    Text,
    Image,
    Input,
    Toggle,
    List,
    Container,
    Widget,
}

/**
 * 快照中的一个节点. 快照文件把节点存成扁平列表, 这里按 pid 还原父子关系,
 * 使选择器可以像在设备上一样从根节点遍历.
 */
class SnapshotNode internal constructor(
    val info: NodeInfo,
    val parent: SnapshotNode?,
) {
    internal var children: List<SnapshotNode> = emptyList()

    val attr: AttrInfo get() = info.attr
    val id: Int get() = info.id

    val text: String? get() = attr.text
    val desc: String? get() = attr.desc
    val className: String? get() = attr.name

    val screenLeft: Int get() = attr.left
    val screenTop: Int get() = attr.top
    val screenRight: Int get() = attr.right
    val screenBottom: Int get() = attr.bottom

    val area: Long get() = (screenRight - screenLeft).toLong() * (screenBottom - screenTop)

    val isVisible: Boolean
        get() = attr.visibleToUser && screenRight > screenLeft && screenBottom > screenTop

    /** 节点自身可辨认的特征文字, 用于界面展示. */
    val label: String?
        get() = text?.trim()?.takeIf { it.isNotEmpty() }
            ?: desc?.trim()?.takeIf { it.isNotEmpty() }

    val role: NodeRole get() = roleOf(className, attr.clickable || attr.longClickable)

    fun contains(x: Int, y: Int): Boolean =
        x in screenLeft until screenRight && y in screenTop until screenBottom

    fun ancestors(): Sequence<SnapshotNode> = generateSequence(parent) { it.parent }

    /**
     * 从根到该节点的子节点序号. 序号越大越晚绘制, 即更靠上层;
     * 同一条路径更长时说明节点是前者的后代, 也画在更上层.
     */
    fun zPath(): List<Int> {
        val path = ArrayList<Int>(attr.depth + 1)
        var current: SnapshotNode? = this
        while (current != null) {
            val parent = current.parent ?: break
            path.add(parent.children.indexOfFirst { it === current })
            current = parent
        }
        path.reverse()
        return path
    }

    override fun equals(other: Any?): Boolean = other is SnapshotNode && other.id == id

    override fun hashCode(): Int = id
}

/** 快照节点树; 只有从根可达的节点参与匹配. */
class SnapshotNodeTree internal constructor(
    val root: SnapshotNode?,
    val nodes: List<SnapshotNode>,
) {
    private val nodeById = nodes.associateBy { it.id }

    fun node(id: Int): SnapshotNode? = nodeById[id]

    /**
     * 命中该坐标的节点, 由上层到下层排列.
     *
     * 重叠的节点里, 后绘制(层级更靠上、或某节点的后代)的那个才是用户看到并点到的目标,
     * 因此按 z 序排列; 面积只作为同序时的兜底比较.
     */
    fun pickCandidates(x: Int, y: Int): List<SnapshotNode> = nodes
        .filter { it.isVisible && it.contains(x, y) }
        .sortedWith(zOrderComparator.thenBy { it.area })

    /**
     * 该位置所有可点击或带文字/说明的节点, 由上层到下层排列.
     *
     * 点选取第一个; 多个可点区域堆叠时, 界面用整份候选让用户自己挑一个。
     * 返回空表示该位置只有无特征的普通容器, 按空白处理。
     */
    fun pickTargets(x: Int, y: Int, limit: Int = 6): List<SnapshotNode> = pickCandidates(x, y)
        .filter { node -> node.attr.clickable || node.attr.longClickable || node.label != null }
        .take(limit)

    /** 可供高亮的可点位置; 过小或不可见的节点不作为目标. */
    fun outlineNodes(): List<SnapshotNode> = nodes.filter { node ->
        node.isVisible &&
                (node.attr.clickable || node.attr.longClickable || node.label != null) &&
                node.screenRight - node.screenLeft >= MIN_OUTLINE_SIZE &&
                node.screenBottom - node.screenTop >= MIN_OUTLINE_SIZE
    }

    companion object {
        private const val MIN_OUTLINE_SIZE = 24

        private val zOrderComparator = Comparator<SnapshotNode> { a, b ->
            val pathA = a.zPath()
            val pathB = b.zPath()
            val shared = minOf(pathA.size, pathB.size)
            var index = 0
            while (index < shared) {
                if (pathA[index] != pathB[index]) {
                    return@Comparator pathB[index] - pathA[index]
                }
                index++
            }
            // 路径是前缀的是祖先, 后代画在上层
            pathB.size - pathA.size
        }

        fun build(list: List<NodeInfo>): SnapshotNodeTree {
            if (list.isEmpty()) return SnapshotNodeTree(null, emptyList())
            val knownIds = list.mapTo(HashSet(list.size)) { it.id }
            val childIds = HashMap<Int, MutableList<Int>>(list.size)
            val rootIds = ArrayList<Int>()
            list.forEach { info ->
                if (info.pid != info.id && info.pid in knownIds) {
                    childIds.getOrPut(info.pid) { ArrayList() }.add(info.id)
                } else {
                    rootIds.add(info.id)
                }
            }
            val infoById = list.associateBy { it.id }
            val rootInfo = rootIds.firstOrNull()?.let { infoById[it] } ?: list.first()
            val nodes = ArrayList<SnapshotNode>(list.size)
            val pending = ArrayDeque<Pair<NodeInfo, SnapshotNode?>>()
            val childNodes = HashMap<Int, MutableList<SnapshotNode>>(list.size)
            val visited = HashSet<Int>(list.size)
            pending.add(rootInfo to null)
            visited.add(rootInfo.id)
            while (pending.isNotEmpty()) {
                val (info, parent) = pending.removeFirst()
                val node = SnapshotNode(info, parent)
                nodes.add(node)
                if (parent != null) {
                    childNodes.getOrPut(parent.id) { ArrayList() }.add(node)
                }
                childIds[info.id]?.forEach { childId ->
                    if (!visited.add(childId)) return@forEach
                    val childInfo = infoById[childId] ?: return@forEach
                    pending.add(childInfo to node)
                }
            }
            nodes.forEach { node -> node.children = childNodes[node.id].orEmpty() }
            return SnapshotNodeTree(nodes.firstOrNull(), nodes)
        }
    }
}

private fun roleOf(className: String?, clickable: Boolean): NodeRole {
    val name = className?.substringAfterLast('.')?.substringAfterLast('$').orEmpty()
    return when {
        name.contains("Button", true) -> NodeRole.Button
        name.contains("EditText", true) -> NodeRole.Input
        name.contains("CheckBox", true) || name.contains("Switch", true) ||
                name.contains("RadioButton", true) -> NodeRole.Toggle
        name.contains("ImageView", true) || name == "Image" -> NodeRole.Image
        name.contains("TextView", true) -> NodeRole.Text
        name.contains("RecyclerView", true) || name.contains("ListView", true) ||
                name.contains("ScrollView", true) || name.contains("ViewPager", true) -> NodeRole.List
        name.contains("Layout", true) || name.contains("ViewGroup", true) ||
                name == "View" -> NodeRole.Container
        clickable -> NodeRole.Button
        else -> NodeRole.Widget
    }
}
