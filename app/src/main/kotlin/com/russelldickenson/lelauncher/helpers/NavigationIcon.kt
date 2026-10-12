package com.russelldickenson.lelauncher.helpers

import androidx.annotation.StringRes
import com.russelldickenson.lelauncher.R

enum class NavigationIcon(@param:StringRes val accessibilityResId: Int) {
    Cross(R.string.close),
    Arrow(R.string.back),
    None(-1)
}
