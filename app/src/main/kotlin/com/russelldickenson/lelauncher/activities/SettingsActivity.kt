package com.russelldickenson.lelauncher.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.russelldickenson.lelauncher.BuildConfig
import com.russelldickenson.lelauncher.R
import com.russelldickenson.lelauncher.databinding.ActivitySettingsBinding
import com.russelldickenson.lelauncher.extensions.config
import com.russelldickenson.lelauncher.extensions.showErrorToast
import com.russelldickenson.lelauncher.extensions.toast
import com.russelldickenson.lelauncher.extensions.viewBinding
import com.russelldickenson.lelauncher.helpers.NavigationIcon
import com.russelldickenson.lelauncher.helpers.SettingsBackup
import com.russelldickenson.lelauncher.helpers.ensureBackgroundThread

class SettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { exportSettingsTo(it) }
        }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { importSettingsFrom(it) }
        }

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
        setupExportImport()
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

    private fun setupExportImport() {
        binding.settingsExportHolder.setOnClickListener {
            exportLauncher.launch("le-launcher-settings.json")
        }
        binding.settingsImportHolder.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
        }
    }

    private fun exportSettingsTo(uri: Uri) {
        ensureBackgroundThread {
            try {
                val json = SettingsBackup.export(config)
                contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
                runOnUiThread { toast(R.string.settings_exported) }
            } catch (e: Exception) {
                runOnUiThread { showErrorToast(e) }
            }
        }
    }

    private fun importSettingsFrom(uri: Uri) {
        ensureBackgroundThread {
            try {
                val json = contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                val imported = SettingsBackup.import(config, json)
                runOnUiThread { toast(if (imported) R.string.settings_imported else R.string.settings_import_invalid) }
            } catch (e: Exception) {
                runOnUiThread { showErrorToast(e) }
            }
        }
    }
}
