package com.local.linkpin.routing

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.local.linkpin.AppContainer
import com.local.linkpin.LinkPinApp
import com.local.linkpin.data.RouteRule
import com.local.linkpin.data.RouteTarget
import com.local.linkpin.data.ShortcutRecord
import com.local.linkpin.shortcut.ShortcutContract
import com.local.linkpin.ui.RouteChoiceScreen
import com.local.linkpin.ui.theme.LinkPinTheme
import kotlinx.coroutines.launch

/**
 * 透明路由 Activity：
 * - 命中规则直接启动目标并结束；目标失效时清除规则并强制刷新候选；
 * - 首次选择时挂载 Compose 选择面板（勾选"记住"才保存规则；启动失败时从面板剔除失效候选）；
 * - 记录不存在时提示桌面图标已失效并给出移除指引（系统不允许应用自动删除桌面快捷方式）。
 */
class RouteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as LinkPinApp).container
        val shortcutId = intent.getStringExtra(ShortcutContract.EXTRA_SHORTCUT_ID)
        if (shortcutId == null) {
            finish()
            return
        }

        lifecycleScope.launch {
            val record = container.shortcutRepository.get(shortcutId)
            if (record == null) {
                showDeletedNotice()
                return@launch
            }
            handleRouting(container, record)
        }
    }

    /**
     * 记录已删除时点击桌面旧图标：系统不允许应用自动移除桌面快捷方式，
     * 这里给出明确指引（长按图标 → 移除），而不是一闪而过的 Toast。
     */
    private fun showDeletedNotice() {
        setContent {
            LinkPinTheme {
                AlertDialog(
                    onDismissRequest = { finish() },
                    title = { Text("快捷方式已失效") },
                    text = {
                        Text(
                            "该链接已在应用内删除。\n\n" +
                                "桌面上的这个图标无法由应用自动移除，请长按图标，选择「移除」即可删除。",
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { finish() }) {
                            Text("知道了")
                        }
                    },
                )
            }
        }
    }

    private suspend fun handleRouting(container: AppContainer, record: ShortcutRecord) {
        val url = record.url
        val domainKey = DomainKey.of(url)
        val isHttp = url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)

        var forceRefresh = false
        if (domainKey != null) {
            val rule = container.routeRuleRepository.get(domainKey)
            if (rule != null) {
                val launched = container.linkResolver.launch(rule, url)
                if (launched) {
                    finish()
                    return
                }
                // 目标失效：清除规则，强制重新解析候选
                container.routeRuleRepository.clear(domainKey)
                forceRefresh = true
            }
        }

        val resolution = container.linkResolver.resolve(url, forceRefresh = forceRefresh)
        val apps = resolution.apps

        when {
            // 自定义 scheme 唯一处理者：直接打开
            apps.size == 1 && !isHttp -> {
                val ok = container.linkResolver.launch(RouteTarget.App(apps.single().packageName), url)
                if (!ok) {
                    Toast.makeText(this, "无法打开该链接", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            // HTTP/HTTPS 无 App 候选：直接浏览器
            apps.isEmpty() && isHttp -> {
                val ok = container.linkResolver.launch(RouteTarget.Browser, url)
                if (!ok) {
                    Toast.makeText(this, "没有可打开该链接的应用", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            apps.isEmpty() -> {
                Toast.makeText(this, "没有可处理该链接的应用", Toast.LENGTH_SHORT).show()
                finish()
            }
            else -> showChoicePanel(container, record, domainKey, resolution)
        }
    }

    private fun showChoicePanel(
        container: AppContainer,
        record: ShortcutRecord,
        domainKey: String?,
        resolution: LinkResolver.Resolution,
    ) {
        setContent {
            // 与主界面保持一致：暖色卡片风
            LinkPinTheme {
                var apps by remember { mutableStateOf(resolution.apps) }
                RouteChoiceScreen(
                    apps = apps,
                    browser = resolution.browser,
                    onSelect = { target, rememberChoice ->
                        lifecycleScope.launch {
                            val launched = container.linkResolver.launch(target, record.url)
                            if (launched) {
                                // 勾选"记住"才保存规则，否则下次继续询问
                                if (rememberChoice && domainKey != null) {
                                    container.routeRuleRepository.put(RouteRule(domainKey, target))
                                }
                                finish()
                            } else {
                                Toast.makeText(
                                    this@RouteActivity,
                                    if (target is RouteTarget.App) "该应用已无法打开此链接" else "浏览器打开失败",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                // 启动失败：从面板剔除该候选，保留面板让用户换一个
                                if (target is RouteTarget.App) {
                                    apps = apps.filterNot { it.packageName == target.packageName }
                                }
                            }
                        }
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }
}
