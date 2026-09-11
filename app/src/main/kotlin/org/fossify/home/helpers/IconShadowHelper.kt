package org.fossify.home.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.ceil
import kotlin.math.roundToInt

// an optional, app-drawer-only drop shadow behind each icon, offset down and to the right like a
// directional key light rather than a symmetric ambient glow. Uses the standard
// Bitmap.extractAlpha() + BlurMaskFilter technique (the same one AOSP Launcher3's own
// ShadowGenerator uses) to blur the icon's own silhouette into a soft shadow shape, so it follows
// whatever the icon's actual outline is - masked/squircled/circular/etc. - rather than a fixed
// shape. This must stay separate from the drawer/home-screen-shared icon bitmap (see
// getAppIconBitmapWithContrastBackdrop) - it's applied at drawer bind time only, never baked into
// the shared AppLauncher.drawable, since the home screen doesn't (and shouldn't) get this
object IconShadowHelper {

    // sized as a fraction of the icon's own bitmap width, not a fixed dp amount - this makes the
    // resulting bitmap growth (and so GROWTH_FACTOR below) a fixed proportion regardless of the
    // source icon's actual resolution, which callers rely on to grow the icon's own display box
    // by the exact same proportion (see LaunchersAdapter.calculateIconWidth()) so the shadow gets
    // genuinely new space to render into instead of the icon itself being scaled down to make room
    private const val SHADOW_OFFSET_FRACTION = 0.035f
    private const val SHADOW_BLUR_RADIUS_FRACTION = 0.045f
    private const val SHADOW_ALPHA = 130 // out of 255, ~51%

    // how much larger the shadowed bitmap is than the source icon, along each dimension - callers
    // displaying a shadowed icon must grow its display box by this same factor, or fitCenter-style
    // scaling will shrink the icon itself to absorb the shadow's extra bitmap space
    const val GROWTH_FACTOR = 1f + SHADOW_OFFSET_FRACTION + 2 * SHADOW_BLUR_RADIUS_FRACTION

    fun applyDropShadow(context: Context, drawable: Drawable): Drawable {
        val iconBitmap = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
        val offset = (iconBitmap.width * SHADOW_OFFSET_FRACTION).roundToInt()
        val blurRadius = iconBitmap.width * SHADOW_BLUR_RADIUS_FRACTION
        // extractAlpha()'s blur can spread the mask a little past blurRadius itself, so pad
        // generously to avoid clipping the softest edge of the shadow
        val blurPad = ceil(blurRadius).toInt() * 2

        val resultWidth = iconBitmap.width + offset + blurPad
        val resultHeight = iconBitmap.height + offset + blurPad
        val result = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val shadowMaskPaint = Paint().apply {
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }
        // extractAlpha() reports how far it shifted the mask's origin to fit the blur spread, on
        // top of the offsetXY we want to draw it at, in offsetXY
        val maskShift = IntArray(2)
        val shadowMask = iconBitmap.extractAlpha(shadowMaskPaint, maskShift)
        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = SHADOW_ALPHA
        }
        canvas.drawBitmap(
            shadowMask,
            (blurPad / 2 + offset + maskShift[0]).toFloat(),
            (blurPad / 2 + offset + maskShift[1]).toFloat(),
            tintPaint
        )
        canvas.drawBitmap(iconBitmap, (blurPad / 2).toFloat(), (blurPad / 2).toFloat(), null)

        return BitmapDrawable(context.resources, result)
    }
}
