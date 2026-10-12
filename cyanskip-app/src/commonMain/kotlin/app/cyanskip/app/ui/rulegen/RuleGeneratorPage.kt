package app.cyanskip.app.ui.rulegen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import kotlin.math.roundToInt
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_cancel
import app.cyanskip.app.resources.action_retry
import app.cyanskip.app.resources.action_save
import app.cyanskip.app.resources.rule_gen_action_click
import app.cyanskip.app.resources.rule_gen_action_long_click
import app.cyanskip.app.resources.rule_gen_action_title
import app.cyanskip.app.resources.rule_gen_choice_similar
import app.cyanskip.app.resources.rule_gen_choice_unique
import app.cyanskip.app.resources.rule_gen_details
import app.cyanskip.app.resources.rule_gen_hide_outlines
import app.cyanskip.app.resources.rule_gen_hint_confirm
import app.cyanskip.app.resources.rule_gen_hint_pick
import app.cyanskip.app.resources.rule_gen_hint_zoom
import app.cyanskip.app.resources.rule_gen_key_desc
import app.cyanskip.app.resources.rule_gen_key_id
import app.cyanskip.app.resources.rule_gen_key_structure
import app.cyanskip.app.resources.rule_gen_key_text
import app.cyanskip.app.resources.rule_gen_load_failed
import app.cyanskip.app.resources.rule_gen_loading
import app.cyanskip.app.resources.rule_gen_name_hint
import app.cyanskip.app.resources.rule_gen_name_key
import app.cyanskip.app.resources.rule_gen_name_role
import app.cyanskip.app.resources.rule_gen_name_title
import app.cyanskip.app.resources.rule_gen_no_nodes
import app.cyanskip.app.resources.rule_gen_no_target
import app.cyanskip.app.resources.rule_gen_overlap_title
import app.cyanskip.app.resources.rule_gen_pick_element
import app.cyanskip.app.resources.rule_gen_result_similar
import app.cyanskip.app.resources.rule_gen_result_unique
import app.cyanskip.app.resources.rule_gen_role_button
import app.cyanskip.app.resources.rule_gen_role_container
import app.cyanskip.app.resources.rule_gen_role_image
import app.cyanskip.app.resources.rule_gen_role_input
import app.cyanskip.app.resources.rule_gen_role_list
import app.cyanskip.app.resources.rule_gen_role_text
import app.cyanskip.app.resources.rule_gen_role_toggle
import app.cyanskip.app.resources.rule_gen_role_widget
import app.cyanskip.app.resources.rule_gen_save
import app.cyanskip.app.resources.rule_gen_scope_page
import app.cyanskip.app.resources.rule_gen_scope_page_desc
import app.cyanskip.app.resources.rule_gen_selected
import app.cyanskip.app.resources.rule_gen_show_outlines
import app.cyanskip.app.resources.rule_gen_strength_hint
import app.cyanskip.app.resources.rule_gen_strength_weak
import app.cyanskip.app.resources.rule_gen_tips
import app.cyanskip.app.resources.rule_gen_title
import app.cyanskip.app.resources.snapshot_screenshot_unavailable
import app.cyanskip.app.rulegen.NodeRole
import app.cyanskip.app.rulegen.RuleDraftAction
import app.cyanskip.app.rulegen.RuleKeyKind
import app.cyanskip.app.rulegen.RuleStrength
import app.cyanskip.app.rulegen.SnapshotNode
import app.cyanskip.app.snapshot.SnapshotStore
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsAlertDialog
import app.cyanskip.app.ui.component.CsAppNameText
import app.cyanskip.app.ui.component.CsIcon
import app.cyanskip.app.ui.component.CsIconButton
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsOutlinedTextField
import app.cyanskip.app.ui.component.CsScaffold
import app.cyanskip.app.ui.component.CsTextSwitch
import app.cyanskip.app.ui.component.CsTopAppBar
import app.cyanskip.app.ui.component.autoFocus
import app.cyanskip.app.ui.image.appImageLoader
import app.cyanskip.app.ui.navigation.SnapshotRuleRoute
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.style.scaffoldPadding
import org.jetbrains.compose.resources.stringResource

/** 竖屏下面板固定高度, 保证下方选项出现时图片区域不变化. */
private val PORTRAIT_PANEL_HEIGHT = 288.dp

private val MAX_LANDSCAPE_PANEL_WIDTH = 340.dp

@Composable
fun RuleGeneratorPage(route: SnapshotRuleRoute) {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { RuleGeneratorViewModel(route) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val imageLoader = appImageLoader()
    val context = LocalPlatformContext.current
    val screenshotPath = remember(route.snapshotId) {
        SnapshotStore.screenshotFile(route.snapshotId).absolutePath
    }
    val painter = rememberAsyncImagePainter(
        model = remember(context, screenshotPath) {
            ImageRequest.Builder(context)
                .data(screenshotPath)
                // 节点坐标按截图像素记录, 因此必须按原始尺寸解码.
                .size(Size.ORIGINAL)
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
        },
        imageLoader = imageLoader,
    )
    val painterState by painter.state.collectAsStateWithLifecycle()
    var detailsExpanded by remember { mutableStateOf(false) }
    // 元素列表与截图同屏: 在面板里切换, 不用弹层盖住截图
    var treeMode by remember { mutableStateOf(false) }
    val ruleName = state.selected
        ?.takeIf { state.source != null }
        ?.let { selected -> suggestedName(state, selected) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 横屏(或宽窗口)时把选项面板放到右侧, 避免图片被挤成一条
        val landscape = maxWidth > maxHeight
        val panelWidth = minOf(MAX_LANDSCAPE_PANEL_WIDTH, maxWidth * 0.45f)
        // 元素列表要把截图一起留在屏幕里, 因此展开时面板占下半屏
        val panelHeight = if (treeMode) maxHeight * 0.52f else PORTRAIT_PANEL_HEIGHT
        CsScaffold(
            topBar = {
                CsTopAppBar(
                    navigationIcon = {
                        CsIconButton(
                            imageVector = CsIcons.ArrowBack,
                            onClick = mainVm.navigator::pop,
                        )
                    },
                    title = {
                        Column {
                            Text(
                                text = stringResource(Res.string.rule_gen_title),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            val appId = state.snapshot?.appId
                            if (appId != null) {
                                CsAppNameText(
                                    appId = appId,
                                    fallbackName = appId,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    },
                    actions = {
                        if (state.tree != null) {
                            TextButton(onClick = vm::toggleOutlines) {
                                Text(
                                    text = stringResource(
                                        if (state.showOutlines) {
                                            Res.string.rule_gen_hide_outlines
                                        } else {
                                            Res.string.rule_gen_show_outlines
                                        }
                                    )
                                )
                            }
                        }
                    },
                )
            },
        ) { contentPadding ->
            if (landscape) {
                Column(modifier = Modifier.scaffoldPadding(contentPadding).fillMaxSize()) {
                    HintBanner(state)
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        PickerArea(
                            state = state,
                            painter = painter,
                            painterState = painterState,
                            vm = vm,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        Surface(
                            tonalElevation = 3.dp,
                            modifier = Modifier.width(panelWidth).fillMaxHeight(),
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                val tree = state.tree
                                if (treeMode && tree != null) {
                                    CsNodeTreePanel(
                                        tree = tree,
                                        selected = state.selected,
                                        matchCount = state.matches.size,
                                        onSelect = vm::chooseNode,
                                        onDone = { treeMode = false },
                                        modifier = Modifier.weight(1f).fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                } else {
                                    ResultPanel(
                                        state = state,
                                        compact = true,
                                        detailsExpanded = detailsExpanded,
                                        onToggleDetails = { detailsExpanded = !detailsExpanded },
                                        onPickElement = { treeMode = true },
                                        onCandidateChange = vm::selectCandidate,
                                        onActionChange = vm::setAction,
                                        onScopeChange = vm::setScopeThisPage,
                                        onPreferUniqueChange = vm::setPreferUnique,
                                        modifier = Modifier.weight(1f).fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                                if (ruleName != null) {
                                    SaveButton(
                                        enabled = !state.saving,
                                        onClick = { vm.openNameDialog(ruleName) },
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.scaffoldPadding(contentPadding).fillMaxSize()) {
                    HintBanner(state)
                    PickerArea(
                        state = state,
                        painter = painter,
                        painterState = painterState,
                        vm = vm,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    Surface(
                        tonalElevation = 3.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier.fillMaxWidth().height(panelHeight),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val tree = state.tree
                            if (treeMode && tree != null) {
                                CsNodeTreePanel(
                                    tree = tree,
                                    selected = state.selected,
                                    matchCount = state.matches.size,
                                    onSelect = vm::chooseNode,
                                    onDone = { treeMode = false },
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            } else {
                                ResultPanel(
                                    state = state,
                                    compact = false,
                                    detailsExpanded = detailsExpanded,
                                    onToggleDetails = { detailsExpanded = !detailsExpanded },
                                    onPickElement = { treeMode = true },
                                    onCandidateChange = vm::selectCandidate,
                                    onActionChange = vm::setAction,
                                    onScopeChange = vm::setScopeThisPage,
                                    onPreferUniqueChange = vm::setPreferUnique,
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                            }
                            if (ruleName != null) {
                                SaveButton(
                                    enabled = !state.saving,
                                    onClick = { vm.openNameDialog(ruleName) },
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.nameDialogVisible) {
        CsAlertDialog(
            onDismissRequest = vm::dismissNameDialog,
            title = { Text(text = stringResource(Res.string.rule_gen_name_title)) },
            text = {
                CsOutlinedTextField(
                    value = state.nameDraft,
                    onValueChange = vm::setNameDraft,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().autoFocus(immediateFocus = true),
                    placeholder = { Text(text = stringResource(Res.string.rule_gen_name_hint)) },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = state.nameDraft.isNotBlank() && !state.saving,
                    onClick = { vm.scope.launchUi { vm.saveRule(state.nameDraft.trim()) } },
                ) {
                    Text(text = stringResource(Res.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissNameDialog) {
                    Text(text = stringResource(Res.string.action_cancel))
                }
            },
        )
    }

}

@Composable
private fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text = stringResource(Res.string.rule_gen_save))
    }
}

@Composable
private fun HintBanner(state: RuleGeneratorUiState) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(
                    if (state.selected == null) {
                        Res.string.rule_gen_hint_pick
                    } else {
                        Res.string.rule_gen_hint_confirm
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            if (state.selected == null) {
                Text(
                    text = stringResource(Res.string.rule_gen_hint_zoom),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PickerArea(
    state: RuleGeneratorUiState,
    painter: coil3.compose.AsyncImagePainter,
    painterState: AsyncImagePainter.State,
    vm: RuleGeneratorViewModel,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val snapshot = state.snapshot
        val imageSize = (painterState as? AsyncImagePainter.State.Success)
            ?.let { painter.intrinsicSize }
        when {
            state.loading -> ProgressHint(stringResource(Res.string.rule_gen_loading))
            state.loadError != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        Res.string.rule_gen_load_failed,
                        state.loadError?.message.orEmpty(),
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = vm::load) {
                    Text(text = stringResource(Res.string.action_retry))
                }
            }

            snapshot == null -> ProgressHint(stringResource(Res.string.rule_gen_loading))

            state.tree?.root == null -> Text(
                text = stringResource(Res.string.rule_gen_no_nodes),
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )

            imageSize == null || imageSize.width <= 0f || imageSize.height <= 0f ->
                when (painterState) {
                    is AsyncImagePainter.State.Error -> Text(
                        text = stringResource(Res.string.snapshot_screenshot_unavailable),
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                    )

                    else -> ProgressHint(stringResource(Res.string.rule_gen_loading))
                }

            else -> {
                val mapping = remember(
                    snapshot.screenWidth,
                    snapshot.screenHeight,
                    imageSize.width,
                    imageSize.height,
                ) {
                    SnapshotImageMapping(
                        screenWidth = snapshot.screenWidth,
                        screenHeight = snapshot.screenHeight,
                        imageWidth = imageSize.width.roundToInt(),
                        imageHeight = imageSize.height.roundToInt(),
                    )
                }
                CsSnapshotNodePicker(
                    painter = painter,
                    mapping = mapping,
                    outlines = state.outlines,
                    matches = state.matches,
                    selected = state.selected,
                    showOutlines = state.showOutlines,
                    focusToken = state.focusToken,
                    focusNode = state.selected,
                    onPick = vm::pick,
                    onClearSelection = vm::clearSelection,
                )
            }
        }
    }
}

@Composable
private fun ResultPanel(
    state: RuleGeneratorUiState,
    compact: Boolean,
    detailsExpanded: Boolean,
    onToggleDetails: () -> Unit,
    onPickElement: () -> Unit,
    onCandidateChange: (SnapshotNode) -> Unit,
    onActionChange: (RuleDraftAction) -> Unit,
    onScopeChange: (Boolean) -> Unit,
    onPreferUniqueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
    ) {
        val selected = state.selected
        if (selected == null) {
            Text(
                text = stringResource(Res.string.rule_gen_hint_pick),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.rule_gen_tips),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.tree?.root != null) {
                TextButton(onClick = onPickElement) {
                    Text(text = stringResource(Res.string.rule_gen_pick_element))
                }
            }
            return@Column
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.rule_gen_selected, nodeLabel(selected)),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(
                onClick = onPickElement,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(text = stringResource(Res.string.rule_gen_pick_element))
            }
        }
        if (state.tapCandidates.size > 1) {
            // 多个可点区域堆叠: 让用户自己挑一个, 而不是只按层级猜
            Text(
                text = stringResource(
                    Res.string.rule_gen_overlap_title,
                    state.tapCandidates.size.toString(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.tapCandidates.forEach { candidate ->
                    FilterChip(
                        selected = candidate.id == selected.id,
                        onClick = { onCandidateChange(candidate) },
                        label = { Text(text = nodeLabel(candidate)) },
                    )
                }
            }
        }
        ActionRow(action = state.action, onActionChange = onActionChange)
        if (state.snapshot?.activityId?.isNotBlank() == true) {
            CsTextSwitch(
                title = stringResource(Res.string.rule_gen_scope_page),
                subtitle = stringResource(Res.string.rule_gen_scope_page_desc),
                paddingDisabled = true,
                checked = state.scopeThisPage,
                onCheckedChange = onScopeChange,
            )
        }
        HorizontalDivider()
        if (state.source == null) {
            Text(
                text = stringResource(Res.string.rule_gen_no_target),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            return@Column
        }
        ResultSection(state = state, onPreferUniqueChange = onPreferUniqueChange)
        TextButton(
            onClick = onToggleDetails,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        ) {
            Text(
                text = stringResource(Res.string.rule_gen_details),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (detailsExpanded) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = state.source.orEmpty(),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ProgressHint(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.size(12.dp))
        Text(text = text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionRow(
    action: RuleDraftAction,
    onActionChange: (RuleDraftAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.rule_gen_action_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilterChip(
            selected = action == RuleDraftAction.Click,
            onClick = { onActionChange(RuleDraftAction.Click) },
            label = { Text(text = stringResource(Res.string.rule_gen_action_click)) },
        )
        FilterChip(
            selected = action == RuleDraftAction.LongClick,
            onClick = { onActionChange(RuleDraftAction.LongClick) },
            label = { Text(text = stringResource(Res.string.rule_gen_action_long_click)) },
        )
    }
}

@Composable
private fun ResultSection(
    state: RuleGeneratorUiState,
    onPreferUniqueChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val matchedCount = state.matches.size
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (matchedCount == 1 && state.preferUnique) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CsIcon(
                        imageVector = if (matchedCount == 1) CsIcons.CheckCircle
                        else CsIcons.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (matchedCount == 1) {
                            stringResource(Res.string.rule_gen_result_unique)
                        } else {
                            stringResource(
                                Res.string.rule_gen_result_similar,
                                matchedCount.toString(),
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (matchedCount > 1 && state.hasUniqueChoice) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.preferUnique,
                            onClick = { onPreferUniqueChange(true) },
                            label = { Text(text = stringResource(Res.string.rule_gen_choice_unique)) },
                        )
                        FilterChip(
                            selected = !state.preferUnique,
                            onClick = { onPreferUniqueChange(false) },
                            label = { Text(text = stringResource(Res.string.rule_gen_choice_similar)) },
                        )
                    }
                }
                Text(
                    text = stringResource(
                        Res.string.rule_gen_strength_hint,
                        keyLabel(state.keyKind, state.keyValue),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.strength == RuleStrength.Weak) {
                    Text(
                        text = stringResource(Res.string.rule_gen_strength_weak),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun suggestedName(state: RuleGeneratorUiState, selected: SnapshotNode): String {
    val actionLabel = stringResource(
        if (state.action == RuleDraftAction.LongClick) {
            Res.string.rule_gen_action_long_click
        } else {
            Res.string.rule_gen_action_click
        }
    )
    return when (state.keyKind) {
        RuleKeyKind.Text, RuleKeyKind.Description, RuleKeyKind.Id -> stringResource(
            Res.string.rule_gen_name_key,
            actionLabel,
            state.keyValue.orEmpty(),
        )

        else -> stringResource(
            Res.string.rule_gen_name_role,
            actionLabel,
            roleLabel(selected.role),
        )
    }
}

@Composable
private fun keyLabel(keyKind: RuleKeyKind?, keyValue: String?): String = when (keyKind) {
    RuleKeyKind.Text -> stringResource(Res.string.rule_gen_key_text, keyValue.orEmpty())
    RuleKeyKind.Description -> stringResource(Res.string.rule_gen_key_desc, keyValue.orEmpty())
    RuleKeyKind.Id -> stringResource(Res.string.rule_gen_key_id, keyValue.orEmpty())
    else -> stringResource(Res.string.rule_gen_key_structure)
}

@Composable
internal fun roleLabel(role: NodeRole): String = stringResource(
    when (role) {
        NodeRole.Button -> Res.string.rule_gen_role_button
        NodeRole.Text -> Res.string.rule_gen_role_text
        NodeRole.Image -> Res.string.rule_gen_role_image
        NodeRole.Input -> Res.string.rule_gen_role_input
        NodeRole.Toggle -> Res.string.rule_gen_role_toggle
        NodeRole.List -> Res.string.rule_gen_role_list
        NodeRole.Container -> Res.string.rule_gen_role_container
        NodeRole.Widget -> Res.string.rule_gen_role_widget
    }
)
