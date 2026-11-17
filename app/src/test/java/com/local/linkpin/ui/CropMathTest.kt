package com.local.linkpin.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CropMathTest {

    @Test
    fun coverScale_square() {
        assertEquals(0.5f, CropMath.coverScale(2000, 2000, 1000f), 0.0001f)
    }

    @Test
    fun coverScale_tall() {
        assertEquals(1f, CropMath.coverScale(1000, 3000, 1000f), 0.0001f)
    }

    @Test
    fun coverScale_wide() {
        assertEquals(1f, CropMath.coverScale(3000, 1000, 1000f), 0.0001f)
    }

    @Test
    fun cropRect_default_is_whole_image() {
        val rect = CropMath.cropRect(2000, 2000, 1000f, 0.5f, 1f, 0f, 0f)
        assertEquals(0, rect.left)
        assertEquals(0, rect.top)
        assertEquals(2000, rect.right)
        assertEquals(2000, rect.bottom)
    }

    @Test
    fun cropRect_zoom_in_keeps_center() {
        val rect = CropMath.cropRect(2000, 2000, 1000f, 0.5f, 2f, 0f, 0f)
        assertEquals(500, rect.left)
        assertEquals(500, rect.top)
        assertEquals(1500, rect.right)
        assertEquals(1500, rect.bottom)
    }

    @Test
    fun cropRect_pan_right_shows_left_region() {
        val rect = CropMath.cropRect(2000, 2000, 1000f, 0.5f, 2f, 250f, 0f)
        assertEquals(250, rect.left)
        assertEquals(500, rect.top)
        assertEquals(1250, rect.right)
    }

    @Test
    fun cropRect_pan_clamped_to_bounds() {
        val rect = CropMath.cropRect(2000, 2000, 1000f, 0.5f, 2f, 5000f, -5000f)
        assertEquals(0, rect.left)
        assertEquals(1000, rect.top)
        assertEquals(1000, rect.right)
        assertEquals(2000, rect.bottom)
    }

    @Test
    fun clampOffset_zero_when_image_fits() {
        val c = CropMath.clampOffset(999f, -999f, 2000, 2000, 1000f, 0.5f, 1f)
        assertEquals(0f, c.offsetX, 0.0001f)
        assertEquals(0f, c.offsetY, 0.0001f)
    }

    @Test
    fun clampOffset_limits_at_zoom() {
        val c = CropMath.clampOffset(1000f, -1000f, 2000, 2000, 1000f, 0.5f, 2f)
        assertEquals(500f, c.offsetX, 0.0001f)
        assertEquals(-500f, c.offsetY, 0.0001f)
    }

    @Test
    fun clampOffset_tall_image_vertical_only() {
        val c = CropMath.clampOffset(500f, 500f, 1000, 3000, 1000f, 1f, 1f)
        assertEquals(0f, c.offsetX, 0.0001f)
        assertEquals(500f, c.offsetY, 0.0001f)
    }
}
