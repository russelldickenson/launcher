package com.russelldickenson.lelauncher.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.view.size
import com.russelldickenson.lelauncher.R
import com.russelldickenson.lelauncher.adapters.HiddenIconsAdapter
import com.russelldickenson.lelauncher.databinding.ActivityHiddenIconsBinding
import com.russelldickenson.lelauncher.extensions.beVisibleIf
import com.russelldickenson.lelauncher.extensions.config
import com.russelldickenson.lelauncher.extensions.getAppIcon
import com.russelldickenson.lelauncher.extensions.getContrastColor
import com.russelldickenson.lelauncher.extensions.getDrawableForPackageName
import com.russelldickenson.lelauncher.extensions.getProperBackgroundColor
import com.russelldickenson.lelauncher.extensions.hiddenIconsDB
import com.russelldickenson.lelauncher.extensions.normalizeString
import com.russelldickenson.lelauncher.extensions.viewBinding
import com.russelldickenson.lelauncher.helpers.NavigationIcon
import com.russelldickenson.lelauncher.helpers.ensureBackgroundThread
import com.russelldickenson.lelauncher.models.HiddenIcon
import com.russelldickenson.lelauncher.views.SafeGridLayoutManager

class HiddenIconsActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityHiddenIconsBinding::inflate)
    private var adapter: HiddenIconsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateIcons()

        setupEdgeToEdge(padBottomSystem = listOf(binding.manageHiddenIconsList))
        setupMaterialScrollListener(binding.manageHiddenIconsList, binding.manageHiddenIconsAppbar)

        val layoutManager = binding.manageHiddenIconsList.layoutManager as SafeGridLayoutManager
        layoutManager.spanCount = config.drawerColumnCount
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.manageHiddenIconsAppbar, NavigationIcon.Arrow)
        setupUnhideAction()
    }

    private fun setupUnhideAction() {
        val toolbar = binding.manageHiddenIconsToolbar
        if (toolbar.menu.size == 0) {
            toolbar.inflateMenu(R.menu.menu_hidden_icons)
            toolbar.setOnMenuItemClickListener {
                if (it.itemId == R.id.cab_unhide_icon) {
                    unhideSelection()
                }
                true
            }
        }

        val unhideItem = toolbar.menu.findItem(R.id.cab_unhide_icon)
        unhideItem.icon?.setTint(getProperBackgroundColor().getContrastColor())
        unhideItem.isVisible = (adapter?.getSelectedIcons()?.size ?: 0) > 0
    }

    private fun unhideSelection() {
        val adapter = adapter ?: return
        val selected = adapter.getSelectedIcons()
        ensureBackgroundThread {
            hiddenIconsDB.removeHiddenIcons(selected)
            runOnUiThread {
                adapter.removeIcons(selected)
                binding.manageHiddenIconsPlaceholder.beVisibleIf(adapter.itemCount == 0)
            }
        }
    }

    private fun updateIcons() {
        ensureBackgroundThread {
            val hiddenIcons = hiddenIconsDB.getHiddenIcons().sortedWith(
                compareBy({
                    it.title.normalizeString().lowercase()
                }, {
                    it.packageName
                })
            ).toMutableList() as ArrayList<HiddenIcon>

            val hiddenIconsEmpty = hiddenIcons.isEmpty()
            runOnUiThread {
                binding.manageHiddenIconsPlaceholder.beVisibleIf(hiddenIconsEmpty)
            }

            if (hiddenIcons.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_MAIN, null)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)

                val list = packageManager.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED)
                for (info in list) {
                    val componentInfo = info.activityInfo.applicationInfo
                    val packageName = componentInfo.packageName
                    val activityName = info.activityInfo.name
                    hiddenIcons.firstOrNull { it.getIconIdentifier() == "$packageName/$activityName" }?.apply {
                        drawable = getAppIcon(packageName, activityName) { info.loadIcon(packageManager) }
                    }
                }

                hiddenIcons.firstOrNull { it.packageName == applicationContext.packageName }?.apply {
                    drawable = getDrawableForPackageName(packageName)
                }
            }

            val iconsToRemove = hiddenIcons.filter { it.drawable == null }
            if (iconsToRemove.isNotEmpty()) {
                hiddenIconsDB.removeHiddenIcons(iconsToRemove)
                hiddenIcons.removeAll(iconsToRemove)
            }

            runOnUiThread {
                adapter = HiddenIconsAdapter(this, hiddenIcons) {
                    binding.manageHiddenIconsToolbar.menu.findItem(R.id.cab_unhide_icon)?.isVisible = it > 0
                }
                binding.manageHiddenIconsList.adapter = adapter
            }
        }
    }
}
