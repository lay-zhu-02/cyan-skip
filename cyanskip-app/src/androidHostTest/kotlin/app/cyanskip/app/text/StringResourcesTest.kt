package app.cyanskip.app.text

import android.app.Application
import app.cyanskip.app.resources.Res
import app.cyanskip.app.resources.action_log_recent_prefix
import app.cyanskip.app.resources.app_rule_input_hint
import app.cyanskip.app.resources.rule_name_duplicate
import app.cyanskip.app.resources.selector_invalid_detail
import app.cyanskip.app.ui.text.getSync
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class StringResourcesTest {
    @Test
    fun androidAssetReadsPreserveWhitespaceAndNewlines() {
        assertEquals("最近触发: ", Res.string.action_log_recent_prefix.getSync())
        assertEquals("请输入应用规则\n", Res.string.app_rule_input_hint.getSync())
    }

    @Test
    fun userSuppliedRuleNamesAreInsertedVerbatimWithoutInterpretingFormatOrXmlCharacters() {
        val name = "50% <button a=\"b\"> & '${'$'}{name}'"
        assertEquals("已存在同名「$name」规则", Res.string.rule_name_duplicate.getSync(name))
    }

    @Test
    fun multilineSelectorErrorsPreserveBothResourceAndArgumentLineBreaks() {
        val selector = "[text=\"a\"]\n[text=\"b\"]"
        val detail = "unexpected %1\$s & <token>"
        assertEquals(
            "非法选择器\n$selector\n$detail",
            Res.string.selector_invalid_detail.getSync(selector, detail)
        )
    }
}
