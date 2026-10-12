package app.cyanskip.app.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cyanskip.app.ui.component.CsTopAppBar

data class ScaffoldExt(
    val navItem: BottomNavItem,
    val modifier: Modifier = Modifier,
    val topBar: @Composable () -> Unit = {
        CsTopAppBar(title = {
            Text(
                text = navItem.label,
            )
        })
    },
    val floatingActionButton: @Composable () -> Unit = {},
    val content: @Composable (PaddingValues) -> Unit
)

