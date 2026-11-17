package com.local.linkpin.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.local.linkpin.data.ShortcutRecord
import com.local.linkpin.data.ShortcutRepository
import com.local.linkpin.routing.LinkValidator
import com.local.linkpin.shortcut.IconProcessor
import com.local.linkpin.shortcut.ShortcutPublisher
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 创建/编辑表单 ViewModel：表单状态、保存（创建或更新）、图标处理。
 */
class EditShortcutViewModel(
    private val repository: ShortcutRepository,
    private val publisher: ShortcutPublisher,
    private val iconProcessor: IconProcessor,
) : ViewModel() {

    data class FormState(
        val url: String = "",
        val label: String = "",
        val pickedIconUri: Uri? = null,
        val existingIconPath: String? = null,
        val removeIcon: Boolean = false,
    )

    sealed interface SaveResult {
        data class Success(val pinned: Boolean) : SaveResult
        data class Failure(val message: String) : SaveResult
    }

    private val _form = MutableStateFlow(FormState())
    val form: StateFlow<FormState> = _form.asStateFlow()

    private var editing: ShortcutRecord? = null
    val isEditing: Boolean get() = editing != null

    fun init(record: ShortcutRecord?) {
        editing = record
        _form.value = FormState(
            url = record?.url.orEmpty(),
            label = record?.label.orEmpty(),
            existingIconPath = record?.iconPath,
        )
    }

    fun onUrlChange(value: String) = _form.update { it.copy(url = value) }

    fun onLabelChange(value: String) =
        _form.update { it.copy(label = value.take(LinkValidator.LABEL_MAX_LENGTH)) }

    fun onIconPicked(uri: Uri) =
        _form.update { it.copy(pickedIconUri = uri, removeIcon = false) }

    fun onRemoveIcon() =
        _form.update { it.copy(pickedIconUri = null, removeIcon = true) }

    fun save(onResult: (SaveResult) -> Unit) {
        viewModelScope.launch {
            val f = _form.value
            val urlResult = LinkValidator.validate(f.url)
            val url = (urlResult as? LinkValidator.Result.Valid)?.url
            if (url == null) {
                onResult(
                    SaveResult.Failure(
                        (urlResult as LinkValidator.Result.Invalid).reason,
                    ),
                )
                return@launch
            }
            val label = LinkValidator.normalizeLabel(f.label)
            if (label == null) {
                onResult(
                    SaveResult.Failure("名称不能为空且不超过 ${LinkValidator.LABEL_MAX_LENGTH} 个字符"),
                )
                return@launch
            }

            val base = editing
            val now = System.currentTimeMillis()
            val record = base?.copy(
                url = url,
                label = label,
                updatedAt = now,
            ) ?: ShortcutRecord(
                id = UUID.randomUUID().toString(),
                url = url,
                label = label,
                createdAt = now,
                updatedAt = now,
            )

            var iconPath = record.iconPath
            val oldPath = base?.iconPath
            if (f.removeIcon) {
                oldPath?.let { runCatching { File(it).delete() } }
                iconPath = null
            } else if (f.pickedIconUri != null) {
                val newPath = iconProcessor.process(f.pickedIconUri, record.id)
                if (newPath != null) {
                    if (oldPath != null && oldPath != newPath) {
                        runCatching { File(oldPath).delete() }
                    }
                    iconPath = newPath
                }
                // 处理失败时保留旧图标（或默认图标），不中断保存
            }

            val finalRecord = record.copy(iconPath = iconPath)
            repository.upsert(finalRecord)
            val pinned = if (base == null) {
                publisher.create(finalRecord)
            } else {
                publisher.update(finalRecord)
            }
            onResult(SaveResult.Success(pinned))
        }
    }
}
