package org.fossify.home.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.normalizeString
import org.fossify.commons.views.MyGridLayoutManager
import org.fossify.home.activities.MainActivity
import org.fossify.home.adapters.LaunchersAdapter
import org.fossify.home.databinding.AllAppsFragmentBinding
import org.fossify.home.extensions.config
import org.fossify.home.extensions.getAppDrawerBackgroundColor
import org.fossify.home.extensions.getAppDrawerSearchBorderColor
import org.fossify.home.extensions.getAppDrawerTextColor
import org.fossify.home.extensions.launchApp
import org.fossify.home.extensions.setupDrawerBackground
import org.fossify.home.helpers.ITEM_TYPE_ICON
import org.fossify.home.interfaces.AllAppsListener
import org.fossify.home.models.AppLauncher
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

    private var launchers = emptyList<AppLauncher>()

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
        if (layoutManager.spanCount != context.config.drawerColumnCount) {
            onConfigurationChanged()
            // Force redraw due to changed item size
            (binding.allAppsGrid.adapter as LaunchersAdapter).notifyDataSetChanged()
        } else if (
            lastIconScalePercent != context.config.drawerIconScalePercent ||
            lastLabelFontSize != context.config.drawerLabelFontSize
        ) {
            getAdapter()?.refreshIconAndLabelSettings()
        }

        lastIconScalePercent = context.config.drawerIconScalePercent
        lastLabelFontSize = context.config.drawerLabelFontSize
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
                    // some devices ACTION_MOVE keeps triggering for the whole long press duration, but we are interested in real moves only, when coords change
                    if (lastTouchCoords.first != event.x || lastTouchCoords.second != event.y) {
                        touchDownY = -1
                        return true
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
            }

            submitList(launchers.toMutableList())
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
        binding.searchBar.requireToolbar().beGone()
        binding.searchBar.updateColors()
        binding.searchBar.setupMenu()
        setupSearchBarColors()

        binding.searchBar.onSearchTextChangedListener = {
            submitList(launchers)
        }

        binding.searchBar.binding.topToolbarSearch.setOnEditorActionListener { _, actionId, _ ->
            if (binding.searchBar.getCurrentQuery().isEmpty()) return@setOnEditorActionListener false
            when (actionId) {
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_GO -> getAdapter()?.launchFirstApp() == true
                else -> false
            }
        }
    }

    // MySearchMenu's own updateColors() fills the search field from the theme's primary color,
    // which stays light in system light mode even though the drawer around it is forced dark -
    // restyle it to match the drawer instead: dark fill, a lighter border so it still reads as a
    // distinct control, and the same text color used for app labels
    private fun setupSearchBarColors() {
        val backgroundColor = context.getAppDrawerBackgroundColor()
        val borderColor = context.getAppDrawerSearchBorderColor()
        val textColor = context.getAppDrawerTextColor()

        // MySearchMenu itself (an AppBarLayout) carries its own default surface-color background
        // behind the search field's padding, independent of toolbarContainer's - match it to the
        // drawer too so no light strip shows around the field
        binding.searchBar.setBackgroundColor(backgroundColor)

        val searchBinding = binding.searchBar.binding
        val cornerRadius = resources.getDimension(org.fossify.commons.R.dimen.material_dialog_corner_radius)
        val borderWidth = resources.getDimensionPixelSize(org.fossify.commons.R.dimen.one_dp)
        searchBinding.toolbarContainer.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            setColor(backgroundColor)
            setStroke(borderWidth, borderColor)
        }

        searchBinding.topToolbarSearch.setTextColor(textColor)
        searchBinding.topToolbarSearch.setHintTextColor(ColorUtils.setAlphaComponent(textColor, 150))
        searchBinding.topToolbarSearchIcon.setColorFilter(textColor)
    }

    private fun showNoResultsPlaceholderIfNeeded() {
        val itemCount = getAdapter()?.itemCount
        binding.noResultsPlaceholder.beVisibleIf(itemCount != null && itemCount == 0)
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

        binding.searchBar.closeSearch()
    }

    fun onBackPressed(): Boolean {
        if (binding.searchBar.isSearchOpen) {
            binding.searchBar.closeSearch()
            return true
        }

        return false
    }

    private fun submitList(items: List<AppLauncher>) {
        val searchQuery = binding.searchBar.getCurrentQuery()
        val filtered = if (searchQuery.isNotEmpty()) {
            items.filter {
                it.title.normalizeString()
                    .contains(searchQuery.normalizeString(), ignoreCase = true)
            }
        } else {
            items
        }

        getAdapter()?.submitList(filtered) {
            showNoResultsPlaceholderIfNeeded()
        }
    }
}
