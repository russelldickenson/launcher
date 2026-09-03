package org.fossify.home.helpers

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.forEach
import com.google.android.material.color.MaterialColors
import org.fossify.home.R
import org.fossify.home.databinding.ItemPillMenuBinding
import org.fossify.home.databinding.PopupPillMenuBinding
import kotlin.math.max
import kotlin.math.roundToInt

// Pixel-style home long-press menu: a single rounded card holding stacked entries, rather than
// AppCompat's default sheet. Menu inflation still goes through a hidden PopupMenu so callers
// keep the usual Menu / MenuItem APIs.
class PillPopupMenu(
    private val context: Context,
    private val anchorView: View,
    gravity: Int = Gravity.TOP or Gravity.END,
) {
    private val menuHost = PopupMenu(context, anchorView, gravity)
    private var popupWindow: PopupWindow? = null
    private var menuItemClickListener: PopupMenu.OnMenuItemClickListener? = null
    private var dismissListener: PopupMenu.OnDismissListener? = null

    val menu: Menu
        get() = menuHost.menu

    fun inflate(@MenuRes menuRes: Int) {
        menuHost.menuInflater.inflate(menuRes, menuHost.menu)
    }

    fun setOnMenuItemClickListener(listener: PopupMenu.OnMenuItemClickListener?) {
        menuItemClickListener = listener
    }

    fun setOnDismissListener(listener: PopupMenu.OnDismissListener?) {
        dismissListener = listener
    }

    fun show() {
        dismiss()

        val inflater = LayoutInflater.from(context)
        val binding = PopupPillMenuBinding.inflate(inflater)
        val visibleItems = ArrayList<MenuItem>()
        menuHost.menu.forEach { item ->
            if (item.isVisible) {
                visibleItems.add(item)
            }
        }
        if (visibleItems.isEmpty()) {
            return
        }

        val dividerColor = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOutlineVariant,
            Color.TRANSPARENT
        )
        val dividerHeight = context.resources.displayMetrics.density.roundToInt().coerceAtLeast(1)
        visibleItems.forEachIndexed { index, item ->
            if (index > 0) {
                binding.pillMenuContainer.addView(
                    View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dividerHeight
                        ).apply {
                            marginStart = context.resources.getDimensionPixelSize(R.dimen.pill_menu_item_padding_horizontal)
                            marginEnd = marginStart
                        }
                        setBackgroundColor(dividerColor)
                    }
                )
            }
            val itemBinding = ItemPillMenuBinding.inflate(inflater, binding.pillMenuContainer, false)
            bindItem(itemBinding, item)
            binding.pillMenuContainer.addView(itemBinding.root)
        }

        val content = binding.root
        content.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val minWidth = context.resources.getDimensionPixelSize(R.dimen.pill_menu_min_width)
        val width = max(content.measuredWidth, minWidth)

        popupWindow = PopupWindow(content, width, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            elevation = 0f
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setOnDismissListener {
                popupWindow = null
                dismissListener?.onDismiss(menuHost)
            }
            showAsDropDown(anchorView)
        }
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    private fun bindItem(binding: ItemPillMenuBinding, item: MenuItem) {
        binding.pillMenuLabel.text = item.title
        val icon = item.icon
        if (icon != null) {
            binding.pillMenuIcon.setImageDrawable(icon.mutate())
            binding.pillMenuIcon.imageTintList = item.iconTintList
            binding.pillMenuIcon.visibility = View.VISIBLE
        } else {
            binding.pillMenuIcon.visibility = View.GONE
        }
        binding.root.alpha = if (item.isEnabled) 1f else 0.38f

        binding.root.setOnClickListener {
            dismiss()
            if (item.isEnabled) {
                menuItemClickListener?.onMenuItemClick(item)
            }
        }
    }
}
