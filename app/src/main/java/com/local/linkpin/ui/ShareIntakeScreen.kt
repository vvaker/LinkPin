package com.local.linkpin.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.local.linkpin.R
import com.local.linkpin.routing.LinkValidator
import com.local.linkpin.share.ShareIntakeViewModel
import com.local.linkpin.ui.theme.LinkPinGradients
import kotlinx.coroutines.delay

/**
 * 分享保存卡片：从系统分享进来后弹出，预填名称与链接，一次点击即可保存到列表。
 * 深色遮罩 + 底部暖白卡片；短链展开与重复检查期间禁用保存按钮。
 */
@Composable
fun ShareIntakeScreen(
    viewModel: ShareIntakeViewModel,
    sharedText: String,
    sharedSubject: String?,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(sharedText) {
        viewModel.prepare(sharedText, sharedSubject)
    }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            val message = if (state.savedAsUpdate) "已更新列表中的链接" else "已保存到链接列表"
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
            // 给 Toast 一点展示时间再关卡片，立即 finish 会把 Toast 一起取消
            delay(600)
            onClose()
        }
    }

    val canSave = remember(state.url, state.label) {
        LinkValidator.validate(state.url) is LinkValidator.Result.Valid &&
            LinkValidator.normalizeLabel(state.label) != null
    }
    val status = when {
        state.loading -> "正在读取分享内容…"
        state.saving -> "正在保存…"
        state.resolving -> "正在展开 B 站短链…"
        state.checking -> "正在检查是否已收藏…"
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrimColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            // 吞掉卡片区域的点击，避免误触关闭
            onClick = {},
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 卡片底色延伸到屏幕底部，内容避开导航栏/输入法
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 22.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_brand_link),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "保存到链接直达",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "来自分享的链接 · 仅保存在本机",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = state.label,
                    onValueChange = viewModel::onLabelChange,
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.saving,
                    shape = RoundedCornerShape(14.dp),
                    supportingText = {
                        Text("${state.label.length}/${LinkValidator.LABEL_MAX_LENGTH}")
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "链接",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    SelectionContainer {
                        Text(
                            text = state.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                if (status != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                val note = state.note
                if (note != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val existing = state.existing
                if (existing != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_pin),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "列表里已有这个链接",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "保存会更新原有记录：${existing.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                val error = state.error
                if (error != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        enabled = !state.busy,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("取消")
                    }
                    GradientSaveButton(
                        text = if (existing != null) "更新保存" else "保存",
                        enabled = canSave && !state.busy && !state.saved,
                        onClick = viewModel::save,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pin),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "保存后可在列表中点击图钉按钮固定到桌面",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** 卡片外遮罩：暖黑半透明，让底部卡片成为唯一焦点。 */
private val ScrimColor = Color(0x99201811)

/** 渐变主按钮：与编辑页保存按钮同款暖沙金→陶土橙。 */
@Composable
private fun GradientSaveButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(
            if (enabled) {
                Modifier.shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                )
            } else {
                Modifier
            },
        ),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) {
                        LinkPinGradients.accent
                    } else {
                        SolidColor(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
