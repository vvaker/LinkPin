package com.local.linkpin.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.local.linkpin.R
import com.local.linkpin.routing.LinkValidator
import com.local.linkpin.shortcut.DefaultIcon
import com.local.linkpin.ui.theme.LinkPinGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 创建/编辑表单：链接、名称、图标（Photo Picker / 默认字母图标）、短链提示、保存并固定。
 * 深色科技风：分区标题、渐变描边图标预览、渐变主按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.onIconPicked(uri)
    }

    val preview = produceState<ImageBitmap>(
        initialValue = DefaultIcon.generate(form.label).asImageBitmap(),
        form.pickedIconUri,
        form.removeIcon,
        form.existingIconPath,
        form.label,
    ) {
        value = withContext(Dispatchers.IO) {
            val uri = form.pickedIconUri
            val loaded = when {
                uri != null -> runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                }.getOrNull()
                !form.removeIcon && form.existingIconPath != null ->
                    runCatching { BitmapFactory.decodeFile(form.existingIconPath)?.asImageBitmap() }
                        .getOrNull()
                else -> null
            }
            loaded ?: DefaultIcon.generate(form.label).asImageBitmap()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "编辑快捷方式" else "新建快捷方式") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            SectionLabel("图标")
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(88.dp)) {
                    Image(
                        bitmap = preview.value,
                        contentDescription = "图标预览",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 1.5.dp,
                                brush = LinkPinGradients.accent,
                                shape = RoundedCornerShape(22.dp),
                            ),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    OutlinedButton(
                        onClick = {
                            pickMedia.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_photo),
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("从相册选择")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = { viewModel.onRemoveIcon() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_restore),
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("恢复默认图标")
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            SectionLabel("链接")
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = form.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text("链接") },
                placeholder = { Text("https://… 或自定义 scheme") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
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

            Spacer(modifier = Modifier.height(16.dp))
            SectionLabel("名称")
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = form.label,
                onValueChange = viewModel::onLabelChange,
                label = { Text("名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = form.label.isNotBlank() && !labelOk,
                colors = linkPinFieldColors(),
                supportingText = { Text("${form.label.length}/${LinkValidator.LABEL_MAX_LENGTH}") },
            )

            Spacer(modifier = Modifier.height(28.dp))
            GradientButton(
                text = if (viewModel.isEditing) "保存" else "保存并固定到桌面",
                enabled = urlResult is LinkValidator.Result.Valid && labelOk,
                onClick = {
                    scope.launch {
                        viewModel.save { result ->
                            when (result) {
                                is EditShortcutViewModel.SaveResult.Success -> {
                                    Toast.makeText(
                                        context,
                                        if (result.pinned) "已保存" else "已保存；桌面未确认固定请求，可在列表重新固定",
                                        Toast.LENGTH_SHORT,
                                    ).show()
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
                    .height(54.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun linkPinFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
)

/** 渐变主按钮：青紫渐变，禁用态回落为灰底。 */
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
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) {
                        Modifier.background(LinkPinGradients.accent)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
