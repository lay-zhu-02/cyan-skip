package app.cyanskip.app.ui.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import app.cyanskip.app.ui.component.CsIcon
import app.cyanskip.app.ui.component.CsScaffold

// A desktop device profile supplies Android's bottom inset; Android uses native defaults.
val LocalHomeNavigationInsets = staticCompositionLocalOf<WindowInsets?> { null }

@Composable
fun CsHomeScaffold(
    page: ScaffoldExt,
    selectedTab: BottomNavItem,
    onSelectTab: (BottomNavItem) -> Unit
) {
    CsScaffold(
        modifier = page.modifier, topBar = page.topBar,
        floatingActionButton = page.floatingActionButton,
        bottomBar = { CsHomeNavigationBar(selectedTab, onSelectTab) },
        content = page.content,
    )
}

@Composable
fun CsHomeNavigationBar(selectedTab: BottomNavItem, onSelectTab: (BottomNavItem) -> Unit) {
    NavigationBar(
        windowInsets = LocalHomeNavigationInsets.current ?: NavigationBarDefaults.windowInsets,
    ) {
        BottomNavItem.primaryItems.forEach { item ->
            NavigationBarItem(
                selected = item == selectedTab,
                onClick = { onSelectTab(item) },
                icon = { CsIcon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
            )
        }
    }
}
