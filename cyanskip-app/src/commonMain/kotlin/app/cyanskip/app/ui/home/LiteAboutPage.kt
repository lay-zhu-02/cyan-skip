package app.cyanskip.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cyanskip.app.network.AppLinks
import app.cyanskip.app.resources.*
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.*
import app.cyanskip.app.ui.settings.appVersion
import org.jetbrains.compose.resources.stringResource

@Composable
fun LiteAboutPage() {
    val mainVm = MainViewModel.requireCurrent()
    val version = appVersion()
    CsScaffold(topBar = {
        CsTopAppBar(title = { Text(stringResource(Res.string.about_title)) },
            navigationIcon = { CsIconButton(CsIcons.ArrowBack, mainVm.navigator::pop) })
    }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(version.appName, style = MaterialTheme.typography.headlineMedium)
                Text(version.versionName, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(Res.string.lite_about_description))
                Text(stringResource(Res.string.lite_local_notice),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CsSettingItem(title = stringResource(Res.string.lite_upstream),
                subtitle = "gkd-kit/gkd · GPL-3.0-only",
                onClick = { mainVm.navigator.openWebPage(AppLinks.Repository) })
            CsSettingItem(title = stringResource(Res.string.lite_help),
                onClick = { mainVm.navigator.openWebPage(AppLinks.Home) })
            CsSettingItem(title = stringResource(Res.string.logs_export),
                onClick = { mainVm.shareLog.show() })
            CsPageBottomSpace()
        }
    }
}
