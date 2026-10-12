package app.cyanskip.app.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import app.cyanskip.app.resources.lite_welcome
import app.cyanskip.app.resources.lite_welcome_notice
import app.cyanskip.app.resources.lite_a11y_notice
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.a11y_about
import app.cyanskip.app.resources.action_agree
import app.cyanskip.app.resources.action_disagree
import org.jetbrains.compose.resources.stringResource

@Composable
fun CsTermsAcceptDialog(
    onAccept: suspend () -> Unit,
    onError: (Exception) -> Unit,
    onDisagree: () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val onAgree: () -> Unit = {
        if (step == 0) step = 1
        else scope.launch {
            try {
                onAccept()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    val modifier = Modifier.fillMaxWidth()
    val stepDataList = arrayOf(
        stringResource(Res.string.lite_welcome) to @Composable {
            Text(modifier = modifier, text = stringResource(Res.string.lite_welcome_notice))
        },
        stringResource(Res.string.a11y_about) to @Composable {
            Text(
                modifier = modifier,
                text = stringResource(Res.string.lite_a11y_notice),
            )
        }
    )

    CsAlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = stepDataList[step].first)
        },
        text = stepDataList[step].second,
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text(text = stringResource(Res.string.action_agree))
            }
        },
        dismissButton = {
            TextButton(onClick = onDisagree) {
                Text(text = stringResource(Res.string.action_disagree))
            }
        }
    )
}
