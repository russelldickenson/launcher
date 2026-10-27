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
import com.russelldickenson.lelauncher.models.DrawerTab

class MoveToTabDialog(
    activity: Activity,
    tabs: List<DrawerTab>,
    onAllSelected: () -> Unit,
    onTabSelected: (DrawerTab) -> Unit,
    onNewTabSelected: () -> Unit,
) {

    init {
        val density = activity.resources.displayMetrics.density
        val tint = ColorUtils.setAlphaComponent(activity.getProperPrimaryColor(), TINT_ALPHA)
        val titles = listOf(activity.getString(R.string.all_tab)) + tabs.map { it.title } +
            "+ ${activity.getString(R.string.new_tab)}"

        val adapter = object : ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, titles) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                if (position == titles.lastIndex) {
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
            .setTitle(R.string.move_to_tab)
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> onAllSelected()
                    titles.lastIndex -> onNewTabSelected()
                    else -> onTabSelected(tabs[which - 1])
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private companion object {
        const val TINT_ALPHA = 48
        const val CORNER_RADIUS_DP = 12
        const val INSET_DP = 6
    }
}
