package com.local.linkpin.share

import java.net.URI

/**
 * 微信读书分享页与深链工具。
 *
 * 分享链接形如 https://weread.qq.com/book-detail?...&v=<分享 token>，
 * 页面在服务端把 token 解析成 bookId；网页上的「去微信读书 App 阅读」按钮
 * 实际拉起 weread://weread.qq.com/reading?bId=<bookId>&style=1
 * （页面内 JS 另有 weread://reading?bId=... 的写法，用于 App 内跳转）。
 *
 * 深链协议未公开、页面结构也可能调整：解析失败时调用方应回退到原网页。
 */
object WeReadLinks {

    /** 微信读书 Android 包名。 */
    const val PACKAGE = "com.tencent.weread"

    /** 封面地址形如 https://cdn.weread.qq.com/weread/cover/25/yuewen_26934843/...。 */
    private val COVER_BOOK_ID = Regex("""/cover/\d+/yuewen_(\d+)""")

    /** 热门划线形如 bookmarkId:"26934843_139_..."（也兼容 JSON 的 "bookmarkId":"..."）。 */
    private val BOOKMARK_BOOK_ID = Regex("""bookmarkId"?\s*:\s*"?(\d+)_""")

    /** 是否是带分享 token 的微信读书书籍详情页。 */
    fun isSharePage(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        if (host != "weread.qq.com" && !host.endsWith(".weread.qq.com")) return false
        if (uri.path.orEmpty().trimEnd('/') != "/book-detail") return false
        return queryValue(uri.rawQuery, "v") != null
    }

    /** 从分享页 HTML 提取 bookId：优先封面路径，其次热门划线 ID。 */
    fun parseBookId(html: String): String? =
        COVER_BOOK_ID.find(html)?.groupValues?.get(1)
            ?: BOOKMARK_BOOK_ID.find(html)?.groupValues?.get(1)

    /** 网页「去微信读书 App 阅读」按钮实际使用的深链。 */
    fun buildDeepLink(bookId: String): String =
        "weread://weread.qq.com/reading?bId=$bookId&style=1"

    private fun queryValue(query: String?, key: String): String? {
        if (query.isNullOrBlank()) return null
        return query.split('&').firstNotNullOfOrNull { part ->
            val separator = part.indexOf('=')
            val name = if (separator >= 0) part.substring(0, separator) else part
            if (name == key) {
                if (separator >= 0) part.substring(separator + 1) else ""
            } else {
                null
            }
        }
    }
}
