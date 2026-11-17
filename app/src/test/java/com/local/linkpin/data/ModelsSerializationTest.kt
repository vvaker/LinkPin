package com.local.linkpin.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsSerializationTest {

    private val json = JsonCodec.json

    @Test
    fun `shortcut record round trip`() {
        val record = ShortcutRecord(
            id = "abc-123",
            url = "https://www.bilibili.com/video/BV1xx",
            label = "我的视频",
            iconPath = "/data/user/0/com.local.linkpin/files/icons/abc-123.png",
            createdAt = 1_000L,
            updatedAt = 2_000L,
        )
        val encoded = json.encodeToString(ShortcutRecord.serializer(), record)
        val decoded = json.decodeFromString(ShortcutRecord.serializer(), encoded)
        assertEquals(record, decoded)
    }

    @Test
    fun `record list round trip`() {
        val records = listOf(
            ShortcutRecord("1", "https://a.com", "A", createdAt = 1L, updatedAt = 1L),
            ShortcutRecord("2", "https://b.com", "B", createdAt = 2L, updatedAt = 2L),
        )
        val encoded = json.encodeToString(records)
        val decoded = json.decodeFromString<List<ShortcutRecord>>(encoded)
        assertEquals(records, decoded)
    }

    @Test
    fun `route rule app round trip with discriminator`() {
        val rule = RouteRule("www.bilibili.com", RouteTarget.App("tv.danmaku.bili"))
        val encoded = json.encodeToString(RouteRule.serializer(), rule)
        assertTrue(encoded.contains("\"type\":\"app\""))
        val decoded = json.decodeFromString(RouteRule.serializer(), encoded)
        assertEquals(rule, decoded)
    }

    @Test
    fun `route rule browser round trip with discriminator`() {
        val rule = RouteRule("www.baidu.com", RouteTarget.Browser)
        val encoded = json.encodeToString(RouteRule.serializer(), rule)
        assertTrue(encoded.contains("\"type\":\"browser\""))
        val decoded = json.decodeFromString(RouteRule.serializer(), encoded)
        assertEquals(rule, decoded)
    }

    @Test
    fun `route rule map round trip`() {
        val rules = mapOf(
            "www.bilibili.com" to RouteTarget.App("tv.danmaku.bili"),
            "www.baidu.com" to RouteTarget.Browser,
        )
        val encoded = json.encodeToString(rules)
        val decoded = json.decodeFromString<Map<String, RouteTarget>>(encoded)
        assertEquals(rules, decoded)
    }

    @Test
    fun `unknown keys are ignored`() {
        val raw = """
            {"id":"1","url":"https://a.com","label":"A","createdAt":1,"updatedAt":2,
             "futureField":"anything"}
        """.trimIndent()
        val decoded = json.decodeFromString(ShortcutRecord.serializer(), raw)
        assertEquals("1", decoded.id)
        assertEquals("A", decoded.label)
    }

    @Test
    fun `missing icon path defaults to null`() {
        val raw = """{"id":"1","url":"https://a.com","label":"A","createdAt":1,"updatedAt":2}"""
        val decoded = json.decodeFromString(ShortcutRecord.serializer(), raw)
        assertEquals(null, decoded.iconPath)
    }
}
