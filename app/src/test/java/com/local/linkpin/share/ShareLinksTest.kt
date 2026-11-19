package com.local.linkpin.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareLinksTest {

    @Test
    fun `tracking params are stripped`() {
        val cleaned = ShareLinks.clean(
            "https://www.bilibili.com/video/BV1GJ411x7h7" +
                "?spm_id_from=333.999&vd_source=abc&share_source=copy_web&p=2",
        )
        assertEquals("https://www.bilibili.com/video/BV1GJ411x7h7?p=2", cleaned)
    }

    @Test
    fun `mobile host is normalized to www`() {
        val cleaned = ShareLinks.clean("https://m.bilibili.com/video/BV1GJ411x7h7")
        assertEquals("https://www.bilibili.com/video/BV1GJ411x7h7", cleaned)
    }

    @Test
    fun `bare bilibili host is normalized to www`() {
        val cleaned = ShareLinks.clean("https://bilibili.com/video/BV1GJ411x7h7/")
        assertEquals("https://www.bilibili.com/video/BV1GJ411x7h7", cleaned)
    }

    @Test
    fun `fragment is dropped`() {
        val cleaned = ShareLinks.clean(
            "https://www.bilibili.com/video/BV1GJ411x7h7#reply123",
        )
        assertFalse(cleaned.contains("#"))
    }

    @Test
    fun `same video with different tracking has same dedupe key`() {
        val a = ShareLinks.dedupeKey(
            "https://www.bilibili.com/video/BV1GJ411x7h7?spm_id_from=333.999&vd_source=x",
        )
        val b = ShareLinks.dedupeKey(
            "https://m.bilibili.com/video/BV1GJ411x7h7/?share_source=copy_web",
        )
        assertEquals(a, b)
        assertEquals("bilibili:video:BV1GJ411x7h7?p=1", a)
    }

    @Test
    fun `different page of same video has different dedupe key`() {
        val first = ShareLinks.dedupeKey("https://www.bilibili.com/video/BV1GJ411x7h7")
        val second = ShareLinks.dedupeKey("https://www.bilibili.com/video/BV1GJ411x7h7?p=2")
        assertNotEquals(first, second)
    }

    @Test
    fun `dedupe key sorts remaining query`() {
        assertEquals(
            "https://example.com/a?a=1&b=2",
            ShareLinks.dedupeKey("https://example.com/a?b=2&a=1"),
        )
    }

    @Test
    fun `bilibili hosts detected`() {
        assertTrue(ShareLinks.isBilibili("https://b23.tv/abcd"))
        assertTrue(ShareLinks.isBilibili("https://www.bilibili.com/video/BV1GJ411x7h7"))
        assertTrue(ShareLinks.isBilibili("https://m.bilibili.com/video/BV1GJ411x7h7"))
        assertFalse(ShareLinks.isBilibili("https://example.com/video/BV1GJ411x7h7"))
    }

    @Test
    fun `short links detected`() {
        assertTrue(ShareLinks.isShortLink("https://b23.tv/abcd"))
        assertFalse(ShareLinks.isShortLink("https://www.bilibili.com/video/BV1GJ411x7h7"))
    }

    @Test
    fun `label falls back to video id for bv links`() {
        assertEquals(
            "B站视频 BV1GJ411x7h7",
            ShareLinks.labelForUrl("https://www.bilibili.com/video/BV1GJ411x7h7"),
        )
    }

    @Test
    fun `label falls back to generic names`() {
        assertEquals("B站视频", ShareLinks.labelForUrl("https://b23.tv/abcd"))
        assertEquals("B站链接", ShareLinks.labelForUrl("https://space.bilibili.com/123"))
        assertEquals("example.com", ShareLinks.labelForUrl("https://www.example.com/a"))
        assertEquals("新链接", ShareLinks.labelForUrl("not a url"))
    }
}
