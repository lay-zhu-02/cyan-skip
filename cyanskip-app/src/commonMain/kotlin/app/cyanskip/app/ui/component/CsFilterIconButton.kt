package app.cyanskip.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.sort_filter
import org.jetbrains.compose.resources.stringResource

@Composable
fun CsFilterIconButton(
    filtered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = stringResource(Res.string.sort_filter),
) {
    Box(modifier) {
        CsIconButton(
            imageVector = CsIcons.Sort,
            onClick = onClick,
            enabled = enabled,
            contentDescription = contentDescription,
        )
        if (filtered) {
            Box(
                Modifier.align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}
