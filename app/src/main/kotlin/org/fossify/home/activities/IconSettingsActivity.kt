package org.fossify.home.activities

import android.os.Bundle
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.RadioItem
import org.fossify.home.databinding.ActivityIconSettingsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.getAppIcon
import org.fossify.home.extensions.getDrawableForPackageName
import org.fossify.home.extensions.getReferenceIconWidth
import org.fossify.home.extensions.showRadioGroupDialog
import org.fossify.home.helpers.ICON_SHAPE_CIRCLE
import org.fossify.home.helpers.ICON_SHAPE_IOS
import org.fossify.home.helpers.ICON_SHAPE_ONE_UI
import org.fossify.home.helpers.ICON_SHAPE_ROUNDED_SQUARE
import org.fossify.home.helpers.ICON_SHAPE_SQUARE
import org.fossify.home.helpers.ICON_SHAPE_SQUIRCLE
import org.fossify.home.helpers.IconPackHelper
import kotlin.system.exitProcess

class IconSettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityIconSettingsBinding::inflate)

    // icon pack/shape changes need a process restart to take effect everywhere (cached icons,
    // Glide, etc.), but restarting on every single tap is jarring - so we just remember that a
    // restart is owed and do it once the user actually leaves this screen
    private var iconSettingsChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.iconSettingsNestedScrollview))
        setupMaterialScrollListener(binding.iconSettingsNestedScrollview, binding.iconSettingsAppbar)
    }

    override fun onPause() {
        super.onPause()
        // isFinishing is what tells apart actually leaving this screen (back/up, which finishes
        // it) from merely navigating elsewhere without finishing it - without this check, a
        // transient pause would kill the whole process mid-navigation
        if (iconSettingsChanged && isFinishing) {
            exitProcess(0)
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.iconSettingsAppbar, NavigationIcon.Arrow)

        setupIconPack()
        setupReshapeAllIcons()
        setupIconShape()
        updateIconPreview()
        updateIconPreviewLabel()
    }

    private fun setupIconPack() {
        updateIconPackLabel()
        binding.settingsIconPackHolder.setOnClickListener {
            val iconPacks = IconPackHelper.getInstalledIconPacks(this)
            val items = ArrayList<RadioItem>()
            items.add(RadioItem(0, getString(org.fossify.commons.R.string.system_default)))
            iconPacks.forEachIndexed { index, iconPack ->
                items.add(RadioItem(index + 1, iconPack.name))
            }

            val selectedIndex = iconPacks.indexOfFirst { it.packageName == config.iconPack }
            val currentId = if (selectedIndex == -1) 0 else selectedIndex + 1

            showRadioGroupDialog(items = items, checkedItemId = currentId) {
                val selectedId = it as Int
                val newIconPack = if (selectedId == 0) "" else iconPacks[selectedId - 1].packageName
                if (config.iconPack != newIconPack) {
                    config.iconPack = newIconPack
                    IconPackHelper.clearCache()
                    updateIconPackLabel()
                    updateIconPreview()
                    iconSettingsChanged = true
                }
            }
        }
    }

    private fun updateIconPackLabel() {
        val iconPack = config.iconPack
        binding.settingsIconPack.text = if (iconPack.isEmpty()) {
            getString(org.fossify.commons.R.string.system_default)
        } else {
            IconPackHelper.getInstalledIconPacks(this).firstOrNull { it.packageName == iconPack }?.name
                ?: getString(org.fossify.commons.R.string.system_default)
        }
    }

    private fun setupIconShape() {
        updateIconShapeVisibility()

        val chipForShape = mapOf(
            ICON_SHAPE_CIRCLE to binding.settingsIconShapeCircle,
            ICON_SHAPE_SQUIRCLE to binding.settingsIconShapeSquircle,
            ICON_SHAPE_IOS to binding.settingsIconShapeIos,
            // a legacy stored value from before "Rounded square" was renamed to "iOS" - same shape
            ICON_SHAPE_ROUNDED_SQUARE to binding.settingsIconShapeIos,
            ICON_SHAPE_ONE_UI to binding.settingsIconShapeOneUi,
            ICON_SHAPE_SQUARE to binding.settingsIconShapeSquare,
        )
        (chipForShape[config.iconShape] ?: binding.settingsIconShapeCircle).isChecked = true

        binding.settingsIconShapeChips.setOnCheckedStateChangeListener { _, checkedIds ->
            val newShape = chipForShape.entries.firstOrNull { it.value.id == checkedIds.firstOrNull() }?.key
                ?: return@setOnCheckedStateChangeListener
            if (config.iconShape != newShape) {
                config.iconShape = newShape
                updateIconPreview()
                iconSettingsChanged = true
            }
        }
    }

    private fun updateIconShapeVisibility() {
        binding.settingsIconShapeHeading.beVisibleIf(config.reshapeAllIcons)
        binding.settingsIconShapeChips.beVisibleIf(config.reshapeAllIcons)
    }

    private fun setupReshapeAllIcons() {
        binding.settingsReshapeAllIcons.isChecked = config.reshapeAllIcons
        binding.settingsReshapeAllIconsHolder.setOnClickListener {
            binding.settingsReshapeAllIcons.toggle()
            config.reshapeAllIcons = binding.settingsReshapeAllIcons.isChecked
            updateIconShapeVisibility()
            updateIconPreview()
            iconSettingsChanged = true
        }
    }

    // shows this launcher's own icon run through the same icon pack/shape/scale pipeline real app
    // icons go through, as a live preview of these settings - it's fetched on a background thread
    // since getShapedIcon()'s pixel analysis can take a moment on a cache miss
    private fun updateIconPreview() {
        ensureBackgroundThread {
            val drawable = getAppIcon(packageName, "") { getDrawableForPackageName(packageName) }
            runOnUiThread {
                binding.settingsIconPreview.setImageDrawable(drawable)
                updateIconPreviewScale()
            }
        }
    }

    // resizes the actual layout box instead of using a scaleX/scaleY transform, so the
    // surrounding wrap_content container measures and reserves the real rendered size up front -
    // a transform-based scale doesn't participate in layout at all, so the container has no way
    // to know a larger icon needs more room, and the icon ends up clipped by whatever space
    // happens to be there
    private fun updateIconPreviewScale() {
        val scaledSize = (getReferenceIconWidth() * (config.drawerIconScalePercent / 100f)).toInt()
        val params = binding.settingsIconPreview.layoutParams
        params.width = scaledSize
        params.height = scaledSize
        binding.settingsIconPreview.layoutParams = params
    }

    private fun updateIconPreviewLabel() {
        binding.settingsIconPreviewLabel.textSize = config.drawerLabelFontSize.toFloat()
    }
}
