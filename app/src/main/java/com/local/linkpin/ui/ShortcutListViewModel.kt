package com.local.linkpin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.local.linkpin.data.ShortcutRecord
import com.local.linkpin.data.ShortcutRepository
import com.local.linkpin.shortcut.IconProcessor
import com.local.linkpin.shortcut.ShortcutPublisher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 列表 ViewModel：观察记录、删除（含图标文件清理）、按需固定到桌面。
 */
class ShortcutListViewModel(
    private val repository: ShortcutRepository,
    private val publisher: ShortcutPublisher,
    private val iconProcessor: IconProcessor,
) : ViewModel() {

    val records: StateFlow<List<ShortcutRecord>> = repository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun delete(record: ShortcutRecord) {
        viewModelScope.launch {
            repository.delete(record.id)
            publisher.deleteRecord(record)
        }
    }

    /** 用户点击列表里的图钉时，才请求在桌面创建快捷方式。 */
    fun pin(record: ShortcutRecord, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onResult(publisher.create(record))
        }
    }
}
