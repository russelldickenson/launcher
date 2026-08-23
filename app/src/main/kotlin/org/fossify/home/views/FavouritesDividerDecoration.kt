package org.fossify.home.views

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.fossify.home.R
import org.fossify.home.adapters.LaunchersAdapter
import org.fossify.home.extensions.config

// draws a horizontal rule above the first fully-unpinned row, so favourited apps read as a
// visually distinct group at the top of the app drawer - stateless by design, everything is
// read fresh from `parent` on every layout/draw pass so it stays correct through search
// filtering, pin/unpin changes, and column count changes without any extra wiring
class FavouritesDividerDecoration : RecyclerView.ItemDecoration() {

    private var linePaint: Paint? = null

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val spanCount = spanCountOf(parent) ?: return
        val dividerRow = dividerRowFor(parent, spanCount) ?: return
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION && position / spanCount == dividerRow) {
            outRect.top = spacingPx(parent)
        }
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val spanCount = spanCountOf(parent) ?: return
        val dividerRow = dividerRowFor(parent, spanCount) ?: return

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position != RecyclerView.NO_POSITION && position / spanCount == dividerRow && position % spanCount == 0) {
                val y = child.top - spacingPx(parent) / 2f
                c.drawLine(parent.paddingLeft.toFloat(), y, (parent.width - parent.paddingRight).toFloat(), y, paintFor(parent))
                return
            }
        }
    }

    private fun spanCountOf(parent: RecyclerView): Int? {
        val layoutManager = parent.layoutManager as? GridLayoutManager ?: return null
        return layoutManager.spanCount.takeIf { it > 0 }
    }

    // the first row made up entirely of non-favourite apps - null while the setting is off, or
    // when there's nothing to separate (no favourites, or every app is a favourite)
    private fun dividerRowFor(parent: RecyclerView, spanCount: Int): Int? {
        if (!parent.context.config.showFavouritesDivider) {
            return null
        }

        val adapter = parent.adapter as? LaunchersAdapter ?: return null
        val itemCount = adapter.itemCount
        if (itemCount == 0) {
            return null
        }

        val pinnedCount = adapter.currentList.indexOfFirst { !it.pinned }.let { if (it == -1) itemCount else it }
        if (pinnedCount == 0 || pinnedCount >= itemCount) {
            return null
        }

        return (pinnedCount + spanCount - 1) / spanCount
    }

    private fun spacingPx(parent: RecyclerView): Int {
        return parent.resources.getDimensionPixelSize(R.dimen.favourites_divider_spacing)
    }

    private fun paintFor(parent: RecyclerView): Paint {
        return linePaint ?: Paint().apply {
            color = ContextCompat.getColor(parent.context, org.fossify.commons.R.color.divider_grey)
            strokeWidth = parent.resources.getDimension(org.fossify.commons.R.dimen.divider_height)
        }.also { linePaint = it }
    }
}
