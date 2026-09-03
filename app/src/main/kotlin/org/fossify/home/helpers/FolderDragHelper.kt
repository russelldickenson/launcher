package org.fossify.home.helpers

import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.home.adapters.LaunchersAdapter
import org.fossify.home.models.AppLauncher
import org.fossify.home.models.DrawerGridItem

// drives long-press-dragging a single app icon in the app drawer onto another icon (creates a
// folder containing both) or an existing folder (adds it) - mirrors the home screen's own
// drag-to-fold gesture, but built directly on RecyclerView.OnItemTouchListener since the app
// drawer is a scrolling list rather than the home screen's fixed, canvas-drawn, paged grid, and
// has no drag-and-drop of its own to build on
class FolderDragHelper(
    private val recyclerView: RecyclerView,
    private val dragShadowContainer: View,
    private val dragShadowIcon: ImageView,
    private val onDragStarted: () -> Unit,
    private val onDragEnded: () -> Unit,
    private val onDropOnFolder: (draggedLauncher: AppLauncher, folderId: Long) -> Unit,
    private val onDropOnApp: (draggedLauncher: AppLauncher, targetLauncher: AppLauncher) -> Unit,
    private val onCancel: () -> Unit,
) : RecyclerView.OnItemTouchListener {

    private var isArmed = false
    private var isDragging = false
    private var pendingLauncher: AppLauncher? = null
    private var hoveredView: View? = null
    private var hoveredViewOriginalAlpha = 1f

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

    // called from the long-click listener on any icon - arms the drag, but the actual drag only
    // starts once the still-ongoing touch sequence produces a MOVE past this point, since a
    // long-click callback has no MotionEvent of its own to seed a position from
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
        onDragStarted()

        val iconSize = (recyclerView.adapter as? LaunchersAdapter)?.getIconSizePx() ?: dragShadowContainer.width
        dragShadowContainer.layoutParams = dragShadowContainer.layoutParams.apply {
            width = iconSize
            height = iconSize
        }
        dragShadowIcon.setImageDrawable(launcher.drawable)

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
        val targetView = dropTargetViewUnder(x, y)
        if (targetView == hoveredView) {
            return
        }

        hoveredView?.let { setHighlighted(it, false) }
        hoveredView = targetView
        targetView?.let { setHighlighted(it, true) }
    }

    private fun setHighlighted(view: View, highlighted: Boolean) {
        if (highlighted) {
            hoveredViewOriginalAlpha = view.alpha
            view.alpha = 1f
            view.background = buildDropTargetBackground(view)
        } else {
            view.alpha = hoveredViewOriginalAlpha
            view.background = null
        }
    }

    private fun buildDropTargetBackground(view: View): GradientDrawable {
        val density = view.resources.displayMetrics.density
        val highlightColor = view.context.getProperPrimaryColor()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = DROP_TARGET_CORNER_RADIUS_DP * density
            setStroke((DROP_TARGET_STROKE_DP * density).toInt(), highlightColor)
            setColor(ColorUtils.setAlphaComponent(highlightColor, DROP_TARGET_FILL_ALPHA))
        }
    }

    // a valid drop target is either an existing folder, or any app other than the one being
    // dragged (dropping an icon on another icon is what creates a new folder)
    private fun dropTargetViewUnder(x: Float, y: Float): View? {
        val child = recyclerView.findChildViewUnder(x, y) ?: return null
        val position = recyclerView.getChildAdapterPosition(child)
        val item = (recyclerView.adapter as? LaunchersAdapter)?.currentList?.getOrNull(position)
        return when {
            item is DrawerGridItem.Folder -> child
            item is DrawerGridItem.App && item.launcher.getLauncherIdentifier() != pendingLauncher?.getLauncherIdentifier() -> child
            else -> null
        }
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
        val draggedLauncher = pendingLauncher
        val targetView = dropTargetViewUnder(e.x, e.y)
        val position = targetView?.let { recyclerView.getChildAdapterPosition(it) } ?: RecyclerView.NO_POSITION
        val targetItem = (recyclerView.adapter as? LaunchersAdapter)?.currentList?.getOrNull(position)

        cancelDrag()

        if (draggedLauncher == null) {
            return
        }

        when (targetItem) {
            is DrawerGridItem.Folder -> onDropOnFolder(draggedLauncher, targetItem.folder.id ?: return)
            is DrawerGridItem.App -> onDropOnApp(draggedLauncher, targetItem.launcher)
            else -> onCancel()
        }
    }

    // stops any in-progress drag visuals/state - only called once a real drag (not just an armed,
    // never-moved long-press) is ending, so onDragEnded() here is the one reliable place to reset
    // whatever the caller set up for the drag's duration (see AllAppsFragment.ignoreTouches: since
    // this class claims the touch stream for the whole gesture, MainActivity.onTouchEvent()'s own
    // ACTION_UP cleanup never runs for a drawer-originated drag)
    private fun cancelDrag() {
        autoScrollHandler.removeCallbacksAndMessages(null)
        autoScrollDirection = 0
        hoveredView?.let { setHighlighted(it, false) }
        hoveredView = null
        dragShadowContainer.visibility = View.GONE
        isArmed = false
        isDragging = false
        pendingLauncher = null
        onDragEnded()
    }

    companion object {
        private const val AUTO_SCROLL_EDGE_DP = 48
        private const val AUTO_SCROLL_STEP_PX = 12
        private const val AUTO_SCROLL_INTERVAL_MS = 16L
        private const val DROP_TARGET_CORNER_RADIUS_DP = 16f
        private const val DROP_TARGET_STROKE_DP = 2f
        private const val DROP_TARGET_FILL_ALPHA = 60
    }
}
