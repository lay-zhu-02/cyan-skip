package app.cyanskip.app.rulegen

import app.cyanskip.selector.Selector
import app.cyanskip.selector.SelectorCompileResult
import app.cyanskip.selector.SelectorTypeResult
import app.cyanskip.selector.createDefaultSelectorTypeModel

/** 规则的识别依据, 只用于界面文案. */
enum class RuleKeyKind {
    Text,
    Description,
    Id,
    Structure,
}

/** 规则的稳定程度; 界面据此提示用户是否需要换一个更明显的位置. */
enum class RuleStrength {
    Strong,
    Normal,
    Weak,
}

/** 一条候选规则及其在快照上的匹配结果. */
data class RuleSuggestion(
    val source: String,
    val matches: List<SnapshotNode>,
    val keyKind: RuleKeyKind,
    val keyValue: String?,
    val strength: RuleStrength,
    val relationBased: Boolean,
) {
    val matchCount: Int get() = matches.size
    val isUnique: Boolean get() = matches.size == 1
}

/** 一个目标位置的两种做法: 只点选中的这一个, 或点击页面上所有相似位置. */
data class RuleDraftOptions(
    val unique: RuleSuggestion?,
    val similar: RuleSuggestion,
)

/** 用选择器匹配快照节点; 非法选择器返回 null. */
class SnapshotSelectorMatcher(private val tree: SnapshotNodeTree) {
    private val adapter = SnapshotNodeAdapter(tree)
    private val cache = HashMap<String, SnapshotSelectorMatch?>()

    fun verify(source: String): SnapshotSelectorMatch? =
        cache.getOrPut(source) { verifyUncached(source) }

    private fun verifyUncached(source: String): SnapshotSelectorMatch? {
        val selector = runCatching {
            when (val result = Selector.compile(source)) {
                is SelectorCompileResult.Success -> result.value
                is SelectorCompileResult.Failure -> null
            }
        }.getOrNull() ?: return null
        if (selector.validateType(createDefaultSelectorTypeModel()) is SelectorTypeResult.Failure) {
            return null
        }
        val root = tree.root ?: return null
        val matches = ArrayList<SnapshotNode>()
        val seen = HashSet<Int>()
        // 与运行期规则查询一致: 先匹配根节点自身, 再从根节点遍历后代.
        selector.match(root, adapter)?.let { node ->
            if (seen.add(node.id)) matches.add(node)
        }
        adapter.querySelectorAll(root, selector).forEach { node ->
            if (seen.add(node.id)) matches.add(node)
        }
        return SnapshotSelectorMatch(selector.toString(), matches)
    }
}

data class SnapshotSelectorMatch(
    val source: String,
    val nodes: List<SnapshotNode>,
)

/**
 * 从快照里的一个节点反推选择器.
 *
 * 候选选择器按"是否唯一命中""特征是否稳定"排序, 每条都会在快照上实际匹配一遍,
 * 只有确实命中目标节点的候选才会保留.
 */
class SnapshotRuleGenerator(private val tree: SnapshotNodeTree) {
    private val matcher = SnapshotSelectorMatcher(tree)

    fun verify(source: String): SnapshotSelectorMatch? = matcher.verify(source)

    fun draftOptions(target: SnapshotNode): RuleDraftOptions? {
        val suggestions = suggestions(target)
        if (suggestions.isEmpty()) return null
        return RuleDraftOptions(
            unique = suggestions.firstOrNull { it.isUnique },
            similar = suggestions.first(),
        )
    }

    fun suggestions(target: SnapshotNode, limit: Int = 8): List<RuleSuggestion> {
        val found = LinkedHashMap<String, RuleSuggestion>()
        fun attempt(
            source: String,
            kind: RuleKeyKind,
            value: String?,
            strength: RuleStrength,
            relationBased: Boolean = false,
        ) {
            val match = matcher.verify(source) ?: return
            if (match.nodes.none { it.id == target.id }) return
            if (found.containsKey(match.source)) return
            found[match.source] = RuleSuggestion(
                source = match.source,
                matches = match.nodes,
                keyKind = kind,
                keyValue = value,
                strength = strength,
                relationBased = relationBased,
            )
        }

        val keys = keyAtoms(target)
        val classPrefix = classAtom(target)
        // 没有文字/说明/控件编号时, 只能靠"可点击""类名"这类弱特征描述目标本身
        val weakUnits = if (keys.isEmpty()) {
            buildList {
                if (target.attr.clickable) {
                    add(KeyAtom("[clickable=true]", RuleKeyKind.Structure, null, RuleStrength.Weak))
                }
                classPrefix?.let {
                    add(KeyAtom(it, RuleKeyKind.Structure, null, RuleStrength.Weak))
                }
                if (target.attr.longClickable) {
                    add(
                        KeyAtom(
                            "[longClickable=true]", RuleKeyKind.Structure, null, RuleStrength.Weak,
                        )
                    )
                }
            }
        } else {
            emptyList()
        }
        val qualifierChains = qualifierChains(target)

        // 1. 只用节点自身的特征
        (keys + weakUnits).forEach { key ->
            attempt(key.selector, key.kind, key.value, key.strength)
        }

        // 2. 特征加上可见/可点击/层级等补充条件
        (keys + weakUnits).forEach { key ->
            qualifierChains.forEach { chain ->
                val source = key.selector + chain.qualifiers.joinToString("") { it.selector }
                attempt(
                    source, key.kind, key.value,
                    if (chain.structural) maxOf(key.strength, RuleStrength.Normal) else key.strength,
                )
            }
        }

        // 3. 类名 + 特征
        if (classPrefix != null) {
            keys.forEach { key ->
                attempt(classPrefix + key.selector, key.kind, key.value, RuleStrength.Normal)
            }
        }

        // 4. 借助相邻节点的文字/编号定位
        relationCandidates(target, keys.ifEmpty { weakUnits }).forEach { candidate ->
            attempt(
                candidate.source,
                candidate.kind,
                candidate.value,
                candidate.strength,
                relationBased = true,
            )
        }

        // 5. 结构兜底
        structuralAtoms(target, classPrefix, keys).forEach { atom ->
            attempt(atom.selector, RuleKeyKind.Structure, null, RuleStrength.Weak)
        }

        // 先看特征是否稳定, 再看是否只依赖节点自身(不依赖事件来自哪个节点), 最后才看唯一性.
        return found.values.sortedWith(
            compareBy<RuleSuggestion> { it.strength.ordinal }
                .thenBy { it.relationBased }
                .thenByDescending { it.isUnique }
                .thenBy { it.source.length }
        ).take(limit)
    }
}

private class KeyAtom(
    val selector: String,
    val kind: RuleKeyKind,
    val value: String?,
    val strength: RuleStrength,
)

private class Qualifier(
    val selector: String,
    val structural: Boolean,
)

private class QualifierChain(
    val qualifiers: List<Qualifier>,
    val structural: Boolean,
)

private class RelationCandidate(
    val source: String,
    val kind: RuleKeyKind,
    val value: String?,
    val strength: RuleStrength,
)

/** 节点自身的可识别特征, 顺序即优先级. */
private fun keyAtoms(node: SnapshotNode): List<KeyAtom> = buildList {
    val text = node.text
    if (!text.isNullOrEmpty()) {
        add(KeyAtom("[text=${quote(text)}]", RuleKeyKind.Text, text, RuleStrength.Strong))
        textCore(text)?.let { core ->
            add(KeyAtom("[text*=${quote(core)}]", RuleKeyKind.Text, core, RuleStrength.Normal))
        }
    }
    val desc = node.desc
    if (!desc.isNullOrEmpty()) {
        add(KeyAtom("[desc=${quote(desc)}]", RuleKeyKind.Description, desc, RuleStrength.Strong))
        textCore(desc)?.let { core ->
            add(KeyAtom("[desc*=${quote(core)}]", RuleKeyKind.Description, core, RuleStrength.Normal))
        }
    }
    val vid = node.attr.vid
    if (!vid.isNullOrBlank()) {
        add(KeyAtom("[vid=${quote(vid)}]", RuleKeyKind.Id, vid, RuleStrength.Strong))
    }
    val viewId = node.attr.id
    if (!viewId.isNullOrBlank() && viewId != vid) {
        add(
            KeyAtom(
                "[id=${quote(viewId)}]", RuleKeyKind.Id, viewId,
                if (vid == null) RuleStrength.Strong else RuleStrength.Normal,
            )
        )
    }
}

private fun classAtom(node: SnapshotNode): String? = node.className
    ?.substringAfterLast('.')
    ?.substringAfterLast('$')
    ?.takeIf { it.matches(IDENTIFIER_REGEX) }

private fun qualifiers(node: SnapshotNode): List<Qualifier> = buildList {
    if (node.attr.visibleToUser) add(Qualifier("[visibleToUser=true]", structural = false))
    if (node.attr.clickable) add(Qualifier("[clickable=true]", structural = false))
    add(Qualifier("[childCount=${node.attr.childCount}]", structural = true))
    add(Qualifier("[index=${node.attr.index}]", structural = true))
    add(Qualifier("[depth=${node.attr.depth}]", structural = true))
}

/** 逐个尝试补充条件及少量组合, 避免候选数量膨胀. */
private fun qualifierChains(node: SnapshotNode): List<QualifierChain> {
    val qualifiers = qualifiers(node)
    val stable = qualifiers.filter { !it.structural }
    val chains = ArrayList<QualifierChain>()
    qualifiers.forEach { qualifier ->
        chains.add(QualifierChain(listOf(qualifier), qualifier.structural))
    }
    if (stable.size >= 2) {
        chains.add(QualifierChain(stable.take(2), structural = false))
    }
    qualifiers.filter { it.structural }.forEach { qualifier ->
        chains.add(QualifierChain(stable + qualifier, structural = true))
    }
    return chains
}

/**
 * 用祖先或相邻节点的文字来定位目标.
 *
 * 目标节点始终是选择器的最后一个单元, 这样运行期匹配到的节点就是需要点击的节点.
 */
private fun relationCandidates(
    target: SnapshotNode,
    keys: List<KeyAtom>,
): List<RelationCandidate> {
    val targetAtoms = keys.filter { it.strength == RuleStrength.Strong }.take(2)
        .ifEmpty { keys.take(2) }
    if (targetAtoms.isEmpty()) return emptyList()
    val result = ArrayList<RelationCandidate>()
    // 祖先节点带文字时, 用 "祖先的文字 > 目标" 定位
    var ancestor = target.parent
    var level = 0
    while (ancestor != null && level < 3) {
        // 同一个祖先可能有文字、说明、控件编号等多个特征, 都要试一遍
        keyAtoms(ancestor).filter { it.strength == RuleStrength.Strong }.take(2).forEach { anchor ->
            targetAtoms.forEach { atom ->
                result.add(
                    RelationCandidate(
                        "${anchor.selector} >n ${atom.selector}",
                        anchor.kind, anchor.value, RuleStrength.Normal,
                    )
                )
            }
        }
        ancestor = ancestor.parent
        level++
    }
    // 相邻节点带文字时, 用 "相邻节点的文字 ± 目标" 定位
    val siblings = target.parent?.children.orEmpty()
    val targetIndex = siblings.indexOfFirst { it === target }
    if (targetIndex >= 0) {
        siblings.forEachIndexed { index, sibling ->
            if (sibling === target) return@forEachIndexed
            val following = index < targetIndex
            keyAtoms(sibling).filter { it.strength == RuleStrength.Strong }.take(2).forEach { anchor ->
                targetAtoms.forEach { atom ->
                    result.add(
                        RelationCandidate(
                            // `+n`: 目标在锚点之后; `-n`: 目标在锚点之前. 目标始终是最后一个单元.
                            if (following) "${anchor.selector} +n ${atom.selector}"
                            else "${anchor.selector} -n ${atom.selector}",
                            anchor.kind, anchor.value, RuleStrength.Normal,
                        )
                    )
                }
            }
        }
    }
    return result
}

/** 无任何可识别特征时的兜底结构: 位置与层级. */
private fun structuralAtoms(
    node: SnapshotNode,
    classPrefix: String?,
    keys: List<KeyAtom>,
): List<KeyAtom> {
    if (keys.isNotEmpty()) return emptyList()
    val index = "[index=${node.attr.index}]"
    val childCount = "[childCount=${node.attr.childCount}]"
    val depth = "[depth=${node.attr.depth}]"
    val result = ArrayList<KeyAtom>()
    result.add(KeyAtom("[depth=${node.attr.depth}][index=${node.attr.index}]", RuleKeyKind.Structure, null, RuleStrength.Weak))
    result.add(KeyAtom("$childCount$index$depth", RuleKeyKind.Structure, null, RuleStrength.Weak))
    if (classPrefix != null) {
        result.add(KeyAtom("$classPrefix$index$childCount", RuleKeyKind.Structure, null, RuleStrength.Weak))
    }
    return result
}

/** "跳过 3s" 这类带倒计时的文字, 取其中稳定的文字部分. */
private fun textCore(value: String): String? =
    VARIABLE_TEXT_REGEX.find(value)?.groupValues?.get(1)

private fun quote(value: String): String = buildString(value.length + 2) {
    append('"')
    value.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
    append('"')
}

private val IDENTIFIER_REGEX = Regex("[A-Za-z_][A-Za-z0-9_]*")
private val VARIABLE_TEXT_REGEX = Regex("^([^0-9\\s]{2,})[\\s0-9].*$")
