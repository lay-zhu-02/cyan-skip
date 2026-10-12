package app.cyanskip.app.ui.snapshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.screenshot_processing
import app.cyanskip.app.resources.screenshot_service
import app.cyanskip.app.resources.screenshot_service_description
import app.cyanskip.app.resources.snapshot_hide_status_bar
import app.cyanskip.app.resources.snapshot_hide_status_bar_description
import app.cyanskip.app.resources.snapshot_settings
import app.cyanskip.app.settings.SettingsRepository
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.component.CsIconButton
import app.cyanskip.app.ui.component.CsIcons
import app.cyanskip.app.ui.component.CsPageBottomSpace
import app.cyanskip.app.ui.component.CsScaffold
import app.cyanskip.app.ui.component.CsTextSwitch
import app.cyanskip.app.ui.component.CsTopAppBar
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.style.titleItemPadding
import org.jetbrains.compose.resources.stringResource

@Composable
fun SnapshotSettingsPage(
    host: UiHost,
) {
    val mainVm = MainViewModel.requireCurrent()
    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val screenshotServiceRunning: Boolean = screenshotServiceRunning()

    val nativeScreenshotAvailable: Boolean = nativeScreenshotAvailable()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    CsScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CsTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    CsIconButton(
                        imageVector = CsIcons.ArrowBack,
                        onClick = mainVm.navigator::pop,
                    )
                },
                title = { Text(text = stringResource(Res.string.snapshot_settings)) },
            )
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding)
        ) {
            Text(
                text = stringResource(Res.string.screenshot_processing),
                modifier = Modifier.titleItemPadding(showTop = false),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!nativeScreenshotAvailable) {
                CsTextSwitch(
                    title = stringResource(Res.string.screenshot_service),
                    subtitle = stringResource(Res.string.screenshot_service_description),
                    checked = screenshotServiceRunning,
                    onCheckedChange = host::setScreenshotServiceEnabled,
                )
            }
            CsTextSwitch(
                title = stringResource(Res.string.snapshot_hide_status_bar),
                subtitle = stringResource(Res.string.snapshot_hide_status_bar_description),
                checked = store.hideSnapshotStatusBar,
                onCheckedChange = { enabled ->
                    SettingsRepository.updateSettings {
                        it.copy(hideSnapshotStatusBar = enabled)
                    }
                },
            )
            CsPageBottomSpace()
        }
    }
}

@Composable expect fun screenshotServiceRunning(): Boolean

expect fun nativeScreenshotAvailable(): Boolean

expect fun UiHost.setScreenshotServiceEnabled(enabled: Boolean)
