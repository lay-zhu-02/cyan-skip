package app.cyanskip.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.app_whitelist
import app.cyanskip.app.resources.unchanged
import app.cyanskip.app.resources.update_success
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsMultiTextField
import app.cyanskip.app.ui.navigation.CsEditor
import app.cyanskip.app.ui.page.EditorSession
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.platform.hideIme
import app.cyanskip.app.ui.platform.inputInsets
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.ui.text.getSync
import app.cyanskip.app.util.ToastUtils
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppWhitelistPage(
    host: UiHost,
) {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { AppWhitelistViewModel() }
    val text by vm.text.collectAsStateWithLifecycle()
    val indicatorSize by vm.indicatorSize.collectAsStateWithLifecycle()
    val inputInsets: Modifier = inputInsets()

    CsEditor(
        EditorSession(
            stringResource(Res.string.app_whitelist),
            { vm.hasChanges(text) },
            onSave = {
                ToastUtils.show(
                    (if (vm.saveChanges(text)) Res.string.update_success else Res.string.unchanged)
                        .getSync()
                )
            },
        ),
        navigator = mainVm.navigator,
        hideIme = host::hideIme,
        scope = mainVm.scope,
    ) { padding ->
        CsMultiTextField(
            modifier = Modifier.scaffoldPadding(padding),
            text = text,
            onTextChange = vm::setText,
            immediateFocus = true,
            indicatorSize = indicatorSize,
            inputInsets = inputInsets,
        )
    }
}
