package com.local.linkpin

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.local.linkpin.data.RouteRuleRepository
import com.local.linkpin.data.ShortcutRepository
import com.local.linkpin.routing.LinkResolver
import com.local.linkpin.shortcut.IconProcessor
import com.local.linkpin.shortcut.ShortcutPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Context.linkPinDataStore by preferencesDataStore(name = "linkpin_prefs")

/**
 * 手动依赖容器：单模块应用不使用 Hilt，由 Application 持有并在各界面按需获取。
 */
class AppContainer(private val appContext: Context) {
    val shortcutRepository: ShortcutRepository by lazy {
        ShortcutRepository(appContext.linkPinDataStore)
    }
    val routeRuleRepository: RouteRuleRepository by lazy {
        RouteRuleRepository(appContext.linkPinDataStore)
    }
    val iconProcessor: IconProcessor by lazy { IconProcessor(appContext) }
    val shortcutPublisher: ShortcutPublisher by lazy {
        ShortcutPublisher(appContext, iconProcessor)
    }
    val linkResolver: LinkResolver by lazy { LinkResolver(appContext) }
}

class LinkPinApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 后台预热：PackageManager 首次查询与 DataStore 首次读取最耗时，
        // 提前完成能显著减少桌面图标点击后的等待（卡顿主要来源）。
        appScope.launch {
            container.linkResolver.resolve("https://warmup.linkpin.invalid/")
            container.shortcutRepository.get("__warmup__")
            container.routeRuleRepository.get("__warmup__")
        }
    }
}
