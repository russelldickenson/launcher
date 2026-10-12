package com.russelldickenson.lelauncher.views

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager

class SafeGridLayoutManager : GridLayoutManager {
    constructor(context: Context, spanCount: Int) : super(context, spanCount)

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    // avoids "IndexOutOfBoundsException: Inconsistency detected" when the list changes mid-animation
    override fun supportsPredictiveItemAnimations() = false
}
