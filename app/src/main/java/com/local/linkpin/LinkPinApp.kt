package com.local.linkpin

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.local.linkpin.data.RouteRuleRepository
import com.local.linkpin.data.ShortcutRepository
import com.local.linkpin.routing.LinkResolver
import com.local.linkpin.shortcut.IconProcessor
import com.local.linkpin.shortcut.ShortcutPublisher

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

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
