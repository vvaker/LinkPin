package com.local.linkpin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.local.linkpin.data.ShortcutRecord
import com.local.linkpin.ui.EditShortcutScreen
import com.local.linkpin.ui.EditShortcutViewModel
import com.local.linkpin.ui.ShortcutListScreen
import com.local.linkpin.ui.ShortcutListViewModel
import com.local.linkpin.ui.theme.LinkPinTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as LinkPinApp).container
        setContent {
            // 深色科技风为默认主题；如需跟随系统深浅色，改回 LinkPinTheme() 即可
            LinkPinTheme(darkTheme = true) {
                LinkPinRoot(container)
            }
        }
    }
}

/**
 * 应用根：列表页与创建/编辑页通过简单屏幕状态切换（不使用导航库）。
 */
@Composable
private fun LinkPinRoot(container: AppContainer) {
    val listViewModel: ShortcutListViewModel = viewModel {
        ShortcutListViewModel(
            repository = container.shortcutRepository,
            publisher = container.shortcutPublisher,
            iconProcessor = container.iconProcessor,
        )
    }

    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ShortcutRecord?>(null) }

    val currentEdit = editing
    if (creating || currentEdit != null) {
        key(currentEdit?.id ?: "new") {
            val editViewModel: EditShortcutViewModel = viewModel(key = currentEdit?.id ?: "new") {
                EditShortcutViewModel(
                    repository = container.shortcutRepository,
                    publisher = container.shortcutPublisher,
                    iconProcessor = container.iconProcessor,
                ).apply { init(currentEdit) }
            }
            EditShortcutScreen(
                viewModel = editViewModel,
                onDone = {
                    creating = false
                    editing = null
                },
            )
        }
    } else {
        ShortcutListScreen(
            viewModel = listViewModel,
            onAdd = { creating = true },
            onEdit = { editing = it },
        )
    }
}
