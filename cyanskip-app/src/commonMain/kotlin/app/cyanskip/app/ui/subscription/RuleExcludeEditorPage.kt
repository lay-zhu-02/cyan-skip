package app.cyanskip.app.ui.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cyanskip.app.model.ExcludeData
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.config_edit
import app.cyanskip.app.resources.global_rule_scope_input_hint
import app.cyanskip.app.resources.page_exclusion
import app.cyanskip.app.resources.page_exclusion_input_hint
import app.cyanskip.app.resources.unchanged
import app.cyanskip.app.resources.update_success
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsMultiTextField
import app.cyanskip.app.ui.component.CsSubscriptionPageContent
import app.cyanskip.app.ui.component.CsTwoLineText
import app.cyanskip.app.ui.navigation.CsEditor
import app.cyanskip.app.ui.navigation.RuleExcludeEditorRoute
import app.cyanskip.app.ui.page.EditorSession
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.platform.hideIme
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.util.ToastUtils
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun RuleExcludeEditorPage(
    host: UiHost,
    route: RuleExcludeEditorRoute,
) {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { RuleExcludeEditorViewModel(route) }
    CsSubscriptionPageContent(vm.uiState, mainVm.navigator::pop) { state ->
        val draftState by vm.draft.collectAsStateWithLifecycle()
        val draft = draftState ?: return@CsSubscriptionPageContent
        val subscription = draft.subscription
        val expected = draft.expected
        val text = draft.text
        val value = remember(text, expected, route.appId) {
            val appId = route.appId
            if (appId == null) ExcludeData.parse(text) else {
                // Editing one application's pages must preserve unrelated exclusions.
                expected.copy(activityIds = expected.activityIds.filterTo(mutableSetOf()) {
                    it.first != route.appId
                } + ExcludeData.parse(text, appId).activityIds)
            }
        }
        CsEditor(
            EditorSession(
                title = state.group.name,
                titleContent = {
                    CsTwoLineText(
                        state.group.name,
                        if (route.appId == null) stringResource(Res.string.config_edit) else stringResource(
                            Res.string.page_exclusion
                        )
                    )
                },
                hasChanges = { value != expected },
                onSave = {
                    val changed = vm.save(subscription, expected, value)
                    ToastUtils.show(
                        if (changed) getString(Res.string.update_success) else getString(Res.string.unchanged)
                    )
                },
            ), null,
            navigator = mainVm.navigator,
            hideIme = host::hideIme,
            scope = mainVm.scope,
        ) { padding ->
            CsMultiTextField(
                modifier = Modifier.scaffoldPadding(padding),
                text = text,
                onTextChange = vm::setText,
                immediateFocus = true,
                placeholderText = if (route.appId == null) stringResource(Res.string.global_rule_scope_input_hint) else stringResource(
                    Res.string.page_exclusion_input_hint
                ),
            )
        }
    }
}
