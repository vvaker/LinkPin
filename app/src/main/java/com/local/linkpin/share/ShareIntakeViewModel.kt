package com.local.linkpin.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.local.linkpin.data.ShortcutRecord
import com.local.linkpin.data.ShortcutRepository
import com.local.linkpin.routing.LinkValidator
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 分享保存卡片的状态机：
 * 解析分享文本 → 展开短链（可选）→ 检查重复 → 保存（新增或更新已有记录）。
 * 只写入本地列表，不请求固定桌面图标；固定仍由用户在列表页手动触发。
 */
class ShareIntakeViewModel(
    private val repository: ShortcutRepository,
    private val shortLinkResolver: ShortLinkResolver,
    private val weReadBookResolver: WeReadBookResolver = WeReadBookResolver(),
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        /** 正在展开短链。 */
        val resolving: Boolean = false,
        /** 正在解析微信读书分享页里的 bookId。 */
        val resolvingBook: Boolean = false,
        /** 正在比对已有记录。 */
        val checking: Boolean = false,
        val url: String = "",
        val label: String = "",
        /** 微信读书深链；识别失败时为空，点击仍走原网页。 */
        val deepLink: String? = null,
        val note: String? = null,
        val existing: ShortcutRecord? = null,
        val error: String? = null,
        val saving: Boolean = false,
        val saved: Boolean = false,
        val savedAsUpdate: Boolean = false,
    ) {
        val busy: Boolean get() = resolving || resolvingBook || checking || saving
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var prepareToken = 0
    private var labelEdited = false

    fun prepare(text: String?, subject: String?) {
        val token = ++prepareToken
        labelEdited = false

        val parsed = ShareTextParser.parse(text, subject)
        val parsedUrl = parsed.url
        if (parsedUrl == null) {
            _state.value = UiState(
                loading = false,
                error = "这段分享内容里没有找到链接",
            )
            return
        }

        val cleaned = ShareLinks.clean(parsedUrl)
        val resolving = ShareLinks.isShortLink(cleaned)
        _state.value = UiState(
            loading = false,
            resolving = resolving,
            url = cleaned,
            label = parsed.suggestedLabel ?: ShareLinks.labelForUrl(cleaned),
        )

        viewModelScope.launch {
            var finalUrl = cleaned
            if (resolving) {
                val expanded = shortLinkResolver.expand(cleaned)
                if (token != prepareToken) return@launch
                finalUrl = expanded?.let(ShareLinks::clean) ?: cleaned
                _state.update { state ->
                    state.copy(
                        resolving = false,
                        url = finalUrl,
                        label = if (!labelEdited && parsed.suggestedLabel.isNullOrBlank()) {
                            ShareLinks.labelForUrl(finalUrl)
                        } else {
                            state.label
                        },
                        note = if (expanded == null) "没能展开短链，将保存原始分享链接" else null,
                    )
                }
            }
            if (WeReadLinks.isSharePage(finalUrl)) {
                _state.update { it.copy(resolvingBook = true, note = null) }
                val bookId = weReadBookResolver.resolveBookId(finalUrl)
                if (token != prepareToken) return@launch
                _state.update { state ->
                    state.copy(
                        resolvingBook = false,
                        deepLink = bookId?.let(WeReadLinks::buildDeepLink),
                        note = if (bookId == null) {
                            "没能识别这本书，点击时仍用浏览器打开"
                        } else {
                            "已识别为微信读书，保存后点击直达 App"
                        },
                    )
                }
            }
            if (token == prepareToken) checkExisting(token)
        }
    }

    fun onLabelChange(value: String) {
        labelEdited = true
        _state.update {
            it.copy(
                label = value.take(LinkValidator.LABEL_MAX_LENGTH),
                error = null,
            )
        }
    }

    fun save() {
        val current = _state.value
        if (current.busy) return

        val url = when (val result = LinkValidator.validate(current.url)) {
            is LinkValidator.Result.Valid -> result.url
            is LinkValidator.Result.Invalid -> {
                _state.update { it.copy(error = result.reason) }
                return
            }
        }
        val label = LinkValidator.normalizeLabel(current.label)
        if (label == null) {
            _state.update {
                it.copy(error = "名称不能为空且不超过 ${LinkValidator.LABEL_MAX_LENGTH} 个字符")
            }
            return
        }

        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val base = current.existing
            // 解析失败时沿用已有记录的深链；新记录没有深链则留空，点击仍走原网页。
            val deepLink = current.deepLink ?: base?.deepLink
            val record = base?.copy(url = url, label = label, deepLink = deepLink, updatedAt = now)
                ?: ShortcutRecord(
                    id = UUID.randomUUID().toString(),
                    url = url,
                    label = label,
                    deepLink = deepLink,
                    createdAt = now,
                    updatedAt = now,
                )
            runCatching { repository.upsert(record) }
                .onSuccess {
                    _state.update {
                        it.copy(saving = false, saved = true, savedAsUpdate = base != null)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            saving = false,
                            error = "保存失败：${error.message ?: "未知错误"}",
                        )
                    }
                }
        }
    }

    private suspend fun checkExisting(token: Int) {
        val current = _state.value
        if (current.url.isBlank()) return
        _state.update { it.copy(checking = true) }
        val key = ShareLinks.dedupeKey(current.url)
        val deepLink = current.deepLink
        val match = runCatching {
            repository.observeAll().first().firstOrNull { record ->
                (deepLink != null && record.deepLink == deepLink) ||
                    ShareLinks.dedupeKey(record.url) == key
            }
        }.getOrNull()
        if (token != prepareToken) return
        _state.update { state ->
            state.copy(
                checking = false,
                existing = match,
                // 本次没解析出深链时沿用已有记录的，避免更新后丢失直达能力
                deepLink = state.deepLink ?: match?.deepLink,
                // 重复链接优先保留用户之前起的名称，避免被分享标题覆盖
                label = if (match != null && !labelEdited) match.label else state.label,
            )
        }
    }
}
