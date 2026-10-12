package com.russelldickenson.lelauncher.dialogs

import android.app.Activity
import android.content.DialogInterface
import android.view.WindowManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.russelldickenson.lelauncher.R
import com.russelldickenson.lelauncher.databinding.DialogRenameItemBinding
import com.russelldickenson.lelauncher.extensions.onGlobalLayout
import com.russelldickenson.lelauncher.extensions.toast

class RenameItemDialog(
    val activity: Activity,
    val currentTitle: String,
    val titleRes: Int = R.string.rename,
    val onConfirm: (newTitle: String, dialog: DialogInterface) -> Unit
) {

    init {
        val binding = DialogRenameItemBinding.inflate(activity.layoutInflater)
        binding.renameItemEdittext.setText(currentTitle)

        if (!activity.isFinishing && !activity.isDestroyed) {
            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(titleRes)
                .setView(binding.root)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create()
            dialog.show()

            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            binding.renameItemEdittext.apply {
                requestFocus()
                onGlobalLayout { setSelection(text?.length ?: 0) }
            }

            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val newTitle = binding.renameItemEdittext.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    onConfirm(newTitle, dialog)
                } else {
                    activity.toast(R.string.value_cannot_be_empty)
                }
            }
        }
    }
}
