package app.cyanskip.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cyanskip.app.resources.*
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.*
import app.cyanskip.app.ui.navigation.*
import app.cyanskip.app.ui.option.DarkThemeOption
import app.cyanskip.app.ui.option.findOption
import app.cyanskip.app.ui.page.CsBackupDialogs
import app.cyanskip.app.ui.platform.UiHost
import org.jetbrains.compose.resources.stringResource

@Composable
fun LiteSettingsPage(host: UiHost) {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { HomeViewModel() }
    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val showBackup by vm.showBackup.collectAsStateWithLifecycle()
    CsBackupDialogs(showBackup, { vm.setShowBackup(false) },
        host::importAppBackup, host::shareAppBackup, host::saveAppBackup)
    CsScaffold(topBar = {
        CsTopAppBar(title = { Text(stringResource(Res.string.settings_title)) },
            navigationIcon = { CsIconButton(CsIcons.ArrowBack, mainVm.navigator::pop) })
    }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            CsSettingItem(title = stringResource(Res.string.a11y_label),
                subtitle = stringResource(Res.string.lite_permission_hint),
                onClick = { app.cyanskip.app.ui.platform.openA11ySettings() })
            CsSettingItem(title = stringResource(Res.string.action_toast),
                subtitle = store.actionToast,
                onClick = { mainVm.navigator.navigate(ActionToastRoute) })
            CsSettingItem(title = stringResource(Res.string.snapshot_settings),
                onClick = { mainVm.navigator.navigate(SnapshotSettingsRoute) })
            CsSettingItem(title = stringResource(Res.string.action_log_title),
                onClick = { mainVm.navigator.navigate(ActionLogRoute()) })
            CsTextMenu(title = stringResource(Res.string.theme_mode),
                option = DarkThemeOption.objects.findOption(store.enableDarkTheme),
                onOptionChange = { option -> SettingsRepository.updateSettings { it.copy(enableDarkTheme = option.value) } })
            CsSettingItem(title = stringResource(Res.string.backup_restore),
                onClick = { vm.setShowBackup(true) })
            CsSettingItem(title = stringResource(Res.string.about_title),
                onClick = { mainVm.navigator.navigate(AboutRoute) })
            Text(stringResource(Res.string.lite_local_notice), modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            CsPageBottomSpace()
        }
    }
}

