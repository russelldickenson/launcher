package org.fossify.home.dialogs

import android.app.Activity
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.home.databinding.DialogRenameItemBinding
import org.fossify.home.extensions.homeScreenGridItemsDB
import org.fossify.home.models.HomeScreenGridItem

class RenameItemDialog(
    val activity: Activity,
    val currentTitle: String,
    val onConfirm: (newTitle: String, dialog: DialogInterface) -> Unit
) {

    constructor(activity: Activity, item: HomeScreenGridItem, callback: () -> Unit) : this(
        activity = activity,
        currentTitle = item.title,
        onConfirm = { newTitle, dialog ->
            ensureBackgroundThread {
                val result = activity.homeScreenGridItemsDB.updateItemTitle(newTitle, item.id!!)
                if (result == 1) {
                    callback()
                    dialog.dismiss()
                } else {
                    activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                }
            }
        }
    )

    init {
        val binding = DialogRenameItemBinding.inflate(activity.layoutInflater)
        val view = binding.root
        binding.renameItemEdittext.setText(currentTitle)

        MaterialAlertDialogBuilder(activity)
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, org.fossify.commons.R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(binding.renameItemEdittext)
                    alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                        val newTitle = binding.renameItemEdittext.value
                        if (newTitle.isNotEmpty()) {
                            onConfirm(newTitle, alertDialog)
                        } else {
                            activity.toast(org.fossify.commons.R.string.value_cannot_be_empty)
                        }
                    }
                }
            }
    }
}
