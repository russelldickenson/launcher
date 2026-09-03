package org.fossify.home.activities

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.models.RadioItem
import org.fossify.home.R
import org.fossify.home.databinding.ActivityHomeScreenSettingsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.showRadioGroupDialog
import org.fossify.home.helpers.DRAWER_ICON_SCALE_PERCENT_STEP
import org.fossify.home.helpers.DRAWER_LABEL_MAX_LINES_STEP
import org.fossify.home.helpers.MAX_DRAWER_COLUMN_COUNT
import org.fossify.home.helpers.MAX_DRAWER_ICON_SCALE_PERCENT
import org.fossify.home.helpers.MAX_DRAWER_LABEL_MAX_LINES
import org.fossify.home.helpers.MIN_DRAWER_COLUMN_COUNT
import org.fossify.home.helpers.MIN_DRAWER_ICON_SCALE_PERCENT
import org.fossify.home.helpers.MIN_DRAWER_LABEL_MAX_LINES
import org.fossify.home.receivers.LockDeviceAdminReceiver

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

        setupHomeColumnCount()
        setupHomeIconScale()
        setupShowHomeAppLabels()
        setupHomeLabelMaxLines()
        setupDoubleTapToLock()
    }

    private fun setupHomeColumnCount() {
        val currentColumnCount = config.homeColumnCount
        binding.settingsHomeScreenColumnCount.text = currentColumnCount.toString()
        binding.settingsHomeScreenColumnCountHolder.setOnClickListener {
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
                    config.homeColumnCount = newColumnCount
                    setupHomeColumnCount()
                }
            }
        }
    }

    // this and the two settings below it (show/max-lines) edit the same underlying setting as
    // their app drawer counterparts - config.homeIconScalePercent etc. are aliases, not separate
    // stored values, so a change here shows up on the App drawer settings page too
    private fun setupHomeIconScale() {
        val currentScale = config.homeIconScalePercent
        binding.settingsHomeIconScale.text = "$currentScale%"
        binding.settingsHomeIconScaleHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            var scale = MIN_DRAWER_ICON_SCALE_PERCENT
            while (scale <= MAX_DRAWER_ICON_SCALE_PERCENT) {
                items.add(RadioItem(id = scale, title = "$scale%"))
                scale += DRAWER_ICON_SCALE_PERCENT_STEP
            }

            showRadioGroupDialog(items = items, checkedItemId = currentScale) {
                val newScale = it as Int
                if (currentScale != newScale) {
                    config.homeIconScalePercent = newScale
                    setupHomeIconScale()
                }
            }
        }
    }

    private fun setupShowHomeAppLabels() {
        binding.settingsShowHomeAppLabels.isChecked = config.showHomeAppLabels
        binding.settingsShowHomeAppLabelsHolder.setOnClickListener {
            binding.settingsShowHomeAppLabels.toggle()
            config.showHomeAppLabels = binding.settingsShowHomeAppLabels.isChecked
            updateHomeLabelSettingsEnabled()
        }
        updateHomeLabelSettingsEnabled()
    }

    private fun updateHomeLabelSettingsEnabled() {
        binding.settingsHomeLabelMaxLinesHolder.apply {
            isEnabled = config.showHomeAppLabels
            alpha = if (config.showHomeAppLabels) 1f else 0.5f
        }
    }

    private fun setupHomeLabelMaxLines() {
        val currentMaxLines = config.homeLabelMaxLines
        binding.settingsHomeLabelMaxLines.text = currentMaxLines.toString()
        binding.settingsHomeLabelMaxLinesHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            var lines = MIN_DRAWER_LABEL_MAX_LINES
            while (lines <= MAX_DRAWER_LABEL_MAX_LINES) {
                items.add(RadioItem(id = lines, title = lines.toString()))
                lines += DRAWER_LABEL_MAX_LINES_STEP
            }

            showRadioGroupDialog(items = items, checkedItemId = currentMaxLines) {
                val newMaxLines = it as Int
                if (currentMaxLines != newMaxLines) {
                    config.homeLabelMaxLines = newMaxLines
                    setupHomeLabelMaxLines()
                }
            }
        }
    }

    // moved here from the main Settings screen - double-tap-to-lock is a home screen gesture
    // (tapping empty space on the home screen), so it belongs alongside the home screen's other
    // settings rather than sitting uncategorised at the top of the main Settings list
    private fun setupDoubleTapToLock() {
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        binding.settingsDoubleTapToLock.isChecked = devicePolicyManager.isAdminActive(
            ComponentName(this, LockDeviceAdminReceiver::class.java)
        )

        binding.settingsDoubleTapToLockHolder.setOnClickListener {
            val isLockDeviceAdminActive = devicePolicyManager.isAdminActive(
                ComponentName(this, LockDeviceAdminReceiver::class.java)
            )
            if (isLockDeviceAdminActive) {
                devicePolicyManager.removeActiveAdmin(
                    ComponentName(this, LockDeviceAdminReceiver::class.java)
                )
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    ComponentName(this, LockDeviceAdminReceiver::class.java)
                )
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.lock_device_admin_hint)
                )
                startActivity(intent)
            }
        }
    }
}
