// Return-URL parsing pinned by the language-neutral vector set shared with
// the Swift Link SDK: contract-tests/fixtures/return-url.json. Runs on a
// plain JVM (the parser has no Android dependencies).

package com.budgetbakers.bbconnect

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnUrlTest {

    private fun loadVectors(): List<JsonObject> {
        // bbconnect/ module dir → sdks/kotlin-link → sdks → repo root.
        var dir = File("").absoluteFile
        while (!File(dir, "contract-tests/fixtures/return-url.json").exists()) {
            dir = dir.parentFile ?: error("contract-tests/fixtures/return-url.json not found")
        }
        val fixture = File(dir, "contract-tests/fixtures/return-url.json").readText()
        return JsonParser.parseString(fixture)
            .asJsonObject.getAsJsonArray("vectors")
            .map { it.asJsonObject }
    }

    private fun optString(obj: JsonObject, key: String): String? =
        if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asString else null

    @Test
    fun allVectors() {
        val vectors = loadVectors()
        assertTrue("expected >= 10 vectors", vectors.size >= 10)

        for (vector in vectors) {
            val name = vector.get("name").asString
            val url = vector.get("url").asString
            val expect = vector.getAsJsonObject("expect")
            val parsed = BBConnectReturnUrl.parse(url)

            if (expect.has("invalid")) {
                assertNull("$name: expected invalid", parsed)
                continue
            }
            assertNotNull("$name: expected a parsed return URL", parsed)
            parsed!!
            assertEquals(name, optString(expect, "sessionId"), parsed.sessionId)
            assertEquals(name, optString(expect, "connectionId"), parsed.connectionId)
            assertEquals(name, optString(expect, "resultCode"), parsed.resultCode.wire)
            assertEquals(name, optString(expect, "error"), parsed.error)
        }
    }

    @Test
    fun outcomeMapping() {
        val ok = BBConnectReturnUrl("cs_1", "conn_1", BBConnectResultCode.OK, null)
        assertEquals(BBConnectOutcome.Success("cs_1", "conn_1"), BBConnectOutcome.from(ok))

        val failed =
            BBConnectReturnUrl("cs_1", null, BBConnectResultCode.ERROR, "authentication_failed")
        assertEquals(
            BBConnectOutcome.Failure("cs_1", "authentication_failed"),
            BBConnectOutcome.from(failed),
        )

        val cancelled = BBConnectReturnUrl("cs_1", null, BBConnectResultCode.CANCELLED, null)
        assertEquals(BBConnectOutcome.Cancelled("cs_1"), BBConnectOutcome.from(cancelled))
    }
}
