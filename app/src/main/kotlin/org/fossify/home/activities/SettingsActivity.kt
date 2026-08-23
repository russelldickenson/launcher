package org.fossify.home.activities

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.launchMoreAppsFromUsIntent
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.commons.models.FAQItem
import org.fossify.home.BuildConfig
import org.fossify.home.R
import org.fossify.home.databinding.ActivitySettingsBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.darkenTextForLightMode
import org.fossify.home.receivers.LockDeviceAdminReceiver
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.settingsNestedScrollview))
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsAppbar)
        setupOptionsMenu()
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)
        refreshMenuItems()

        setupUseEnglish()
        setupDoubleTapToLock()
        setupLanguage()
        setupShowFavouritesDivider()
        setupIconSettings()
        setupNotificationBadgeSettings()
        setupDrawerSettings()
        setupHomeScreenSettings()
        updateTextColors(binding.settingsHolder)
        arrayOf(
            binding.settingsAppearanceSettingsLabel,
            binding.settingsGeneralSettingsLabel
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
        darkenTextForLightMode(binding.settingsHolder)
    }

    private fun setupOptionsMenu() {
        binding.settingsToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.about -> launchAbout()
                R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun refreshMenuItems() {
        binding.settingsToolbar.menu.apply {
            findItem(R.id.more_apps_from_us).isVisible =
                !resources.getBoolean(org.fossify.commons.R.bool.hide_google_relations)
        }
    }

    private fun setupUseEnglish() {
        binding.settingsUseEnglishHolder.beVisibleIf(
            beVisible = (config.wasUseEnglishToggled || Locale.getDefault().language != "en")
                    && !isTiramisuPlus()
        )

        binding.settingsUseEnglish.isChecked = config.useEnglish
        binding.settingsUseEnglishHolder.setOnClickListener {
            binding.settingsUseEnglish.toggle()
            config.useEnglish = binding.settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

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

    private fun setupShowFavouritesDivider() {
        binding.settingsShowFavouritesDivider.isChecked = config.showFavouritesDivider
        binding.settingsShowFavouritesDividerHolder.setOnClickListener {
            binding.settingsShowFavouritesDivider.toggle()
            config.showFavouritesDivider = binding.settingsShowFavouritesDivider.isChecked
        }
    }

    private fun setupIconSettings() {
        binding.settingsIconSettingsChevron.applyColorFilter(getProperTextColor())
        binding.settingsIconSettingsHolder.setOnClickListener {
            startActivity(Intent(this, IconSettingsActivity::class.java))
        }
    }

    private fun setupNotificationBadgeSettings() {
        binding.settingsNotificationBadgeSettingsChevron.applyColorFilter(getProperTextColor())
        binding.settingsNotificationBadgeSettingsHolder.setOnClickListener {
            startActivity(Intent(this, NotificationBadgeSettingsActivity::class.java))
        }
    }

    private fun setupDrawerSettings() {
        binding.settingsDrawerSettingsChevron.applyColorFilter(getProperTextColor())
        binding.settingsDrawerSettingsHolder.setOnClickListener {
            startActivity(Intent(this, DrawerSettingsActivity::class.java))
        }
    }

    private fun setupHomeScreenSettings() {
        binding.settingsHomeScreenSettingsChevron.applyColorFilter(getProperTextColor())
        binding.settingsHomeScreenSettingsHolder.setOnClickListener {
            startActivity(Intent(this, HomeScreenSettingsActivity::class.java))
        }
    }

    @SuppressLint("NewApi")
    private fun setupLanguage() {
        binding.settingsLanguage.text = Locale.getDefault().displayLanguage
        binding.settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
        binding.settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun launchAbout() {
        val licenses = 0L
        val faqItems = ArrayList<FAQItem>()

        if (!resources.getBoolean(org.fossify.commons.R.bool.hide_google_relations)) {
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_2_title_commons,
                    text = org.fossify.commons.R.string.faq_2_text_commons
                )
            )
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_6_title_commons,
                    text = org.fossify.commons.R.string.faq_6_text_commons
                )
            )
        }

        startAboutActivity(
            appNameId = R.string.app_name,
            licenseMask = licenses,
            versionName = BuildConfig.VERSION_NAME,
            faqItems = faqItems,
            showFAQBeforeMail = true
        )
    }
}
