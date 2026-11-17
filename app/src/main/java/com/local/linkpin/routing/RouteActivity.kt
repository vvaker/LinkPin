package com.local.linkpin.routing

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
 * - 记录不存在只提示"快捷方式已删除"。
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
                Toast.makeText(this@RouteActivity, "快捷方式已删除", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            handleRouting(container, record)
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
            // 与主界面保持一致：深色科技风
            LinkPinTheme(darkTheme = true) {
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
