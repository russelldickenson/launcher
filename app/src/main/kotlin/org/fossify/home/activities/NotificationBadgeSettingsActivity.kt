package org.fossify.home.activities

import android.os.Bundle
import org.fossify.commons.extensions.adjustForContrast
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.home.R
import org.fossify.home.databinding.ActivityNotificationBadgeSettingsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.isNotificationListenerEnabled
import org.fossify.home.extensions.openNotificationListenerSettings
import org.fossify.home.extensions.showColorPickerDialog
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
            showColorPickerDialog(config.notificationBadgeColor) { newColor ->
                config.notificationBadgeColor = newColor
                updateNotificationBadgeColorSwatch()
                updateNotificationBadgePreview()
            }
        }
    }

    private fun updateNotificationBadgeColorSwatch() {
        binding.settingsNotificationBadgeColor.background?.mutate()?.setTint(config.notificationBadgeColor)
    }

    private fun setupNotificationBadgeShape() {
        val chipForShape = mapOf(
            NOTIFICATION_BADGE_SHAPE_CIRCLE to binding.settingsNotificationBadgeShapeCircle,
            NOTIFICATION_BADGE_SHAPE_ROUNDED_SQUARE to binding.settingsNotificationBadgeShapeRoundedSquare,
            NOTIFICATION_BADGE_SHAPE_SHARP_SQUARE to binding.settingsNotificationBadgeShapeSharpSquare,
        )
        (chipForShape[config.notificationBadgeShape] ?: binding.settingsNotificationBadgeShapeCircle).isChecked = true

        binding.settingsNotificationBadgeShapeChips.setOnCheckedStateChangeListener { _, checkedIds ->
            val newShape = chipForShape.entries.firstOrNull { it.value.id == checkedIds.firstOrNull() }?.key
                ?: return@setOnCheckedStateChangeListener
            if (config.notificationBadgeShape != newShape) {
                config.notificationBadgeShape = newShape
                updateNotificationBadgePreview()
            }
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
