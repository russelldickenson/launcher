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
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.realScreenSize
import org.fossify.home.R
import org.fossify.home.activities.SimpleActivity
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

class LaunchersAdapter(
    val activity: SimpleActivity,
    val allAppsListener: AllAppsListener,
    val itemClick: (Any) -> Unit
) : ListAdapter<AppLauncher, LaunchersAdapter.ViewHolder>(AppLauncherDiffCallback()),
    RecyclerViewFastScroller.OnPopupTextUpdate {

    private var textColor = activity.getAppDrawerTextColor()
    private var iconPadding = 0
    private var targetIconWidth = 0

    init {
        setHasStableIds(true)
        calculateIconWidth()
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).getLauncherIdentifier().hashCode().toLong()
    }

    fun launchFirstApp(): Boolean {
        val launcher = currentList.firstOrNull() ?: return false
        itemClick(launcher)
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLauncherLabelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(getItem(position))
    }

    override fun submitList(list: MutableList<AppLauncher>?) {
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

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
                    binding.launcherNotificationBadge.setBackgroundResource(badgeDrawableRes)
                    binding.launcherNotificationBadge.background?.mutate()?.setTint(activity.config.notificationBadgeColor)
                    binding.launcherNotificationBadge.setTextColor(activity.config.notificationBadgeColor.getContrastColor())
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

    override fun onChange(position: Int) = currentList.getOrNull(position)?.getBubbleText() ?: ""

    companion object {
        private const val LAUNCHER_SCALE_NORMAL = 1f
        private const val LAUNCHER_SCALE_PRESSED = 1.15f
        private const val LAUNCHER_SCALE_UP_DURATION = 100L
        private const val LAUNCHER_SCALE_DOWN_DURATION = 50L
        private const val LAUNCHER_ALPHA_NORMAL = 255
        private const val LAUNCHER_ALPHA_PRESSED = 220
    }
}

private class AppLauncherDiffCallback : DiffUtil.ItemCallback<AppLauncher>() {
    override fun areItemsTheSame(oldItem: AppLauncher, newItem: AppLauncher): Boolean {
        return oldItem.getLauncherIdentifier().hashCode().toLong() ==
                newItem.getLauncherIdentifier().hashCode().toLong()
    }

    override fun areContentsTheSame(oldItem: AppLauncher, newItem: AppLauncher): Boolean {
        return oldItem.title == newItem.title &&
                oldItem.order == newItem.order &&
                oldItem.thumbnailColor == newItem.thumbnailColor &&
                oldItem.pinned == newItem.pinned &&
                oldItem.drawable != null &&
                newItem.drawable != null
    }
}
