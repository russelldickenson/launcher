package org.fossify.home.activities

import android.os.Bundle
import org.fossify.commons.dialogs.ColorPickerDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.models.RadioItem
import org.fossify.home.databinding.ActivityDrawerLabelsSettingsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.getAppDrawerBackgroundColor
import org.fossify.home.extensions.getAppDrawerTextColor
import org.fossify.home.extensions.showRadioGroupDialog
import org.fossify.home.helpers.DRAWER_LABEL_FONT_SIZE_STEP
import org.fossify.home.helpers.DRAWER_LABEL_MAX_LINES_STEP
import org.fossify.home.helpers.MAX_DRAWER_LABEL_FONT_SIZE
import org.fossify.home.helpers.MAX_DRAWER_LABEL_MAX_LINES
import org.fossify.home.helpers.MIN_DRAWER_LABEL_FONT_SIZE
import org.fossify.home.helpers.MIN_DRAWER_LABEL_MAX_LINES

class DrawerLabelsSettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityDrawerLabelsSettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.drawerLabelsSettingsNestedScrollview))
        setupMaterialScrollListener(binding.drawerLabelsSettingsNestedScrollview, binding.drawerLabelsSettingsAppbar)
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.drawerLabelsSettingsAppbar, NavigationIcon.Arrow)

        setupShowDrawerAppLabels()
        setupDrawerLabelFontSize()
        setupDrawerLabelMaxLines()
        setupUseDefaultDrawerColors()
        setupDrawerTextColor()
        setupDrawerBackgroundColor()
    }

    private fun setupShowDrawerAppLabels() {
        binding.settingsShowDrawerAppLabels.isChecked = config.showDrawerAppLabels
        binding.settingsShowDrawerAppLabelsHolder.setOnClickListener {
            binding.settingsShowDrawerAppLabels.toggle()
            config.showDrawerAppLabels = binding.settingsShowDrawerAppLabels.isChecked
            updateLabelSettingsEnabled()
        }
        updateLabelSettingsEnabled()
    }

    private fun updateLabelSettingsEnabled() {
        val enabled = config.showDrawerAppLabels
        val alpha = if (enabled) 1f else 0.5f

        binding.settingsDrawerLabelFontSizeHolder.apply {
            isEnabled = enabled
            this.alpha = alpha
        }

        binding.settingsDrawerLabelMaxLinesHolder.apply {
            isEnabled = enabled
            this.alpha = alpha
        }

        binding.settingsUseDefaultDrawerColorsHolder.apply {
            isEnabled = enabled
            this.alpha = alpha
        }

        binding.settingsDrawerTextColorHolder.apply {
            isEnabled = enabled
            this.alpha = alpha
        }

        binding.settingsDrawerBackgroundColorHolder.apply {
            isEnabled = enabled
            this.alpha = alpha
        }
    }

    private fun setupDrawerLabelFontSize() {
        val currentFontSize = config.drawerLabelFontSize
        binding.settingsDrawerLabelFontSize.text = "${currentFontSize}sp"
        binding.settingsDrawerLabelFontSizeHolder.setOnClickListener {
            if (!config.showDrawerAppLabels) return@setOnClickListener
            val items = ArrayList<RadioItem>()
            var size = MIN_DRAWER_LABEL_FONT_SIZE
            while (size <= MAX_DRAWER_LABEL_FONT_SIZE) {
                items.add(RadioItem(id = size, title = "${size}sp"))
                size += DRAWER_LABEL_FONT_SIZE_STEP
            }

            showRadioGroupDialog(items = items, checkedItemId = currentFontSize) {
                val newFontSize = it as Int
                if (currentFontSize != newFontSize) {
                    config.drawerLabelFontSize = newFontSize
                    setupDrawerLabelFontSize()
                }
            }
        }
    }

    private fun setupDrawerLabelMaxLines() {
        val currentMaxLines = config.drawerLabelMaxLines
        binding.settingsDrawerLabelMaxLines.text = currentMaxLines.toString()
        binding.settingsDrawerLabelMaxLinesHolder.setOnClickListener {
            if (!config.showDrawerAppLabels) return@setOnClickListener
            val items = ArrayList<RadioItem>()
            var lines = MIN_DRAWER_LABEL_MAX_LINES
            while (lines <= MAX_DRAWER_LABEL_MAX_LINES) {
                items.add(RadioItem(id = lines, title = lines.toString()))
                lines += DRAWER_LABEL_MAX_LINES_STEP
            }

            showRadioGroupDialog(items = items, checkedItemId = currentMaxLines) {
                val newMaxLines = it as Int
                if (currentMaxLines != newMaxLines) {
                    config.drawerLabelMaxLines = newMaxLines
                    setupDrawerLabelMaxLines()
                }
            }
        }
    }

    private fun setupUseDefaultDrawerColors() {
        val useDefault = config.useDefaultDrawerColors
        binding.settingsUseDefaultDrawerColors.isChecked = useDefault
        updateDrawerColorHoldersVisibility(useDefault)
        binding.settingsUseDefaultDrawerColorsHolder.setOnClickListener {
            if (!config.showDrawerAppLabels) return@setOnClickListener
            binding.settingsUseDefaultDrawerColors.toggle()
            val isChecked = binding.settingsUseDefaultDrawerColors.isChecked
            config.useDefaultDrawerColors = isChecked
            updateDrawerColorHoldersVisibility(isChecked)
            updateDrawerTextColorSwatch()
            updateDrawerBackgroundColorSwatch()
        }
    }

    private fun updateDrawerColorHoldersVisibility(useDefault: Boolean) {
        binding.settingsDrawerTextColorHolder.beVisibleIf(!useDefault)
        binding.settingsDrawerBackgroundColorHolder.beVisibleIf(!useDefault)
    }

    private fun setupDrawerTextColor() {
        updateDrawerTextColorSwatch()
        binding.settingsDrawerTextColorHolder.setOnClickListener {
            if (!config.showDrawerAppLabels) return@setOnClickListener
            ColorPickerDialog(this, currentDrawerTextColor()) { wasPositivePressed, newColor ->
                if (wasPositivePressed) {
                    config.drawerTextColor = newColor
                    updateDrawerTextColorSwatch()
                }
            }
        }
    }

    private fun updateDrawerTextColorSwatch() {
        binding.settingsDrawerTextColor.background?.mutate()?.setTint(currentDrawerTextColor())
    }

    private fun currentDrawerTextColor() = config.drawerTextColor.takeIf { it != 0 } ?: getAppDrawerTextColor()

    private fun setupDrawerBackgroundColor() {
        updateDrawerBackgroundColorSwatch()
        binding.settingsDrawerBackgroundColorHolder.setOnClickListener {
            if (!config.showDrawerAppLabels) return@setOnClickListener
            ColorPickerDialog(this, currentDrawerBackgroundColor()) { wasPositivePressed, newColor ->
                if (wasPositivePressed) {
                    config.drawerBackgroundColor = newColor
                    updateDrawerBackgroundColorSwatch()
                }
            }
        }
    }

    private fun updateDrawerBackgroundColorSwatch() {
        binding.settingsDrawerBackgroundColor.background?.mutate()?.setTint(currentDrawerBackgroundColor())
    }

    private fun currentDrawerBackgroundColor() = config.drawerBackgroundColor.takeIf { it != 0 } ?: getAppDrawerBackgroundColor()
}
