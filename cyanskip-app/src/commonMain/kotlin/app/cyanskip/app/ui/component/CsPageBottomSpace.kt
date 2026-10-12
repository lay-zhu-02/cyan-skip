package app.cyanskip.app.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object CsPageBottomSpaceDefaults {
    val Height = 80.dp
    val CompactHeight = Height / 2
}

/**
 * Extra scrollable breathing room after page content, in addition to the host's system insets.
 * Place last inside a scrolling Column, or inside an existing lazy footer item.
 * Do not put it outside the scroll container or use it instead of system-bar/IME insets.
 */
@Composable
fun CsPageBottomSpace(height: Dp = CsPageBottomSpaceDefaults.Height) {
    Spacer(Modifier.height(height))
}

/** Add once at the end of a LazyColumn; an existing footer may use CsPageBottomSpace directly. */
fun LazyListScope.csPageBottomSpace(
    key: String = "gk_page_bottom_space",
    height: Dp = CsPageBottomSpaceDefaults.Height,
) {
    item(key = key, contentType = "gk_page_bottom_space") {
        CsPageBottomSpace(height)
    }
}
