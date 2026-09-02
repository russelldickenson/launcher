package org.fossify.home.helpers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.min

// detects icons that are too close in luminance to the app drawer's own background colour to
// read clearly against it (a light icon on the light-mode drawer, or symmetrically a near-black
// icon on the dark-mode drawer) and gives them a subtle backdrop disc, since some icons are
// designed assuming a dark or neutral background and become nearly invisible otherwise
object IconContrastHelper {

    // relative luminance delta below which an icon is considered too close to the background to
    // read clearly - covers a near-white icon (lum ~1.0) against the light drawer background
    // (lum ~0.90, delta ~0.10) and a near-black icon (lum ~0.0) against the dark drawer
    // background (lum ~0.03, delta ~0.03) with margin, while leaving icons of clearly different
    // brightness (e.g. a mid-grey icon, delta ~0.40) untouched
    private const val LOW_CONTRAST_LUMINANCE_DELTA = 0.20

    // pixels this transparent or more aren't part of the icon's visible silhouette
    private const val MIN_OPAQUE_ALPHA_TO_COUNT = 20

    private const val BACKDROP_ALPHA = 0.12f
    private const val BACKDROP_RADIUS_FRACTION = 0.92f

    // alpha-weighted average relative luminance (WCAG coefficients) of the icon's visible
    // pixels - alpha-weighting matters here because a small logo on a mostly transparent canvas
    // should be judged by the logo's own colour, not diluted by treating the transparent margin
    // as black. Returns null for a fully (or almost fully) transparent icon, since there's
    // nothing to contrast against
    fun averageLuminance(bitmap: Bitmap): Double? {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) {
            return null
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var weightedLuminance = 0.0
        var totalWeight = 0.0
        for (pixel in pixels) {
            val alpha = Color.alpha(pixel)
            if (alpha < MIN_OPAQUE_ALPHA_TO_COUNT) {
                continue
            }

            weightedLuminance += relativeLuminance(pixel) * alpha
            totalWeight += alpha
        }

        return if (totalWeight <= 0.0) null else weightedLuminance / totalWeight
    }

    private fun relativeLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun isLowContrast(iconBitmap: Bitmap, backgroundColor: Int): Boolean {
        val iconLuminance = averageLuminance(iconBitmap) ?: return false
        val backgroundLuminance = relativeLuminance(backgroundColor)
        return abs(iconLuminance - backgroundLuminance) < LOW_CONTRAST_LUMINANCE_DELTA
    }

    // returns the icon unchanged unless it's low-contrast against backgroundColor, in which case
    // it returns a new bitmap with a soft, centered backdrop disc drawn behind the icon
    fun drawContrastBackdropIfNeeded(iconBitmap: Bitmap, backgroundColor: Int): Bitmap {
        if (!isLowContrast(iconBitmap, backgroundColor)) {
            return iconBitmap
        }

        val width = iconBitmap.width
        val height = iconBitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(centerX, centerY) * BACKDROP_RADIUS_FRACTION
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.setAlphaComponent(Color.BLACK, (BACKDROP_ALPHA * 255).toInt())
        }
        canvas.drawCircle(centerX, centerY, radius, paint)
        canvas.drawBitmap(iconBitmap, 0f, 0f, null)

        return result
    }
}
