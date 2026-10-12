package app.cyanskip.app.backup

import app.cyanskip.app.rule.RuleGroupPolicy
import app.cyanskip.app.rule.RuleGroupSummaryBuilder
import app.cyanskip.app.subscription.RawSubscription
import app.cyanskip.app.subscription.UsedSubsEntry
import app.cyanskip.db.SubsGlobalGroupConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {
    private val policy = RuleGroupPolicy()

    private val current = """
        {"formatVersion":2,
         "subsItems":[{"id":7,"ctime":1,"mtime":2,"enable":true,"order":0}],
         "appConfigs":[{"subsId":7,"appId":"app.one","enable":true}],
         "categoryConfigs":[{"subsId":7,"categoryKey":3}],
         "appGroupConfigs":[{"subsId":7,"appId":"app.one","groupKey":4,"exclude":"app-exclude"}],
         "globalGroupConfigs":[{"subsId":7,"groupKey":4,"enable":false,"exclude":"global-exclude"}]}
    """.trimIndent()

    @Test
    fun importedConfigurationGivesUiAndRuntimeTheSameEffectiveSwitchState() {
        val data = BackupFormat.decode(current).toSnapshot()
        val subscription = RawSubscription.parse(
            """
            {
              id: 7, name: 'Sample', version: 1,
              categories: [{key: 3, name: 'Batch', enable: false}],
              apps: [{id: 'app.one', groups: [{key: 4, name: 'Batch', enable: true, rules: []}]}],
              globalGroups: [{key: 4, name: 'Global', enable: true, rules: []}],
            }
            """.trimIndent(),
        )
        val group = subscription.apps.single().groups.single()
        val category = subscription.categories.single()
        val uiEnabled = policy.getGroupEnabled(
            group,
            data.appGroupConfigs.find { it.groupKey == group.key },
            category,
            data.categoryConfigs.find { it.categoryKey == category.key },
        )
        val summary = RuleGroupSummaryBuilder.build(
            policy,
            subscriptions = listOf(UsedSubsEntry(data.subsItems.single(), subscription)),
            appInfoById = emptyMap(),
            appConfigs = data.appConfigs,
            groupConfigs = data.appGroupConfigs + data.globalGroupConfigs,
            categoryConfigs = data.categoryConfigs,
        )
        assertTrue(uiEnabled)
        assertEquals(uiEnabled, summary.appIdToAllGroups.getValue("app.one").single().enable)
        assertTrue(summary.globalGroups.isEmpty())
    }

    @Test
    fun reexportedBackupKeepsNullRecordsAndAllConfigurationValues() {
        val converted = BackupFormat.decode(current)
        val encoded = BackupFormat.encode(converted)
        assertTrue(encoded.contains("\"formatVersion\":2"))
        assertFalse(encoded.contains("\"subsConfigs\""))
        assertEquals(converted.toSnapshot(), BackupFormat.decode(encoded).toSnapshot())
        assertNull(BackupFormat.decode(encoded).categoryConfigs.single().enable)
    }

    @Test
    fun defaultAppScopeRoundTripsAllThreeValuesWithoutChangingOtherOverrides() {
        listOf(null, false, true).forEach { value ->
            val original = BackupFormat.decode(current).toSnapshot().copy(
                globalGroupConfigs = listOf(SubsGlobalGroupConfig(7, 4, false, "!app.one", value)),
            )
            val encoded = BackupFormat.encode(BackupDatabaseData.fromSnapshot(original))
            assertEquals(original, BackupFormat.decode(encoded).toSnapshot())
        }
    }

    @Test
    fun missingOrUnsupportedVersionsFailBeforeImportCanStart() {
        assertThrows(app.cyanskip.app.storage.StorageException::class.java) {
            BackupFormat.decode("""{}""")
        }
        assertThrows(app.cyanskip.app.storage.StorageException::class.java) {
            BackupFormat.decode("""{"formatVersion":99}""")
        }
        assertThrows(app.cyanskip.app.storage.StorageException::class.java) {
            BackupFormat.decode("""{"formatVersion":1}""")
        }
    }
}
