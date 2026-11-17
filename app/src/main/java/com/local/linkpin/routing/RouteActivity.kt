package com.local.linkpin.routing

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
 * - 命中规则直接启动目标并结束；
 * - 首次选择时挂载 Compose 选择面板；
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

        if (domainKey != null) {
            val rule = container.routeRuleRepository.get(domainKey)
            if (rule != null) {
                val launched = container.linkResolver.launch(rule, url)
                if (launched) {
                    finish()
                    return
                }
                // 目标失效：清除规则，重新进入选择流程
                container.routeRuleRepository.clear(domainKey)
            }
        }

        val resolution = container.linkResolver.resolve(url)
        val apps = resolution.apps

        when {
            // 自定义 scheme 唯一处理者：直接打开
            apps.size == 1 && !isHttp -> {
                container.linkResolver.launch(RouteTarget.App(apps.single().packageName), url)
                finish()
            }
            // HTTP/HTTPS 无 App 候选：直接浏览器
            apps.isEmpty() && isHttp -> {
                container.linkResolver.launch(RouteTarget.Browser, url)
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
                RouteChoiceScreen(
                    apps = resolution.apps,
                    browser = resolution.browser,
                    onSelect = { target ->
                        lifecycleScope.launch {
                            if (domainKey != null) {
                                container.routeRuleRepository.put(RouteRule(domainKey, target))
                            }
                            container.linkResolver.launch(target, record.url)
                        }
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }
}
