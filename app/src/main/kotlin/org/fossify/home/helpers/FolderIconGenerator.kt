package org.fossify.home.helpers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.color.MaterialColors
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.getProperBackgroundColor
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

    fun generate(context: Context, memberIcons: List<Drawable?>, iconSize: Int): Drawable? {
        if (iconSize <= 0 || memberIcons.isEmpty()) {
            return null
        }

        val backgroundColor = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorSurfaceContainer,
            context.getProperBackgroundColor()
        ).adjustAlpha(0.9f)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }

        val bitmap = createBitmap(iconSize, iconSize)
        val canvas = Canvas(bitmap)
        val circlePath = Path().apply {
            addCircle((iconSize / 2).toFloat(), (iconSize / 2).toFloat(), (iconSize / 2).toFloat(), Path.Direction.CCW)
        }
        canvas.clipPath(circlePath)
        canvas.drawPaint(backgroundPaint)

        val itemCount = memberIcons.size
        val columnCount = ceil(sqrt(itemCount.toDouble())).roundToInt()
        val rowCount = ceil(itemCount.toFloat() / columnCount).roundToInt()
        val scaledCellSize = iconSize.toFloat() / columnCount
        val scaledGap = scaledCellSize / 4f
        val scaledIconSize = (iconSize - (columnCount + 1) * scaledGap) / columnCount
        val extraYMargin = if (rowCount < columnCount) (scaledIconSize + scaledGap) / 2 else 0f

        memberIcons.forEachIndexed { index, drawable ->
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
