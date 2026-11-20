package com.local.linkpin.routing

import java.net.URI
import java.util.Locale

/**
 * 必须先在浏览器里打开、再由网页跳转 App deep link 的站点。
 *
 * 微信读书的分享链接形如 `https://weread.qq.com/book-detail?v=<分享 token>`：
 * token 要在网页端解析成 bookId，网页再唤起 App 定位到具体书。若把这个 https 地址
 * 直接交给微信读书 App，它的网页入口解析不了 token，只会落到主页。命中这些域名时
 * 优先默认浏览器，让网页完成 token → bookId 的转换。
 */
object BrowserFirst {

    private val HOSTS = setOf(
        "weread.qq.com",
    )

    /** 是否为需要浏览器中转的链接（含子域名，如 www.weread.qq.com）。 */
    fun shouldUseBrowser(url: String): Boolean {
        val host = runCatching { URI(url.trim()).host }
            .getOrNull()
            ?.lowercase(Locale.ROOT)
            ?: return false
        return HOSTS.any { host == it || host.endsWith(".$it") }
    }
}
