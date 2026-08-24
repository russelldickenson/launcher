package org.fossify.home.activities

import android.os.Bundle
import org.fossify.commons.dialogs.ColorPickerDialog
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.adjustForContrast
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.models.RadioItem
import org.fossify.home.R
import org.fossify.home.databinding.ActivityNotificationBadgeSettingsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.darkenTextForLightMode
import org.fossify.home.extensions.isNotificationListenerEnabled
import org.fossify.home.extensions.openNotificationListenerSettings
import org.fossify.home.helpers.NOTIFICATION_BADGE_SHAPE_CIRCLE
import org.fossify.home.helpers.NOTIFICATION_BADGE_SHAPE_ROUNDED_SQUARE
import org.fossify.home.helpers.NOTIFICATION_BADGE_SHAPE_SHARP_SQUARE

class NotificationBadgeSettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityNotificationBadgeSettingsBinding::inflate)
    private var hasPromptedForNotificationAccess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.notificationBadgeSettingsNestedScrollview))
        setupMaterialScrollListener(binding.notificationBadgeSettingsNestedScrollview, binding.notificationBadgeSettingsAppbar)
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.notificationBadgeSettingsAppbar, NavigationIcon.Arrow)

        setupShowNotificationBadges()
        setupShowNotificationCount()
        setupNotificationBadgeColor()
        setupNotificationBadgeShape()
        promptForNotificationAccessIfNeeded()
        updateTextColors(binding.notificationBadgeSettingsHolder)
        darkenTextForLightMode(binding.notificationBadgeSettingsHolder)
        updateNotificationBadgePreview()
    }

    private fun setupShowNotificationBadges() {
        binding.settingsShowNotificationBadges.isChecked = config.showNotificationBadges
        binding.settingsShowNotificationBadgesHolder.setOnClickListener {
            val newValue = !binding.settingsShowNotificationBadges.isChecked
            binding.settingsShowNotificationBadges.isChecked = newValue
            config.showNotificationBadges = newValue

            if (newValue && !isNotificationListenerEnabled()) {
                promptForNotificationAccess()
            }
        }
    }

    private fun promptForNotificationAccessIfNeeded() {
        if (
            config.showNotificationBadges &&
            !isNotificationListenerEnabled() &&
            !hasPromptedForNotificationAccess
        ) {
            hasPromptedForNotificationAccess = true
            promptForNotificationAccess()
        }
    }

    private fun promptForNotificationAccess() {
        toast(R.string.notification_access_required)
        openNotificationListenerSettings()
    }

    private fun setupNotificationBadgeColor() {
        updateNotificationBadgeColorSwatch()
        binding.settingsNotificationBadgeColorHolder.setOnClickListener {
            ColorPickerDialog(this, config.notificationBadgeColor) { wasPositivePressed, newColor ->
                if (wasPositivePressed) {
                    config.notificationBadgeColor = newColor
                    updateNotificationBadgeColorSwatch()
                    updateNotificationBadgePreview()
                }
            }
        }
    }

    private fun updateNotificationBadgeColorSwatch() {
        binding.settingsNotificationBadgeColor.background?.mutate()?.setTint(config.notificationBadgeColor)
    }

    private fun setupNotificationBadgeShape() {
        updateNotificationBadgeShapeLabel()
        binding.settingsNotificationBadgeShapeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(NOTIFICATION_BADGE_SHAPE_CIRCLE, getString(R.string.notification_badge_shape_circle)),
                RadioItem(NOTIFICATION_BADGE_SHAPE_ROUNDED_SQUARE, getString(R.string.notification_badge_shape_rounded_square)),
                RadioItem(NOTIFICATION_BADGE_SHAPE_SHARP_SQUARE, getString(R.string.notification_badge_shape_sharp_square))
            )

            RadioGroupDialog(this, items, config.notificationBadgeShape) {
                val newShape = it as Int
                if (config.notificationBadgeShape != newShape) {
                    config.notificationBadgeShape = newShape
                    updateNotificationBadgeShapeLabel()
                    updateNotificationBadgePreview()
                }
            }
        }
    }

    private fun updateNotificationBadgeShapeLabel() {
        binding.settingsNotificationBadgeShape.text = when (config.notificationBadgeShape) {
            NOTIFICATION_BADGE_SHAPE_ROUNDED_SQUARE -> getString(R.string.notification_badge_shape_rounded_square)
            NOTIFICATION_BADGE_SHAPE_SHARP_SQUARE -> getString(R.string.notification_badge_shape_sharp_square)
            else -> getString(R.string.notification_badge_shape_circle)
        }
    }

    private fun setupShowNotificationCount() {
        binding.settingsShowNotificationCount.isChecked = config.showNotificationCount
        binding.settingsShowNotificationCountHolder.setOnClickListener {
            binding.settingsShowNotificationCount.toggle()
            config.showNotificationCount = binding.settingsShowNotificationCount.isChecked
            updateNotificationBadgePreview()
        }
    }

    private fun updateNotificationBadgePreview() {
        val badgeDrawableRes = when (config.notificationBadgeShape) {
            NOTIFICATION_BADGE_SHAPE_ROUNDED_SQUARE -> R.drawable.notification_badge_rounded_square
            NOTIFICATION_BADGE_SHAPE_SHARP_SQUARE -> R.drawable.notification_badge_sharp_square
            else -> R.drawable.notification_badge_dot
        }

        val badgeColor = config.notificationBadgeColor
        binding.settingsNotificationBadgePreview.apply {
            setBackgroundResource(badgeDrawableRes)
            background?.mutate()?.setTint(badgeColor)
            setTextColor(badgeColor.getContrastColor().adjustForContrast(badgeColor))
            // a representative sample count, just to preview how digits look on the badge
            text = if (config.showNotificationCount) "3" else ""
        }
    }
}
