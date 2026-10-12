package com.russelldickenson.lelauncher.activities

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ScrollingView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.russelldickenson.lelauncher.R
import com.russelldickenson.lelauncher.extensions.applyColorFilter
import com.russelldickenson.lelauncher.extensions.getColoredDrawableWithColor
import com.russelldickenson.lelauncher.extensions.getContrastColor
import com.russelldickenson.lelauncher.extensions.getProperBackgroundColor
import com.russelldickenson.lelauncher.extensions.getScrolledTopBarColor
import com.russelldickenson.lelauncher.extensions.hideKeyboard
import com.russelldickenson.lelauncher.extensions.setSystemBarsAppearance
import com.russelldickenson.lelauncher.extensions.updateMarginWithBase
import com.russelldickenson.lelauncher.extensions.updatePaddingWithBase
import com.russelldickenson.lelauncher.helpers.NavigationIcon
import com.russelldickenson.lelauncher.views.InsetAppBarLayout

open class SimpleActivity : AppCompatActivity() {
    private var topAppBar: InsetAppBarLayout? = null
    private var scrollingView: ScrollingView? = null
    private var topBarColorAnimation: ValueAnimator? = null

    private val contentRoot by lazy { findViewById<View>(android.R.id.content) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)

        onBackPressedDispatcher.addCallback(this) {
            if (onBackPressedCompat()) return@addCallback
            // nothing consumed it, so let the system handle the back press
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
        }
    }

    /** Return true if the back press was consumed. */
    protected open fun onBackPressedCompat(): Boolean = false

    override fun onResume() {
        super.onResume()
        window.setSystemBarsAppearance(getProperBackgroundColor())
    }

    override fun onDestroy() {
        topBarColorAnimation?.cancel()
        topBarColorAnimation = null
        super.onDestroy()
    }

    fun setupEdgeToEdge(
        padTopSystem: List<View> = emptyList(),
        padBottomSystem: List<View> = emptyList(),
        padBottomImeAndSystem: List<View> = emptyList(),
        moveBottomSystem: List<View> = emptyList(),
        animateIme: Boolean = false,
    ) {
        window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insets = WindowInsetsCompat.toWindowInsetsCompat(windowInsets)
            val system = insets.getInsetsIgnoringVisibility(Type.systemBars())
            val imeAndSystem = insets.getInsets(Type.ime() or Type.systemBars())

            padTopSystem.forEach { it.updatePaddingWithBase(top = system.top) }
            padBottomSystem.forEach { it.updatePaddingWithBase(bottom = system.bottom) }
            padBottomImeAndSystem.forEach { it.updatePaddingWithBase(bottom = imeAndSystem.bottom) }
            moveBottomSystem.forEach { it.updateMarginWithBase(bottom = system.bottom) }

            val cutout = insets.getInsets(Type.displayCutout())
            contentRoot.updatePaddingWithBase(
                left = maxOf(system.left, cutout.left),
                right = maxOf(system.right, cutout.right)
            )

            if (animateIme) {
                ViewCompat.setWindowInsetsAnimationCallback(
                    contentRoot,
                    object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                        override fun onProgress(
                            insets: WindowInsetsCompat,
                            runningAnimations: MutableList<WindowInsetsAnimationCompat>
                        ): WindowInsetsCompat {
                            val bottom = insets.getInsets(Type.systemBars() or Type.ime()).bottom
                            padBottomImeAndSystem.forEach { it.updatePaddingWithBase(bottom = bottom) }
                            return insets
                        }
                    }
                )
            }

            view.onApplyWindowInsets(windowInsets)
            windowInsets
        }
    }

    // the top bar blends into the background until the content below it is scrolled
    fun setupMaterialScrollListener(scrollingView: ScrollingView?, topAppBar: InsetAppBarLayout) {
        this.scrollingView = scrollingView
        this.topAppBar = topAppBar
        var lastScrollY = 0
        when (scrollingView) {
            is RecyclerView -> scrollingView.setOnScrollChangeListener { _, _, _, _, _ ->
                val scrollY = scrollingView.computeVerticalScrollOffset()
                scrollChanged(scrollY, lastScrollY)
                lastScrollY = scrollY
            }

            is NestedScrollView -> scrollingView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                scrollChanged(scrollY, oldScrollY)
            }
        }
    }

    private fun scrollChanged(scrollY: Int, oldScrollY: Int) {
        if (scrollY > 0 && oldScrollY == 0) {
            animateTopBarColors(getProperBackgroundColor(), getScrolledTopBarColor())
        } else if (scrollY == 0 && oldScrollY > 0) {
            animateTopBarColors(getScrolledTopBarColor(), getProperBackgroundColor())
        }
    }

    private fun animateTopBarColors(colorFrom: Int, colorTo: Int) {
        val appBar = topAppBar ?: return
        topBarColorAnimation?.end()
        topBarColorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            addUpdateListener { updateTopBarColors(appBar, it.animatedValue as Int) }
            start()
        }
    }

    private fun getRequiredTopBarColor(): Int {
        val isScrolled = (scrollingView?.computeVerticalScrollOffset() ?: 0) > 0
        return if (isScrolled) getScrolledTopBarColor() else getProperBackgroundColor()
    }

    fun setupTopAppBar(
        topAppBar: InsetAppBarLayout,
        navigationIcon: NavigationIcon = NavigationIcon.None,
        topBarColor: Int = getRequiredTopBarColor(),
    ) {
        val contrastColor = topBarColor.getContrastColor()
        if (navigationIcon != NavigationIcon.None) {
            val drawableId = if (navigationIcon == NavigationIcon.Cross) {
                R.drawable.ic_cross_vector
            } else {
                R.drawable.ic_arrow_left_vector
            }

            topAppBar.toolbar?.navigationIcon = resources.getColoredDrawableWithColor(drawableId, contrastColor)
            topAppBar.toolbar?.setNavigationContentDescription(navigationIcon.accessibilityResId)
        }

        topAppBar.toolbar?.setNavigationOnClickListener {
            hideKeyboard()
            finish()
        }

        updateTopBarColors(topAppBar, topBarColor)
    }

    private fun updateTopBarColors(topAppBar: InsetAppBarLayout, color: Int) {
        val contrastColor = color.getContrastColor()
        window.setSystemBarsAppearance(color)
        topAppBar.setBackgroundColor(color)
        topAppBar.toolbar?.apply {
            setBackgroundColor(color)
            setTitleTextColor(contrastColor)
            navigationIcon?.applyColorFilter(contrastColor)
            overflowIcon = resources.getColoredDrawableWithColor(R.drawable.ic_three_dots_vector, contrastColor)
        }
    }
}
