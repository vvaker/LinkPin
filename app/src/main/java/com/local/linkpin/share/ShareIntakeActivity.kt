package com.local.linkpin.share

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.local.linkpin.LinkPinApp
import com.local.linkpin.ui.ShareIntakeScreen
import com.local.linkpin.ui.theme.LinkPinTheme

/**
 * 接收系统分享文本（哔哩哔哩「分享 → 链接直达」）的半透明入口：
 * 只在底部弹出一张保存卡片，保存后直接写入链接列表并关闭；
 * 不写桌面快捷方式，固定入口仍由用户在列表页手动点击图钉触发。
 */
class ShareIntakeActivity : ComponentActivity() {

    private val sharedText = mutableStateOf("")
    private val sharedSubject = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 背景是深色遮罩，系统栏图标保持浅色（导航栏系统会加黑色对比底）
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        if (!consumeShare(intent)) {
            finish()
            return
        }
        val container = (application as LinkPinApp).container

        setContent {
            LinkPinTheme {
                val viewModel: ShareIntakeViewModel = viewModel {
                    ShareIntakeViewModel(
                        repository = container.shortcutRepository,
                        shortLinkResolver = ShortLinkResolver(),
                    )
                }
                ShareIntakeScreen(
                    viewModel = viewModel,
                    sharedText = sharedText.value,
                    sharedSubject = sharedSubject.value,
                    onClose = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 卡片还开着时又分享了一条：直接换内容，不再叠一个新的 Activity
        consumeShare(intent)
    }

    /** 读取分享文本；读不到时提示并返回 false。 */
    private fun consumeShare(intent: Intent?): Boolean {
        val text = extractSharedText(intent)
        if (text.isNullOrBlank()) {
            Toast.makeText(this, "没有收到可保存的分享内容", Toast.LENGTH_SHORT).show()
            return false
        }
        sharedText.value = text
        sharedSubject.value = intent?.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()
        return true
    }

    /** ACTION_SEND 的正文可能在 EXTRA_TEXT，也可能只在 ClipData 里。 */
    private fun extractSharedText(intent: Intent?): String? {
        if (intent == null) return null
        intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val clip = intent.clipData ?: return null
        for (index in 0 until clip.itemCount) {
            val text = runCatching { clip.getItemAt(index)?.coerceToText(this)?.toString() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
            if (text != null) return text
        }
        return null
    }
}
