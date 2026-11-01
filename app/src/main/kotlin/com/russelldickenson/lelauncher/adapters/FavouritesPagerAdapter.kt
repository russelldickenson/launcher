package com.russelldickenson.lelauncher.adapters

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.russelldickenson.lelauncher.models.AppLauncher
import com.russelldickenson.lelauncher.models.DrawerGridItem
import com.russelldickenson.lelauncher.views.SafeGridLayoutManager

// one swipeable page per screenful of favourites; each page is a small non-scrolling grid, so
// rows line up exactly like the main drawer grid does
class FavouritesPagerAdapter(
    private val createPageAdapter: () -> LaunchersAdapter,
    private val onPageHeight: (Int) -> Unit,
) : RecyclerView.Adapter<FavouritesPagerAdapter.PageViewHolder>() {

    private var pages = emptyList<List<AppLauncher>>()
    private var columns = 1

    @SuppressLint("NotifyDataSetChanged")
    fun submitPages(pages: List<List<AppLauncher>>, columns: Int) {
        this.pages = pages
        this.columns = columns
        notifyDataSetChanged()
    }

    override fun getItemCount() = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val grid = RecyclerView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isNestedScrollingEnabled = false
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            itemAnimator = null
            layoutManager = SafeGridLayoutManager(context, 1)
            addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ -> onPageHeight(bottom - top) }
        }
        return PageViewHolder(grid)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position], columns)
    }

    inner class PageViewHolder(private val grid: RecyclerView) : RecyclerView.ViewHolder(grid) {
        fun bind(apps: List<AppLauncher>, columns: Int) {
            (grid.layoutManager as SafeGridLayoutManager).spanCount = columns
            val adapter = createPageAdapter()
            grid.adapter = adapter
            adapter.submitList(apps.map<AppLauncher, DrawerGridItem> { DrawerGridItem.App(it) }.toMutableList())
        }
    }
}
