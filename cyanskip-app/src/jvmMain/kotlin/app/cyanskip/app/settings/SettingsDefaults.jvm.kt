package app.cyanskip.app.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import app.cyanskip.app.DesktopProfile

private val settingsJson = Json { ignoreUnknownKeys = true }

actual fun defaultSettings(): SettingsStore {
    return settingsJson.decodeFromJsonElement(
        JsonObject(
            mapOf(
                "actionToast" to JsonPrimitive("青跳❤"),
            ) + DesktopProfile.settings
        )
    )
}

actual fun defaultBlockMatchAppList(): Set<String> = DesktopProfile.blocked
