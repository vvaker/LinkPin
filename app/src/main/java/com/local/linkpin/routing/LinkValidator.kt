package com.local.linkpin.routing

import java.net.URI

/**
 * 输入校验：
 * - 名称：去除首尾空白后非空，长度 1..40。
 * - 链接：HTTP/HTTPS 必须含 host；自定义 scheme 非空且无空白；
 *   拒绝 file/content/javascript/intent。
 */
object LinkValidator {

    sealed interface Result {
        data class Valid(val url: String) : Result
        data class Invalid(val reason: String) : Result
    }

    const val LABEL_MAX_LENGTH = 40

    private val BLOCKED_SCHEMES = setOf("file", "content", "javascript", "intent")
    private val SHORT_LINK_HOSTS = setOf("b23.tv", "v.douyin.com")

    fun validate(raw: String): Result {
        val url = raw.trim()
        if (url.isEmpty()) return Result.Invalid("链接不能为空")
        if (url.any { it.isWhitespace() }) return Result.Invalid("链接不能包含空白字符")

        val uri = try {
            URI(url)
        } catch (e: Exception) {
            return Result.Invalid("链接格式无效")
        }

        val scheme = uri.scheme?.lowercase() ?: return Result.Invalid("链接缺少协议，例如 https://")
        if (scheme in BLOCKED_SCHEMES) return Result.Invalid("不允许使用 $scheme:// 链接")
        if ((scheme == "http" || scheme == "https") && uri.host.isNullOrBlank()) {
            return Result.Invalid("HTTP/HTTPS 链接必须包含有效域名")
        }
        return Result.Valid(url)
    }

    fun normalizeLabel(raw: String): String? {
        val label = raw.trim()
        if (label.isEmpty() || label.length > LABEL_MAX_LENGTH) return null
        return label
    }

    fun isShortLink(url: String): Boolean {
        val host = try {
            URI(url.trim()).host?.lowercase()
        } catch (e: Exception) {
            null
        }
        return host in SHORT_LINK_HOSTS
    }
}
