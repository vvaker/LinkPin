package com.local.linkpin.share

import com.local.linkpin.routing.LinkValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextParserTest {

    @Test
    fun `bilibili share text with bracket title`() {
        val parsed = ShareTextParser.parse("【测试视频标题】 https://b23.tv/abc123 ")
        assertEquals("https://b23.tv/abc123", parsed.url)
        assertEquals("测试视频标题", parsed.suggestedLabel)
    }

    @Test
    fun `bilibili title suffix is removed`() {
        val parsed = ShareTextParser.parse("【测试视频 - 哔哩哔哩】 https://b23.tv/abc123")
        assertEquals("测试视频", parsed.suggestedLabel)
    }

    @Test
    fun `bracket content whitespace is squeezed`() {
        val parsed = ShareTextParser.parse("【  标题   两空格  】 https://b23.tv/x")
        assertEquals("标题 两空格", parsed.suggestedLabel)
    }

    @Test
    fun `chinese punctuation after link is not part of url`() {
        val parsed = ShareTextParser.parse(
            "快看这个 https://www.bilibili.com/video/BV1GJ411x7h7？超好看",
        )
        assertEquals("https://www.bilibili.com/video/BV1GJ411x7h7", parsed.url)
        assertEquals("快看这个", parsed.suggestedLabel)
    }

    @Test
    fun `trailing bracket of sentence is trimmed`() {
        val parsed = ShareTextParser.parse("链接 https://example.com/a)")
        assertEquals("https://example.com/a", parsed.url)
    }

    @Test
    fun `paired brackets inside url are kept`() {
        val parsed = ShareTextParser.parse("https://example.com/a_(b)")
        assertEquals("https://example.com/a_(b)", parsed.url)
    }

    @Test
    fun `text before link is used as label when no bracket title`() {
        val parsed = ShareTextParser.parse("推荐视频 https://b23.tv/xyz")
        assertEquals("推荐视频", parsed.suggestedLabel)
    }

    @Test
    fun `text without link yields null url`() {
        val parsed = ShareTextParser.parse("【片段】这里没有链接")
        assertNull(parsed.url)
        assertEquals("片段", parsed.suggestedLabel)
    }

    @Test
    fun `subject is used as fallback label`() {
        val parsed = ShareTextParser.parse(null, "来自 B 站的分享")
        assertNull(parsed.url)
        assertEquals("来自 B 站的分享", parsed.suggestedLabel)
    }

    @Test
    fun `label longer than max length is truncated`() {
        val parsed = ShareTextParser.parse("【${"标".repeat(50)}】 https://b23.tv/x")
        assertEquals(LinkValidator.LABEL_MAX_LENGTH, parsed.suggestedLabel?.length)
    }

    @Test
    fun `blank text has no url and no label`() {
        val parsed = ShareTextParser.parse("   ")
        assertNull(parsed.url)
        assertNull(parsed.suggestedLabel)
        assertTrue(parsed.text.isEmpty())
    }
}
