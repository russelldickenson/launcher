package org.fossify.home.models

sealed class DrawerGridItem {
    data class App(val launcher: AppLauncher) : DrawerGridItem()
    data class Header(val titleRes: Int) : DrawerGridItem()
    data object Divider : DrawerGridItem()
}
