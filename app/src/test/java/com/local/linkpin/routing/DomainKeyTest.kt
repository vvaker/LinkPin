package com.local.linkpin.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DomainKeyTest {

    @Test
    fun `http host is lowercased`() {
        assertEquals("www.bilibili.com", DomainKey.of("HTTPS://WWW.Bilibili.COM/video/BV1"))
    }

    @Test
    fun `port path query fragment are ignored`() {
        assertEquals("example.com", DomainKey.of("https://example.com:8443/a/b?x=1#frag"))
        assertEquals("example.com", DomainKey.of("https://example.com/other?y=2"))
    }

    @Test
    fun `same host shares key across pages`() {
        val a = DomainKey.of("https://www.bilibili.com/video/BV1xx")
        val b = DomainKey.of("https://www.bilibili.com/bangumi/play/ep123")
        assertEquals(a, b)
    }

    @Test
    fun `custom scheme with host`() {
        assertEquals("wxapp://miniapp", DomainKey.of("wxapp://MiniApp/pages/index"))
    }

    @Test
    fun `custom scheme without host`() {
        // 无 // 的 opaque URI 没有 host（authority）
        assertEquals("mailto:", DomainKey.of("mailto:someone@example.com"))
    }

    @Test
    fun `custom scheme authority is treated as host`() {
        // bilibili://video/123 中 video 是 authority，按 URI 语义作为 host
        assertEquals("bilibili://video", DomainKey.of("bilibili://video/123"))
    }

    @Test
    fun `invalid input returns null`() {
        assertNull(DomainKey.of(""))
        assertNull(DomainKey.of("not a url"))
    }
}
