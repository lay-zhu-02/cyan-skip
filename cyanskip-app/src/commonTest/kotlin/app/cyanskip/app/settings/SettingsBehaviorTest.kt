package app.cyanskip.app.settings

import kotlinx.serialization.json.Json
import app.cyanskip.app.app.AppQuery
import app.cyanskip.app.model.AppInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsBehaviorTest {
    @Test
    fun persistedNumericSettingsDriveFilteringAndSort() {
        // A saved selection must drive the filter and sort.
        val store = Json.decodeFromString<SettingsStore>(
            """{"appGroupType":2,"appSort":3}"""
        )
        val first = AppInfo("a", "A", 1, "1", false, 0, false, 0)
        val recent = first.copy(id = "b", name = "B")
        val system = first.copy(id = "system", isSystem = true)
        assertEquals(
            listOf(recent, first), AppQuery.select(
                listOf(system, first, recent), "",
                store.appGroupType, store.appSort, visitOrder = mapOf("b" to 0)
            ).apps
        )
    }

}
