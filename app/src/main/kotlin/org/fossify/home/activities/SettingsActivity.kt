package org.fossify.home.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.home.databinding.ActivitySettingsBinding
import org.fossify.home.extensions.config
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.settingsNestedScrollview))
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsAppbar)
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)

        setupIconSettings()
        setupNotificationBadgeSettings()
        setupDrawerSettings()
        setupHomeScreenSettings()
        setupUseEnglish()
        setupLanguage()
    }

    private fun setupUseEnglish() {
        val showUseEnglish = (config.wasUseEnglishToggled || Locale.getDefault().language != "en")
                && !isTiramisuPlus()
        binding.settingsUseEnglishHolder.beVisibleIf(showUseEnglish)
        // this row's own divider would otherwise stay put and stack on top of the next visible
        // row's divider, rendering as one noticeably thicker line
        binding.settingsUseEnglishDivider.root.beVisibleIf(showUseEnglish)

        binding.settingsUseEnglish.isChecked = config.useEnglish
        binding.settingsUseEnglishHolder.setOnClickListener {
            binding.settingsUseEnglish.toggle()
            config.useEnglish = binding.settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupIconSettings() {
        binding.settingsIconSettingsHolder.setOnClickListener {
            startActivity(Intent(this, IconSettingsActivity::class.java))
        }
    }

    private fun setupNotificationBadgeSettings() {
        binding.settingsNotificationBadgeSettingsHolder.setOnClickListener {
            startActivity(Intent(this, NotificationBadgeSettingsActivity::class.java))
        }
    }

    private fun setupDrawerSettings() {
        binding.settingsDrawerSettingsHolder.setOnClickListener {
            startActivity(Intent(this, DrawerSettingsActivity::class.java))
        }
    }

    private fun setupHomeScreenSettings() {
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
}
