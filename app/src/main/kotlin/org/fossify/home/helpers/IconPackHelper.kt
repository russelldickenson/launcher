package org.fossify.home.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object IconPackHelper {
    private const val TAG = "IconPackHelper"

    private val THEME_INTENT_ACTIONS = arrayOf(
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "org.adw.launcher.THEMES"
    )

    private const val SHAPED_ICON_SIZE = 108

    // how much of the badge an icon whose own silhouette already matches the chosen shape
    // should fill - just enough inset that it doesn't touch the very edge
    private const val SHAPE_MATCH_FILL_FRACTION = 0.94f

    // alpha at which a pixel counts as part of the icon rather than its transparent margin -
    // above zero so a faint antialiasing fringe isn't mistaken for the icon's real edge
    private const val MIN_EDGE_ALPHA = 32

    private data class ParsedAppFilter(
        val componentMap: Map<ComponentName, String>
    )

    // packageName -> parsed appfilter.xml contents
    private val appFilterCache = HashMap<String, ParsedAppFilter>()

    // "packageName/activityName:shape" -> the shaped icon built for it, so the pixel-level
    // normalization/edge-color analysis in getShapedIcon() isn't redone on every app-list refresh
    private val shapedIconCache = HashMap<String, Drawable>()

    fun getInstalledIconPacks(context: Context): List<IconPack> {
        val packageManager = context.packageManager
        val packages = LinkedHashMap<String, IconPack>()

        for (action in THEME_INTENT_ACTIONS) {
            val intent = Intent(action)
            val resolveInfos = try {
                packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                emptyList()
            }

            for (resolveInfo in resolveInfos) {
                val packageName = resolveInfo.activityInfo.packageName
                if (!packages.containsKey(packageName)) {
                    val label = try {
                        resolveInfo.loadLabel(packageManager).toString()
                    } catch (e: Exception) {
                        packageName
                    }
                    packages[packageName] = IconPack(name = label, packageName = packageName)
                }
            }
        }

        Log.d(TAG, "getInstalledIconPacks: found ${packages.size} pack(s): ${packages.keys}")
        return packages.values.sortedBy { it.name.lowercase() }
    }

    fun getIcon(context: Context, iconPackPackageName: String, packageName: String, activityName: String): Drawable? {
        if (iconPackPackageName.isEmpty()) {
            return null
        }

        val appFilter = getParsedAppFilter(context, iconPackPackageName)
        val componentKey = ComponentName(packageName, activityName)
        val drawableName = appFilter.componentMap[componentKey]
        if (drawableName == null) {
            Log.d(TAG, "getIcon: no appfilter entry for $componentKey in $iconPackPackageName (${appFilter.componentMap.size} entries parsed)")
            return null
        }

        val drawable = loadDrawable(context, iconPackPackageName, drawableName)
        if (drawable == null) {
            Log.d(TAG, "getIcon: appfilter mapped $componentKey -> drawable '$drawableName', but it could not be loaded from $iconPackPackageName")
        }
        return drawable
    }

    // gives any icon - whether it's the app's real stock icon or one supplied by an icon pack -
    // a shape/backdrop consistent with the chosen icon theme, since icon packs don't always shape
    // every icon consistently themselves. Icon packs' own iconback/iconmask/iconupon assets are
    // designed to backdrop a transparent adaptive-icon foreground, not a fully opaque stock icon,
    // so reusing them here produces overlapping/blank results - a plain, user-chosen shape behind
    // the icon is what actually looks consistent.
    //
    // the pixel-level analysis this does is too expensive to redo on every app-list refresh, so
    // results are cached by component + shape; pass a stable packageName/activityName as
    // componentKey so repeated calls for the same app can hit the cache
    fun getShapedIcon(context: Context, componentKey: String, originalIcon: Drawable, shape: Int): Drawable {
        val cacheKey = "$componentKey:$shape"
        shapedIconCache[cacheKey]?.let { return it }

        val shapedIcon = buildShapedIcon(context, originalIcon, shape)
        shapedIconCache[cacheKey] = shapedIcon
        return shapedIcon
    }

    private fun buildShapedIcon(context: Context, originalIcon: Drawable, shape: Int): Drawable {
        val size = SHAPED_ICON_SIZE
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val path = getShapePath(shape, size.toFloat())

        val iconSize = if (originalIcon is AdaptiveIconDrawable) {
            // already designed to fill its own bounds and be clipped externally, no normalization needed
            size
        } else {
            // legacy icons vary wildly in how much of their canvas is actual "ink" (a small round
            // logo vs. a full-bleed square icon), so a flat scale either over- or under-fills the
            // badge depending on the icon - normalize based on the icon's own visible silhouette instead
            val normalized = IconNormalizer.analyze(
                drawable = originalIcon,
                analysisSize = size * 2,
                normalizedShapePath = getShapePath(shape, 1f)
            )
            if (normalized.matchesShape) {
                // the icon's own silhouette already closely matches the chosen shape - a small
                // inset is enough, no need to shrink it further onto a visible colored backdrop
                (size * SHAPE_MATCH_FILL_FRACTION).toInt()
            } else {
                (size * normalized.scale).toInt()
            }.coerceAtLeast(1)
        }

        val offset = (size - iconSize) / 2
        val iconBitmap = originalIcon.toBitmap(width = iconSize, height = iconSize, config = Bitmap.Config.ARGB_8888)

        // fill the gap between the icon and the shape with a color sampled from the icon's own
        // edge pixels, so it reads as a natural bleed instead of an unrelated colored halo
        val themedFallback = ColorUtils.blendARGB(context.getProperBackgroundColor(), context.getProperPrimaryColor(), 0.18f)
        val backgroundColor = getEdgeColor(iconBitmap) ?: themedFallback
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor })

        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(iconBitmap, offset.toFloat(), offset.toFloat(), null)
        canvas.restore()

        return BitmapDrawable(context.resources, result)
    }

    // averages the color of the icon's own silhouette edge (alpha-weighted), to use as the
    // backdrop color filling the gap between the icon and the chosen shape. Walks inward along
    // every row and column and takes the first pixel that is actually part of the icon, rather
    // than reading the canvas' outer border - most icons sit on a transparent margin, and the
    // ones normalization shrinks the most (so the ones with the largest gap to fill) have the
    // widest margin of all, whose border pixels carry no color at all. Returns null only if the
    // icon is entirely transparent, so the caller can fall back to a themed color instead
    private fun getEdgeColor(bitmap: Bitmap): Int? {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) {
            return null
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var totalWeight = 0L

        fun accumulate(index: Int) {
            val pixel = pixels[index]
            val alpha = Color.alpha(pixel)
            totalR += Color.red(pixel) * alpha
            totalG += Color.green(pixel) * alpha
            totalB += Color.blue(pixel) * alpha
            totalWeight += alpha
        }

        fun isInk(index: Int) = Color.alpha(pixels[index]) >= MIN_EDGE_ALPHA

        for (y in 0 until height) {
            val rowStart = y * width
            val left = (0 until width).firstOrNull { isInk(rowStart + it) } ?: continue
            val right = (width - 1 downTo 0).first { isInk(rowStart + it) }
            accumulate(rowStart + left)
            if (right != left) {
                accumulate(rowStart + right)
            }
        }

        for (x in 0 until width) {
            val top = (0 until height).firstOrNull { isInk(it * width + x) } ?: continue
            val bottom = (height - 1 downTo 0).first { isInk(it * width + x) }
            accumulate(top * width + x)
            if (bottom != top) {
                accumulate(bottom * width + x)
            }
        }

        if (totalWeight == 0L) {
            return null
        }

        return Color.rgb(
            (totalR / totalWeight).toInt(),
            (totalG / totalWeight).toInt(),
            (totalB / totalWeight).toInt()
        )
    }

    private fun getShapePath(shape: Int, size: Float): Path {
        val path = Path()
        when (shape) {
            ICON_SHAPE_SQUARE -> path.addRect(0f, 0f, size, size, Path.Direction.CW)
            ICON_SHAPE_ROUNDED_SQUARE -> path.addRoundRect(RectF(0f, 0f, size, size), size * 0.22f, size * 0.22f, Path.Direction.CW)
            // approximated with an extra-rounded rect - a true superellipse isn't worth the complexity here
            ICON_SHAPE_SQUIRCLE -> path.addRoundRect(RectF(0f, 0f, size, size), size * 0.4f, size * 0.4f, Path.Direction.CW)
            else -> path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
        }
        return path
    }

    fun clearCache() {
        appFilterCache.clear()
        shapedIconCache.clear()
    }

    private fun getParsedAppFilter(context: Context, iconPackPackageName: String): ParsedAppFilter {
        return appFilterCache.getOrPut(iconPackPackageName) {
            parseAppFilter(context, iconPackPackageName)
        }
    }

    private fun loadDrawable(context: Context, iconPackPackageName: String, drawableName: String): Drawable? {
        return try {
            val resources = context.packageManager.getResourcesForApplication(iconPackPackageName)
            val resId = resources.getIdentifier(drawableName, "drawable", iconPackPackageName)
            if (resId == 0) {
                null
            } else {
                ResourcesCompat.getDrawable(resources, resId, null)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseAppFilter(context: Context, iconPackPackageName: String): ParsedAppFilter {
        val componentMap = HashMap<ComponentName, String>()

        try {
            val parser = getAppFilterParser(context, iconPackPackageName) ?: return ParsedAppFilter(componentMap)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    val parsedComponent = component?.let { parseComponent(it) }
                    if (parsedComponent != null && drawable != null) {
                        componentMap[parsedComponent] = drawable
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
        }

        Log.d(TAG, "parseAppFilter: $iconPackPackageName -> ${componentMap.size} component(s)")

        return ParsedAppFilter(componentMap)
    }

    // most icon packs ship appfilter.xml as a compiled res/xml resource, but some only
    // include it as a raw asset file
    private fun getAppFilterParser(context: Context, iconPackPackageName: String): XmlPullParser? {
        try {
            val resources = context.packageManager.getResourcesForApplication(iconPackPackageName)
            val xmlResId = resources.getIdentifier("appfilter", "xml", iconPackPackageName)
            if (xmlResId != 0) {
                Log.d(TAG, "getAppFilterParser: $iconPackPackageName -> using compiled res/xml/appfilter.xml")
                return resources.getXml(xmlResId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "getAppFilterParser: failed reading compiled resources for $iconPackPackageName", e)
        }

        return try {
            val packageContext = context.createPackageContext(iconPackPackageName, 0)
            val inputStream = packageContext.assets.open("appfilter.xml")
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(inputStream, null)
            Log.d(TAG, "getAppFilterParser: $iconPackPackageName -> using assets/appfilter.xml")
            parser
        } catch (e: Exception) {
            Log.w(TAG, "getAppFilterParser: no appfilter.xml found for $iconPackPackageName (neither compiled resource nor asset)", e)
            null
        }
    }

    // parses a component attribute value into a ComponentName, stripping the optional
    // "ComponentInfo{...}" wrapper some icon packs include. ComponentName.unflattenFromString
    // already handles the ".ClassName" shorthand for activities in the app's own package.
    private fun parseComponent(raw: String): ComponentName? {
        var value = raw.trim()
        if (value.startsWith("ComponentInfo{") && value.endsWith("}")) {
            value = value.substring("ComponentInfo{".length, value.length - 1).trim()
        }

        return try {
            ComponentName.unflattenFromString(value)
        } catch (e: Exception) {
            null
        }
    }
}
