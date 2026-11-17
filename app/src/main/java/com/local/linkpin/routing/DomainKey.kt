package com.local.linkpin.routing

import java.net.URI

/**
 * 路由键提取：
 * - HTTP/HTTPS：小写 host（不含端口）。
 * - 自定义 scheme：有 host 用 `scheme://host`，无 host 用 `scheme:`。
 * - 路径、查询参数、fragment 不参与。
 */
object DomainKey {

    private const val HTTP = "http"
    private const val HTTPS = "https"

    fun of(url: String): String? {
        val uri = try {
            URI(url.trim())
        } catch (e: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase() ?: return null
        return when (scheme) {
            HTTP, HTTPS -> uri.host?.lowercase()
            else -> {
                val host = uri.host
                if (host.isNullOrBlank()) "$scheme:" else "$scheme://${host.lowercase()}"
            }
        }
    }
}
