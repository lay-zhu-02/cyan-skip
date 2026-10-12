package app.cyanskip.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cyanskip.app.ui.MainViewModel
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.snapshot.SnapshotPage

@Composable
fun HomePage(host: UiHost) {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { HomeViewModel() }
    DisposableEffect(vm, mainVm.homeNavigation) {
        mainVm.homeNavigation.bind(vm.homeState)
        onDispose { mainVm.homeNavigation.unbind(vm.homeState) }
    }
    val tab by vm.homeState.selectedTab.collectAsStateWithLifecycle()
    val holder = rememberSaveableStateHolder()
    holder.SaveableStateProvider(tab.key) {
        when (tab) {
            BottomNavItem.Snapshots -> SnapshotPage(host, homeTab = true) {
                CsHomeNavigationBar(tab, vm.homeState::clickTab)
            }
            BottomNavItem.SubsManage -> CsHomeScaffold(subsManagePage(vm), tab, vm.homeState::clickTab)
            else -> CsHomeScaffold(
                when (tab) {
                    BottomNavItem.AppList -> appListPage(host, vm)
                    BottomNavItem.Settings -> dashboardPage(vm)
                    else -> dashboardPage(vm)
                }, tab, vm.homeState::clickTab,
            )
        }
    }
}
