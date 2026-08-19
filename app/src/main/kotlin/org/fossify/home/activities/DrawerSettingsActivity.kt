package org.fossify.home.activities

import android.os.Bundle
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.models.RadioItem
import org.fossify.home.databinding.ActivityDrawerSettingsBinding
import org.fossify.home.extensions.config
import org.fossify.home.helpers.MAX_COLUMN_COUNT

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

        setupDrawerColumnCount()
        setupDrawerSearchBar()
        setupOpenKeyboardOnAppDrawer()
        setupCloseAppDrawerOnOtherAppOpen()
        setupShowDrawerAppLabels()
        setupMultilineDrawerAppLabels()
        updateTextColors(binding.drawerSettingsHolder)
    }

    private fun setupDrawerColumnCount() {
        val currentColumnCount = config.drawerColumnCount
        binding.settingsDrawerColumnCount.text = currentColumnCount.toString()
        binding.settingsDrawerColumnCountHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            for (i in 1..MAX_COLUMN_COUNT) {
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
                    setupDrawerColumnCount()
                }
            }
        }
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

    private fun setupMultilineDrawerAppLabels() {
        binding.settingsMultilineDrawerAppLabels.isChecked = config.multilineDrawerAppLabels
        binding.settingsMultilineDrawerAppLabelsHolder.setOnClickListener {
            binding.settingsMultilineDrawerAppLabels.toggle()
            config.multilineDrawerAppLabels = binding.settingsMultilineDrawerAppLabels.isChecked
        }
    }
}
