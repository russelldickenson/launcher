package org.fossify.home.activities

import android.os.Bundle
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.models.RadioItem
import org.fossify.home.databinding.ActivityHomeScreenSettingsBinding
import org.fossify.home.extensions.config
import org.fossify.home.helpers.MAX_COLUMN_COUNT
import org.fossify.home.helpers.MAX_ROW_COUNT
import org.fossify.home.helpers.MIN_COLUMN_COUNT
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
        setupHomeColumnCount()
        setupShowHomeAppLabels()
        setupMultilineHomeAppLabels()
        updateTextColors(binding.homeScreenSettingsHolder)
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

    private fun setupHomeColumnCount() {
        val currentColumnCount = config.homeColumnCount
        binding.settingsHomeScreenColumnCount.text = currentColumnCount.toString()
        binding.settingsHomeScreenColumnCountHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            for (i in MIN_COLUMN_COUNT..MAX_COLUMN_COUNT) {
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
                    config.homeColumnCount = newColumnCount
                    setupHomeColumnCount()
                }
            }
        }
    }

    private fun setupShowHomeAppLabels() {
        binding.settingsShowHomeAppLabels.isChecked = config.showHomeAppLabels
        binding.settingsShowHomeAppLabelsHolder.setOnClickListener {
            binding.settingsShowHomeAppLabels.toggle()
            config.showHomeAppLabels = binding.settingsShowHomeAppLabels.isChecked
        }
    }

    private fun setupMultilineHomeAppLabels() {
        binding.settingsMultilineHomeAppLabels.isChecked = config.multilineHomeAppLabels
        binding.settingsMultilineHomeAppLabelsHolder.setOnClickListener {
            binding.settingsMultilineHomeAppLabels.toggle()
            config.multilineHomeAppLabels = binding.settingsMultilineHomeAppLabels.isChecked
        }
    }
}
