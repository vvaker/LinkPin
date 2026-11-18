package com.local.linkpin.shortcut

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/**
 * 默认字母图标：按名称首字 + 名称哈希渐变生成 512x512 位图。
 * 暖色渐变底（陶土/琥珀/焦糖等）+ 左上柔光 + 底部压暗 + 圆角内描边 + 居中白色大字，
 * 文字限制在画布中心约 42% 区域，为启动器遮罩预留安全区。
 */
object DefaultIcon {

    const val SIZE = 512

    /** 暖色渐变对：按名称哈希取色，浅色在上、深色在下。 */
    private val GRADIENT_PAIRS = listOf(
        intArrayOf(0xFFD98A5B.toInt(), 0xFFB4512F.toInt()),
        intArrayOf(0xFFE0A458.toInt(), 0xFFC0762E.toInt()),
        intArrayOf(0xFFC98A5E.toInt(), 0xFF9A5B33.toInt()),
        intArrayOf(0xFFD08A78.toInt(), 0xFFA65441.toInt()),
        intArrayOf(0xFFB9A05E.toInt(), 0xFF8C7633.toInt()),
        intArrayOf(0xFFC07C8C.toInt(), 0xFF92495C.toInt()),
        intArrayOf(0xFFD8B47A.toInt(), 0xFFAE8446.toInt()),
        intArrayOf(0xFFA9805B.toInt(), 0xFF7C5638.toInt()),
        intArrayOf(0xFFE39B6B.toInt(), 0xFFB96A3A.toInt()),
        intArrayOf(0xFFC68155.toInt(), 0xFF8A4A2C.toInt()),
    )

    fun generate(label: String): Bitmap {
        val normalized = label.trim()
        val firstChar = firstCharacter(normalized)
        val pair = GRADIENT_PAIRS[(normalized.hashCode() and Int.MAX_VALUE) % GRADIENT_PAIRS.size]
        val size = SIZE.toFloat()

        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 对角渐变底
        val background = Paint().apply {
            shader = LinearGradient(
                0f, 0f, size, size,
                pair[0], pair[1],
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, size, size, background)

        // 左上柔光
        val glow = Paint().apply {
            shader = RadialGradient(
                size * 0.30f, size * 0.25f, size * 0.80f,
                Color.argb(66, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, size, size, glow)

        // 底部压暗，增强层次（偏暖的深棕，避免发黑）
        val shade = Paint().apply {
            shader = LinearGradient(
                0f, size * 0.62f, 0f, size,
                Color.argb(0, 44, 26, 14),
                Color.argb(88, 44, 26, 14),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, size, size, shade)

        // 圆角内描边
        val inset = size * 0.055f
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.013f
            color = Color.argb(64, 255, 255, 255)
        }
        canvas.drawRoundRect(
            RectF(inset, inset, size - inset, size - inset),
            size * 0.13f,
            size * 0.13f,
            stroke,
        )

        // 居中大字 + 轻阴影
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = size * 0.40f
            setShadowLayer(size * 0.03f, 0f, size * 0.014f, Color.argb(120, 0, 0, 0))
        }
        val bounds = Rect()
        paint.getTextBounds(firstChar, 0, firstChar.length, bounds)
        val x = size / 2f
        val y = size / 2f - (bounds.top + bounds.bottom) / 2f
        canvas.drawText(firstChar, x, y, paint)
        return bitmap
    }

    private fun firstCharacter(label: String): String {
        if (label.isEmpty()) return "链"
        val codePoint = label.codePointAt(0)
        return String(Character.toChars(codePoint))
    }
}
