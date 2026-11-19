package com.local.linkpin.share

import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 把 `b23.tv` 这类 B 站短链展开成最终视频页地址。
 *
 * 分享文本通常给的是短链（`https://b23.tv/xxxxx`），直接保存的话路由键会变成
 * `b23.tv`，跳转还要经浏览器；展开后保存的是 `www.bilibili.com/video/BV...`，
 * 与手动输入的长链行为一致，也能和已有记录去重。
 *
 * 只跟随 http/https 跳转，最终地址必须仍属于 B 站；任何一步失败都返回 null，
 * 由调用方回退保存原始短链。
 */
class ShortLinkResolver(
    private val connectTimeoutMs: Int = 5_000,
    private val readTimeoutMs: Int = 5_000,
    private val maxHops: Int = 6,
) {

    suspend fun expand(url: String): String? = withContext(Dispatchers.IO) {
        if (!ShareLinks.isShortLink(url)) return@withContext null
        runCatching { follow(url) }
            .onFailure { Log.d(TAG, "短链展开失败：$url", it) }
            .getOrNull()
    }

    private fun follow(start: String): String? {
        var current = start
        repeat(maxHops) {
            val uri = runCatching { URI(current) }.getOrNull() ?: return null
            if (uri.scheme?.lowercase(Locale.ROOT) !in HTTP_SCHEMES) return null

            val connection = runCatching {
                (uri.toURL().openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", MOBILE_UA)
                    setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
                }
            }.getOrNull() ?: return null

            try {
                when (val code = connection.responseCode) {
                    in 300..399 -> {
                        val location = connection.getHeaderField("Location")
                        if (location.isNullOrBlank()) return null
                        current = uri.resolve(location).toString()
                        return@repeat
                    }

                    200 -> {
                        val finalUrl = connection.url.toString()
                        if (ShareLinks.isBilibili(finalUrl)) return finalUrl
                        // 少数短链服务返回 200 + HTML（meta refresh / canonical 链接）
                        val body = runCatching { readBounded(connection.inputStream) }.getOrDefault("")
                        val fromHtml = BILIBILI_URL_IN_HTML.find(body)?.value
                        return fromHtml?.takeIf { ShareLinks.isBilibili(it) }
                    }

                    else -> return null
                }
            } finally {
                connection.disconnect()
            }
        }
        return current.takeIf { ShareLinks.isBilibili(it) }
    }

    private fun readBounded(stream: InputStream): String {
        val output = ByteArrayOutputStream()
        BufferedInputStream(stream).use { input ->
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (total < MAX_BODY_BYTES) {
                val read = input.read(buffer, 0, minOf(buffer.size, MAX_BODY_BYTES - total))
                if (read <= 0) break
                output.write(buffer, 0, read)
                total += read
            }
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private companion object {
        const val TAG = "ShortLinkResolver"
        const val MAX_BODY_BYTES = 64 * 1024
        val HTTP_SCHEMES = setOf("http", "https")

        const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        val BILIBILI_URL_IN_HTML =
            Regex("""https?://(?:www\.|m\.)?bilibili\.com/video/BV[0-9A-Za-z]{10}[^\s"'<>]*""")
    }
}
