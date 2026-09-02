package org.fossify.home.interfaces

import android.view.Menu
import org.fossify.home.models.HomeScreenGridItem

interface ItemMenuListener {
    fun onAnyClick()
    fun pinToggled(gridItem: HomeScreenGridItem)
    fun hide(gridItem: HomeScreenGridItem)
    fun rename(gridItem: HomeScreenGridItem)
    fun resize(gridItem: HomeScreenGridItem)
    fun appInfo(gridItem: HomeScreenGridItem)
    fun remove(gridItem: HomeScreenGridItem)
    fun uninstall(gridItem: HomeScreenGridItem)
    fun addToFolder(gridItem: HomeScreenGridItem)
    fun removeFromFolder(gridItem: HomeScreenGridItem)
    fun onDismiss()
    fun beforeShow(menu: Menu)
}

abstract class ItemMenuListenerAdapter : ItemMenuListener {
    override fun onAnyClick() = Unit
    override fun pinToggled(gridItem: HomeScreenGridItem) = Unit
    override fun hide(gridItem: HomeScreenGridItem) = Unit
    override fun rename(gridItem: HomeScreenGridItem) = Unit
    override fun resize(gridItem: HomeScreenGridItem) = Unit
    override fun appInfo(gridItem: HomeScreenGridItem) = Unit
    override fun remove(gridItem: HomeScreenGridItem) = Unit
    override fun addToFolder(gridItem: HomeScreenGridItem) = Unit
    override fun removeFromFolder(gridItem: HomeScreenGridItem) = Unit
    override fun uninstall(gridItem: HomeScreenGridItem) = Unit
    override fun onDismiss() = Unit
    override fun beforeShow(menu: Menu) = Unit
}
