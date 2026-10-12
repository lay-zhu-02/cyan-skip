package app.cyanskip.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_save_to_downloads
import app.cyanskip.app.resources.action_share
import org.jetbrains.compose.resources.stringResource

@Composable
fun CsShareLogDialog(
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    CsDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            val modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            Text(
                text = stringResource(Res.string.action_share),
                modifier = Modifier
                    .clickable(onClick = onShare)
                    .then(modifier),
            )
            Text(
                text = stringResource(Res.string.action_save_to_downloads),
                modifier = Modifier
                    .clickable(onClick = onSave)
                    .then(modifier),
            )

        }
    }
}
