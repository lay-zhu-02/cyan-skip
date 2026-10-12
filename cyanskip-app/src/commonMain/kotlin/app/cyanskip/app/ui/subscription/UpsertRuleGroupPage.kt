package app.cyanskip.app.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.app_rule_input_hint
import app.cyanskip.app.resources.global_rule_input_hint
import app.cyanskip.app.resources.rule_add
import app.cyanskip.app.resources.rule_edit
import app.cyanskip.app.resources.lite_import_rule
import app.cyanskip.app.resources.lite_import_rule_hint
import app.cyanskip.app.storage.readFileBytes
import app.cyanskip.app.ui.share.pickRuleFile
import app.cyanskip.app.ui.share.launchUi
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsSubscriptionPageContent
import app.cyanskip.app.ui.component.autoFocus
import app.cyanskip.app.ui.navigation.CsEditor
import app.cyanskip.app.ui.navigation.UpsertRuleGroupRoute
import app.cyanskip.app.ui.page.EditorSession
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.platform.hideIme
import app.cyanskip.app.ui.platform.inputInsets
import app.cyanskip.app.ui.share.LocalDarkTheme
import app.cyanskip.app.ui.style.getJson5Transformation
import app.cyanskip.app.ui.style.scaffoldPadding
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpsertRuleGroupPage(
    host: UiHost,
    route: UpsertRuleGroupRoute,
) {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel {
        UpsertRuleGroupViewModel(route)
    }
    val draft by vm.draft.collectAsStateWithLifecycle()
    CsSubscriptionPageContent(vm.uiState, mainVm.navigator::pop) { data ->
        val text = draft ?: data.initialText
        val inputInsets: Modifier = inputInsets()

        CsEditor(
            EditorSession(
                title =
                    stringResource(if (vm.isEdit) Res.string.rule_edit else Res.string.rule_add),
                hasChanges = { vm.hasTextChanged(text) },
                saveEnabled = text.isNotBlank(),
                onSave = { vm.saveRule(text) },
            ),
            onSaved = vm::onSaved,
            navigator = mainVm.navigator,
            hideIme = host::hideIme,
            scope = mainVm.scope,
        ) { paddingValues ->
            val textColors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                )
            Column(modifier = Modifier.scaffoldPadding(paddingValues).fillMaxSize()) {
                if (!vm.isEdit) {
                    TextButton(onClick = {
                        vm.scope.launchUi {
                            val source = host.pickRuleFile() ?: return@launchUi
                            // Loading only fills the draft; the existing Save path validates and persists it.
                            vm.setDraft(readFileBytes(source, maxBytes = 2 * 1024 * 1024).decodeToString(throwOnInvalidSequence = true).removePrefix("\uFEFF"))
                        }
                    }) { Text(stringResource(Res.string.lite_import_rule)) }
                    Text(stringResource(Res.string.lite_import_rule_hint),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.weight(1f).fillMaxSize()) {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.bodyLarge
                    ) {
                        val modifier =
                            Modifier.autoFocus(immediateFocus = true).fillMaxSize().then(inputInsets)
                        TextField(
                            value = text,
                            onValueChange = vm::setDraft,
                            modifier = modifier,
                            shape = RectangleShape,
                            colors = textColors,
                            visualTransformation = getJson5Transformation(LocalDarkTheme.current),
                            placeholder = {
                                Text(
                                    text =
                                        if (vm.isApp) stringResource(Res.string.app_rule_input_hint)
                                        else stringResource(Res.string.global_rule_input_hint)
                                )
                            },
                        )
                    }
                    if (text.isNotEmpty()) {
                        Text(
                            text = text.length.toString(),
                            modifier =
                                Modifier.padding(8.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(horizontal = 2.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}
