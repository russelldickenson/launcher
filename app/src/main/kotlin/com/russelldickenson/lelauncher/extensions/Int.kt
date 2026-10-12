package com.russelldickenson.lelauncher.extensions

import android.graphics.Color
import androidx.core.graphics.ColorUtils

val DARK_GREY = 0xFF333333.toInt()

private const val WCAG_AA_NORMAL = 4.5
private const val LUMINANCE_THRESHOLD = 0.5
private const val CONTRAST_STEP = 0.05f

fun Int.getContrastColor(): Int {
    val luminance = ColorUtils.calculateLuminance(this)
    return if (luminance > LUMINANCE_THRESHOLD) DARK_GREY else Color.WHITE
}

fun Int.adjustAlpha(factor: Float): Int {
    return Color.argb(Math.round(Color.alpha(this) * factor), Color.red(this), Color.green(this), Color.blue(this))
}

// blends the color towards white/black until it reaches the contrast ratio against the background
fun Int.adjustForContrast(background: Int, minContrast: Double = WCAG_AA_NORMAL): Int {
    val target = if (ColorUtils.calculateLuminance(background) < LUMINANCE_THRESHOLD) Color.WHITE else Color.BLACK
    var color = this
    var ratio = 0f
    while (ColorUtils.calculateContrast(color, background) < minContrast && ratio < 1f) {
        ratio += CONTRAST_STEP
        color = ColorUtils.blendARGB(this, target, ratio)
    }

    return color
}
