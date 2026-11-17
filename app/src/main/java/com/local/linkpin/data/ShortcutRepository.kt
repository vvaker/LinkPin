package com.local.linkpin.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * 快捷方式记录仓库：单 DataStore 文件，`shortcuts` 键存 List<ShortcutRecord> 的 JSON。
 * 损坏 JSON 一律降级为空列表，不崩溃。
 */
class ShortcutRepository(private val dataStore: DataStore<Preferences>) {

    private val key = stringPreferencesKey("shortcuts")

    private fun decode(raw: String?): List<ShortcutRecord> =
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                JsonCodec.json.decodeFromString<List<ShortcutRecord>>(raw)
            }.getOrDefault(emptyList())
        }

    fun observeAll(): Flow<List<ShortcutRecord>> =
        dataStore.data.map { prefs -> decode(prefs[key]) }

    suspend fun get(id: String): ShortcutRecord? =
        observeAll().first().firstOrNull { it.id == id }

    suspend fun upsert(record: ShortcutRecord) {
        dataStore.edit { prefs ->
            val current = decode(prefs[key]).toMutableList()
            val index = current.indexOfFirst { it.id == record.id }
            if (index >= 0) {
                current[index] = record
            } else {
                current.add(record)
            }
            prefs[key] = JsonCodec.json.encodeToString(current)
        }
    }

    suspend fun delete(id: String) {
        dataStore.edit { prefs ->
            val current = decode(prefs[key])
            prefs[key] = JsonCodec.json.encodeToString(current.filterNot { it.id == id })
        }
    }
}
