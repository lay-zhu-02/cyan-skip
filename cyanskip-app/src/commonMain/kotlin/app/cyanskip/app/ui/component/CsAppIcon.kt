package app.cyanskip.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CsAppIcon(painter: Painter?, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val iconModifier = modifier.size(size)
    if (painter != null) {
        Image(painter = painter, contentDescription = null, modifier = iconModifier)
    } else {
        CsIcon(imageVector = CsIcons.Android, modifier = iconModifier)
    }
}
