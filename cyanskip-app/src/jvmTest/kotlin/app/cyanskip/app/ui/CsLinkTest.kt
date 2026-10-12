package app.cyanskip.app.ui

import app.cyanskip.app.ui.navigation.CsLink
import app.cyanskip.app.ui.navigation.SnapshotPageRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CsLinkTest {
    @Test
    fun existingWebsiteLinksKeepTheirNavigationMeaning() {
        assertEquals(CsLink.Home(2), CsLink.parse("cyanskip://page?tab=%32"))
        assertEquals(CsLink.Home(null), CsLink.parse("cyanskip://page?tab=invalid"))
        assertEquals(CsLink.Page(SnapshotPageRoute), CsLink.parse("cyanskip://page/2"))
        assertEquals(CsLink.WeChatScanner, CsLink.parse("cyanskip://invoke/1"))
    }

    @Test
    fun malformedAndUnknownLinksDoNotTriggerActions() {
        for (value in listOf(
            "https://page/1",
            "cyanskip://page/100",
            "cyanskip://page/3",
            "cyanskip://page/4",
            "cyanskip://invoke/2",
            "cyanskip://page?tab=%ZZ"
        )) {
            assertNull(CsLink.parse(value), value)
        }
    }
}
