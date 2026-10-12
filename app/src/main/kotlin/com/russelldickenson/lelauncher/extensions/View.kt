package com.russelldickenson.lelauncher.extensions

import android.graphics.drawable.LayerDrawable
import android.view.HapticFeedbackConstants
import android.view.RoundedCorner.POSITION_TOP_LEFT
import android.view.RoundedCorner.POSITION_TOP_RIGHT
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.russelldickenson.lelauncher.R
import com.russelldickenson.lelauncher.helpers.isSPlus

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
fun View.beVisible() {
    visibility = View.VISIBLE
}

fun View.beGone() {
    visibility = View.GONE
}

fun View.beVisibleIf(visible: Boolean) = if (visible) beVisible() else beGone()

fun View.onGlobalLayout(callback: () -> Unit) {
    viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (viewTreeObserver != null) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                callback()
            }
        }
    })
}

fun View.performHapticFeedback() = performHapticFeedback(
    HapticFeedbackConstants.VIRTUAL_KEY,
    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
)

// system-bar insets get added on top of whatever padding/margin the layout declared, so repeated
// inset dispatches don't accumulate
private fun View.basePadding(): IntArray {
    val saved = getTag(R.id.tag_base_padding) as? IntArray
    if (saved != null) return saved
    return intArrayOf(paddingLeft, paddingTop, paddingRight, paddingBottom).also { setTag(R.id.tag_base_padding, it) }
}

private fun View.baseMargin(): IntArray {
    val saved = getTag(R.id.tag_base_margin) as? IntArray
    if (saved != null) return saved
    val params = layoutParams as? ViewGroup.MarginLayoutParams
    return intArrayOf(
        params?.leftMargin ?: 0,
        params?.topMargin ?: 0,
        params?.rightMargin ?: 0,
        params?.bottomMargin ?: 0
    ).also { setTag(R.id.tag_base_margin, it) }
}

fun View.updatePaddingWithBase(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
    val base = basePadding()
    updatePadding(
        left = left?.let { base[0] + it } ?: paddingLeft,
        top = top?.let { base[1] + it } ?: paddingTop,
        right = right?.let { base[2] + it } ?: paddingRight,
        bottom = bottom?.let { base[3] + it } ?: paddingBottom
    )
}

fun View.updateMarginWithBase(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
    val base = baseMargin()
    val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    updateLayoutParams<ViewGroup.MarginLayoutParams> {
        leftMargin = left?.let { base[0] + it } ?: params.leftMargin
        topMargin = top?.let { base[1] + it } ?: params.topMargin
        rightMargin = right?.let { base[2] + it } ?: params.rightMargin
        bottomMargin = bottom?.let { base[3] + it } ?: params.bottomMargin
    }
}
