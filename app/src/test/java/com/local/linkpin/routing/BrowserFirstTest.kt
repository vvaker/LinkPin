package com.local.linkpin.routing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserFirstTest {

    @Test
    fun `weread share link uses browser`() {
        val url = "https://weread.qq.com/book-detail?type=1&v=bcb32150719afe3bbcbad52" +
            "&wfrom=app&scene=bottomSheetShare"
        assertTrue(BrowserFirst.shouldUseBrowser(url))
    }

    @Test
    fun `weread subdomain uses browser`() {
        assertTrue(BrowserFirst.shouldUseBrowser("https://www.weread.qq.com/book-detail?v=1"))
    }

    @Test
    fun `suffix lookalike domain is not matched`() {
        assertFalse(BrowserFirst.shouldUseBrowser("https://weread.qq.com.example.com/book-detail?v=1"))
        assertFalse(BrowserFirst.shouldUseBrowser("https://notweread.qq.com/book-detail?v=1"))
    }

    @Test
    fun `other sites keep the normal routing flow`() {
        assertFalse(BrowserFirst.shouldUseBrowser("https://www.bilibili.com/video/BV1GJ411x7h7"))
        assertFalse(BrowserFirst.shouldUseBrowser("https://example.com/weread.qq.com"))
    }

    @Test
    fun `custom scheme and blank input are not browser-first`() {
        assertFalse(BrowserFirst.shouldUseBrowser("weread://book?bookId=1"))
        assertFalse(BrowserFirst.shouldUseBrowser("   "))
    }
}
