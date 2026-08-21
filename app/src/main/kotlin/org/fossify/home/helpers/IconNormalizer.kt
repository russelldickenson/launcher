package org.fossify.home.helpers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.Drawable
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/*
 * A partial Kotlin port of AOSP's com.android.launcher3.icons.IconNormalizer
 * (frameworks/libs/systemui/iconloaderlib), licensed under the Apache License, Version 2.0:
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * This is the analysis Lawnchair's "Auto-adaptive icons" feature is built on. Only the pieces
 * this launcher needs are ported: a normalization scale based on how much of the icon's canvas is
 * actually opaque ("ink"), and a check for whether the icon's own silhouette already closely
 * matches the target shape. Left out on purpose: AdaptiveIconDrawable-specific normalization
 * (handled separately, see IconPackHelper.getShapedIcon), drop-shadow generation, and badge/
 * dominant-color extraction - none of which this launcher's icon shaping needs.
 */
object IconNormalizer {

    // ratio of icon visible area to full icon size for a square-shaped icon
    private const val MAX_SQUARE_AREA_FACTOR = 375f / 576
    // ratio of icon visible area to full icon size for a circle-shaped icon
    private const val MAX_CIRCLE_AREA_FACTOR = 380f / 576
    private val CIRCLE_AREA_BY_RECT = (PI / 4).toFloat()
    private val LINEAR_SCALE_SLOPE = (MAX_CIRCLE_AREA_FACTOR - MAX_SQUARE_AREA_FACTOR) / (1 - CIRCLE_AREA_BY_RECT)

    private const val MIN_VISIBLE_ALPHA = 40
    private const val BOUND_RATIO_MARGIN = .05f
    private const val PIXEL_DIFF_PERCENTAGE_THRESHOLD = 0.005f

    data class Result(val scale: Float, val matchesShape: Boolean)

    /**
     * @param drawable the icon to analyze
     * @param analysisSize the square canvas size (px) to render/scan the icon at - use roughly
     *   2x the final icon size to keep the pixel scan precise without scaling artifacts
     * @param normalizedShapePath the target shape's outline, in [0,1]x[0,1] coordinates
     */
    fun analyze(drawable: Drawable, analysisSize: Int, normalizedShapePath: Path): Result {
        val bitmap = Bitmap.createBitmap(analysisSize, analysisSize, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(bitmap)

        val oldBounds = Rect(drawable.bounds)
        drawable.setBounds(0, 0, analysisSize, analysisSize)
        drawable.draw(canvas)
        drawable.bounds = oldBounds

        val pixels = ByteArray(analysisSize * analysisSize)
        val buffer = ByteBuffer.wrap(pixels)
        bitmap.copyPixelsToBuffer(buffer)

        val leftBorder = FloatArray(analysisSize)
        val rightBorder = FloatArray(analysisSize)

        var topY = -1
        var bottomY = -1
        var leftX = analysisSize + 1
        var rightX = -1

        var index = 0
        for (y in 0 until analysisSize) {
            var firstX = -1
            var lastX = -1
            for (x in 0 until analysisSize) {
                if ((pixels[index].toInt() and 0xFF) > MIN_VISIBLE_ALPHA) {
                    if (firstX == -1) {
                        firstX = x
                    }
                    lastX = x
                }
                index++
            }

            leftBorder[y] = firstX.toFloat()
            rightBorder[y] = lastX.toFloat()

            if (firstX != -1) {
                bottomY = y
                if (topY == -1) {
                    topY = y
                }
                leftX = min(leftX, firstX)
                rightX = max(rightX, lastX)
            }
        }

        if (topY == -1 || rightX == -1) {
            // fully transparent icon, nothing to normalize
            return Result(scale = 1f, matchesShape = false)
        }

        convertToConvexArray(leftBorder, 1, topY, bottomY)
        convertToConvexArray(rightBorder, -1, topY, bottomY)

        var hullArea = 0f
        for (y in 0 until analysisSize) {
            if (leftBorder[y] <= -1) {
                continue
            }
            hullArea += rightBorder[y] - leftBorder[y] + 1
        }

        val bounds = Rect(leftX, topY, rightX, bottomY)
        val rectArea = (bottomY + 1 - topY).toFloat() * (rightX + 1 - leftX)
        val scale = getScale(hullArea, rectArea, (analysisSize * analysisSize).toFloat())
        val matchesShape = isShape(bitmap, pixels, bounds, analysisSize, normalizedShapePath)

        return Result(scale = scale, matchesShape = matchesShape)
    }

    private fun getScale(hullArea: Float, boundingArea: Float, fullArea: Float): Float {
        val hullByRect = hullArea / boundingArea
        val scaleRequired = if (hullByRect < CIRCLE_AREA_BY_RECT) {
            MAX_CIRCLE_AREA_FACTOR
        } else {
            MAX_SQUARE_AREA_FACTOR + LINEAR_SCALE_SLOPE * (1 - hullByRect)
        }

        val areaScale = hullArea / fullArea
        return if (areaScale > scaleRequired) sqrt(scaleRequired / areaScale) else 1f
    }

    // draws the shape outline over the icon's already-rendered alpha bitmap (XOR, then clear the
    // outline stroke) and checks whether the result is basically empty - if so, the icon's own
    // silhouette already closely matches the shape and doesn't need to be scaled down onto a
    // colored backdrop
    private fun isShape(
        bitmap: Bitmap,
        pixels: ByteArray,
        bounds: Rect,
        size: Int,
        normalizedShapePath: Path
    ): Boolean {
        val iconRatio = bounds.width().toFloat() / bounds.height()
        if (abs(iconRatio - 1) > BOUND_RATIO_MARGIN) {
            return false
        }

        val canvas = Canvas(bitmap)
        val matrix = Matrix()
        matrix.setScale(bounds.width().toFloat(), bounds.height().toFloat())
        matrix.postTranslate(bounds.left.toFloat(), bounds.top.toFloat())
        val shapePath = Path()
        normalizedShapePath.transform(matrix, shapePath)

        val maskShapePaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
        }
        canvas.drawPath(shapePath, maskShapePaint)

        val outlinePaint = Paint().apply {
            strokeWidth = 2f
            style = Paint.Style.STROKE
            color = Color.BLACK
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        canvas.drawPath(shapePath, outlinePaint)

        val buffer = ByteBuffer.wrap(pixels)
        buffer.rewind()
        bitmap.copyPixelsToBuffer(buffer)

        var sum = 0
        var index = bounds.top * size
        val rowSizeDiff = size - bounds.right
        for (y in bounds.top until bounds.bottom) {
            index += bounds.left
            for (x in bounds.left until bounds.right) {
                if ((pixels[index].toInt() and 0xFF) > MIN_VISIBLE_ALPHA) {
                    sum++
                }
                index++
            }
            index += rowSizeDiff
        }

        val percentageDiffPixels = sum.toFloat() / (bounds.width() * bounds.height())
        return percentageDiffPixels < PIXEL_DIFF_PERCENTAGE_THRESHOLD
    }

    // modifies xCoordinates in place to represent a convex border, filling in the rows the icon
    // itself left as gaps with values interpolated along the hull
    private fun convertToConvexArray(xCoordinates: FloatArray, direction: Int, topY: Int, bottomY: Int) {
        val angles = FloatArray(xCoordinates.size - 1)

        val first = topY
        var last = -1
        var lastAngle = Float.MAX_VALUE

        for (i in (topY + 1)..bottomY) {
            if (xCoordinates[i] <= -1) {
                continue
            }

            var start: Int
            if (lastAngle == Float.MAX_VALUE) {
                start = first
            } else {
                var currentAngle = (xCoordinates[i] - xCoordinates[last]) / (i - last)
                start = last
                // if this position creates a concave angle, keep moving up until we find a
                // position which creates a convex angle
                if ((currentAngle - lastAngle) * direction < 0) {
                    while (start > first) {
                        start--
                        currentAngle = (xCoordinates[i] - xCoordinates[start]) / (i - start)
                        if ((currentAngle - angles[start]) * direction >= 0) {
                            break
                        }
                    }
                }
            }

            lastAngle = (xCoordinates[i] - xCoordinates[start]) / (i - start)
            for (j in start until i) {
                angles[j] = lastAngle
                xCoordinates[j] = xCoordinates[start] + lastAngle * (j - start)
            }
            last = i
        }
    }
}
