package org.fossify.home.helpers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import org.xmlpull.v1.XmlPullParser

object IconPackHelper {
    private val THEME_INTENT_ACTIONS = arrayOf(
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "org.adw.launcher.THEMES"
    )

    private const val DEFAULT_MASKED_ICON_SIZE = 108

    data class MaskInfo(
        val backNames: List<String>,
        val maskName: String?,
        val uponName: String?,
        val scale: Float
    )

    private data class ParsedAppFilter(
        val componentMap: Map<String, String>,
        val maskInfo: MaskInfo?
    )

    // packageName -> parsed appfilter.xml contents
    private val appFilterCache = HashMap<String, ParsedAppFilter>()

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

        return packages.values.sortedBy { it.name.lowercase() }
    }

    fun getIcon(context: Context, iconPackPackageName: String, packageName: String, activityName: String): Drawable? {
        if (iconPackPackageName.isEmpty()) {
            return null
        }

        val appFilter = getParsedAppFilter(context, iconPackPackageName)
        val componentKey = "ComponentInfo{$packageName/$activityName}"
        val drawableName = appFilter.componentMap[componentKey] ?: return null
        return loadDrawable(context, iconPackPackageName, drawableName)
    }

    // composites an app's real icon onto the icon pack's themed back/mask/upon shapes,
    // for apps the icon pack doesn't explicitly map
    fun getMaskedIcon(context: Context, iconPackPackageName: String, originalIcon: Drawable): Drawable? {
        if (iconPackPackageName.isEmpty()) {
            return null
        }

        val maskInfo = getParsedAppFilter(context, iconPackPackageName).maskInfo ?: return null
        val backName = maskInfo.backNames.firstOrNull() ?: return null
        val backDrawable = loadDrawable(context, iconPackPackageName, backName) ?: return null
        val maskDrawable = maskInfo.maskName?.let { loadDrawable(context, iconPackPackageName, it) }
        val uponDrawable = maskInfo.uponName?.let { loadDrawable(context, iconPackPackageName, it) }

        return try {
            compositeIcon(context, originalIcon, backDrawable, maskDrawable, uponDrawable, maskInfo.scale)
        } catch (e: Exception) {
            null
        }
    }

    fun clearCache() {
        appFilterCache.clear()
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

    private fun compositeIcon(
        context: Context,
        original: Drawable,
        back: Drawable,
        mask: Drawable?,
        upon: Drawable?,
        scale: Float
    ): Drawable {
        val size = maxOf(back.intrinsicWidth, back.intrinsicHeight, 1).let {
            if (it <= 1) DEFAULT_MASKED_ICON_SIZE else it
        }

        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        back.setBounds(0, 0, size, size)
        back.draw(canvas)

        val iconSize = (size * scale).toInt().coerceAtLeast(1)
        val offset = (size - iconSize) / 2
        val iconBitmap = original.toBitmap(width = iconSize, height = iconSize, config = Bitmap.Config.ARGB_8888)

        if (mask != null) {
            val maskedIcon = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
            val maskCanvas = Canvas(maskedIcon)
            maskCanvas.drawBitmap(iconBitmap, 0f, 0f, null)

            val maskBitmap = mask.toBitmap(width = iconSize, height = iconSize, config = Bitmap.Config.ARGB_8888)
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            maskCanvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint)
            canvas.drawBitmap(maskedIcon, offset.toFloat(), offset.toFloat(), null)
        } else {
            canvas.drawBitmap(iconBitmap, offset.toFloat(), offset.toFloat(), null)
        }

        if (upon != null) {
            upon.setBounds(0, 0, size, size)
            upon.draw(canvas)
        }

        return BitmapDrawable(context.resources, result)
    }

    private fun parseAppFilter(context: Context, iconPackPackageName: String): ParsedAppFilter {
        val componentMap = HashMap<String, String>()
        var backNames: List<String> = emptyList()
        var maskName: String? = null
        var uponName: String? = null
        var scale = 1f

        try {
            val resources = context.packageManager.getResourcesForApplication(iconPackPackageName)
            val xmlResId = resources.getIdentifier("appfilter", "xml", iconPackPackageName)
            if (xmlResId == 0) {
                return ParsedAppFilter(componentMap, null)
            }

            val parser = resources.getXml(xmlResId)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item" -> {
                            val component = parser.getAttributeValue(null, "component")
                            val drawable = parser.getAttributeValue(null, "drawable")
                            if (component != null && drawable != null) {
                                componentMap[component] = drawable
                            }
                        }

                        "iconback" -> backNames = getIndexedAttributeValues(parser, "img")
                        "iconmask" -> maskName = parser.getAttributeValue(null, "img1")
                        "iconupon" -> uponName = parser.getAttributeValue(null, "img1")
                        "scale" -> {
                            parser.getAttributeValue(null, "factor")?.toFloatOrNull()?.let { scale = it }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
        }

        val maskInfo = if (backNames.isNotEmpty()) {
            MaskInfo(backNames = backNames, maskName = maskName, uponName = uponName, scale = scale)
        } else {
            null
        }

        return ParsedAppFilter(componentMap, maskInfo)
    }

    private fun getIndexedAttributeValues(parser: XmlPullParser, attributePrefix: String): List<String> {
        val values = ArrayList<String>()
        var index = 1
        while (true) {
            val value = parser.getAttributeValue(null, "$attributePrefix$index") ?: break
            values.add(value)
            index++
        }
        return values
    }
}
