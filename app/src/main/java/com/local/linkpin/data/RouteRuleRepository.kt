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
 * 域名路由规则仓库：`route_rules` 键存 Map<domainKey, RouteTarget> 的 JSON。
 */
class RouteRuleRepository(private val dataStore: DataStore<Preferences>) {

    private val key = stringPreferencesKey("route_rules")

    private fun decode(raw: String?): Map<String, RouteTarget> =
        if (raw.isNullOrBlank()) {
            emptyMap()
        } else {
            runCatching {
                JsonCodec.json.decodeFromString<Map<String, RouteTarget>>(raw)
            }.getOrDefault(emptyMap())
        }

    fun observe(): Flow<Map<String, RouteTarget>> =
        dataStore.data.map { prefs -> decode(prefs[key]) }

    suspend fun get(domainKey: String): RouteTarget? =
        observe().first()[domainKey]

    suspend fun put(rule: RouteRule) {
        dataStore.edit { prefs ->
            val current = decode(prefs[key]).toMutableMap()
            current[rule.domainKey] = rule.target
            prefs[key] = JsonCodec.json.encodeToString(current)
        }
    }

    suspend fun clear(domainKey: String) {
        dataStore.edit { prefs ->
            val current = decode(prefs[key]).toMutableMap()
            current.remove(domainKey)
            prefs[key] = JsonCodec.json.encodeToString(current)
        }
    }
}
