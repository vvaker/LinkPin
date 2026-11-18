package com.local.linkpin.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.local.linkpin.R
import com.local.linkpin.data.ShortcutRecord
import com.local.linkpin.routing.DomainKey
import com.local.linkpin.shortcut.DefaultIcon
import com.local.linkpin.shortcut.IconDecode
import com.local.linkpin.ui.theme.LinkPinGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 快捷方式列表主页：暖色奶油底 + 陶土橙强调，卡片化布局。
 * 支持点击卡片编辑、点击图标预览大图、重新固定、删除（确认）。
 */
@Composable
fun ShortcutListScreen(
    viewModel: ShortcutListViewModel,
    onAdd: () -> Unit,
    onEdit: (ShortcutRecord) -> Unit,
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<ShortcutRecord?>(null) }
    var previewRecord by remember { mutableStateOf<ShortcutRecord?>(null) }

    // 预览大图：后台解码原始图标文件，避免主线程卡顿
    val previewBitmap by produceState<ImageBitmap?>(initialValue = null, previewRecord) {
        val record = previewRecord
        value = if (record == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                val bitmap = record.iconPath?.let { IconDecode.fromFile(it, 2048) }
                (bitmap ?: DefaultIcon.generate(record.label)).asImageBitmap()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LinkPinGradients.background),
    ) {
        // 顶部暖色氛围光
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 150.dp, y = (-170).dp)
                .background(LinkPinGradients.glow),
        )
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = { GradientFab(onClick = onAdd) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                BrandHeader(count = records.size)
                if (records.isEmpty()) {
                    EmptyState(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(records, key = { it.id }) { record ->
                            ShortcutCard(
                                record = record,
                                onEdit = { onEdit(record) },
                                onRepin = {
                                    viewModel.repin(record) { ok ->
                                        Toast.makeText(
                                            context,
                                            if (ok) "已重新请求固定" else "桌面未确认，请检查桌面或重试",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                },
                                onDelete = { pendingDelete = record },
                                onPreview = { previewRecord = record },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("删除快捷方式") },
            text = {
                Text("将删除本地记录与图标文件。桌面上的旧图标需要你长按后手动移除。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(record)
                        pendingDelete = null
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            },
        )
    }

    if (previewRecord != null) {
        ImagePreviewDialog(
            bitmap = previewBitmap,
            onDismiss = { previewRecord = null },
        )
    }
}

@Composable
private fun GradientFab(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(60.dp)
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            ),
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LinkPinGradients.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新建快捷方式",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun BrandHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(LinkPinGradients.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_brand_link),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "链接直达",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (count > 0) "已固定 $count 个快捷方式" else "把常用链接固定到桌面，一键直达",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (count > 0) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_brand_link),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp),
            )
        }
        Spacer(modifier = Modifier.height(26.dp))
        Text(
            text = "还没有快捷方式",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击右下角的按钮，把视频、书籍或网页链接固定到桌面，之后一键直达",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                text = "选图后支持裁剪 · 点击图标可预览",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun ShortcutCard(
    record: ShortcutRecord,
    onEdit: () -> Unit,
    onRepin: () -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit,
) {
    // 后台解码图标文件，避免主线程卡顿
    val icon by produceState<ImageBitmap?>(initialValue = null, record.iconPath, record.label) {
        value = withContext(Dispatchers.IO) {
            val bitmap = record.iconPath?.let { IconDecode.fromFile(it, 512) }
            (bitmap ?: DefaultIcon.generate(record.label)).asImageBitmap()
        }
    }
    val domain = remember(record.url) { DomainKey.of(record.url) }
    Surface(
        onClick = onEdit,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x33B27A55),
                spotColor = Color(0x26B27A55),
            ),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onPreview),
            ) {
                icon?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "图标预览（点击放大）",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                        ),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (domain != null) {
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        ) {
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = record.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            CircleAction(
                icon = Icons.Default.Refresh,
                contentDescription = "重新固定",
                tint = MaterialTheme.colorScheme.secondary,
                onClick = onRepin,
            )
            Spacer(modifier = Modifier.width(6.dp))
            CircleAction(
                icon = Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun CircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}
