package org.fossify.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import org.fossify.home.R
import org.fossify.home.activities.SimpleActivity
import org.fossify.home.databinding.ItemHiddenIconBinding
import org.fossify.home.extensions.getProperTextColor
import org.fossify.home.extensions.getThemeAwareTextColor
import org.fossify.home.extensions.portrait
import org.fossify.home.extensions.realScreenSize
import org.fossify.home.models.HiddenIcon

// tapping an icon toggles whether it is selected; the activity unhides the selection on request
class HiddenIconsAdapter(
    private val activity: SimpleActivity,
    private val hiddenIcons: ArrayList<HiddenIcon>,
    private val onSelectionChanged: (selectedCount: Int) -> Unit,
) : RecyclerView.Adapter<HiddenIconsAdapter.ViewHolder>() {

    private val selectedIdentifiers = HashSet<String>()
    private val iconPadding = calculateIconPadding()

    class ViewHolder(val binding: ItemHiddenIconBinding) : RecyclerView.ViewHolder(binding.root)

    fun getSelectedIcons() = hiddenIcons.filter { selectedIdentifiers.contains(it.getIconIdentifier()) }

    fun removeIcons(icons: List<HiddenIcon>) {
        hiddenIcons.removeAll(icons.toSet())
        selectedIdentifiers.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    override fun getItemCount() = hiddenIcons.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemHiddenIconBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val icon = hiddenIcons[position]
        val identifier = icon.getIconIdentifier()
        holder.binding.apply {
            hiddenIconHolder.isSelected = selectedIdentifiers.contains(identifier)
            hiddenIconHolder.setOnClickListener {
                if (!selectedIdentifiers.remove(identifier)) {
                    selectedIdentifiers.add(identifier)
                }

                notifyItemChanged(holder.bindingAdapterPosition)
                onSelectionChanged(selectedIdentifiers.size)
            }

            hiddenIconLabel.text = icon.title
            hiddenIconLabel.setTextColor(activity.getThemeAwareTextColor(activity.getProperTextColor()))
            hiddenIcon.setPadding(iconPadding, iconPadding, iconPadding, 0)

            val factory = DrawableCrossFadeFactory.Builder(CROSS_FADE_MS).setCrossFadeEnabled(true).build()
            Glide.with(activity)
                .load(icon.drawable)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .transition(DrawableTransitionOptions.withCrossFade(factory))
                .into(hiddenIcon)
        }
    }

    private fun calculateIconPadding(): Int {
        val columnCount = activity.resources.getInteger(
            if (activity.portrait) R.integer.portrait_column_count else R.integer.landscape_column_count
        )
        return (activity.realScreenSize.x / columnCount * PADDING_FRACTION).toInt()
    }

    private companion object {
        const val CROSS_FADE_MS = 150
        const val PADDING_FRACTION = 0.1f
    }
}
