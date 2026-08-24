package org.fossify.home

import com.google.android.material.color.DynamicColors
import org.fossify.commons.FossifyApp

class App : FossifyApp() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
