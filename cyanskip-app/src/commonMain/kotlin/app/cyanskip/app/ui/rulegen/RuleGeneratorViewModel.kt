package app.cyanskip.app.ui.rulegen

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import app.cyanskip.app.model.ComplexSnapshot
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.rule_gen_enable_local_confirm
import app.cyanskip.app.resources.rule_gen_enable_local_dismiss
import app.cyanskip.app.resources.rule_gen_enable_local_text
import app.cyanskip.app.resources.rule_gen_saved
import app.cyanskip.app.resources.rule_gen_saved_title
import app.cyanskip.app.resources.rule_gen_save_failed
import app.cyanskip.app.rulegen.RuleDraftAction
import app.cyanskip.app.rulegen.RuleDraftOptions
import app.cyanskip.app.rulegen.RuleKeyKind
import app.cyanskip.app.rulegen.RuleStrength
import app.cyanskip.app.rulegen.SnapshotNode
import app.cyanskip.app.rulegen.SnapshotNodeTree
import app.cyanskip.app.rulegen.SnapshotRuleDraft
import app.cyanskip.app.rulegen.SnapshotRuleGenerator
import app.cyanskip.app.rulegen.SnapshotRuleWriter
import app.cyanskip.app.snapshot.SnapshotStore
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.navigation.SnapshotRuleRoute
import app.cyanskip.app.ui.state.BaseViewModel
import app.cyanskip.app.util.ToastUtils
import org.jetbrains.compose.resources.getString

data class RuleGeneratorUiState(
    val loading: Boolean = true,
    val loadError: Throwable? = null,
    val snapshot: ComplexSnapshot? = null,
    val tree: SnapshotNodeTree? = null,
    val outlines: List<SnapshotNode> = emptyList(),
    val showOutlines: Boolean = false,
    val selected: SnapshotNode? = null,
    val matches: List<SnapshotNode> = emptyList(),
    val source: String? = null,
    val strength: RuleStrength? = null,
    val keyKind: RuleKeyKind? = null,
    val keyValue: String? = null,
    val hasUniqueChoice: Boolean = false,
    val preferUnique: Boolean = true,
    /** 最近一次点选位置上堆叠的可点元素, 供用户在重叠时挑一个. */
    val tapCandidates: List<SnapshotNode> = emptyList(),
    val action: RuleDraftAction = RuleDraftAction.Click,
    val scopeThisPage: Boolean = true,
    val nameDialogVisible: Boolean = false,
    val nameDraft: String = "",
    val saving: Boolean = false,
    /** 从元素列表选中的次数; 每次变化都让截图把该元素移到视野内. */
    val focusToken: Int = 0,
)

/** 从快照点选目标并生成规则. */
class RuleGeneratorViewModel(
    private val route: SnapshotRuleRoute,
) : BaseViewModel() {
    val uiState: StateFlow<RuleGeneratorUiState>
        field = MutableStateFlow(RuleGeneratorUiState())

    private var generator: SnapshotRuleGenerator? = null
    private var options: RuleDraftOptions? = null

    init {
        load()
    }

    fun load() {
        uiState.update { it.copy(loading = true, loadError = null) }
        scope.launch {
            try {
                val snapshot = SnapshotStore.loadSnapshot(route.snapshotId)
                val tree = SnapshotNodeTree.build(snapshot.nodes)
                generator = SnapshotRuleGenerator(tree)
                options = null
                uiState.update {
                    it.copy(
                        loading = false,
                        loadError = null,
                        snapshot = snapshot,
                        tree = tree,
                        outlines = tree.outlineNodes(),
                        selected = null,
                        matches = emptyList(),
                        source = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(loading = false, loadError = e) }
            }
        }
    }

    fun pick(screenX: Int, screenY: Int) {
        val tree = uiState.value.tree ?: return
        // 点到按钮内部的文字时默认选中这个按钮本身; 点在空白处则取消选中.
        val candidates = tree.pickTargets(screenX, screenY)
        if (candidates.isEmpty()) {
            clearSelection()
            return
        }
        select(candidates.first())
        uiState.update { it.copy(tapCandidates = candidates) }
    }

    /** 在重叠的可点元素里改选另一个. */
    fun selectCandidate(node: SnapshotNode) {
        select(node)
    }

    fun clearSelection() {
        options = null
        uiState.update {
            it.copy(
                selected = null,
                matches = emptyList(),
                source = null,
                strength = null,
                keyKind = null,
                keyValue = null,
                hasUniqueChoice = false,
                tapCandidates = emptyList(),
            )
        }
    }

    /** 从元素列表里选中某个节点: 同样重新生成规则, 并让截图把它移动视野内. */
    fun chooseNode(node: SnapshotNode) {
        select(node)
        uiState.update {
            it.copy(focusToken = it.focusToken + 1, tapCandidates = emptyList())
        }
    }

    fun toggleOutlines() {
        uiState.update { it.copy(showOutlines = !it.showOutlines) }
    }

    fun setAction(action: RuleDraftAction) {
        uiState.update { it.copy(action = action) }
    }

    fun setScopeThisPage(value: Boolean) {
        uiState.update { it.copy(scopeThisPage = value) }
    }

    fun setPreferUnique(value: Boolean) {
        val currentOptions = options ?: return
        val node = uiState.value.selected ?: return
        applyOptions(node, currentOptions, preferUnique = value && currentOptions.unique != null)
    }

    fun openNameDialog(suggestedName: String) {
        uiState.update { it.copy(nameDialogVisible = true, nameDraft = suggestedName) }
    }

    fun dismissNameDialog() {
        uiState.update { it.copy(nameDialogVisible = false) }
    }

    fun setNameDraft(value: String) {
        uiState.update { it.copy(nameDraft = value) }
    }

    private fun select(node: SnapshotNode) {
        val draftOptions = generator?.draftOptions(node)
        options = draftOptions
        if (draftOptions == null) {
            uiState.update {
                it.copy(
                    selected = node,
                    matches = emptyList(),
                    source = null,
                    strength = null,
                    hasUniqueChoice = false,
                )
            }
            return
        }
        applyOptions(node, draftOptions, preferUnique = draftOptions.unique != null)
    }

    private fun applyOptions(
        node: SnapshotNode,
        draftOptions: RuleDraftOptions,
        preferUnique: Boolean,
    ) {
        val suggestion = if (preferUnique) draftOptions.unique ?: draftOptions.similar
        else draftOptions.similar
        uiState.update {
            it.copy(
                selected = node,
                matches = suggestion.matches,
                source = suggestion.source,
                strength = suggestion.strength,
                keyKind = suggestion.keyKind,
                keyValue = suggestion.keyValue,
                hasUniqueChoice = draftOptions.unique != null,
                preferUnique = preferUnique,
            )
        }
    }

    suspend fun saveRule(name: String) {
        val state = uiState.value
        val snapshot = state.snapshot ?: return
        val source = state.source ?: return
        if (state.saving) return
        uiState.update { it.copy(saving = true) }
        try {
            SnapshotRuleWriter.save(
                SnapshotRuleDraft(
                    appId = snapshot.appId,
                    appName = snapshot.appInfo?.name,
                    activityId = snapshot.activityId,
                    scopeThisPage = state.scopeThisPage,
                    action = state.action,
                    source = source,
                    name = name,
                )
            )
            uiState.update { it.copy(saving = false, nameDialogVisible = false) }
            val mainVm = MainViewModel.requireCurrent()
            if (!SnapshotRuleWriter.localSubscriptionEnabled()) {
                val enableNow = mainVm.dialogRequests.confirm(
                    title = getString(Res.string.rule_gen_saved_title),
                    text = getString(Res.string.rule_gen_enable_local_text),
                    confirmText = getString(Res.string.rule_gen_enable_local_confirm),
                    dismissText = getString(Res.string.rule_gen_enable_local_dismiss),
                )
                if (enableNow) SnapshotRuleWriter.enableLocalSubscription()
            } else {
                ToastUtils.show(getString(Res.string.rule_gen_saved))
            }
            mainVm.navigator.pop()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            uiState.update { it.copy(saving = false) }
            ToastUtils.show(e.message ?: getString(Res.string.rule_gen_save_failed))
        }
    }
}
