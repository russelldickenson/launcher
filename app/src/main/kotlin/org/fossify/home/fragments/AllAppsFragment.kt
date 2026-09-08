package org.fossify.home.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import androidx.core.graphics.ColorUtils
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.normalizeString
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.views.MyGridLayoutManager
import org.fossify.home.R
import org.fossify.home.activities.MainActivity
import org.fossify.home.adapters.LaunchersAdapter
import org.fossify.home.databinding.AllAppsFragmentBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.getAppDrawerBackgroundColor
import org.fossify.home.extensions.getAppDrawerSearchFillColor
import org.fossify.home.extensions.getAppDrawerTextColor
import org.fossify.home.extensions.launchApp
import org.fossify.home.extensions.setupDrawerBackground
import org.fossify.home.helpers.FolderDragHelper
import org.fossify.home.helpers.IconCache
import org.fossify.home.helpers.ITEM_TYPE_ICON
import org.fossify.home.helpers.NotificationCache
import org.fossify.home.interfaces.AllAppsListener
import org.fossify.home.models.AppLauncher
import org.fossify.home.models.DrawerFolder
import org.fossify.home.models.DrawerGridItem
import org.fossify.home.models.HomeScreenGridItem

class AllAppsFragment(
    context: Context,
    attributeSet: AttributeSet
) : MyFragment<AllAppsFragmentBinding>(context, attributeSet), AllAppsListener {

    private var lastTouchCoords = Pair(0f, 0f)
    var touchDownY = -1
    var ignoreTouches = false

    private var lastIconScalePercent = -1
    private var lastLabelFontSize = -1
    private var lastLabelMaxLines = -1
    private var lastShowFavouritesDivider: Boolean? = null

    private var launchers = emptyList<AppLauncher>()
    private var folders = emptyList<DrawerFolder>()

    // set while the folder "Add" flow (from the folder's ellipsis/long-press menu) is active -
    // see startFolderSelectionMode()/exitFolderSelectionMode()
    private var selectionTargetFolder: DrawerFolder? = null
    private var selectedForFolder: MutableSet<String> = mutableSetOf()

    private val folderDragHelper by lazy {
        FolderDragHelper(
            recyclerView = binding.allAppsGrid,
            dragShadowContainer = binding.dragShadowContainer,
            dragShadowIcon = binding.dragShadowIcon,
            onDragStarted = { activity?.dismissOpenPopupMenu() },
            onDragEnded = {
                ignoreTouches = false
                touchDownY = -1
            },
            onDropOnFolder = { draggedLauncher, folderId ->
                activity?.assignSelectedAppsToFolder(listOf(draggedLauncher), folderId)
            },
            onDropOnApp = { draggedLauncher, targetLauncher ->
                activity?.createFolderFromApps(draggedLauncher, targetLauncher)
            },
            onCancel = {}
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setupFragment(activity: MainActivity) {
        this.activity = activity
        this.binding = AllAppsFragmentBinding.bind(this)

        binding.allAppsGrid.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                touchDownY = -1
            }

            return@setOnTouchListener false
        }

        folderDragHelper.attach()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupDrawerBackground(context.getAppDrawerBackgroundColor())
    }

    @SuppressLint("NotifyDataSetChanged")
    fun onResume() {
        if (binding.allAppsGrid.layoutManager == null || binding.allAppsGrid.adapter == null) {
            return
        }

        val layoutManager = binding.allAppsGrid.layoutManager as MyGridLayoutManager
        val showFavouritesDividerChanged = lastShowFavouritesDivider != null && lastShowFavouritesDivider != context.config.showFavouritesDivider
        if (layoutManager.spanCount != context.config.drawerColumnCount || showFavouritesDividerChanged) {
            lastShowFavouritesDivider = context.config.showFavouritesDivider
            onConfigurationChanged()
            // Force redraw due to changed item size
            (binding.allAppsGrid.adapter as LaunchersAdapter).notifyDataSetChanged()
        } else if (
            lastIconScalePercent != context.config.drawerIconScalePercent ||
            lastLabelFontSize != context.config.drawerLabelFontSize ||
            lastLabelMaxLines != context.config.drawerLabelMaxLines
        ) {
            getAdapter()?.refreshIconAndLabelSettings()
        }

        lastIconScalePercent = context.config.drawerIconScalePercent
        lastLabelFontSize = context.config.drawerLabelFontSize
        lastLabelMaxLines = context.config.drawerLabelMaxLines
        lastShowFavouritesDivider = context.config.showFavouritesDivider
    }

    fun onConfigurationChanged() {
        binding.allAppsGrid.scrollToPosition(0)
        binding.allAppsFastscroller.resetManualScrolling()
        setupViews()

        val layoutManager = binding.allAppsGrid.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = context.config.drawerColumnCount
        setupAdapter(launchers)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onInterceptTouchEvent(event)
        }

        var shouldIntercept = false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownY = event.y.toInt()
            }

            MotionEvent.ACTION_MOVE -> {
                if (ignoreTouches) {
                    // some devices ACTION_MOVE keeps triggering for the whole long press duration, but we
                    // are interested in real moves only, when coords change. This branch's only remaining
                    // job is to suppress the pull-to-dismiss gesture below during a long-press interaction -
                    // it deliberately does NOT intercept (no `return true`) so the move still reaches the
                    // RecyclerView, where FolderDragHelper's own OnItemTouchListener decides whether to
                    // claim it as a folder-creation drag
                    if (lastTouchCoords.first != event.x || lastTouchCoords.second != event.y) {
                        touchDownY = -1
                    }
                }

                // pull the whole fragment down if it is scrolled way to the top and the user pulls it even further
                if (touchDownY != -1) {
                    val distance = event.y.toInt() - touchDownY
                    shouldIntercept =
                        distance > 0 && binding.allAppsGrid.computeVerticalScrollOffset() == 0
                    if (shouldIntercept) {
                        // Hiding is expensive, only do it if focused
                        if (binding.searchBar.hasFocus()) {
                            activity?.hideKeyboard()
                        }
                        activity?.startHandlingTouches(touchDownY)
                        touchDownY = -1
                    }
                }
            }
        }

        lastTouchCoords = Pair(event.x, event.y)
        return shouldIntercept
    }

    fun gotLaunchers(appLaunchers: List<AppLauncher>) {
        launchers = appLaunchers.sortedWith(
            compareByDescending<AppLauncher> { it.pinned }
                .thenBy { it.title.normalizeString().lowercase() }
                .thenBy { it.packageName }
        )
        folders = IconCache.folders.sortedBy { it.title.normalizeString().lowercase() }

        setupAdapter(launchers)
    }

    private fun getAdapter() = binding.allAppsGrid.adapter as? LaunchersAdapter

    @SuppressLint("NotifyDataSetChanged")
    fun refreshNotificationBadges() {
        getAdapter()?.notifyDataSetChanged()
    }

    private fun setupAdapter(launchers: List<AppLauncher>) {
        activity?.runOnUiThread {
            val layoutManager = binding.allAppsGrid.layoutManager as MyGridLayoutManager
            layoutManager.spanCount = context.config.drawerColumnCount

            if (getAdapter() == null) {
                LaunchersAdapter(activity!!, this) {
                    activity?.launchApp((it as AppLauncher).packageName, it.activityName)
                    if (activity?.config?.closeAppDrawer == true) {
                        activity?.closeAppDrawer(delayed = true)
                    }
                    ignoreTouches = false
                    touchDownY = -1
                }.apply {
                    binding.allAppsGrid.itemAnimator = null
                    binding.allAppsGrid.adapter = this
                }

                layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        val viewType = getAdapter()?.getItemViewType(position)
                        return if (viewType == LaunchersAdapter.VIEW_TYPE_DIVIDER || viewType == LaunchersAdapter.VIEW_TYPE_HEADER) {
                            layoutManager.spanCount
                        } else {
                            1
                        }
                    }
                }
            }

            submitList(launchers)
        }
    }

    fun onIconHidden(item: HomeScreenGridItem) {
        val itemToRemove = launchers.firstOrNull {
            it.getLauncherIdentifier() == item.getItemIdentifier()
        }

        if (itemToRemove != null) {
            val position = launchers.indexOfFirst {
                it.getLauncherIdentifier() == item.getItemIdentifier()
            }

            launchers = launchers.toMutableList().apply {
                removeAt(position)
            }

            submitList(launchers.toMutableList())
        }
    }

    fun onIconPinChanged(packageName: String, activityName: String, pinned: Boolean) {
        val identifier = "$packageName/$activityName"
        val index = launchers.indexOfFirst { it.getLauncherIdentifier() == identifier }
        if (index != -1) {
            launchers = launchers.toMutableList().apply {
                this[index] = this[index].copy(pinned = pinned)
            }.sortedWith(
                compareByDescending<AppLauncher> { it.pinned }
                    .thenBy { it.title.normalizeString().lowercase() }
                    .thenBy { it.packageName }
            )

            submitList(launchers.toMutableList())
        }
    }

    fun onIconTitleChanged(packageName: String, activityName: String, newTitle: String) {
        val identifier = "$packageName/$activityName"
        val index = launchers.indexOfFirst { it.getLauncherIdentifier() == identifier }
        if (index != -1) {
            launchers = launchers.toMutableList().apply {
                this[index] = this[index].copy(title = newTitle, customTitle = newTitle)
            }.sortedWith(
                compareByDescending<AppLauncher> { it.pinned }
                    .thenBy { it.title.normalizeString().lowercase() }
                    .thenBy { it.packageName }
            )

            submitList(launchers.toMutableList())
        }
    }

    fun setupViews() {
        if (activity == null) {
            return
        }

        binding.allAppsFastscroller.updateColors(context.getProperPrimaryColor())
        binding.allAppsGrid.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Hiding is expensive, only do it if focused
                if (binding.searchBar.hasFocus() && dy > 0 && binding.allAppsGrid.computeVerticalScrollOffset() > 0) {
                    activity?.hideKeyboard()
                }
            }
        })

        setupDrawerBackground(context.getAppDrawerBackgroundColor())
        getAdapter()?.updateTextColor(context.getAppDrawerTextColor())

        binding.searchBar.beVisibleIf(context.config.showSearchBar)
        setupSearchBarColors()

        binding.selectionCloseButton.setOnClickListener { exitFolderSelectionMode(commit = false) }
        binding.selectionConfirmButton.setOnClickListener { exitFolderSelectionMode(commit = true) }

        binding.searchEditText.doAfterTextChanged {
            submitList(launchers)
        }

        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (binding.searchEditText.text.isNullOrEmpty()) return@setOnEditorActionListener false
            when (actionId) {
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_GO -> getAdapter()?.launchFirstApp() == true
                else -> false
            }
        }
    }

    // the field is always visible - this just focuses it and raises the keyboard, for
    // MainActivity's auto-show-keyboard-on-drawer-open path
    fun focusSearchBar() {
        binding.searchEditText.requestFocus()
        activity?.showKeyboard(binding.searchEditText)
    }

    // a real Material3 TextInputLayout themes itself from the app's own M3 attrs already, unlike
    // MySearchMenu (which pulled from the theme's primary color and needed hand-drawn overrides
    // to match the drawer) - only the fill/text colors still need setting explicitly, since those
    // come from the drawer's own (possibly custom) background rather than the app theme. No border
    // is drawn (boxStrokeWidth is 0 in the layout) - the fill color alone separates the field from
    // the drawer background behind it
    private fun setupSearchBarColors() {
        val fillColor = context.getAppDrawerSearchFillColor()
        val textColor = context.getAppDrawerTextColor()

        // the TextInputLayout's own background sits behind its box padding, independent of the
        // box fill itself - match it to the drawer so no light strip shows around the field
        binding.searchBar.setBackgroundColor(context.getAppDrawerBackgroundColor())
        binding.searchBar.setBoxBackgroundColor(fillColor)
        binding.searchBar.setStartIconTintList(ColorStateList.valueOf(textColor))

        binding.searchEditText.setTextColor(textColor)
        binding.searchEditText.setHintTextColor(ColorUtils.setAlphaComponent(textColor, 150))
    }

    private fun showNoResultsPlaceholderIfNeeded() {
        val adapter = getAdapter() ?: return
        val hasResults = adapter.currentList.any { it is DrawerGridItem.App || it is DrawerGridItem.Folder }
        binding.noResultsPlaceholder.beVisibleIf(!hasResults)
    }

    override fun onFolderClicked(folder: DrawerFolder) {
        val members = launchers.filter { it.folderId == folder.id }
        activity?.showFolderContents(folder, members)
    }

    override fun onAppSelectionToggled(appLauncher: AppLauncher) {
        val identifier = appLauncher.getLauncherIdentifier()
        if (!selectedForFolder.remove(identifier)) {
            selectedForFolder.add(identifier)
        }
        getAdapter()?.setSelectionState(active = true, selected = selectedForFolder)
        getAdapter()?.notifySelectionChanged(identifier)
        updateSelectionCountLabel()
    }

    // entry point for the folder menu's "Add" action - switches the drawer grid into a
    // multi-select mode where tapping an app toggles it instead of launching it, and folder
    // tiles are hidden (nothing meaningful to do with one while picking apps to add to another)
    fun startFolderSelectionMode(folder: DrawerFolder) {
        selectionTargetFolder = folder
        selectedForFolder = mutableSetOf()
        closeSearch()
        setupSelectionModeBarColors()
        binding.searchBar.beVisibleIf(false)
        binding.selectionModeBar.beVisibleIf(true)
        updateSelectionCountLabel()
        getAdapter()?.setSelectionState(active = true, selected = selectedForFolder)
        submitList(launchers)
    }

    private fun exitFolderSelectionMode(commit: Boolean) {
        val folderId = selectionTargetFolder?.id
        val selected = selectedForFolder.toSet()
        if (commit && folderId != null && selected.isNotEmpty()) {
            val selectedLaunchers = launchers.filter { it.getLauncherIdentifier() in selected }
            activity?.assignSelectedAppsToFolder(selectedLaunchers, folderId)
        }

        selectionTargetFolder = null
        selectedForFolder = mutableSetOf()
        binding.selectionModeBar.beVisibleIf(false)
        binding.searchBar.beVisibleIf(context.config.showSearchBar)
        getAdapter()?.setSelectionState(active = false, selected = emptySet())
        submitList(launchers)
    }

    private fun updateSelectionCountLabel() {
        binding.selectionCountLabel.text = resources.getString(R.string.n_apps_selected, selectedForFolder.size)
    }

    private fun setupSelectionModeBarColors() {
        val textColor = context.getAppDrawerTextColor()
        binding.selectionModeBar.setCardBackgroundColor(context.getAppDrawerSearchFillColor())
        binding.selectionCountLabel.setTextColor(textColor)
        binding.selectionCloseButton.setColorFilter(textColor)
        binding.selectionConfirmButton.setColorFilter(textColor)
    }

    override fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: AppLauncher) {
        val gridItem = HomeScreenGridItem(
            id = null,
            left = -1,
            top = -1,
            right = -1,
            bottom = -1,
            page = 0,
            packageName = appLauncher.packageName,
            activityName = appLauncher.activityName,
            title = appLauncher.title,
            type = ITEM_TYPE_ICON,
            className = "",
            widgetId = -1,
            shortcutId = "",
            icon = null,
            docked = false,
            parentId = null,
            drawable = appLauncher.drawable
        )

        activity?.showHomeIconMenu(x, y, gridItem, true)
        ignoreTouches = true
        if (selectionTargetFolder == null) {
            folderDragHelper.armDrag(appLauncher)
        }

        closeSearch()
    }

    fun closeSearch() {
        binding.searchEditText.text = null
        binding.searchEditText.clearFocus()
        activity?.hideKeyboard()
    }

    fun onBackPressed(): Boolean {
        if (selectionTargetFolder != null) {
            exitFolderSelectionMode(commit = false)
            return true
        }

        val query = binding.searchEditText.text
        if (!query.isNullOrEmpty() || binding.searchEditText.hasFocus()) {
            closeSearch()
            return true
        }

        return false
    }

    private fun submitList(items: List<AppLauncher>) {
        val searchQuery = binding.searchEditText.text?.toString().orEmpty()
        val filtered = if (searchQuery.isNotEmpty()) {
            items.filter {
                it.title.normalizeString()
                    .contains(searchQuery.normalizeString(), ignoreCase = true)
            }
        } else {
            items
        }

        val membersByFolderId = filtered.filter { it.folderId != null }.groupBy { it.folderId }
        val topLevel = filtered.filter { it.folderId == null }

        val drawerItems = mutableListOf<DrawerGridItem>()

        // during a search, a folder with no matching members is left out entirely rather than
        // shown empty; during the folder "Add" selection mode, folder tiles are hidden entirely -
        // there's nothing to do with one while picking apps for another folder, and topLevel
        // above already excludes every folder's members (including the target folder's own),
        // which is exactly the set of apps that should be pickable
        val folderItems = if (selectionTargetFolder != null) {
            emptyList()
        } else {
            folders.mapNotNull { folder ->
                val members = membersByFolderId[folder.id].orEmpty()
                if (searchQuery.isEmpty() || members.isNotEmpty()) {
                    DrawerGridItem.Folder(folder, members)
                } else {
                    null
                }
            }
        }

        if (context.config.showFavouritesDivider) {
            val pinned = topLevel.filter { it.pinned }
            val unpinned = topLevel.filter { !it.pinned }
            if (pinned.isNotEmpty()) {
                drawerItems.add(DrawerGridItem.Header(R.string.favourites_header))
                pinned.forEach { drawerItems.add(DrawerGridItem.App(it)) }
                if (folderItems.isNotEmpty() || unpinned.isNotEmpty()) {
                    drawerItems.add(DrawerGridItem.Divider)
                    drawerItems.addAll(folderItems)
                    unpinned.forEach { drawerItems.add(DrawerGridItem.App(it)) }
                }
            } else {
                drawerItems.addAll(folderItems)
                topLevel.forEach { drawerItems.add(DrawerGridItem.App(it)) }
            }
        } else {
            drawerItems.addAll(folderItems)
            topLevel.forEach { drawerItems.add(DrawerGridItem.App(it)) }
        }

        getAdapter()?.submitList(drawerItems) {
            showNoResultsPlaceholderIfNeeded()
        }
    }
}
