package com.local.linkpin.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.local.linkpin.shortcut.IconDecode
import com.local.linkpin.ui.theme.LinkPinGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** 裁剪解码的最大边长：兼顾质量与内存。 */
private const val CROP_MAX_DIM = 2048

/** 裁剪页深色底：暖调近黑，比纯黑更柔和。 */
private val CropBackdrop = Color(0xFF1D1612)
private val CropTextPrimary = Color(0xFFF7F0E8)
private val CropTextSecondary = Color(0xFFCDBFB2)

private sealed interface CropSource {
    data object Loading : CropSource
    data class Ready(val bitmap: Bitmap) : CropSource
    data object Failed : CropSource
}

/**
 * 正方形图标裁剪页：拖动平移、双指缩放、双击复位，另提供缩放滑杆（鼠标/单指也能缩放）。
 * 绘制直接按 [CropMath] 的 cover*scale 比例把源图铺到裁剪窗口，保证画面与裁剪结果严格一致。
 */
@Composable
fun ImageCropScreen(
    sourceUri: Uri,
    onConfirm: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current

    val sourceState by produceState<CropSource>(CropSource.Loading, sourceUri) {
        value = withContext(Dispatchers.IO) {
            val bitmap = IconDecode.fromUri(context, sourceUri, CROP_MAX_DIM)
            if (bitmap != null) CropSource.Ready(bitmap) else CropSource.Failed
        }
    }
    // 注意：位图不要在此处 recycle。Compose 的绘制由 RenderNode 显示列表驱动，
    // 在 onDispose 里回收位图会导致「Canvas: trying to use a recycled bitmap」崩溃；
    // minSdk 26 起位图原生内存由 GC 管理，离开组合后无引用会自动释放。
    BackHandler(onBack = onCancel)

    val source = (sourceState as? CropSource.Ready)?.bitmap

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CropBackdrop),
    ) {
        when {
            sourceState is CropSource.Loading -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("正在载入图片…", color = CropTextSecondary)
            }

            sourceState is CropSource.Failed -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("无法读取该图片", color = CropTextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onCancel) {
                    Text("返回", color = CropTextPrimary)
                }
            }

            source != null -> CropContent(
                source = source,
                onConfirm = onConfirm,
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun CropContent(
    source: Bitmap,
    onConfirm: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var scale by remember(source) { mutableFloatStateOf(1f) }
    var offset by remember(source) { mutableStateOf(Offset.Zero) }
    val image: ImageBitmap = remember(source) { source.asImageBitmap() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 裁剪窗口：留出顶部标题栏与底部操作区
        val windowDp = minOf(maxWidth * 0.9f, maxHeight * 0.58f)
        val windowPx = with(density) { windowDp.toPx() }
        val cover = remember(source, windowPx) {
            CropMath.coverScale(source.width, source.height, windowPx)
        }

        /** 统一入口：改比例时同步把平移量钳制到合法范围，避免露出空白边。 */
        fun applyScale(target: Float) {
            val clampedScale = target.coerceIn(1f, CropMath.MAX_SCALE)
            val clamped = CropMath.clampOffset(
                offsetX = offset.x,
                offsetY = offset.y,
                bitmapW = source.width,
                bitmapH = source.height,
                windowPx = windowPx,
                coverScale = cover,
                scale = clampedScale,
            )
            scale = clampedScale
            offset = Offset(clamped.offsetX, clamped.offsetY)
        }

        // 窗口外遮罩
        Canvas(modifier = Modifier.fillMaxSize()) {
            val windowRect = Rect(
                offset = Offset(size.width / 2f - windowPx / 2f, size.height / 2f - windowPx / 2f),
                size = Size(windowPx, windowPx),
            )
            val path = Path().apply {
                addRect(Rect(Offset.Zero, size))
                addRect(windowRect)
                fillType = PathFillType.EvenOdd
            }
            drawPath(path, Color.Black.copy(alpha = 0.58f))
        }

        // 标题栏（避开状态栏）
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消裁剪",
                    tint = CropTextPrimary,
                )
            }
            Text(
                text = "调整图标",
                style = MaterialTheme.typography.titleMedium,
                color = CropTextPrimary,
            )
        }

        // 图片 + 裁剪窗口：手势挂在窗口外层，触控区域覆盖整个窗口
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(windowDp)
                .clipToBounds()
                .pointerInput(source, windowPx, cover) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, CropMath.MAX_SCALE)
                        val clamped = CropMath.clampOffset(
                            offsetX = offset.x + pan.x,
                            offsetY = offset.y + pan.y,
                            bitmapW = source.width,
                            bitmapH = source.height,
                            windowPx = windowPx,
                            coverScale = cover,
                            scale = newScale,
                        )
                        scale = newScale
                        offset = Offset(clamped.offsetX, clamped.offsetY)
                    }
                }
                .pointerInput(source) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                    )
                },
        ) {
            // 按 cover*scale 铺图：与 CropMath.cropRect 使用同一比例，所见即所得
            Canvas(modifier = Modifier.fillMaxSize()) {
                val den = cover * scale
                val drawW = source.width * den
                val drawH = source.height * den
                val left = size.width / 2f + offset.x - drawW / 2f
                val top = size.height / 2f + offset.y - drawH / 2f
                drawImage(
                    image = image,
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt()),
                    filterQuality = FilterQuality.High,
                )
            }
            // 三分构图辅助线
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = size.width / 3f
                val stepY = size.height / 3f
                val gridColor = Color.White.copy(alpha = 0.30f)
                for (i in 1..2) {
                    drawLine(gridColor, Offset(stepX * i, 0f), Offset(stepX * i, size.height), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, stepY * i), Offset(size.width, stepY * i), strokeWidth = 1f)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.White.copy(alpha = 0.88f)),
            )
        }

        // 底部操作区（避开手势导航条/任务栏）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            // 缩放滑杆：单指拖动即可缩放，不依赖双指手势
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "缩放",
                    style = MaterialTheme.typography.labelLarge,
                    color = CropTextSecondary,
                )
                Slider(
                    value = scale,
                    onValueChange = { applyScale(it) },
                    valueRange = 1f..CropMath.MAX_SCALE,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                    ),
                )
                Text(
                    text = "${"%.1f".format(scale)}×",
                    style = MaterialTheme.typography.labelLarge,
                    color = CropTextPrimary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(46.dp),
                )
            }
            Text(
                text = "拖动调整位置 · 双指缩放 · 双击复位",
                style = MaterialTheme.typography.bodySmall,
                color = CropTextSecondary.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                ) {
                    Text("取消", color = CropTextPrimary)
                }
                Surface(
                    onClick = {
                        val rect = CropMath.cropRect(
                            bitmapW = source.width,
                            bitmapH = source.height,
                            windowPx = windowPx,
                            coverScale = cover,
                            scale = scale,
                            offsetX = offset.x,
                            offsetY = offset.y,
                        )
                        val cropped = runCatching {
                            Bitmap.createBitmap(source, rect.left, rect.top, rect.width, rect.height)
                        }.getOrNull()
                        if (cropped != null) {
                            onConfirm(cropped)
                        } else {
                            Toast.makeText(context, "裁剪失败，请重试", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LinkPinGradients.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "确认裁剪",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
