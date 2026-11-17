package com.local.linkpin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 一条桌面快捷方式的本地记录。 */
@Serializable
data class ShortcutRecord(
    val id: String,
    val url: String,
    val label: String,
    val iconPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 一条域名路由规则。 */
@Serializable
data class RouteRule(
    val domainKey: String,
    val target: RouteTarget,
)

/** 路由目标：指定 App 或默认浏览器。 */
@Serializable
sealed interface RouteTarget {

    @Serializable
    @SerialName("app")
    data class App(val packageName: String) : RouteTarget

    @Serializable
    @SerialName("browser")
    data object Browser : RouteTarget
}

/** 统一 JSON 编解码器：忽略未知字段，多态判别键为 "type"。 */
object JsonCodec {
    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }
}

/** 运行期候选目标，不持久化。 */
sealed interface LinkTarget {

    data class App(
        val packageName: String,
        val label: String,
        val icon: Any? = null,
    ) : LinkTarget

    data class Browser(
        val label: String,
        val packageName: String?,
    ) : LinkTarget
}
