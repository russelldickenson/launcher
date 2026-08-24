package org.fossify.home.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import org.fossify.commons.extensions.adjustForContrast
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.realScreenSize
import org.fossify.home.R
import org.fossify.home.activities.SimpleActivity
import org.fossify.home.databinding.ItemFavouritesDividerBinding
import org.fossify.home.databinding.ItemLauncherLabelBinding
import org.fossify.home.extensions.animateScale
import org.fossify.home.extensions.config
import org.fossify.home.extensions.getAppDrawerTextColor
import org.fossify.home.extensions.getReferenceIconWidth
import org.fossify.home.helpers.NOTIFICATION_BADGE_SHAPE_ROUNDED_SQUARE
import org.fossify.home.helpers.NOTIFICATION_BADGE_SHAPE_SHARP_SQUARE
import org.fossify.home.helpers.NotificationCache
import org.fossify.home.interfaces.AllAppsListener
import org.fossify.home.models.AppLauncher
import org.fossify.home.models.DrawerGridItem

class LaunchersAdapter(
    val activity: SimpleActivity,
    val allAppsListener: AllAppsListener,
    val itemClick: (Any) -> Unit
) : ListAdapter<DrawerGridItem, RecyclerView.ViewHolder>(DrawerGridItemDiffCallback()),
    RecyclerViewFastScroller.OnPopupTextUpdate {

    private var textColor = activity.getAppDrawerTextColor()
    private var iconPadding = 0
    private var targetIconWidth = 0

    init {
        setHasStableIds(true)
        calculateIconWidth()
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DrawerGridItem.Divider -> VIEW_TYPE_DIVIDER
            is DrawerGridItem.App -> VIEW_TYPE_APP
        }
    }

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is DrawerGridItem.Divider -> -1L
            is DrawerGridItem.App -> item.launcher.getLauncherIdentifier().hashCode().toLong()
        }
    }

    fun launchFirstApp(): Boolean {
        val launcher = (currentList.firstOrNull { it is DrawerGridItem.App } as? DrawerGridItem.App)?.launcher ?: return false
        itemClick(launcher)
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_DIVIDER) {
            val binding = ItemFavouritesDividerBinding.inflate(inflater, parent, false)
            DividerViewHolder(binding.root)
        } else {
            val binding = ItemLauncherLabelBinding.inflate(inflater, parent, false)
            AppViewHolder(binding.root)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AppViewHolder) {
            val item = getItem(position) as? DrawerGridItem.App ?: return
            holder.bindView(item.launcher)
        }
    }

    override fun submitList(list: MutableList<DrawerGridItem>?) {
        calculateIconWidth()
        super.submitList(list)
    }

    // icon size is set directly on the icon's own layoutParams (not a scaleX/scaleY transform),
    // so it never depends on the grid cell's size - and thus never depends on column count -
    // regardless of any imprecision in estimating the cell's actual on-screen size
    private fun calculateIconWidth() {
        targetIconWidth = (activity.getReferenceIconWidth() * (activity.config.drawerIconScalePercent / 100f)).toInt()
        iconPadding = (targetIconWidth * 0.1f).toInt()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTextColor(newTextColor: Int) {
        if (newTextColor != textColor) {
            textColor = newTextColor
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshIconAndLabelSettings() {
        calculateIconWidth()
        notifyDataSetChanged()
    }

    inner class DividerViewHolder(view: View) : RecyclerView.ViewHolder(view)

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        @SuppressLint("ClickableViewAccessibility")
        fun bindView(launcher: AppLauncher): View {
            val binding = ItemLauncherLabelBinding.bind(itemView)
            itemView.apply {
                binding.launcherLabel.text = launcher.title
                binding.launcherLabel.setTextColor(textColor)
                binding.launcherLabel.maxLines = activity.config.drawerLabelMaxLines
                binding.launcherLabel.beVisibleIf(activity.config.showDrawerAppLabels)
                binding.launcherLabel.textSize = activity.config.drawerLabelFontSize.toFloat()
                binding.launcherIcon.setPadding(iconPadding, iconPadding, iconPadding, 0)
                binding.launcherIcon.layoutParams = binding.launcherIcon.layoutParams.apply {
                    width = targetIconWidth
                    height = targetIconWidth
                }

                binding.launcherPinBadge.beVisibleIf(launcher.pinned)
                (binding.launcherPinBadge.layoutParams as RelativeLayout.LayoutParams).apply {
                    marginEnd = iconPadding
                }

                val notificationCount = NotificationCache.notificationCounts[launcher.packageName] ?: 0
                val hasNotification = activity.config.showNotificationBadges && notificationCount > 0
                binding.launcherNotificationBadge.beVisibleIf(hasNotification)
                if (hasNotification) {
                    binding.launcherNotificationBadge.text = if (activity.config.showNotificationCount) {
                        if (notificationCount > 9) "9+" else notificationCount.toString()
                    } else {
                        ""
                    }

                    val badgeDrawableRes = when (activity.config.notificationBadgeShape) {
                        NOTIFICATION_BADGE_SHAPE_ROUNDED_SQUARE -> R.drawable.notification_badge_rounded_square
                        NOTIFICATION_BADGE_SHAPE_SHARP_SQUARE -> R.drawable.notification_badge_sharp_square
                        else -> R.drawable.notification_badge_dot
                    }
                    val badgeColor = activity.config.notificationBadgeColor
                    binding.launcherNotificationBadge.setBackgroundResource(badgeDrawableRes)
                    binding.launcherNotificationBadge.background?.mutate()?.setTint(badgeColor)
                    binding.launcherNotificationBadge.setTextColor(badgeColor.getContrastColor().adjustForContrast(badgeColor))
                }
                (binding.launcherNotificationBadge.layoutParams as RelativeLayout.LayoutParams).apply {
                    marginEnd = iconPadding
                }

                if (launcher.drawable != null && binding.launcherIcon.tag == true) {
                    binding.launcherIcon.setImageDrawable(launcher.drawable)
                } else {
                    val placeholderDrawable = activity.resources.getColoredDrawableWithColor(
                        drawableId = R.drawable.placeholder_drawable,
                        color = launcher.thumbnailColor
                    )
                    Glide.with(activity)
                        .load(launcher.drawable)
                        .placeholder(placeholderDrawable)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(object : DrawableImageViewTarget(binding.launcherIcon) {
                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable>?
                            ) {
                                super.onResourceReady(resource, transition)
                                view.tag = true
                            }
                        })
                }

                setOnClickListener { itemClick(launcher) }
                setOnLongClickListener {
                    val location = IntArray(2)
                    getLocationOnScreen(location)
                    allAppsListener.onAppLauncherLongPressed(
                        x = (location[0] + width / 2).toFloat(),
                        y = location[1].toFloat(),
                        appLauncher = launcher
                    )
                    true
                }

                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            binding.launcherIcon.drawable.alpha = LAUNCHER_ALPHA_PRESSED
                            animateScale(
                                from = LAUNCHER_SCALE_NORMAL,
                                to = LAUNCHER_SCALE_PRESSED,
                                duration = LAUNCHER_SCALE_UP_DURATION
                            )
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            binding.launcherIcon.drawable.alpha = LAUNCHER_ALPHA_NORMAL
                            animateScale(
                                from = LAUNCHER_SCALE_PRESSED,
                                to = LAUNCHER_SCALE_NORMAL,
                                duration = LAUNCHER_SCALE_DOWN_DURATION
                            )
                        }
                    }
                    false
                }
            }

            return itemView
        }
    }

    override fun onChange(position: Int) =
        (currentList.getOrNull(position) as? DrawerGridItem.App)?.launcher?.getBubbleText() ?: ""

    companion object {
        const val VIEW_TYPE_APP = 0
        const val VIEW_TYPE_DIVIDER = 1

        private const val LAUNCHER_SCALE_NORMAL = 1f
        private const val LAUNCHER_SCALE_PRESSED = 1.15f
        private const val LAUNCHER_SCALE_UP_DURATION = 100L
        private const val LAUNCHER_SCALE_DOWN_DURATION = 50L
        private const val LAUNCHER_ALPHA_NORMAL = 255
        private const val LAUNCHER_ALPHA_PRESSED = 220
    }
}

private class DrawerGridItemDiffCallback : DiffUtil.ItemCallback<DrawerGridItem>() {
    override fun areItemsTheSame(oldItem: DrawerGridItem, newItem: DrawerGridItem): Boolean {
        return if (oldItem is DrawerGridItem.App && newItem is DrawerGridItem.App) {
            oldItem.launcher.getLauncherIdentifier().hashCode().toLong() ==
                    newItem.launcher.getLauncherIdentifier().hashCode().toLong()
        } else {
            oldItem == newItem
        }
    }

    override fun areContentsTheSame(oldItem: DrawerGridItem, newItem: DrawerGridItem): Boolean {
        return if (oldItem is DrawerGridItem.App && newItem is DrawerGridItem.App) {
            oldItem.launcher.title == newItem.launcher.title &&
                    oldItem.launcher.order == newItem.launcher.order &&
                    oldItem.launcher.thumbnailColor == newItem.launcher.thumbnailColor &&
                    oldItem.launcher.pinned == newItem.launcher.pinned &&
                    oldItem.launcher.drawable != null &&
                    newItem.launcher.drawable != null
        } else {
            oldItem == newItem
        }
    }
}
