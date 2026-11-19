package com.local.linkpin.share

import com.local.linkpin.routing.LinkValidator
import java.net.URI
import java.util.Locale

/**
 * 分享链接的清洗与去重键：
 * - 去掉各站点分享时附带的追踪/会话参数；
 * - `m.bilibili.com` 归一成 `www.bilibili.com`，继续沿用「按域名记忆」的路由规则；
 * - 去重键把同一个 B 站视频的不同分享形式（带/不带追踪参数、不同分P）视为同一条，
 *   避免重复收藏。
 */
object ShareLinks {

    private val SHORT_LINK_HOSTS = setOf("b23.tv")

    private val BILIBILI_HOSTS = setOf(
        "bilibili.com",
        "www.bilibili.com",
        "m.bilibili.com",
        "b23.tv",
    )

    /** 常见分享/追踪参数，保存前统一剥离。 */
    private val TRACKING_PARAMS = setOf(
        "spm_id_from",
        "vd_source",
        "from_source",
        "from_spmid",
        "share_source",
        "share_medium",
        "share_plat",
        "share_session_id",
        "share_tag",
        "share_times",
        "share_from",
        "timestamp",
        "unique_k",
        "bbid",
        "ts",
        "buvid",
        "up_id",
        "is_story_h5",
        "plat_id",
        "seid",
        "srch_tag",
    )

    private val BILIBILI_VIDEO_PATH = Regex("""/video/(BV[0-9A-Za-z]{10})/?""")

    fun isShortLink(url: String): Boolean = hostOf(url) in SHORT_LINK_HOSTS

    fun isBilibili(url: String): Boolean = hostOf(url) in BILIBILI_HOSTS

    /** 保存前清洗：去掉追踪参数、统一 B 站主域名、去掉视频页多余尾斜杠。 */
    fun clean(url: String): String {
        val trimmed = url.trim()
        val uri = uriOrNull(trimmed) ?: return trimmed
        val host = uri.host?.lowercase(Locale.ROOT) ?: return trimmed
        val normalizedHost = when (host) {
            "m.bilibili.com", "bilibili.com" -> "www.bilibili.com"
            else -> host
        }
        var path = uri.rawPath.orEmpty()
        if (normalizedHost.endsWith("bilibili.com") && path.length > 1 && path.endsWith("/")) {
            path = path.dropLast(1)
        }
        return rebuild(uri, normalizedHost, path, filteredQuery(uri.rawQuery))
    }

    /** 稳定去重键：同一个 B 站视频（BV + 分P）视为同一条。 */
    fun dedupeKey(url: String): String {
        val cleaned = clean(url)
        val uri = uriOrNull(cleaned) ?: return cleaned.lowercase(Locale.ROOT)
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return cleaned
        val host = uri.host?.lowercase(Locale.ROOT) ?: return cleaned
        val path = uri.rawPath.orEmpty().trimEnd('/').ifEmpty { "/" }

        if (host.endsWith("bilibili.com")) {
            val bv = BILIBILI_VIDEO_PATH.matchEntire(path)?.groupValues?.get(1)
            if (bv != null) {
                val page = queryValue(uri.rawQuery, "p") ?: "1"
                return "bilibili:video:$bv?p=$page"
            }
        }

        val query = filteredQuery(uri.rawQuery)
            ?.split('&')
            ?.sorted()
            ?.joinToString("&")
        return buildString {
            append(scheme).append("://").append(host).append(path)
            if (!query.isNullOrEmpty()) append('?').append(query)
        }
    }

    /** 分享文本里没有标题时的兜底名称。 */
    fun labelForUrl(url: String): String {
        val uri = uriOrNull(url)
        val host = uri?.host?.lowercase(Locale.ROOT).orEmpty()
        val bv = uri?.rawPath?.let { BILIBILI_VIDEO_PATH.find(it)?.groupValues?.get(1) }
        val label = when {
            bv != null -> "B站视频 $bv"
            host == "b23.tv" -> "B站视频"
            host.endsWith("bilibili.com") || host == "bilibili.tv" -> "B站链接"
            host.isNotBlank() -> host.removePrefix("www.")
            else -> "新链接"
        }
        return label.take(LinkValidator.LABEL_MAX_LENGTH)
    }

    private fun hostOf(url: String): String? =
        uriOrNull(url)?.host?.lowercase(Locale.ROOT)

    private fun uriOrNull(url: String): URI? =
        runCatching { URI(url.trim()) }.getOrNull()?.takeIf { it.scheme != null }

    private fun filteredQuery(rawQuery: String?): String? {
        if (rawQuery.isNullOrEmpty()) return null
        val kept = rawQuery.split('&').filter { part ->
            val key = part.substringBefore('=').lowercase(Locale.ROOT)
            key.isNotEmpty() && key !in TRACKING_PARAMS
        }
        return kept.takeIf { it.isNotEmpty() }?.joinToString("&")
    }

    private fun queryValue(rawQuery: String?, key: String): String? =
        rawQuery?.split('&')
            ?.firstOrNull { it.substringBefore('=').lowercase(Locale.ROOT) == key }
            ?.substringAfter('=', "")
            ?.takeIf { it.isNotBlank() }

    private fun rebuild(uri: URI, host: String, path: String, query: String?): String {
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return uri.toString()
        val port = uri.port.takeIf { it != -1 }
        return buildString {
            append(scheme).append("://").append(host)
            if (port != null) append(':').append(port)
            append(path)
            if (!query.isNullOrEmpty()) append('?').append(query)
        }
    }
}
