package org.fossify.home.activities

import android.content.Intent
import android.os.Bundle
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.models.RadioItem
import org.fossify.home.databinding.ActivityDrawerSettingsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.darkenTextForLightMode
import org.fossify.home.helpers.DRAWER_LABEL_MAX_LINES_STEP
import org.fossify.home.helpers.DRAWER_ICON_SCALE_PERCENT_STEP
import org.fossify.home.helpers.DRAWER_LABEL_FONT_SIZE_STEP
import org.fossify.home.helpers.MAX_DRAWER_COLUMN_COUNT
import org.fossify.home.helpers.MAX_DRAWER_ICON_SCALE_PERCENT
import org.fossify.home.helpers.MAX_DRAWER_LABEL_FONT_SIZE
import org.fossify.home.helpers.MAX_DRAWER_LABEL_MAX_LINES
import org.fossify.home.helpers.MIN_DRAWER_COLUMN_COUNT
import org.fossify.home.helpers.MIN_DRAWER_ICON_SCALE_PERCENT
import org.fossify.home.helpers.MIN_DRAWER_LABEL_FONT_SIZE
import org.fossify.home.helpers.MIN_DRAWER_LABEL_MAX_LINES

class DrawerSettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityDrawerSettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.drawerSettingsNestedScrollview))
        setupMaterialScrollListener(binding.drawerSettingsNestedScrollview, binding.drawerSettingsAppbar)
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.drawerSettingsAppbar, NavigationIcon.Arrow)

        setupDrawerSearchBar()
        setupOpenKeyboardOnAppDrawer()
        setupCloseAppDrawerOnOtherAppOpen()
        setupShowDrawerAppLabels()
        setupColumnCount()
        setupDrawerIconScale()
        setupDrawerLabelFontSize()
        setupDrawerLabelMaxLines()
        setupManageHiddenIcons()
        updateTextColors(binding.drawerSettingsHolder)
        darkenTextForLightMode(binding.drawerSettingsHolder)
    }

    private fun setupDrawerSearchBar() {
        val showSearchBar = config.showSearchBar
        binding.settingsShowSearchBar.isChecked = showSearchBar
        binding.settingsDrawerSearchHolder.setOnClickListener {
            binding.settingsShowSearchBar.toggle()
            config.showSearchBar = binding.settingsShowSearchBar.isChecked
            binding.settingsOpenKeyboardOnAppDrawerHolder.beVisibleIf(config.showSearchBar)
        }
    }

    private fun setupOpenKeyboardOnAppDrawer() {
        binding.settingsOpenKeyboardOnAppDrawerHolder.beVisibleIf(config.showSearchBar)
        binding.settingsOpenKeyboardOnAppDrawer.isChecked = config.autoShowKeyboardInAppDrawer
        binding.settingsOpenKeyboardOnAppDrawerHolder.setOnClickListener {
            binding.settingsOpenKeyboardOnAppDrawer.toggle()
            config.autoShowKeyboardInAppDrawer = binding.settingsOpenKeyboardOnAppDrawer.isChecked
        }
    }

    private fun setupCloseAppDrawerOnOtherAppOpen() {
        binding.settingsCloseAppDrawerOnOtherApp.isChecked = config.closeAppDrawer
        binding.settingsCloseAppDrawerOnOtherAppHolder.setOnClickListener {
            binding.settingsCloseAppDrawerOnOtherApp.toggle()
            config.closeAppDrawer = binding.settingsCloseAppDrawerOnOtherApp.isChecked
        }
    }

    private fun setupShowDrawerAppLabels() {
        binding.settingsShowDrawerAppLabels.isChecked = config.showDrawerAppLabels
        binding.settingsShowDrawerAppLabelsHolder.setOnClickListener {
            binding.settingsShowDrawerAppLabels.toggle()
            config.showDrawerAppLabels = binding.settingsShowDrawerAppLabels.isChecked
        }
    }

    private fun setupColumnCount() {
        val currentColumnCount = config.drawerColumnCount
        binding.settingsColumnCount.text = currentColumnCount.toString()
        binding.settingsColumnCountHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            for (i in MIN_DRAWER_COLUMN_COUNT..MAX_DRAWER_COLUMN_COUNT) {
                items.add(
                    RadioItem(
                        id = i,
                        title = resources.getQuantityString(
                            org.fossify.commons.R.plurals.column_counts, i, i
                        )
                    )
                )
            }

            RadioGroupDialog(this, items, currentColumnCount) {
                val newColumnCount = it as Int
                if (currentColumnCount != newColumnCount) {
                    config.drawerColumnCount = newColumnCount
                    setupColumnCount()
                }
            }
        }
    }

    private fun setupDrawerIconScale() {
        val currentScale = config.drawerIconScalePercent
        binding.settingsDrawerIconScale.text = "$currentScale%"
        binding.settingsDrawerIconScaleHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            var scale = MIN_DRAWER_ICON_SCALE_PERCENT
            while (scale <= MAX_DRAWER_ICON_SCALE_PERCENT) {
                items.add(RadioItem(id = scale, title = "$scale%"))
                scale += DRAWER_ICON_SCALE_PERCENT_STEP
            }

            RadioGroupDialog(this, items, currentScale) {
                val newScale = it as Int
                if (currentScale != newScale) {
                    config.drawerIconScalePercent = newScale
                    setupDrawerIconScale()
                }
            }
        }
    }

    private fun setupDrawerLabelFontSize() {
        val currentFontSize = config.drawerLabelFontSize
        binding.settingsDrawerLabelFontSize.text = "${currentFontSize}sp"
        binding.settingsDrawerLabelFontSizeHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            var size = MIN_DRAWER_LABEL_FONT_SIZE
            while (size <= MAX_DRAWER_LABEL_FONT_SIZE) {
                items.add(RadioItem(id = size, title = "${size}sp"))
                size += DRAWER_LABEL_FONT_SIZE_STEP
            }

            RadioGroupDialog(this, items, currentFontSize) {
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
            val items = ArrayList<RadioItem>()
            var lines = MIN_DRAWER_LABEL_MAX_LINES
            while (lines <= MAX_DRAWER_LABEL_MAX_LINES) {
                items.add(RadioItem(id = lines, title = lines.toString()))
                lines += DRAWER_LABEL_MAX_LINES_STEP
            }

            RadioGroupDialog(this, items, currentMaxLines) {
                val newMaxLines = it as Int
                if (currentMaxLines != newMaxLines) {
                    config.drawerLabelMaxLines = newMaxLines
                    setupDrawerLabelMaxLines()
                }
            }
        }
    }

    private fun setupManageHiddenIcons() {
        binding.settingsManageHiddenIconsHolder.setOnClickListener {
            startActivity(Intent(this, HiddenIconsActivity::class.java))
        }
    }
}
