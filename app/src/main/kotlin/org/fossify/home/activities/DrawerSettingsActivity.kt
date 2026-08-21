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
import org.fossify.home.helpers.MAX_DRAWER_LABEL_MAX_LINES
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
