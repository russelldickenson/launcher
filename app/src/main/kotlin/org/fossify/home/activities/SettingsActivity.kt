package org.fossify.home.activities

import android.content.Intent
import android.os.Bundle
import org.fossify.home.extensions.viewBinding
import org.fossify.home.helpers.NavigationIcon
import org.fossify.home.BuildConfig
import org.fossify.home.R
import org.fossify.home.databinding.ActivitySettingsBinding
import org.fossify.home.extensions.config

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
        setupVersion()
    }

    private fun setupVersion() {
        binding.settingsVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
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
}
