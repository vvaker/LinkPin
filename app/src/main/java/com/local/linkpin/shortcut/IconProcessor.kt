package com.local.linkpin.shortcut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import com.local.linkpin.data.ShortcutRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 通用图片解码：先读边界计算采样率，限制最大边长，避免大图 OOM。
 * 裁剪页、预览页与图标处理共用的解码入口。
 */
object IconDecode {

    fun fromUri(context: Context, uri: Uri, maxDim: Int): Bitmap? =
        decodeSampled(
            boundsOf = { options ->
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                }
                options.outWidth > 0 && options.outHeight > 0
            },
            decode = { options ->
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                }
            },
            maxDim = maxDim,
        )

    fun fromFile(path: String, maxDim: Int): Bitmap? =
        decodeSampled(
            boundsOf = { options ->
                BitmapFactory.decodeFile(path, options)
                options.outWidth > 0 && options.outHeight > 0
            },
            decode = { options -> BitmapFactory.decodeFile(path, options) },
            maxDim = maxDim,
        )

    private fun decodeSampled(
        boundsOf: (BitmapFactory.Options) -> Boolean,
        decode: (BitmapFactory.Options) -> Bitmap?,
        maxDim: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (!boundsOf(bounds)) return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDim ||
            bounds.outHeight / sampleSize > maxDim
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return decode(options)
    }
}

/**
 * 图标处理：采样解码（≤1024px）→ 居中裁剪正方形 → 512x512 PNG 存应用私有目录。
 */
class IconProcessor(private val context: Context) {

    private val maxDecodeDimension = 1024

    suspend fun process(source: Uri, id: String): String? = withContext(Dispatchers.IO) {
        val bitmap = IconDecode.fromUri(context, source, maxDecodeDimension) ?: return@withContext null
        runCatching { writeIcon(bitmap, id) }.getOrNull()
    }

    /** 把裁剪确认后的位图居中裁剪为正方形并保存为 512x512 PNG。 */
    suspend fun saveBitmap(source: Bitmap, id: String): String? = withContext(Dispatchers.IO) {
        runCatching { writeIcon(source, id) }.getOrNull()
    }

    private fun writeIcon(bitmap: Bitmap, id: String): String? {
        val cropped = centerCropSquare(bitmap)
        val scaled = if (cropped.width != DefaultIcon.SIZE || cropped.height != DefaultIcon.SIZE) {
            Bitmap.createScaledBitmap(cropped, DefaultIcon.SIZE, DefaultIcon.SIZE, true)
        } else {
            cropped
        }
        val file = iconFile(id)
        file.parentFile?.mkdirs()
        val ok = file.outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (!ok) return null
        return file.absolutePath
    }

    fun iconFile(id: String): File = File(File(context.filesDir, "icons"), "$id.png")

    /** 读取记录图标；不存在或损坏时回落默认字母图标。 */
    fun loadIcon(record: ShortcutRecord): Bitmap {
        record.iconPath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()?.let { return it }
        }
        return DefaultIcon.generate(record.label)
    }

    /** 删除该记录对应的私有图标文件（不触碰任何外部路径）。 */
    fun delete(id: String) {
        iconFile(id).delete()
    }

    /** 旧版 INSTALL_SHORTCUT 广播图标：内容缩至画布 66% 居中，预留遮罩安全区。 */
    fun legacySafeIcon(bitmap: Bitmap): Bitmap {
        val canvas = Bitmap.createBitmap(DefaultIcon.SIZE, DefaultIcon.SIZE, Bitmap.Config.ARGB_8888)
        val c = Canvas(canvas)
        val contentSize = (DefaultIcon.SIZE * 0.66f).toInt()
        val inset = (DefaultIcon.SIZE - contentSize) / 2
        val scaled = Bitmap.createScaledBitmap(bitmap, contentSize, contentSize, true)
        c.drawBitmap(scaled, inset.toFloat(), inset.toFloat(), null)
        return canvas
    }

    private fun centerCropSquare(source: Bitmap): Bitmap {
        val side = minOf(source.width, source.height)
        val left = (source.width - side) / 2
        val top = (source.height - side) / 2
        return Bitmap.createBitmap(source, left, top, side, side)
    }
}
