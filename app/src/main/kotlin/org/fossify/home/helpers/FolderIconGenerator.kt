package org.fossify.home.helpers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import org.fossify.commons.extensions.getContrastColor
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

// tiles a closed folder's member icons into one circular preview bitmap - the same tiling
// formula (columns = ceil(sqrt(count)), a quarter-cell gap, centered extra rows) as the home
// screen's own closed-folder icon (HomeScreenGrid.kt's HomeScreenFolder.generateDrawable()),
// factored out so both surfaces render folders consistently. Not a literal extraction of that
// function, since it lays out children by their stored home-screen grid position - drawer folders
// have no such position and just tile members in list order instead
object FolderIconGenerator {

    // a soft tint of whichever of black/white contrasts with the drawer's own background colour
    // (the same getContrastColor() already used for notification badge text), rather than a
    // generic theme colour that can end up barely different from the drawer background it sits on
    private const val BACKGROUND_TINT_ALPHA = 0.32f

    // only ever preview the first 4 members - a folder with more than that still just shows a
    // 2x2 sample of it, the same convention most launchers use, rather than cramming every icon in
    private const val MAX_PREVIEW_ICONS = 4

    fun generate(
        context: Context,
        memberIcons: List<Drawable?>,
        iconSize: Int,
        drawerBackgroundColor: Int,
        iconShape: Int,
    ): Drawable? {
        if (iconSize <= 0 || memberIcons.isEmpty()) {
            return null
        }

        val previewIcons = memberIcons.take(MAX_PREVIEW_ICONS)

        val backgroundColor = ColorUtils.setAlphaComponent(
            drawerBackgroundColor.getContrastColor(), (BACKGROUND_TINT_ALPHA * 255).toInt()
        )
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }

        val bitmap = createBitmap(iconSize, iconSize)
        val canvas = Canvas(bitmap)
        // same shape path IconPackHelper uses to reshape real app icons, so a folder's backdrop
        // matches whatever icon shape the user has chosen instead of always being a circle
        val shapePath = IconPackHelper.getShapePath(iconShape, iconSize.toFloat())
        canvas.clipPath(shapePath)
        canvas.drawPaint(backgroundPaint)

        val itemCount = previewIcons.size
        val columnCount = ceil(sqrt(itemCount.toDouble())).roundToInt()
        val rowCount = ceil(itemCount.toFloat() / columnCount).roundToInt()
        val scaledCellSize = iconSize.toFloat() / columnCount
        val scaledGap = scaledCellSize / 4f
        val scaledIconSize = (iconSize - (columnCount + 1) * scaledGap) / columnCount
        val extraYMargin = if (rowCount < columnCount) (scaledIconSize + scaledGap) / 2 else 0f

        previewIcons.forEachIndexed { index, drawable ->
            val row = index / columnCount
            val column = index % columnCount
            val drawableX = (scaledGap + column * scaledIconSize + column * scaledGap).toInt()
            val drawableY = (extraYMargin + scaledGap + row * scaledIconSize + row * scaledGap).toInt()
            val childDrawable = drawable?.constantState?.newDrawable()?.mutate()
            childDrawable?.setBounds(
                drawableX,
                drawableY,
                drawableX + scaledIconSize.toInt(),
                drawableY + scaledIconSize.toInt()
            )
            childDrawable?.draw(canvas)
        }

        return bitmap.toDrawable(context.resources)
    }
}
