package app.cyanskip.app.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.subscription_title
import app.cyanskip.app.state.Loadable
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.ui.text.subscriptionMessageResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T : Any> CsSubscriptionPageContent(
    stateFlow: StateFlow<Loadable<T>>,
    onBack: () -> Unit,
    retainContent: Boolean = false,
    content: @Composable (T) -> Unit,
) {
    val state by stateFlow.collectAsStateWithLifecycle()
    var lastReady by remember(stateFlow) { mutableStateOf<Loadable.Ready<T>?>(null) }
    val ready = state as? Loadable.Ready<T>
    if (!retainContent && ready != null) {
        SideEffect { lastReady = ready }
    }
    // An explicit removal may invalidate this page before its navigation exit ends.
    // Keep this purely visual snapshot; failures still render normally otherwise.
    when (val current = if (retainContent) lastReady ?: state else state) {
        Loadable.Loading -> SubscriptionStatePage(onBack = onBack)
        is Loadable.Failure -> SubscriptionStatePage(
            onBack = onBack,
            message = current.cause.subscriptionMessageResource(),
        )

        is Loadable.Ready -> content(current.value)
    }
}

@Composable
private fun SubscriptionStatePage(onBack: () -> Unit, message: String? = null) {
    CsScaffold(
        topBar = {
            CsTopAppBar(
                navigationIcon = {
                    CsIconButton(
                        imageVector = CsIcons.ArrowBack,
                        onClick = onBack,
                    )
                },
                title = { Text(stringResource(Res.string.subscription_title)) },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .scaffoldPadding(contentPadding)
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (message == null) {
                CircularProgressIndicator()
            } else {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
