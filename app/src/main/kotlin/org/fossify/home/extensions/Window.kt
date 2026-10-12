package org.fossify.home.extensions

import android.view.View
import android.view.Window
import androidx.core.view.WindowInsetsControllerCompat

fun Window.insetsController(view: View? = null) = WindowInsetsControllerCompat(this, view ?: decorView)

fun Window.setSystemBarsAppearance(backgroundColor: Int) {
    val isLightBackground = backgroundColor.getContrastColor() == DARK_GREY
    insetsController().apply {
        isAppearanceLightStatusBars = isLightBackground
        isAppearanceLightNavigationBars = isLightBackground
    }
}
