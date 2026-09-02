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
import org.fossify.home.databinding.ItemDrawerSectionHeaderBinding
import org.fossify.home.databinding.ItemFavouritesDividerBinding
import org.fossify.home.databinding.ItemLauncherLabelBinding
import org.fossify.home.databinding.ItemDrawerFolderBinding
import org.fossify.home.extensions.animateScale
import org.fossify.home.extensions.config
import org.fossify.home.extensions.getAppDrawerTextColor
import org.fossify.home.extensions.getReferenceIconWidth
import org.fossify.home.helpers.FolderIconGenerator
import org.fossify.home.helpers.NOTIFICATION_BADGE_SHAPE_ROUNDED_SQUARE
import org.fossify.home.helpers.NOTIFICATION_BADGE_SHAPE_SHARP_SQUARE
import org.fossify.home.helpers.NotificationCache
import org.fossify.home.interfaces.AllAppsListener
import org.fossify.home.models.AppLauncher
import org.fossify.home.models.DrawerFolder
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
    private var isSelectionModeActive = false
    private var selectedIdentifiers: Set<String> = emptySet()

    init {
        setHasStableIds(true)
        calculateIconWidth()
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DrawerGridItem.Header -> VIEW_TYPE_HEADER
            is DrawerGridItem.Divider -> VIEW_TYPE_DIVIDER
            is DrawerGridItem.App -> VIEW_TYPE_APP
            is DrawerGridItem.Folder -> VIEW_TYPE_FOLDER
        }
    }

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is DrawerGridItem.Header -> -2L
            is DrawerGridItem.Divider -> -1L
            is DrawerGridItem.App -> item.launcher.getLauncherIdentifier().hashCode().toLong()
            is DrawerGridItem.Folder -> "folder:${item.folder.id}".hashCode().toLong()
        }
    }

    fun launchFirstApp(): Boolean {
        val launcher = (currentList.firstOrNull { it is DrawerGridItem.App } as? DrawerGridItem.App)?.launcher ?: return false
        itemClick(launcher)
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemDrawerSectionHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_DIVIDER -> {
                val binding = ItemFavouritesDividerBinding.inflate(inflater, parent, false)
                DividerViewHolder(binding.root)
            }
            VIEW_TYPE_FOLDER -> {
                val binding = ItemDrawerFolderBinding.inflate(inflater, parent, false)
                FolderViewHolder(binding.root)
            }
            else -> {
                val binding = ItemLauncherLabelBinding.inflate(inflater, parent, false)
                AppViewHolder(binding.root)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val item = getItem(position) as? DrawerGridItem.Header ?: return
                holder.bind(item.titleRes)
            }
            is AppViewHolder -> {
                val item = getItem(position) as? DrawerGridItem.App ?: return
                holder.bindView(item.launcher)
            }
            is FolderViewHolder -> {
                val item = getItem(position) as? DrawerGridItem.Folder ?: return
                holder.bindView(item.folder, item.members)
            }
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

    // exposes the already-computed icon size for the drag-shadow view to match, rather than
    // duplicating the calculateIconWidth() formula elsewhere
    fun getIconSizePx() = targetIconWidth

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

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectionMode(active: Boolean, selected: Set<String>) {
        isSelectionModeActive = active
        selectedIdentifiers = selected
        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(private val binding: ItemDrawerSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(titleRes: Int) {
            binding.sectionHeaderLabel.setText(titleRes)
        }
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

                val isSelected = isSelectionModeActive && selectedIdentifiers.contains(launcher.getLauncherIdentifier())
                binding.launcherSelectionCheckmark.beVisibleIf(isSelectionModeActive)
                binding.launcherSelectionCheckmark.setImageResource(
                    if (isSelected) org.fossify.commons.R.drawable.ic_check_circle_vector else 0
                )
                binding.launcherIcon.alpha = if (isSelectionModeActive && !isSelected) LAUNCHER_ALPHA_DESELECTED else 1f

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

                setOnClickListener {
                    if (isSelectionModeActive) {
                        allAppsListener.onAppLauncherSelectionToggled(launcher)
                    } else {
                        itemClick(launcher)
                    }
                }
                setOnLongClickListener {
                    if (isSelectionModeActive) {
                        // long-pressing an already-checked icon arms a drag of the whole
                        // selection; long-pressing an unchecked one does nothing special here
                        if (selectedIdentifiers.contains(launcher.getLauncherIdentifier())) {
                            allAppsListener.onSelectionDragRequested(launcher)
                            return@setOnLongClickListener true
                        }
                        return@setOnLongClickListener false
                    }

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

    inner class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(folder: DrawerFolder, members: List<AppLauncher>) {
            val binding = ItemDrawerFolderBinding.bind(itemView)
            binding.drawerFolderLabel.text = folder.title
            binding.drawerFolderLabel.setTextColor(textColor)
            binding.drawerFolderLabel.maxLines = activity.config.drawerLabelMaxLines
            binding.drawerFolderLabel.beVisibleIf(activity.config.showDrawerAppLabels)
            binding.drawerFolderLabel.textSize = activity.config.drawerLabelFontSize.toFloat()
            binding.drawerFolderIcon.layoutParams = binding.drawerFolderIcon.layoutParams.apply {
                width = targetIconWidth
                height = targetIconWidth
            }

            binding.drawerFolderIcon.setImageDrawable(
                FolderIconGenerator.generate(activity, members.map { it.drawable }, targetIconWidth)
            )

            // folders can't be selected into another folder, so they're just dimmed and inert
            // while picking apps to add
            itemView.alpha = if (isSelectionModeActive) LAUNCHER_ALPHA_DESELECTED else 1f
            itemView.setOnClickListener {
                if (!isSelectionModeActive) {
                    allAppsListener.onFolderClicked(folder)
                }
            }
            itemView.setOnLongClickListener {
                if (isSelectionModeActive) {
                    return@setOnLongClickListener false
                }

                val location = IntArray(2)
                itemView.getLocationOnScreen(location)
                allAppsListener.onFolderLongPressed(
                    x = (location[0] + itemView.width / 2).toFloat(),
                    y = location[1].toFloat(),
                    folder = folder
                )
                true
            }
        }
    }

    override fun onChange(position: Int) =
        (currentList.getOrNull(position) as? DrawerGridItem.App)?.launcher?.getBubbleText() ?: ""

    companion object {
        const val VIEW_TYPE_APP = 0
        const val VIEW_TYPE_DIVIDER = 1
        const val VIEW_TYPE_HEADER = 2
        const val VIEW_TYPE_FOLDER = 3

        private const val LAUNCHER_SCALE_NORMAL = 1f
        private const val LAUNCHER_SCALE_PRESSED = 1.15f
        private const val LAUNCHER_SCALE_UP_DURATION = 100L
        private const val LAUNCHER_SCALE_DOWN_DURATION = 50L
        private const val LAUNCHER_ALPHA_NORMAL = 255
        private const val LAUNCHER_ALPHA_PRESSED = 220

        // View.alpha (0f-1f), unlike the Drawable.alpha (0-255) constants above - used to dim
        // items that aren't selected/selectable while a folder selection is in progress
        private const val LAUNCHER_ALPHA_DESELECTED = 0.4f
    }
}

private class DrawerGridItemDiffCallback : DiffUtil.ItemCallback<DrawerGridItem>() {
    override fun areItemsTheSame(oldItem: DrawerGridItem, newItem: DrawerGridItem): Boolean {
        return if (oldItem is DrawerGridItem.App && newItem is DrawerGridItem.App) {
            oldItem.launcher.getLauncherIdentifier().hashCode().toLong() ==
                    newItem.launcher.getLauncherIdentifier().hashCode().toLong()
        } else if (oldItem is DrawerGridItem.Folder && newItem is DrawerGridItem.Folder) {
            oldItem.folder.id == newItem.folder.id
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
        } else if (oldItem is DrawerGridItem.Folder && newItem is DrawerGridItem.Folder) {
            // AppLauncher.equals() only compares packageName, so relying on the default List
            // equality here would miss a member's drawable actually changing (e.g. after an icon
            // shape/pack setting change) - compare titles/membership explicitly instead, and
            // require every member to already have a drawable so the preview icon isn't drawn
            // half-built while icons are still loading
            oldItem.folder.title == newItem.folder.title &&
                    oldItem.members.map { it.getLauncherIdentifier() } == newItem.members.map { it.getLauncherIdentifier() } &&
                    newItem.members.all { it.drawable != null }
        } else {
            oldItem == newItem
        }
    }
}
