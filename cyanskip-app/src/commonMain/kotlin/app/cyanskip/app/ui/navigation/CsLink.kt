package app.cyanskip.app.ui.navigation

import java.net.URI
import java.net.URLDecoder

sealed interface CsLink {
    data class Home(val tab: Int?) : CsLink
    data class Page(val route: AppRoute) : CsLink
    data object WeChatScanner : CsLink

    companion object {
        fun parse(value: String): CsLink? = runCatching {
            val uri = URI(value)
            if (!uri.scheme.equals("cyanskip", ignoreCase = true)) null
            else when (uri.host) {
                "page" -> when (uri.path.orEmpty()) {
                    "" -> Home(
                        uri.rawQuery?.split('&')?.firstOrNull { it.substringBefore('=') == "tab" }
                            ?.substringAfter('=', "")
                            ?.let { URLDecoder.decode(it, "UTF-8").toIntOrNull() })

                    "/2" -> Page(SnapshotPageRoute)
                    else -> null
                }

                "invoke" -> if (uri.path == "/1") WeChatScanner else null
                else -> null
            }
        }.getOrNull()
    }
}
