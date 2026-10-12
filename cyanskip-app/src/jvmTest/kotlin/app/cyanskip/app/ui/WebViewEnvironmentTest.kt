package app.cyanskip.app.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import app.cyanskip.app.model.WebViewEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class WebViewEnvironmentTest {
    @Test
    fun webContractPreservesTypesAndEscapesTextIncludingFalseAndZero() {
        val name = "应用\"名称\\\n😀"
        val json = Json.parseToJsonElement(
            WebViewEnvironment(
                "android", "test.app", name, 0, "1.0", "cyanskip", false,
            ).toJson()
        ).jsonObject
        // These names and types are the external JavaScript API contract.
        assertEquals(
            setOf(
                "platform",
                "appId",
                "appName",
                "versionCode",
                "versionName",
                "channel",
                "debuggable"
            ), json.keys
        )
        assertEquals(JsonPrimitive("android"), json["platform"])
        assertEquals(JsonPrimitive("test.app"), json["appId"])
        assertEquals(JsonPrimitive(name), json["appName"])
        assertEquals(JsonPrimitive(0), json["versionCode"])
        assertEquals(JsonPrimitive("1.0"), json["versionName"])
        assertEquals(JsonPrimitive("cyanskip"), json["channel"])
        assertEquals(JsonPrimitive(false), json["debuggable"])
    }
}
