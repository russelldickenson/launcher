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
import org.fossify.home.helpers.MAX_ROW_COUNT
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
        setupShowHomeAppLabels()
        setupMultilineHomeAppLabels()
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
