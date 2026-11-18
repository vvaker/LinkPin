package com.local.linkpin.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import com.local.linkpin.R
import com.local.linkpin.routing.LinkValidator
import com.local.linkpin.shortcut.DefaultIcon
import com.local.linkpin.shortcut.IconDecode
import com.local.linkpin.ui.theme.LinkPinGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 创建/编辑表单：链接、名称、图标（Photo Picker 选图后裁剪 / 默认字母图标）、保存。
 * 保存后回到列表，点击卡片即可直达；桌面入口由列表中的图钉按钮按需生成。
 * 暖色卡片式布局：分区卡片 + 渐变主按钮；图标可点击预览大图。
 */
@Composable
fun EditShortcutScreen(
    viewModel: EditShortcutViewModel,
    onDone: () -> Unit,
) {
    val form by viewModel.form.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val urlResult = remember(form.url) { LinkValidator.validate(form.url) }
    val labelOk = remember(form.label) { LinkValidator.normalizeLabel(form.label) != null }
    val shortLink = remember(form.url) { LinkValidator.isShortLink(form.url) }

    var croppingUri by remember { mutableStateOf<Uri?>(null) }
    var previewOpen by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.onIconPicked(uri)
            croppingUri = uri
        }
    }

    val preview = produceState<ImageBitmap>(
        initialValue = DefaultIcon.generate(form.label).asImageBitmap(),
        form.pickedIconUri,
        form.croppedIcon,
        form.removeIcon,
        form.existingIconPath,
        form.label,
    ) {
        value = withContext(Dispatchers.IO) {
            val loaded = when {
                form.croppedIcon != null -> form.croppedIcon
                form.pickedIconUri != null -> IconDecode.fromUri(context, form.pickedIconUri!!, 1024)
                !form.removeIcon && form.existingIconPath != null ->
                    IconDecode.fromFile(form.existingIconPath!!, 1024)
                else -> null
            }
            (loaded ?: DefaultIcon.generate(form.label)).asImageBitmap()
        }
    }

    // 预览对话框的原始图（比 92dp 缩略图更清晰）
    val previewSource by produceState<ImageBitmap?>(
        initialValue = null,
        previewOpen,
        form.pickedIconUri,
        form.croppedIcon,
        form.existingIconPath,
        form.label,
    ) {
        if (!previewOpen) {
            value = null
        } else {
            value = withContext(Dispatchers.IO) {
                val cropped = form.croppedIcon
                when {
                    cropped != null -> cropped.asImageBitmap()
                    form.pickedIconUri != null ->
                        IconDecode.fromUri(context, form.pickedIconUri!!, 2048)?.asImageBitmap()
                    form.existingIconPath != null ->
                        IconDecode.fromFile(form.existingIconPath!!, 2048)?.asImageBitmap()
                    else -> DefaultIcon.generate(form.label).asImageBitmap()
                }
            }
        }
    }

    val cropping = croppingUri
    if (cropping != null) {
        ImageCropScreen(
            sourceUri = cropping,
            onConfirm = { bitmap ->
                viewModel.onCropConfirmed(bitmap)
                croppingUri = null
            },
            onCancel = {
                viewModel.onCropCancelled()
                croppingUri = null
            },
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LinkPinGradients.background),
        ) {
            // 顶部暖色氛围光
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-120).dp, y = (-190).dp)
                    .background(LinkPinGradients.glow),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                // 自定义头部：返回 + 大标题
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDone,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (viewModel.isEditing) "编辑快捷方式" else "新建快捷方式",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = if (viewModel.isEditing) "修改后点击卡片即可直达内容" else "保存后点击卡片即可直达内容",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 图标卡片
                SectionCard {
                    SectionLabel("图标")
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                )
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(onClick = { previewOpen = true }),
                        ) {
                            Image(
                                bitmap = preview.value,
                                contentDescription = "图标预览（点击放大）",
                                modifier = Modifier.fillMaxSize(),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = 2.dp,
                                        brush = LinkPinGradients.accent,
                                        shape = RoundedCornerShape(24.dp),
                                    ),
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Surface(
                                onClick = {
                                    pickMedia.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                                        ),
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_photo),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(17.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "从相册选择",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row {
                                TextButton(onClick = { viewModel.onRemoveIcon() }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_restore),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("恢复默认", style = MaterialTheme.typography.labelMedium)
                                }
                                if (form.pickedIconUri != null && form.croppedIcon != null) {
                                    TextButton(onClick = { croppingUri = form.pickedIconUri }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_crop),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            "重新裁剪",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "选图后可拖动/缩放裁剪；点击图标可预览大图",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 链接卡片
                SectionCard {
                    SectionLabel("链接")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = form.url,
                        onValueChange = viewModel::onUrlChange,
                        label = { Text("链接地址") },
                        placeholder = { Text("https://… 或自定义 scheme") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        shape = RoundedCornerShape(14.dp),
                        isError = urlResult is LinkValidator.Result.Invalid,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_brand_link),
                                contentDescription = null,
                                tint = if (urlResult is LinkValidator.Result.Invalid) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        colors = linkPinFieldColors(),
                        supportingText = {
                            when {
                                urlResult is LinkValidator.Result.Invalid ->
                                    Text((urlResult as LinkValidator.Result.Invalid).reason)
                                shortLink ->
                                    Text("提示：短链可能先经过浏览器跳转，建议使用最终内容页地址")
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 名称卡片
                SectionCard {
                    SectionLabel("名称")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = form.label,
                        onValueChange = viewModel::onLabelChange,
                        label = { Text("显示名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        isError = form.label.isNotBlank() && !labelOk,
                        colors = linkPinFieldColors(),
                        supportingText = { Text("${form.label.length}/${LinkValidator.LABEL_MAX_LENGTH}") },
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))
                GradientButton(
                    text = if (viewModel.isEditing) "保存" else "保存链接",
                    enabled = urlResult is LinkValidator.Result.Valid && labelOk,
                    onClick = {
                        scope.launch {
                            viewModel.save { result ->
                                when (result) {
                                    EditShortcutViewModel.SaveResult.Success -> {
                                        Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                                        onDone()
                                    }
                                    is EditShortcutViewModel.SaveResult.Failure -> {
                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "链接支持网页与自定义 scheme；保存后点击卡片直达，需要桌面入口时在列表点图钉固定",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }

    if (previewOpen) {
        ImagePreviewDialog(
            bitmap = previewSource,
            onDismiss = { previewOpen = false },
        )
    }
}

/** 分区卡片：暖白底 + 细描边 + 轻阴影。 */
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun linkPinFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    unfocusedContainerColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
)

/** 渐变主按钮：暖沙金→陶土橙渐变，禁用态回落为浅陶土底。 */
@Composable
private fun GradientButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    )
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) {
                        Modifier.background(LinkPinGradients.accent)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (enabled) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
