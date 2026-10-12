package org.fossify.home.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updatePadding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import org.fossify.home.R

open class InsetAppBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppBarLayout(context, attrs, defStyleAttr) {
    private var applyWindowInsets = true
    private val basePaddingLeft = paddingLeft
    private val basePaddingTop = paddingTop
    private val basePaddingRight = paddingRight

    init {
        elevation = 0f
        stateListAnimator = null
        isLiftOnScroll = false
        isLifted = false

        context.withStyledAttributes(attrs, R.styleable.InsetAppBarLayout) {
            applyWindowInsets = getBoolean(R.styleable.InsetAppBarLayout_applyWindowInsets, true)
        }

        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            if (applyWindowInsets) {
                val system = insets.getInsetsIgnoringVisibility(Type.systemBars())
                view.updatePadding(
                    left = basePaddingLeft + system.left,
                    top = basePaddingTop + system.top,
                    right = basePaddingRight + system.right
                )
            }
            insets
        }
    }

    open val toolbar: MaterialToolbar?
        get() {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child is MaterialToolbar) {
                    return child
                }
            }

            return null
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ViewCompat.requestApplyInsets(this)
    }
}
