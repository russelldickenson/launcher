package org.fossify.home.activities

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.home.R
import org.fossify.home.databinding.ActivitySettingsBinding
import org.fossify.home.extensions.config
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
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)

        setupUseEnglish()
        setupDoubleTapToLock()
        setupLanguage()
        setupIconSettings()
        setupNotificationBadgeSettings()
        setupDrawerSettings()
        setupHomeScreenSettings()
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
