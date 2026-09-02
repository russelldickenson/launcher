package org.fossify.home.helpers

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.home.adapters.LaunchersAdapter
import org.fossify.home.extensions.animateScale
import org.fossify.home.models.AppLauncher
import org.fossify.home.models.DrawerGridItem

// drives dragging a batch of already-checked (selection mode) app icons onto a folder cell to
// add them to it. The app drawer has no other drag-and-drop, so this is built directly on
// RecyclerView.OnItemTouchListener rather than reusing anything from the home screen's own
// (structurally different, canvas-drawn, paged-grid) drag system
class FolderDragHelper(
    private val recyclerView: RecyclerView,
    private val dragShadowContainer: View,
    private val dragShadowIcon: ImageView,
    private val dragShadowCountBadge: TextView,
    private val getCurrentSelectionSize: () -> Int,
    private val onDrop: (folderId: Long) -> Unit,
    private val onCancel: () -> Unit,
) : RecyclerView.OnItemTouchListener {

    private var isArmed = false
    private var isDragging = false
    private var pendingLauncher: AppLauncher? = null
    private var hoveredFolderView: View? = null

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private var autoScrollDirection = 0
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            if (autoScrollDirection != 0) {
                recyclerView.scrollBy(0, autoScrollDirection * AUTO_SCROLL_STEP_PX)
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_INTERVAL_MS)
            }
        }
    }

    fun attach() {
        recyclerView.addOnItemTouchListener(this)
    }

    fun detach() {
        recyclerView.removeOnItemTouchListener(this)
        cancelDrag()
    }

    // called from the long-click listener on an already-checked icon - arms the drag, but the
    // actual drag only starts once the still-ongoing touch sequence produces a MOVE past this
    // point, since a long-click callback has no MotionEvent of its own to seed a position from
    fun armDrag(launcher: AppLauncher) {
        isArmed = true
        pendingLauncher = launcher
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (!isArmed && !isDragging) {
            return false
        }

        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (isArmed && !isDragging) {
                    startDragging(e)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    finishDrag(e)
                } else {
                    isArmed = false
                }
            }
        }

        return isDragging
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (!isDragging) {
            return
        }

        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> updateDrag(e)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> finishDrag(e)
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit

    private fun startDragging(e: MotionEvent) {
        val launcher = pendingLauncher ?: return
        isDragging = true

        val iconSize = (recyclerView.adapter as? LaunchersAdapter)?.getIconSizePx() ?: dragShadowContainer.width
        dragShadowContainer.layoutParams = dragShadowContainer.layoutParams.apply {
            width = iconSize
            height = iconSize
        }
        dragShadowIcon.setImageDrawable(launcher.drawable)

        val selectionSize = getCurrentSelectionSize()
        dragShadowCountBadge.text = selectionSize.toString()
        dragShadowCountBadge.beVisibleIf(selectionSize > 1)

        dragShadowContainer.visibility = View.VISIBLE
        moveShadowTo(e.rawX, e.rawY)
    }

    private fun updateDrag(e: MotionEvent) {
        moveShadowTo(e.rawX, e.rawY)
        updateHoverTarget(e.x, e.y)
        updateAutoScroll(e.y)
    }

    private fun moveShadowTo(rawX: Float, rawY: Float) {
        val parent = dragShadowContainer.parent as? ViewGroup ?: return
        val parentLocation = IntArray(2)
        parent.getLocationOnScreen(parentLocation)
        dragShadowContainer.x = rawX - parentLocation[0] - dragShadowContainer.width / 2f
        dragShadowContainer.y = rawY - parentLocation[1] - dragShadowContainer.height / 2f
    }

    private fun updateHoverTarget(x: Float, y: Float) {
        val folderView = folderViewUnder(x, y)
        if (folderView == hoveredFolderView) {
            return
        }

        hoveredFolderView?.animateScale(from = HOVER_SCALE, to = 1f, duration = HOVER_ANIMATION_DURATION)
        hoveredFolderView = folderView
        folderView?.animateScale(from = 1f, to = HOVER_SCALE, duration = HOVER_ANIMATION_DURATION)
    }

    private fun folderViewUnder(x: Float, y: Float): View? {
        val child = recyclerView.findChildViewUnder(x, y) ?: return null
        val position = recyclerView.getChildAdapterPosition(child)
        val item = (recyclerView.adapter as? LaunchersAdapter)?.currentList?.getOrNull(position)
        return if (item is DrawerGridItem.Folder) child else null
    }

    private fun updateAutoScroll(y: Float) {
        val edgePx = (AUTO_SCROLL_EDGE_DP * recyclerView.resources.displayMetrics.density).toInt()
        val newDirection = when {
            y < edgePx -> -1
            y > recyclerView.height - edgePx -> 1
            else -> 0
        }

        if (newDirection == autoScrollDirection) {
            return
        }

        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        autoScrollDirection = newDirection
        if (autoScrollDirection != 0) {
            autoScrollHandler.post(autoScrollRunnable)
        }
    }

    private fun finishDrag(e: MotionEvent) {
        val folderView = folderViewUnder(e.x, e.y)
        val position = folderView?.let { recyclerView.getChildAdapterPosition(it) } ?: RecyclerView.NO_POSITION
        val folder = ((recyclerView.adapter as? LaunchersAdapter)?.currentList?.getOrNull(position) as? DrawerGridItem.Folder)?.folder

        cancelDrag()

        val folderId = folder?.id
        if (folderId != null) {
            onDrop(folderId)
        } else {
            onCancel()
        }
    }

    // stops any in-progress drag visuals/state without reporting a drop or a cancel callback -
    // used when detaching the helper entirely (e.g. leaving selection mode by another route)
    private fun cancelDrag() {
        autoScrollHandler.removeCallbacksAndMessages(null)
        autoScrollDirection = 0
        hoveredFolderView?.animateScale(from = HOVER_SCALE, to = 1f, duration = HOVER_ANIMATION_DURATION)
        hoveredFolderView = null
        dragShadowContainer.visibility = View.GONE
        isArmed = false
        isDragging = false
        pendingLauncher = null
    }

    companion object {
        private const val AUTO_SCROLL_EDGE_DP = 48
        private const val AUTO_SCROLL_STEP_PX = 12
        private const val AUTO_SCROLL_INTERVAL_MS = 16L
        private const val HOVER_SCALE = 1.12f
        private const val HOVER_ANIMATION_DURATION = 150L
    }
}
