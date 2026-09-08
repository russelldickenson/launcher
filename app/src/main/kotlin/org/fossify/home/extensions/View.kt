package org.fossify.home.extensions

import android.graphics.drawable.LayerDrawable
import android.view.RoundedCorner.POSITION_TOP_LEFT
import android.view.RoundedCorner.POSITION_TOP_RIGHT
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import org.fossify.commons.R
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.helpers.isSPlus

fun View.animateScale(
    from: Float,
    to: Float,
    duration: Long,
) = animate()
    .scaleX(to)
    .scaleY(to)
    .setDuration(duration)
    .setInterpolator(AccelerateDecelerateInterpolator())
    .withStartAction {
        scaleX = from
        scaleY = from
    }

// positions this view so it renders at the given absolute screen coordinates, regardless of which
// window it lives in - plain `view.x = screenX` only lands correctly when the view's own window
// starts at the screen origin (a fullscreen Activity window), since View.x/y are relative to the
// view's parent, not the screen. A view inside a WRAP_CONTENT/centered Dialog window (or any
// window not anchored at (0,0)) needs that window+ancestor offset subtracted out first, which
// this computes by comparing the view's actual laid-out screen location against the target
fun View.moveToScreenPosition(x: Float, y: Float) {
    val location = IntArray(2)
    getLocationOnScreen(location)
    this.x += x - location[0]
    this.y += y - location[1]
}

fun View.setupDrawerBackground(backgroundColor: Int = context.getProperBackgroundColor()) {
    background = backgroundColor.toDrawable()

    val insets = rootWindowInsets
    if (isSPlus() && insets != null) {
        val topRightCorner = insets.getRoundedCorner(POSITION_TOP_RIGHT)?.radius ?: 0
        val topLeftCorner = insets.getRoundedCorner(POSITION_TOP_LEFT)?.radius ?: 0
        if (topRightCorner > 0 && topLeftCorner > 0) {
            background = ResourcesCompat.getDrawable(
                context.resources, R.drawable.bottom_sheet_bg, context.theme
            ).apply {
                (this as LayerDrawable)
                    .findDrawableByLayerId(R.id.bottom_sheet_background)
                    .applyColorFilter(backgroundColor)
            }
        }
    }
}