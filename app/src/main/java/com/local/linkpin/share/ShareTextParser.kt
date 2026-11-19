package com.local.linkpin.share

import com.local.linkpin.routing.LinkValidator

/**
 * 把系统分享进来的文本拆成「第一个链接 + 推测标题」。
 *
 * 哔哩哔哩 App 的分享文本通常是：
 * `【视频标题】 https://b23.tv/xxxxx`（部分版本还会带 ` - 哔哩哔哩` 后缀），
 * 因此优先取【】内的文字作为名称，没有【】时取链接前的文字，再退回分享主题。
 */
object ShareTextParser {

    data class Parsed(
        val text: String,
        val url: String?,
        val suggestedLabel: String?,
    )

    /**
     * 链接只允许 http/https，遇到空白、引号、中文标点或方括号即结束。
     * 西文括号允许出现在链接里（部分站点路径合法），末尾未配对的括号由 [trimTrailing] 去掉。
     */
    private val URL_REGEX = Regex(
        """https?://[^\s<>"'“”‘’【】「」『』（）〔〕，。、；：！？\[\]{}]+""",
        RegexOption.IGNORE_CASE,
    )

    private val TITLE_BRACKET = Regex("""【([^】]+)】""")

    /** 从链接或标题尾部剥掉的装饰性字符。 */
    private val TRAILING_PUNCTUATION = charArrayOf(
        ' ', '\t', '\n',
        '.', ',', ';', ':', '!', '?',
        '，', '。', '；', '：', '！', '？', '、',
        '-', '—', '|', '·', '」', '』', '》', '>', '”', '’', '\'',
    )

    private const val ZERO_WIDTH = "\u200B\u200C\u200D\uFEFF"

    private val SQUEEZE_SPACES = Regex("""\s+""")

    private val TITLE_SUFFIXES = listOf(
        " - 哔哩哔哩",
        "- 哔哩哔哩",
        "_哔哩哔哩",
        " 哔哩哔哩",
        " - bilibili",
        "- bilibili",
    )

    fun parse(text: CharSequence?, subject: CharSequence? = null): Parsed {
        val raw = text?.toString().orEmpty().trim()
        val match = URL_REGEX.find(raw)
        val url = match?.let { trimTrailing(it.value) }?.takeIf { it.isNotBlank() }
        val label = extractLabel(raw, match, subject)
        return Parsed(text = raw, url = url, suggestedLabel = label)
    }

    private fun extractLabel(
        raw: String,
        match: MatchResult?,
        subject: CharSequence?,
    ): String? {
        val beforeUrl = if (match != null) raw.substring(0, match.range.first) else raw
        val bracketTitle = TITLE_BRACKET.find(beforeUrl)?.groupValues?.get(1)
        val fromText = cleanLabel(bracketTitle) ?: cleanLabel(beforeUrl)
        return fromText ?: cleanLabel(subject)
    }

    /** 去掉尾部未配对的西文括号与标点，避免把“，快看”之类带进链接。 */
    private fun trimTrailing(value: String): String {
        var end = value.length
        while (end > 0) {
            val last = value[end - 1]
            val opening = when (last) {
                ')' -> '('
                ']' -> '['
                '}' -> '{'
                else -> null
            }
            if (opening != null) {
                val opens = value.take(end).count { it == opening }
                val closes = value.take(end).count { it == last }
                if (opens < closes) {
                    end -= 1
                    continue
                }
                break
            }
            if (last.isWhitespace() || last in TRAILING_PUNCTUATION || last in ZERO_WIDTH) {
                end -= 1
                continue
            }
            break
        }
        return value.substring(0, end)
    }

    private fun cleanLabel(value: CharSequence?): String? {
        if (value == null) return null
        var text = value.toString()
            .replace(SQUEEZE_SPACES, " ")
            .trim()
        if (text.isEmpty()) return null

        for (suffix in TITLE_SUFFIXES) {
            if (text.endsWith(suffix, ignoreCase = true)) {
                text = text.dropLast(suffix.length).trim()
                break
            }
        }

        text = text.trim(*TRAILING_PUNCTUATION)
        if (text.isEmpty()) return null
        return text.take(LinkValidator.LABEL_MAX_LENGTH).trim().ifEmpty { null }
    }
}
