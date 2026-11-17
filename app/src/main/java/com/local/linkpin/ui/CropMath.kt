package com.local.linkpin.ui

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 裁剪窗口与手势参数的纯函数换算（不依赖 Android/Compose，可单元测试）：
 * 图片以"覆盖裁剪窗口"的基准缩放展示，用户可继续缩放（1x..[MAX_SCALE]）并平移；
 * 屏幕坐标 → 源图坐标：源点 = 中心 - 可视半宽 - 平移量 / 像素密度。
 */
object CropMath {

    const val MAX_SCALE = 5f

    /** 源图像素矩形，left/top 已钳制在合法范围。 */
    data class CropRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    /** 钳制后的平移量。 */
    data class Clamped(val offsetX: Float, val offsetY: Float)

    /** 覆盖基准缩放：图片恰好完全盖住方形窗口。 */
    fun coverScale(bitmapW: Int, bitmapH: Int, windowPx: Float): Float =
        max(windowPx / bitmapW, windowPx / bitmapH)

    /** 平移量钳制：保证图片始终完全覆盖窗口（不露边）。 */
    fun clampOffset(
        offsetX: Float,
        offsetY: Float,
        bitmapW: Int,
        bitmapH: Int,
        windowPx: Float,
        coverScale: Float,
        scale: Float,
    ): Clamped {
        val drawnW = bitmapW * coverScale * scale
        val drawnH = bitmapH * coverScale * scale
        val maxX = ((drawnW - windowPx) / 2f).coerceAtLeast(0f)
        val maxY = ((drawnH - windowPx) / 2f).coerceAtLeast(0f)
        return Clamped(offsetX.coerceIn(-maxX, maxX), offsetY.coerceIn(-maxY, maxY))
    }

    /** 当前窗口对应的源图矩形（像素，含钳制）。 */
    fun cropRect(
        bitmapW: Int,
        bitmapH: Int,
        windowPx: Float,
        coverScale: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
    ): CropRect {
        val density = coverScale * scale
        val visibleW = windowPx / density
        val visibleH = windowPx / density
        val maxLeft = (bitmapW - visibleW).coerceAtLeast(0f)
        val maxTop = (bitmapH - visibleH).coerceAtLeast(0f)
        val left = (bitmapW / 2f - visibleW / 2f - offsetX / density).coerceIn(0f, maxLeft)
        val top = (bitmapH / 2f - visibleH / 2f - offsetY / density).coerceIn(0f, maxTop)
        val right = (left + visibleW).roundToInt().coerceAtMost(bitmapW)
        val bottom = (top + visibleH).roundToInt().coerceAtMost(bitmapH)
        return CropRect(left.roundToInt(), top.roundToInt(), right, bottom)
    }
}
