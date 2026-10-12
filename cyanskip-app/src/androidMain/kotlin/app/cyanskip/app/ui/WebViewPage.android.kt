package app.cyanskip.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import app.cyanskip.app.META
import app.cyanskip.app.MainActivity
import app.cyanskip.app.model.WebViewEnvironment
import app.cyanskip.app.network.NetworkClients
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.compatibility_notice
import app.cyanskip.app.resources.webview_load_failed
import app.cyanskip.app.resources.webview_outdated_notice
import app.cyanskip.app.ui.component.CsWebViewErrorContent
import app.cyanskip.app.ui.navigation.WebViewRoute
import app.cyanskip.app.ui.page.WebViewScreen
import app.cyanskip.app.ui.platform.SystemActionFeedback
import app.cyanskip.app.ui.platform.UiHost
import app.cyanskip.app.ui.share.LocalDarkTheme
import app.cyanskip.app.ui.share.launchUiAction
import app.cyanskip.app.ui.style.scaffoldPadding
import app.cyanskip.app.util.AndroidTarget
import app.cyanskip.app.util.LogUtils
import app.cyanskip.app.util.copyText
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun WebViewPage(
    route: WebViewRoute,
    host: UiHost,
) {
    val mainVm = MainViewModel.requireCurrent()
    val initUrl = route.initUrl
    val state = remember(initUrl) { BrowserState() }
    val dark = LocalDarkTheme.current
    val webView = state.view
    val background = MaterialTheme.colorScheme.background.toArgb()
    val reload = {
        webView?.let { view ->
            val failedUrl = state.failure?.url
            state.failure = null
            state.loading = true
            state.contentVisible = false
            state.title = ""
            if (failedUrl != null) view.loadUrl(failedUrl) else view.reload()
        }
        Unit
    }
    val openExternal = { SystemActionFeedback.openExternal(state.failure?.url ?: webView?.url ?: initUrl) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(webView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView?.onResume()
                Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // The toolbar leaves the route; system Back first traverses browser history.
    BackHandler(state.canGoBack) { state.view?.goBack() }
    WebViewScreen(
        title = if (state.failure != null) stringResource(Res.string.webview_load_failed) else state.title,
        loading = state.loading,
        onBack = mainVm.navigator::pop,
        onReload = reload,
        onCopyLink = { copyText(state.failure?.url ?: webView?.url ?: initUrl) },
        onOpenExternal = openExternal,
        onCompatibilityNotice = if (chromeVersion in 1..<MINI_CHROME_VERSION) {
            mainVm.scope.launchUiAction {
                mainVm.dialogRequests.showMessage(
                    title = getString(Res.string.compatibility_notice),
                    text = getString(Res.string.webview_outdated_notice, chromeVersion),
                )
            }
        } else null,
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .scaffoldPadding(contentPadding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        state.view = this
                        state.dark = dark
                        setBackgroundColor(background)
                        visibility = View.INVISIBLE
                        webViewClient = CsWebViewClient(state)
                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView, title: String?) {
                                if (state.failure == null) state.title = title.orEmpty()
                            }
                        }
                        addJavascriptInterface(CsWebViewJsApi(state), "cyanskip")
                        settings.apply {
                            @SuppressLint("SetJavaScriptEnabled")
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            if (AndroidTarget.TIRAMISU) {
                                setAlgorithmicDarkeningAllowed(false)
                            }
                        }
                        // Load once when creating the native view, never on recomposition.
                        loadUrl(initUrl)
                    }
                },
                update = { view ->
                    if (state.dark != dark) {
                        state.dark = dark
                        view.evaluateJavascript(
                            "window.dispatchEvent(new Event('cyanskip:themechange'));",
                            null
                        )
                    }
                    view.setBackgroundColor(background)
                    view.visibility = if (state.contentVisible) View.VISIBLE else View.INVISIBLE
                },
                onRelease = { view ->
                    state.view = null
                    state.canGoBack = false
                    view.stopLoading()
                    view.removeJavascriptInterface("cyanskip")
                    view.webChromeClient = null
                    view.webViewClient = WebViewClient()
                    view.destroy()
                },
            )
            state.failure?.let { failure ->
                CsWebViewErrorContent(
                    onRetry = reload, onOpenExternal = openExternal,
                    url = failure.url, errorText = failure.code
                )
            }
        }
    }
}

@Suppress("unused")
private class CsWebViewJsApi(private val state: BrowserState) {
    @JavascriptInterface
    fun isDarkTheme(): Boolean = state.dark

    @JavascriptInterface
    fun getEnvironment(): String = EnvironmentCache.json
}

private object EnvironmentCache {
    val json by lazy {
        WebViewEnvironment(
            platform = "android",
            appId = META.appId,
            appName = META.appName,
            versionCode = META.versionCode,
            versionName = META.versionName,
            channel = META.channel,
            debuggable = META.debuggable,
        ).toJson()
    }

}

private const val MINI_CHROME_VERSION = 107
private val chromeVersion by lazy {
    WebView.getCurrentWebViewPackage()?.versionName?.run {
        splitToSequence('.').first().toIntOrNull()
    } ?: 0
}

private const val DOC_CONFIG_URL =
    "https://registry.npmmirror.com/@gkd-kit/docs/latest/files/_config.json"

private const val DEBUG_JS_TEXT = """
<script src="https://registry.npmmirror.com/eruda/latest/files"></script>
<script>eruda.init();</script>
"""

@Serializable
private data class DocConfig(
    val mirrorBaseUrl: String,
    val htmlUrlMap: Map<String, String>
)

private data class BrowserFailure(val url: String, val code: String)

private class BrowserState {
    @Volatile
    var dark = false
    var view by mutableStateOf<WebView?>(null)
    var title by mutableStateOf("")
    var loading by mutableStateOf(true)
    var canGoBack by mutableStateOf(false)
    var failure by mutableStateOf<BrowserFailure?>(null)
    var contentVisible by mutableStateOf(false)

    fun fail(view: WebView, url: String, code: String) {
        failure = BrowserFailure(url, code)
        loading = false
        contentVisible = false
        canGoBack = view.canGoBack()
        // Hide Chromium's error document immediately, before the next Compose frame.
        view.visibility = View.INVISIBLE
    }
}

private class CsWebViewClient(private val state: BrowserState) : WebViewClient() {
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        state.loading = true
        state.failure = null
        state.title = ""
        state.contentVisible = false
        view.visibility = View.INVISIBLE
        state.canGoBack = view.canGoBack()
    }

    override fun onPageCommitVisible(view: WebView, url: String?) {
        if (state.failure == null) state.contentVisible = true
    }

    override fun onPageFinished(view: WebView, url: String?) {
        state.loading = false
        if (state.failure == null) {
            state.title = view.title.orEmpty()
            state.contentVisible = true
        }
        state.canGoBack = view.canGoBack()
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        state.canGoBack = view.canGoBack()
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        if (request.isForMainFrame) {
            state.fail(
                view, request.url.toString(),
                error.description.toString().ifBlank { "ERROR_CODE: ${error.errorCode}" })
        }
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        if (request.isForMainFrame) {
            state.fail(
                view, request.url.toString(),
                "HTTP ${errorResponse.statusCode} ${errorResponse.reasonPhrase}".trim()
            )
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url
        if (uri != null && uri.host != "gkd.li") {
            if (uri.scheme == "cyanskip") {
                (view?.context as? MainActivity)?.mainVm?.androidState?.handleDeepLink(uri)
            } else {
                SystemActionFeedback.openExternal(uri.toString())
            }
            return true
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        try {
            if (request != null && request.run { isForMainFrame && url.host == "gkd.li" && method == "GET" }) {
                LogUtils.d(request.method, request.url)
                runBlocking(Dispatchers.IO) {
                    val docConfig = NetworkClients.client.get(DOC_CONFIG_URL).body<DocConfig>()
                    val path = request.url.path.let { if (it.isNullOrEmpty()) "/" else it }
                    val textUrl = docConfig.htmlUrlMap[path]?.let { docConfig.mirrorBaseUrl + it }
                    if (textUrl != null) {
                        val textContent = NetworkClients.client.get(textUrl).body<String>().let {
                            if (META.debuggable) {
                                DEBUG_JS_TEXT + it
                            } else {
                                it
                            }
                        }
                        return@runBlocking WebResourceResponse(
                            "text/html",
                            "UTF-8",
                            textContent.byteInputStream()
                        )
                    }
                    return@runBlocking null
                }?.let { return it }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return super.shouldInterceptRequest(view, request)
    }
}
