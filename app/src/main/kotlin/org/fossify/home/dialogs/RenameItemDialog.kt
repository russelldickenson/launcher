package org.fossify.home.dialogs

import android.app.Activity
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.fossify.commons.extensions.*
import org.fossify.home.databinding.DialogRenameItemBinding

class RenameItemDialog(
    val activity: Activity,
    val currentTitle: String,
    val onConfirm: (newTitle: String, dialog: DialogInterface) -> Unit
) {

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
