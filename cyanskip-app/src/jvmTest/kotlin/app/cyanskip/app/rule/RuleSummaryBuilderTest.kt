package app.cyanskip.app.rule

import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.UsedSubsEntry
import app.cyanskip.db.SubsAppConfig
import app.cyanskip.db.SubsItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RuleSummaryBuilderTest {
    private val policy = RuleGroupPolicy()
    private val runtime = RuleRuntime()

    @Test
    fun enabledGroupWithoutEnabledRulesDoesNotCountAsRuleApp() {
        val subscription = RawSubscription.parse(
            """
            {
              id: 1,
              name: 'Empty rules',
              version: 1,
              apps: [{
                id: 'app.id',
                groups: [{ key: 1, name: 'Group', rules: [] }],
              }],
            }
            """.trimIndent(),
        )

        val summary = RuleSummaryBuilder.build(

            policy, runtime,
            subscriptions = listOf(
                UsedSubsEntry(
                    subsItem = SubsItem(id = subscription.id, enable = true, order = 0),
                    subscription = subscription,
                ),
            ),
            appInfoById = emptyMap(),
            appConfigs = listOf(
                SubsAppConfig(
                    enable = true,
                    subsId = subscription.id,
                    appId = "app.id",
                ),
            ),
            groupConfigs = emptyList(),
            categoryConfigs = emptyList(),
        )

        assertFalse(summary.appIdToRules.containsKey("app.id"))
        assertEquals(1, summary.appIdToGroups.getValue("app.id").size)
        assertEquals(0, summary.appSize)
    }
}
