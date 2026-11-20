package com.local.linkpin.share

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 微信读书分享页解析：
 * 页面是服务端渲染的，token 会在服务端换成 bookId，因此需要取一次 HTML 再正则提取。
 * 5 秒超时、最多读取 512KB；任何失败都返回 null，由调用方回退到原网页。
 */
class WeReadBookResolver(
    private val connectTimeoutMs: Int = 5_000,
    private val readTimeoutMs: Int = 5_000,
) {

    suspend fun resolveBookId(pageUrl: String): String? = withContext(Dispatchers.IO) {
        if (!WeReadLinks.isSharePage(pageUrl)) return@withContext null
        runCatching { fetch(pageUrl) }
            .onFailure { Log.d(TAG, "resolve failed: ${it.message}") }
            .getOrNull()
            ?.let(WeReadLinks::parseBookId)
    }

    private fun fetch(pageUrl: String): String {
        val connection = (URL(pageUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", MOBILE_UA)
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")

            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            connection.inputStream.use { input ->
                while (total < MAX_BYTES) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    val keep = minOf(read, MAX_BYTES - total)
                    output.write(buffer, 0, keep)
                    total += keep
                }
            }
            return output.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "WeReadBookResolver"
        const val MAX_BYTES = 512 * 1024
        const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }
}
