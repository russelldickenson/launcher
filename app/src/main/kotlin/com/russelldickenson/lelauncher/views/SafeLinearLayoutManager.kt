package com.russelldickenson.lelauncher.views

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager

class SafeLinearLayoutManager : LinearLayoutManager {
    constructor(context: Context) : super(context)

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    // avoids "IndexOutOfBoundsException: Inconsistency detected" when the list changes mid-animation
    override fun supportsPredictiveItemAnimations() = false
}
