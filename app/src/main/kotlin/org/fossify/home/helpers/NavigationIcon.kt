package org.fossify.home.helpers

import androidx.annotation.StringRes
import org.fossify.home.R

enum class NavigationIcon(@param:StringRes val accessibilityResId: Int) {
    Cross(R.string.close),
    Arrow(R.string.back),
    None(-1)
}
