package org.fossify.home.dialogs

import android.app.Activity
import org.fossify.commons.models.RadioItem
import org.fossify.home.R
import org.fossify.home.extensions.showRadioGroupDialog
import org.fossify.home.models.DrawerFolder

// lets the user pick an existing drawer folder to add an app to, or create a new one - a thin UI
// wrapper around the existing showRadioGroupDialog/RenameItemDialog patterns, with no DB access
// of its own; the caller is responsible for actually persisting the choice
class AddToFolderDialog(
    activity: Activity,
    existingFolders: List<DrawerFolder>,
    val onCreateNewFolder: (title: String) -> Unit,
    val onAddToExistingFolder: (folderId: Long) -> Unit
) {
    init {
        // RadioItem.id is a plain Int, so the real (Long) folder id can't ride along as the id
        // itself - use each item's position instead (0 = "new folder", 1..n = existingFolders'
        // index + 1) and look the real folder id back up by position once a choice is made
        val items = ArrayList<RadioItem>()
        items.add(RadioItem(0, activity.getString(R.string.new_folder)))
        existingFolders.forEachIndexed { index, folder ->
            items.add(RadioItem(index + 1, folder.title))
        }

        activity.showRadioGroupDialog(titleId = R.string.add_to_folder, items = items, checkedItemId = null) { selectedId ->
            val position = selectedId as Int
            if (position == 0) {
                RenameItemDialog(activity, "", titleRes = R.string.new_folder) { title, dialog ->
                    onCreateNewFolder(title)
                    dialog.dismiss()
                }
            } else {
                existingFolders[position - 1].id?.let(onAddToExistingFolder)
            }
        }
    }
}
