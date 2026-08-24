package org.fossify.home.activities

import android.content.Intent
import android.os.Bundle
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.models.RadioItem
import org.fossify.home.databinding.ActivityDrawerSettingsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.showRadioGroupDialog
import org.fossify.home.helpers.DRAWER_ICON_SCALE_PERCENT_STEP
import org.fossify.home.helpers.MAX_DRAWER_COLUMN_COUNT
import org.fossify.home.helpers.MAX_DRAWER_ICON_SCALE_PERCENT
import org.fossify.home.helpers.MIN_DRAWER_COLUMN_COUNT
import org.fossify.home.helpers.MIN_DRAWER_ICON_SCALE_PERCENT

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

        setupShowFavouritesDivider()
        setupDrawerSearchBar()
        setupOpenKeyboardOnAppDrawer()
        setupCloseAppDrawerOnOtherAppOpen()
        setupColumnCount()
        setupDrawerIconScale()
        setupDrawerLabels()
        setupManageHiddenIcons()
    }

    private fun setupShowFavouritesDivider() {
        binding.settingsShowFavouritesDivider.isChecked = config.showFavouritesDivider
        binding.settingsShowFavouritesDividerHolder.setOnClickListener {
            binding.settingsShowFavouritesDivider.toggle()
            config.showFavouritesDivider = binding.settingsShowFavouritesDivider.isChecked
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

            showRadioGroupDialog(items = items, checkedItemId = currentColumnCount) {
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

            showRadioGroupDialog(items = items, checkedItemId = currentScale) {
                val newScale = it as Int
                if (currentScale != newScale) {
                    config.drawerIconScalePercent = newScale
                    setupDrawerIconScale()
                }
            }
        }
    }

    private fun setupDrawerLabels() {
        binding.settingsDrawerLabelsHolder.setOnClickListener {
            startActivity(Intent(this, DrawerLabelsSettingsActivity::class.java))
        }
    }

    private fun setupManageHiddenIcons() {
        binding.settingsManageHiddenIconsHolder.setOnClickListener {
            startActivity(Intent(this, HiddenIconsActivity::class.java))
        }
    }
}
