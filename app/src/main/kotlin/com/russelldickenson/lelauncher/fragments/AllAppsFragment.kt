package com.russelldickenson.lelauncher.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import android.view.inputmethod.EditorInfo
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.google.android.material.tabs.TabLayout
import com.russelldickenson.lelauncher.R
import com.russelldickenson.lelauncher.activities.MainActivity
import com.russelldickenson.lelauncher.adapters.FavouritesPagerAdapter
import com.russelldickenson.lelauncher.adapters.LaunchersAdapter
import com.russelldickenson.lelauncher.databinding.AllAppsFragmentBinding
import com.russelldickenson.lelauncher.extensions.beVisibleIf
import com.russelldickenson.lelauncher.extensions.config
import com.russelldickenson.lelauncher.extensions.getAppDrawerBackgroundColor
import com.russelldickenson.lelauncher.extensions.getAppDrawerSearchFillColor
import com.russelldickenson.lelauncher.extensions.getAppDrawerTextColor
import com.russelldickenson.lelauncher.extensions.getProperPrimaryColor
import com.russelldickenson.lelauncher.extensions.isSystemInLightMode
import com.russelldickenson.lelauncher.extensions.hideKeyboard
import com.russelldickenson.lelauncher.extensions.launchApp
import com.russelldickenson.lelauncher.extensions.normalizeString
import com.russelldickenson.lelauncher.extensions.setupDrawerBackground
import com.russelldickenson.lelauncher.extensions.showKeyboard
import com.russelldickenson.lelauncher.helpers.FAVOURITES_PLACEMENT_TAB
import com.russelldickenson.lelauncher.helpers.FAVOURITES_ROWS_UNLIMITED
import com.russelldickenson.lelauncher.helpers.FolderDragHelper
import com.russelldickenson.lelauncher.helpers.ITEM_TYPE_ICON
import com.russelldickenson.lelauncher.helpers.IconCache
import com.russelldickenson.lelauncher.helpers.NotificationCache
import com.russelldickenson.lelauncher.interfaces.AllAppsListener
import com.russelldickenson.lelauncher.models.AppLauncher
import com.russelldickenson.lelauncher.models.DrawerFolder
import com.russelldickenson.lelauncher.models.DrawerTab
import com.russelldickenson.lelauncher.models.DrawerGridItem
import com.russelldickenson.lelauncher.models.HomeScreenGridItem
import com.russelldickenson.lelauncher.views.SafeGridLayoutManager
import kotlin.math.abs
import kotlin.math.roundToInt

class AllAppsFragment(
    context: Context,
    attributeSet: AttributeSet
) : MyFragment<AllAppsFragmentBinding>(context, attributeSet), AllAppsListener {

    private var lastTouchCoords = Pair(0f, 0f)
    var touchDownY = -1
    private var touchDownX = 0f
    var ignoreTouches = false

    private var lastIconScalePercent = -1
    private var lastLabelFontSize = -1
    private var lastLabelMaxLines = -1
    private var lastFavouritesPlacement: Int? = null
    private var lastFavouritesMaxRows: Int? = null
    private var lastShowDrawerIconDropShadow: Boolean? = null
    private var lastIncludeTabAppsInAll: Boolean? = null

    private var launchers = emptyList<AppLauncher>()
    private var folders = emptyList<DrawerFolder>()
    private var tabs = emptyList<DrawerTab>()
    private var favouritesPageCount = 0
    private var favouritesSignature: List<Any?>? = null
    private var favouritesFirstPageRows = 0
    private var favouritesPagerAdapter: FavouritesPagerAdapter? = null

    // null is the implicit "All" tab
    private var selectedTabId: Long? = null
    private var isBuildingTabs = false

    // set while the folder "Add" flow (from the folder's ellipsis/long-press menu) is active -
    // see startFolderSelectionMode()/exitFolderSelectionMode()
    private var selectionTargetFolder: DrawerFolder? = null
    private var selectedForFolder: MutableSet<String> = mutableSetOf()

    // set while the tab "Add apps" flow is active; shares selectedForFolder and the selection
    // bar with the folder flow, and never overlaps with it
    private var selectionTargetTab: DrawerTab? = null

    // set while picking several apps to move to a tab chosen afterwards (started from an app's menu)
    private var selectionMovingToTab = false

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

        PagerSnapHelper().attachToRecyclerView(binding.favouritesPager)
        binding.favouritesPager.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateFavouritesDots()
            }
        })
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

        val layoutManager = binding.allAppsGrid.layoutManager as SafeGridLayoutManager
        val favouritesPlacementChanged = lastFavouritesPlacement != null && lastFavouritesPlacement != context.config.favouritesPlacement
        if (layoutManager.spanCount != context.config.drawerColumnCount || favouritesPlacementChanged) {
            onConfigurationChanged()
            // Force redraw due to changed item size
            (binding.allAppsGrid.adapter as LaunchersAdapter).notifyDataSetChanged()
        } else if (
            lastIconScalePercent != context.config.drawerIconScalePercent ||
            lastLabelFontSize != context.config.drawerLabelFontSize ||
            lastLabelMaxLines != context.config.drawerLabelMaxLines ||
            lastShowDrawerIconDropShadow != context.config.showDrawerIconDropShadow
        ) {
            getAdapter()?.refreshIconAndLabelSettings()
            favouritesPagerAdapter?.notifyDataSetChanged()
        }

        lastIconScalePercent = context.config.drawerIconScalePercent
        lastLabelFontSize = context.config.drawerLabelFontSize
        lastLabelMaxLines = context.config.drawerLabelMaxLines
        lastFavouritesPlacement = context.config.favouritesPlacement
        lastShowDrawerIconDropShadow = context.config.showDrawerIconDropShadow

        val favouritesMaxRows = context.config.favouritesMaxRows
        if (lastFavouritesMaxRows != null && lastFavouritesMaxRows != favouritesMaxRows) {
            submitList(launchers)
        }
        lastFavouritesMaxRows = favouritesMaxRows

        val includeTabAppsInAll = context.config.includeTabAppsInAll
        if (lastIncludeTabAppsInAll != null && lastIncludeTabAppsInAll != includeTabAppsInAll) {
            submitList(launchers)
        }
        lastIncludeTabAppsInAll = includeTabAppsInAll
    }

    fun onConfigurationChanged() {
        binding.allAppsGrid.scrollToPosition(0)
        binding.allAppsFastscroller.resetManualScrolling()
        setupViews()

        val layoutManager = binding.allAppsGrid.layoutManager as SafeGridLayoutManager
        layoutManager.spanCount = context.config.drawerColumnCount
        setupAdapter(launchers)
    }

    fun selectTab(tabId: Long?) {
        selectedTabId = tabId
        rebuildTabs()
        submitList(launchers)
    }

    private fun rebuildTabs() {
        val tabLayout = binding.drawerTabs
        tabLayout.beVisibleIf(true)
        isBuildingTabs = true
        tabLayout.removeAllTabs()
        tabLayout.clearOnTabSelectedListeners()
        run {
            if (isFavouritesTabMode()) {
                tabLayout.addTab(
                    tabLayout.newTab().setText(R.string.favourites_header).setTag(FAVOURITES_TAB_ID),
                    selectedTabId == FAVOURITES_TAB_ID
                )
            }
            tabLayout.addTab(tabLayout.newTab().setText(R.string.all_tab).setTag(null), selectedTabId == null)
            tabs.forEach { tab ->
                tabLayout.addTab(tabLayout.newTab().setText(tab.title).setTag(tab.id), selectedTabId == tab.id)
                tabLayout.getTabAt(tabLayout.tabCount - 1)?.view?.setOnLongClickListener {
                    onTabLongPressed(tab)
                    true
                }
            }

            tabLayout.addTab(
                tabLayout.newTab().setText("+").setContentDescription(R.string.new_tab).setTag(ADD_TAB_TAG),
                false
            )

            val textColor = context.getAppDrawerTextColor()
            tabLayout.setTabTextColors(ColorUtils.setAlphaComponent(textColor, 150), textColor)
            for (i in 0 until tabLayout.tabCount) {
                tabLayout.getTabAt(i)?.view?.background = createTabBackground()
            }
            tabLayout.setBackgroundColor(context.getAppDrawerBackgroundColor())
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (isBuildingTabs) {
                        return
                    }

                    if (tab.tag == ADD_TAB_TAG) {
                        // "+" is an action, not a destination - snap back to the real selection
                        tabLayout.post { rebuildTabs() }
                        activity?.showNewTabDialog()
                    } else {
                        selectedTabId = tab.tag as Long?
                        submitList(launchers)
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit
                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            })
        }
        isBuildingTabs = false
    }

    // each tab is its own tinted pill, inset so neighbouring pills never touch; the selected one is
    // tinted more strongly, which replaces the indicator line
    private fun createTabBackground(): Drawable {
        val density = resources.displayMetrics.density
        val primary = context.getProperPrimaryColor()
        fun pill(alpha: Int) = InsetDrawable(
            GradientDrawable().apply {
                setColor(ColorUtils.setAlphaComponent(primary, alpha))
                cornerRadius = TAB_CORNER_RADIUS_DP * density
            },
            (TAB_INSET_HORIZONTAL_DP * density).toInt(),
            (TAB_INSET_VERTICAL_DP * density).toInt(),
            (TAB_INSET_HORIZONTAL_DP * density).toInt(),
            (TAB_INSET_VERTICAL_DP * density).toInt()
        )

        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_selected), pill(TAB_SELECTED_ALPHA))
            addState(intArrayOf(), pill(TAB_UNSELECTED_ALPHA))
        }
    }

    companion object {
        private const val ADD_TAB_TAG = "add_tab"
        private const val FAVOURITES_TAB_ID = -1L
        private const val FAVOURITES_TINT_ALPHA = 40
        private const val FAVOURITES_CORNER_RADIUS_DP = 20
        private const val FAVOURITES_DARK_SURFACE = 0xFF5D5C69.toInt()
        private const val FAVOURITES_LIGHT_TEXT = 0xFF1C1B1F.toInt()
        private const val FAVOURITES_LIGHT_SURFACE = 0xFFE2E1E8.toInt()
        private const val DOT_SIZE_DP = 6
        private const val DOT_MARGIN_DP = 3
        private const val DOT_ACTIVE_ALPHA = 255
        private const val DOT_INACTIVE_ALPHA = 90
        private const val SEARCH_POPOUT_DURATION_MS = 180L
        private const val SEARCH_POPOUT_START_SCALE = 0.3f
        private const val TAB_CORNER_RADIUS_DP = 16
        private const val TAB_INSET_HORIZONTAL_DP = 4
        private const val TAB_INSET_VERTICAL_DP = 6
        private const val TAB_SELECTED_ALPHA = 90
        private const val TAB_UNSELECTED_ALPHA = 36
    }

    override fun onTabLongPressed(tab: DrawerTab) {
        activity?.showTabMenu(tab)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onInterceptTouchEvent(event)
        }

        var shouldIntercept = false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownY = event.y.toInt()
                touchDownX = event.x
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
                    // a mostly sideways drag (the favourites row) must not count as a pull
                    val isMostlyVertical = distance > abs(event.x - touchDownX)
                    shouldIntercept = distance > ViewConfiguration.get(context).scaledTouchSlop &&
                        isMostlyVertical && binding.allAppsGrid.computeVerticalScrollOffset() == 0
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

        // can be called from a background thread (refreshLaunchers), but the tab strip is a view
        activity?.runOnUiThread {
            tabs = IconCache.tabs
            val favouritesTabSelected = selectedTabId == FAVOURITES_TAB_ID && isFavouritesTabMode()
            if (!favouritesTabSelected && tabs.none { it.id == selectedTabId }) {
                selectedTabId = null
            }
            rebuildTabs()
        }

        setupAdapter(launchers)
    }

    private fun getAdapter() = binding.allAppsGrid.adapter as? LaunchersAdapter

    private fun createAdapter(listener: AllAppsListener) = LaunchersAdapter(activity!!, listener) {
        activity?.launchApp((it as AppLauncher).packageName, it.activityName)
        if (activity?.config?.closeAppDrawer == true) {
            activity?.closeAppDrawer(delayed = true)
        }
        ignoreTouches = false
        touchDownY = -1
    }

    private fun fullWidthRowsSpanSizeLookup(
        layoutManager: GridLayoutManager,
        adapter: () -> LaunchersAdapter?
    ) = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            val viewType = adapter()?.getItemViewType(position)
            return if (viewType == LaunchersAdapter.VIEW_TYPE_DIVIDER || viewType == LaunchersAdapter.VIEW_TYPE_HEADER) {
                layoutManager.spanCount
            } else {
                1
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshNotificationBadges() {
        getAdapter()?.notifyDataSetChanged()
        favouritesPagerAdapter?.notifyDataSetChanged()
    }

    private fun setupAdapter(launchers: List<AppLauncher>) {
        activity?.runOnUiThread {
            val layoutManager = binding.allAppsGrid.layoutManager as SafeGridLayoutManager
            layoutManager.spanCount = context.config.drawerColumnCount

            if (getAdapter() == null) {
                createAdapter(this).apply {
                    binding.allAppsGrid.itemAnimator = null
                    binding.allAppsGrid.adapter = this
                }
                layoutManager.spanSizeLookup = fullWidthRowsSpanSizeLookup(layoutManager) { getAdapter() }

                // favourites live outside the tabbed grid so they stay put when a tab changes; a
                // drag-to-folder gesture only listens to the main grid, so it is never armed here
                val favouritesListener = object : AllAppsListener by this {
                    override fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: AppLauncher) {
                        showAppMenu(x, y, appLauncher, armDrag = false)
                    }
                }
                favouritesPagerAdapter = FavouritesPagerAdapter(
                    createPageAdapter = {
                        createAdapter(favouritesListener).apply { updateTextColor(favouritesTextColor()) }
                    },
                    onPageHeight = ::onFavouritesPageHeight
                ).also { binding.favouritesPager.adapter = it }
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
        favouritesPagerAdapter?.notifyDataSetChanged()

        binding.searchButton.beVisibleIf(context.config.showSearchBar && !isSelecting())
        binding.searchButton.setColorFilter(context.getAppDrawerTextColor())
        setupFavouritesBackground()
        binding.searchButton.setOnClickListener { openSearch() }
        binding.searchBar.setEndIconOnClickListener { closeSearch() }
        if (!context.config.showSearchBar) {
            hideSearchPopout()
        }
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
    fun focusSearchBar() = openSearch()

    // the field "pops out" of the search icon on the left and overlaps whatever is at the top of
    // the drawer (the tab row); closeSearch() tucks it away again
    fun openSearch() {
        val popout = binding.searchPopout
        if (!popout.isVisible) {
            val margin = resources.getDimension(R.dimen.activity_margin)
            popout.pivotX = 2 * margin
            popout.pivotY = popout.height / 2f
            popout.alpha = 0f
            popout.scaleX = SEARCH_POPOUT_START_SCALE
            popout.visibility = View.VISIBLE
            popout.animate().alpha(1f).scaleX(1f).setDuration(SEARCH_POPOUT_DURATION_MS).start()
        }

        binding.searchEditText.requestFocus()
        activity?.showKeyboard(binding.searchEditText)
    }

    private fun hideSearchPopout() {
        binding.searchPopout.animate().cancel()
        binding.searchPopout.visibility = View.GONE
    }

    private fun isFavouritesTabMode() = context.config.favouritesPlacement == FAVOURITES_PLACEMENT_TAB

    private fun isSelecting() = selectionTargetFolder != null || selectionTargetTab != null || selectionMovingToTab

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
        binding.searchPopout.setCardBackgroundColor(fillColor)
        binding.searchBar.setBackgroundColor(fillColor)
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
        binding.searchButton.beVisibleIf(false)
        binding.selectionModeBar.beVisibleIf(true)
        updateSelectionCountLabel()
        getAdapter()?.setSelectionState(active = true, selected = selectedForFolder)
        submitList(launchers)
    }

    fun startTabSelectionMode(tab: DrawerTab) {
        selectionTargetTab = tab
        selectedForFolder = mutableSetOf()
        closeSearch()
        setupSelectionModeBarColors()
        binding.searchButton.beVisibleIf(false)
        binding.selectionModeBar.beVisibleIf(true)
        updateSelectionCountLabel()
        getAdapter()?.setSelectionState(active = true, selected = selectedForFolder)
        submitList(launchers)
    }

    fun startMoveSelectionMode(first: AppLauncher) {
        selectionMovingToTab = true
        selectedForFolder = mutableSetOf(first.getLauncherIdentifier())
        closeSearch()
        setupSelectionModeBarColors()
        binding.searchButton.beVisibleIf(false)
        binding.selectionModeBar.beVisibleIf(true)
        updateSelectionCountLabel()
        getAdapter()?.setSelectionState(active = true, selected = selectedForFolder)
        submitList(launchers)
    }

    private fun exitFolderSelectionMode(commit: Boolean) {
        val folderId = selectionTargetFolder?.id
        val tabId = selectionTargetTab?.id
        val selected = selectedForFolder.toSet()
        val moving = selectionMovingToTab
        if (commit && selected.isNotEmpty()) {
            val selectedLaunchers = launchers.filter { it.getLauncherIdentifier() in selected }
            if (folderId != null) {
                activity?.assignSelectedAppsToFolder(selectedLaunchers, folderId)
            } else if (tabId != null) {
                activity?.assignSelectedAppsToTab(selectedLaunchers, tabId)
            } else if (moving) {
                activity?.showMoveSelectedToTabDialog(selectedLaunchers)
            }
        }

        selectionTargetFolder = null
        selectionMovingToTab = false
        selectionTargetTab = null
        selectedForFolder = mutableSetOf()
        binding.selectionModeBar.beVisibleIf(false)
        binding.searchButton.beVisibleIf(context.config.showSearchBar)
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
        showAppMenu(x, y, appLauncher, armDrag = true)
    }

    private fun showAppMenu(x: Float, y: Float, appLauncher: AppLauncher, armDrag: Boolean) {
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
        if (armDrag && !isSelecting()) {
            folderDragHelper.armDrag(appLauncher)
        }

        closeSearch()
    }

    fun closeSearch() {
        // hide while the field still has focus - hideKeyboard() needs the focused view's window token
        activity?.hideKeyboard()
        binding.searchEditText.text = null
        binding.searchEditText.clearFocus()
        hideSearchPopout()
    }

    fun onBackPressed(): Boolean {
        if (isSelecting()) {
            exitFolderSelectionMode(commit = false)
            return true
        }

        val query = binding.searchEditText.text
        if (!query.isNullOrEmpty() || binding.searchEditText.hasFocus() || binding.searchPopout.isVisible) {
            closeSearch()
            return true
        }

        return false
    }

    // a dot per page of favourites, shown only when there is more than one page, with the dot for
    // the page currently in view filled in
    private fun updateFavouritesDots() {
        val pages = favouritesPageCount
        val dots = binding.favouritesDots
        dots.beVisibleIf(pages > 1)
        if (pages <= 1) {
            return
        }

        if (dots.childCount != pages) {
            dots.removeAllViews()
            val density = resources.displayMetrics.density
            val size = (DOT_SIZE_DP * density).toInt()
            val margin = (DOT_MARGIN_DP * density).toInt()
            repeat(pages) {
                dots.addView(
                    View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(margin, 0, margin, 0) }
                    }
                )
            }
        }

        val pager = binding.favouritesPager
        val current = if (pager.width > 0) {
            (pager.computeHorizontalScrollOffset().toFloat() / pager.width).roundToInt().coerceIn(0, pages - 1)
        } else {
            0
        }
        val textColor = favouritesTextColor()
        for (i in 0 until dots.childCount) {
            val alpha = if (i == current) DOT_ACTIVE_ALPHA else DOT_INACTIVE_ALPHA
            dots.getChildAt(i).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ColorUtils.setAlphaComponent(textColor, alpha))
            }
        }
    }

    // pages are as tall as their rows need, so the fullest page sets the pager's minimum height -
    // otherwise it would resize while swiping onto a shorter last page
    private fun onFavouritesPageHeight(height: Int) {
        if (height > binding.favouritesPager.minimumHeight) {
            binding.favouritesPager.minimumHeight = height
        }
    }

    // the favourites panel uses the opposite of the system theme so it stands out from the drawer
    private fun favouritesTextColor() = if (context.isSystemInLightMode()) Color.WHITE else FAVOURITES_LIGHT_TEXT

    private fun setupFavouritesBackground() {
        val base = if (context.isSystemInLightMode()) FAVOURITES_DARK_SURFACE else FAVOURITES_LIGHT_SURFACE
        binding.favouritesSection.background = GradientDrawable().apply {
            setColor(ColorUtils.compositeColors(ColorUtils.setAlphaComponent(context.getProperPrimaryColor(), FAVOURITES_TINT_ALPHA), base))
            cornerRadius = FAVOURITES_CORNER_RADIUS_DP * resources.displayMetrics.density
        }
    }

    private fun submitFavourites(favourites: List<AppLauncher>) {
        val columns = context.config.drawerColumnCount
        val maxRows = context.config.favouritesMaxRows
        val perPage = if (maxRows == FAVOURITES_ROWS_UNLIMITED) favourites.size else maxRows * columns
        val pages = if (favourites.isEmpty()) emptyList() else favourites.chunked(perPage)

        // tab switches resubmit the same favourites; rebinding them would make the row flicker
        val signature = listOf(columns, perPage) + favourites.map { listOf(it.getLauncherIdentifier(), it.title, it.customTitle, it.drawable) }
        if (signature == favouritesSignature) return
        favouritesSignature = signature

        val firstPageRows = ((pages.firstOrNull()?.size ?: 0) + columns - 1) / columns
        if (firstPageRows != favouritesFirstPageRows) {
            favouritesFirstPageRows = firstPageRows
            binding.favouritesPager.minimumHeight = 0
        }

        favouritesPageCount = pages.size
        binding.favouritesSection.beVisibleIf(favourites.isNotEmpty())
        favouritesPagerAdapter?.submitPages(pages, columns)
        binding.favouritesPager.post { updateFavouritesDots() }
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

        // favourites sit above the tabs, independent of the selected one; while searching or picking
        // apps they stay in the main list instead, so they remain findable and selectable
        val pinned = filtered.filter { it.pinned && it.folderId == null }
        val showFavouritesRow = searchQuery.isEmpty() && !isSelecting() && !isFavouritesTabMode()
        val favourites = if (showFavouritesRow) pinned else emptyList()
        submitFavourites(favourites)
        val favouriteIds = favourites.map { it.getLauncherIdentifier() }.toSet()
        val candidates = filtered.filter { it.getLauncherIdentifier() !in favouriteIds }

        // a search looks across every tab; otherwise a tab shows its own apps, and All shows the
        // untabbed ones unless the user opted to include tabbed apps there too
        val inTab = when {
            searchQuery.isNotEmpty() -> candidates
            selectedTabId == FAVOURITES_TAB_ID -> pinned
            selectedTabId != null -> candidates.filter { it.tabId == selectedTabId }
            context.config.includeTabAppsInAll -> candidates
            else -> candidates.filter { it.tabId == null }
        }
        val targetTab = selectionTargetTab
        if (targetTab != null) {
            // picking apps for a tab: every app not already in it, flat (folders and the
            // current tab filter would otherwise hide apps that can't be reached)
            val pickable = items.filter { it.tabId != targetTab.id }
            getAdapter()?.submitList(pickable.map { DrawerGridItem.App(it) }) {
                showNoResultsPlaceholderIfNeeded()
            }
            return
        }

        val membersByFolderId = inTab.filter { it.folderId != null }.groupBy { it.folderId }
        val topLevel = inTab.filter { it.folderId == null }

        val drawerItems = mutableListOf<DrawerGridItem>()

        // during a search, a folder with no matching members is left out entirely rather than
        // shown empty; during the folder "Add" selection mode, folder tiles are hidden entirely -
        // there's nothing to do with one while picking apps for another folder, and topLevel
        // above already excludes every folder's members (including the target folder's own),
        // which is exactly the set of apps that should be pickable
        val folderItems = if (selectionTargetFolder != null || selectionMovingToTab || selectedTabId == FAVOURITES_TAB_ID) {
            emptyList()
        } else {
            folders.mapNotNull { folder ->
                val members = membersByFolderId[folder.id].orEmpty()
                if ((searchQuery.isEmpty() && selectedTabId == null) || members.isNotEmpty()) {
                    DrawerGridItem.Folder(folder, members)
                } else {
                    null
                }
            }
        }

        drawerItems.addAll(folderItems)
        topLevel.forEach { drawerItems.add(DrawerGridItem.App(it)) }

        getAdapter()?.submitList(drawerItems) {
            showNoResultsPlaceholderIfNeeded()
        }
    }
}
