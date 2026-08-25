package org.fossify.home

import com.google.android.material.color.DynamicColors
import org.fossify.commons.FossifyApp
import org.fossify.home.extensions.config

class App : FossifyApp() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Commons' own toolbar-recoloring code (setupTopAppBar/updateTopBarColors) reads
        // config.primaryColor/backgroundColor directly, bypassing this app's own Material 3
        // theme whenever dynamic theming isn't active (pre-Android 12, or system theme
        // disabled) - keep them in sync with this app's actual palette instead of Commons'
        // generic default green. Re-synced on every cold start (not just once) so day/night
        // changes get picked up too, since these resource names are night-qualified.
        config.primaryColor = getColor(R.color.m3_primary)
        config.backgroundColor = getColor(R.color.m3_background)
        config.accentColor = getColor(R.color.m3_primary)
    }
}
