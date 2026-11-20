package com.local.linkpin.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeReadLinksTest {

    private val shareUrl =
        "https://weread.qq.com/book-detail?type=1&senderVid=279182457" +
            "&v=bcb32150719afe3bbcbad52&wtheme=white&wfrom=app&wvid=279182457"

    @Test
    fun `weread share page is detected`() {
        assertTrue(WeReadLinks.isSharePage(shareUrl))
        assertTrue(WeReadLinks.isSharePage(shareUrl.replace("weread.qq.com", "m.weread.qq.com")))
    }

    @Test
    fun `book detail without token is not a share page`() {
        assertFalse(WeReadLinks.isSharePage("https://weread.qq.com/book-detail?bookId=921826"))
        assertFalse(WeReadLinks.isSharePage("https://weread.qq.com/store"))
    }

    @Test
    fun `lookalike host is not a share page`() {
        assertFalse(
            WeReadLinks.isSharePage(
                "https://weread.qq.com.example.com/book-detail?v=token",
            ),
        )
    }

    @Test
    fun `book id is parsed from cover url`() {
        val html = """
            <img src="https://cdn.weread.qq.com/weread/cover/25/yuewen_26934843/t8_yuewen_269348431702464198.jpg">
        """.trimIndent()
        assertEquals("26934843", WeReadLinks.parseBookId(html))
    }

    @Test
    fun `book id falls back to bookmark id`() {
        assertEquals("26934843", WeReadLinks.parseBookId("""{bookmarkId:"26934843_139_abc"}"""))
        assertEquals("26934843", WeReadLinks.parseBookId("""{"bookmarkId":"26934843_139_abc"}"""))
    }

    @Test
    fun `unrelated html has no book id`() {
        assertNull(WeReadLinks.parseBookId("<html><body>微信读书</body></html>"))
    }

    @Test
    fun `deep link matches the web launch button`() {
        assertEquals(
            "weread://weread.qq.com/reading?bId=26934843&style=1",
            WeReadLinks.buildDeepLink("26934843"),
        )
    }
}
