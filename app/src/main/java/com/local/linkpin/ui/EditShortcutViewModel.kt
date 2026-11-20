package com.local.linkpin.ui

import android.graphics.Bitmap
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
 * 创建/编辑表单 ViewModel：表单状态、保存（创建或更新）、图标处理与裁剪状态。
 * 保存不再自动固定桌面，桌面入口由列表里的图钉按钮按需触发。
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
        /** 裁剪确认后的位图；非空时保存优先使用它。 */
        val croppedIcon: Bitmap? = null,
        val existingIconPath: String? = null,
        val removeIcon: Boolean = false,
    )

    sealed interface SaveResult {
        data object Success : SaveResult
        data class Failure(val message: String) : SaveResult
    }

    private val _form = MutableStateFlow(FormState())
    val form: StateFlow<FormState> = _form.asStateFlow()

    private var editing: ShortcutRecord? = null
    val isEditing: Boolean get() = editing != null

    /** 选新图时的兜底：取消裁剪后回退到上一张已确认的裁剪图。 */
    private var cropBackup: Bitmap? = null

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

    fun onIconPicked(uri: Uri) {
        cropBackup = _form.value.croppedIcon
        _form.update { it.copy(pickedIconUri = uri, croppedIcon = null, removeIcon = false) }
    }

    fun onCropConfirmed(bitmap: Bitmap) {
        cropBackup = null
        _form.update { it.copy(croppedIcon = bitmap) }
    }

    fun onCropCancelled() {
        _form.update { it.copy(pickedIconUri = null, croppedIcon = cropBackup) }
        cropBackup = null
    }

    fun onRemoveIcon() =
        _form.update { it.copy(pickedIconUri = null, croppedIcon = null, removeIcon = true) }

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
            // 改过链接就丢弃旧深链，避免点开时跳到另一个页面。
            val keepDeepLink = base?.takeIf { it.url == url }?.deepLink
            val record = base?.copy(
                url = url,
                label = label,
                deepLink = keepDeepLink,
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
            when {
                f.removeIcon -> {
                    oldPath?.let { runCatching { File(it).delete() } }
                    iconPath = null
                }
                f.croppedIcon != null -> {
                    val newPath = iconProcessor.saveBitmap(f.croppedIcon!!, record.id)
                    if (newPath != null) {
                        if (oldPath != null && oldPath != newPath) {
                            runCatching { File(oldPath).delete() }
                        }
                        iconPath = newPath
                    }
                    // 处理失败时保留旧图标（或默认图标），不中断保存
                }
                f.pickedIconUri != null -> {
                    // 兜底：裁剪被跳过时仍按居中裁剪处理
                    val newPath = iconProcessor.process(f.pickedIconUri!!, record.id)
                    if (newPath != null) {
                        if (oldPath != null && oldPath != newPath) {
                            runCatching { File(oldPath).delete() }
                        }
                        iconPath = newPath
                    }
                }
            }

            val finalRecord = record.copy(iconPath = iconPath)
            repository.upsert(finalRecord)
            // 创建时不再自动请求桌面固定；已有桌面入口的编辑场景仍刷新图标与名称。
            if (base != null) {
                publisher.update(finalRecord)
            }
            onResult(SaveResult.Success)
        }
    }
}
