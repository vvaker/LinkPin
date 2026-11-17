package com.local.linkpin.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkValidatorTest {

    @Test
    fun `valid https url passes`() {
        assertTrue(
            LinkValidator.validate("https://www.bilibili.com/video/BV1xx") is
                LinkValidator.Result.Valid,
        )
    }

    @Test
    fun `valid custom scheme passes`() {
        val result = LinkValidator.validate("bilibili://video/123")
        assertTrue(result is LinkValidator.Result.Valid)
        assertEquals("bilibili://video/123", (result as LinkValidator.Result.Valid).url)
    }

    @Test
    fun `input is trimmed`() {
        val result = LinkValidator.validate("  https://example.com/a  ")
        assertEquals("https://example.com/a", (result as LinkValidator.Result.Valid).url)
    }

    @Test
    fun `empty url rejected`() {
        assertTrue(LinkValidator.validate("   ") is LinkValidator.Result.Invalid)
    }

    @Test
    fun `url with whitespace rejected`() {
        assertTrue(LinkValidator.validate("https://a b.com") is LinkValidator.Result.Invalid)
    }

    @Test
    fun `url without scheme rejected`() {
        assertTrue(LinkValidator.validate("www.baidu.com") is LinkValidator.Result.Invalid)
    }

    @Test
    fun `http without host rejected`() {
        assertTrue(LinkValidator.validate("https://") is LinkValidator.Result.Invalid)
        assertTrue(LinkValidator.validate("http:///path") is LinkValidator.Result.Invalid)
    }

    @Test
    fun `blocked schemes rejected`() {
        assertTrue(LinkValidator.validate("file:///etc/passwd") is LinkValidator.Result.Invalid)
        assertTrue(LinkValidator.validate("content://media/1") is LinkValidator.Result.Invalid)
        assertTrue(LinkValidator.validate("javascript:alert(1)") is LinkValidator.Result.Invalid)
        assertTrue(
            LinkValidator.validate("intent://x#Intent;end") is LinkValidator.Result.Invalid,
        )
    }

    @Test
    fun `label normalization trims and bounds length`() {
        assertEquals("哔哩哔哩", LinkValidator.normalizeLabel("  哔哩哔哩  "))
        assertNull(LinkValidator.normalizeLabel("   "))
        assertNull(LinkValidator.normalizeLabel("a".repeat(41)))
        assertEquals("a".repeat(40), LinkValidator.normalizeLabel("a".repeat(40)))
    }

    @Test
    fun `short link hosts detected`() {
        assertTrue(LinkValidator.isShortLink("https://b23.tv/abcd"))
        assertTrue(LinkValidator.isShortLink("https://v.douyin.com/abcd/"))
        assertTrue(!LinkValidator.isShortLink("https://www.bilibili.com/video/BV1xx"))
    }
}
