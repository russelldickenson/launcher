package org.fossify.home.activities

import android.os.Bundle
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.models.RadioItem
import org.fossify.home.databinding.ActivityHomeScreenSettingsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.darkenTextForLightMode
import org.fossify.home.helpers.HOME_ICON_SCALE_PERCENT_STEP
import org.fossify.home.helpers.HOME_LABEL_MAX_LINES_STEP
import org.fossify.home.helpers.MAX_HOME_ICON_SCALE_PERCENT
import org.fossify.home.helpers.MAX_HOME_LABEL_MAX_LINES
import org.fossify.home.helpers.MAX_ROW_COUNT
import org.fossify.home.helpers.MIN_HOME_ICON_SCALE_PERCENT
import org.fossify.home.helpers.MIN_HOME_LABEL_MAX_LINES
import org.fossify.home.helpers.MIN_ROW_COUNT

class HomeScreenSettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityHomeScreenSettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.homeScreenSettingsNestedScrollview))
        setupMaterialScrollListener(binding.homeScreenSettingsNestedScrollview, binding.homeScreenSettingsAppbar)
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.homeScreenSettingsAppbar, NavigationIcon.Arrow)

        setupHomeRowCount()
        setupHomeIconScale()
        setupHomeLabelMaxLines()
        updateTextColors(binding.homeScreenSettingsHolder)
        darkenTextForLightMode(binding.homeScreenSettingsHolder)
    }

    private fun setupHomeRowCount() {
        val currentRowCount = config.homeRowCount
        binding.settingsHomeScreenRowCount.text = currentRowCount.toString()
        binding.settingsHomeScreenRowCountHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            for (i in MIN_ROW_COUNT..MAX_ROW_COUNT) {
                items.add(
                    RadioItem(
                        id = i,
                        title = resources.getQuantityString(
                            org.fossify.commons.R.plurals.row_counts, i, i
                        )
                    )
                )
            }

            RadioGroupDialog(this, items, currentRowCount) {
                val newRowCount = it as Int
                if (currentRowCount != newRowCount) {
                    config.homeRowCount = newRowCount
                    setupHomeRowCount()
                }
            }
        }
    }

    private fun setupHomeIconScale() {
        val currentScale = config.homeIconScalePercent
        binding.settingsHomeIconScale.text = "$currentScale%"
        binding.settingsHomeIconScaleHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            var scale = MIN_HOME_ICON_SCALE_PERCENT
            while (scale <= MAX_HOME_ICON_SCALE_PERCENT) {
                items.add(RadioItem(id = scale, title = "$scale%"))
                scale += HOME_ICON_SCALE_PERCENT_STEP
            }

            RadioGroupDialog(this, items, currentScale) {
                val newScale = it as Int
                if (currentScale != newScale) {
                    config.homeIconScalePercent = newScale
                    setupHomeIconScale()
                }
            }
        }
    }

    private fun setupHomeLabelMaxLines() {
        val currentMaxLines = config.homeLabelMaxLines
        binding.settingsHomeLabelMaxLines.text = currentMaxLines.toString()
        binding.settingsHomeLabelMaxLinesHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            var lines = MIN_HOME_LABEL_MAX_LINES
            while (lines <= MAX_HOME_LABEL_MAX_LINES) {
                items.add(RadioItem(id = lines, title = lines.toString()))
                lines += HOME_LABEL_MAX_LINES_STEP
            }

            RadioGroupDialog(this, items, currentMaxLines) {
                val newMaxLines = it as Int
                if (currentMaxLines != newMaxLines) {
                    config.homeLabelMaxLines = newMaxLines
                    setupHomeLabelMaxLines()
                }
            }
        }
    }
}
