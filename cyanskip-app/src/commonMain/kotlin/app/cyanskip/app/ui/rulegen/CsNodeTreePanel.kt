package app.cyanskip.app.ui.rulegen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.rule_gen_tree_clickable
import app.cyanskip.app.resources.rule_gen_tree_clickable_only
import app.cyanskip.app.resources.rule_gen_tree_current
import app.cyanskip.app.resources.rule_gen_tree_done
import app.cyanskip.app.resources.rule_gen_tree_title
import app.cyanskip.app.rulegen.NodeRole
import app.cyanskip.app.rulegen.SnapshotNode
import app.cyanskip.app.rulegen.SnapshotNodeTree
import app.cyanskip.app.ui.component.CsIcon
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsTextSwitch
import org.jetbrains.compose.resources.stringResource

/**
 * 可展开的元素列表, 内嵌在选项面板里, 与截图同屏显示.
 *
 * 点一行即把它设为要点击的元素: 截图会同步高亮, 有必要时把它移入视野, 列表保持展开.
 */
@Composable
fun CsNodeTreePanel(
    tree: SnapshotNodeTree,
    selected: SnapshotNode?,
    matchCount: Int,
    onSelect: (SnapshotNode) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(selected?.id) {
        mutableStateOf(nodeTreeDefaultExpanded(tree.root, selected))
    }
    var onlyClickable by remember { mutableStateOf(false) }
    val rows = remember(tree, expanded, onlyClickable) {
        nodeTreeRows(tree.root, expanded, onlyClickable)
    }
    val listState = rememberLazyListState()
    // 选中项变化时滚到它附近, 往上留几行便于直接改选父级
    LaunchedEffect(rows, selected?.id) {
        val index = rows.indexOfFirst { it.id == selected?.id }
        if (index >= 0) listState.scrollToItem(maxOf(0, index - 3))
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.rule_gen_tree_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(
                onClick = onDone,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 8.dp, vertical = 0.dp,
                ),
            ) {
                Text(text = stringResource(Res.string.rule_gen_tree_done))
            }
        }
        if (selected != null) {
            Text(
                text = stringResource(
                    Res.string.rule_gen_tree_current,
                    nodeLabel(selected),
                    matchCount.toString(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        CsTextSwitch(
            title = stringResource(Res.string.rule_gen_tree_clickable_only),
            paddingDisabled = true,
            checked = onlyClickable,
            onCheckedChange = { onlyClickable = it },
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(rows, key = { it.id }) { row ->
                NodeTreeRowItem(
                    row = row,
                    selected = row.id == selected?.id,
                    onToggle = {
                        expanded = if (row.id in expanded) expanded - row.id
                        else expanded + row.id
                    },
                    onSelect = { onSelect(row.node) },
                )
            }
        }
    }
}

@Composable
private fun NodeTreeRowItem(
    row: NodeTreeRow,
    selected: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
) {
    val node = row.node
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onSelect)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .padding(start = (cappedIndentLevels(row.depth) * 8).dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (row.hasChildren) {
            CsIcon(
                imageVector = if (row.expanded) CsIcons.ExpandMore else CsIcons.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp).clickable(onClick = onToggle),
            )
        } else {
            Spacer(modifier = Modifier.size(20.dp))
        }
        if (row.depth > MAX_INDENT_LEVELS) {
            // 缩进到上限后改用层级编号说明真实深度
            Text(
                text = "L${row.depth}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = nodeLabel(node),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (node.label == null && node.attr.vid == null) {
            Text(
                text = roleLabel(node.role),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (node.attr.clickable || node.attr.longClickable) {
            Text(
                text = stringResource(Res.string.rule_gen_tree_clickable),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun nodeLabel(node: SnapshotNode): String {
    node.label?.let { return it }
    node.attr.vid?.let { return it }
    val simple = node.className?.substringAfterLast('.')?.substringAfterLast('$')
    return simple?.takeIf { it.isNotBlank() } ?: roleLabel(NodeRole.Widget)
}
