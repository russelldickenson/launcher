package com.russelldickenson.lelauncher.dialogs

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.russelldickenson.lelauncher.R
import com.russelldickenson.lelauncher.extensions.getProperPrimaryColor
import com.russelldickenson.lelauncher.models.DrawerFolder
import com.russelldickenson.lelauncher.models.DrawerTab

// folders are optional: the single-app "Move to tab" menu passes none, the multi-select flow passes
// them so the chosen apps can go to either a tab or a folder
class MoveToTabDialog(
    activity: Activity,
    tabs: List<DrawerTab>,
    onAllSelected: () -> Unit,
    onTabSelected: (DrawerTab) -> Unit,
    onNewTabSelected: () -> Unit,
    folders: List<DrawerFolder>? = null,
    onFolderSelected: (DrawerFolder) -> Unit = {},
    onNewFolderSelected: () -> Unit = {},
) {

    private class Row(val title: String, val isPill: Boolean, val action: () -> Unit)

    init {
        val density = activity.resources.displayMetrics.density
        val tint = ColorUtils.setAlphaComponent(activity.getProperPrimaryColor(), TINT_ALPHA)

        val rows = mutableListOf<Row>()
        rows.add(Row(activity.getString(R.string.all_tab), true, onAllSelected))
        tabs.forEach { tab -> rows.add(Row(tab.title, true) { onTabSelected(tab) }) }
        rows.add(Row("+ ${activity.getString(R.string.new_tab)}", false, onNewTabSelected))
        if (folders != null) {
            folders.forEach { folder -> rows.add(Row(folder.title, true) { onFolderSelected(folder) }) }
            rows.add(Row("+ ${activity.getString(R.string.new_folder)}", false, onNewFolderSelected))
        }

        val adapter = object : ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, rows.map { it.title }) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                if (!rows[position].isPill) {
                    view.background = null
                } else {
                    val pill = GradientDrawable().apply {
                        setColor(tint)
                        cornerRadius = CORNER_RADIUS_DP * density
                    }
                    view.background = InsetDrawable(pill, (INSET_DP * density).toInt())
                }
                return view
            }
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(if (folders != null) R.string.move_to else R.string.move_to_tab)
            .setAdapter(adapter) { _, which -> rows[which].action() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private companion object {
        const val TINT_ALPHA = 48
        const val CORNER_RADIUS_DP = 12
        const val INSET_DP = 6
    }
}
