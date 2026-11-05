package com.russelldickenson.lelauncher.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.russelldickenson.lelauncher.models.DrawerTab

@Dao
interface DrawerTabsDao {
    @Query("SELECT * FROM drawer_tabs ORDER BY `order`, id")
    fun getTabs(): List<DrawerTab>

    @Insert
    fun insert(tab: DrawerTab): Long

    @Query("UPDATE drawer_tabs SET title = :title WHERE id = :id")
    fun renameTab(id: Long, title: String)

    @Query("UPDATE drawer_tabs SET `order` = :order WHERE id = :id")
    fun updateOrder(id: Long, order: Int)

    @Transaction
    fun saveOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> updateOrder(id, index) }
    }

    @Query("UPDATE apps SET tab_id = NULL WHERE tab_id = :tabId")
    fun ungroupMembers(tabId: Long)

    @Query("DELETE FROM drawer_tabs WHERE id = :id")
    fun deleteTabById(id: Long)

    // deleting a tab only moves its apps back to "All" - it never deletes the apps themselves
    @Transaction
    fun deleteTab(id: Long) {
        ungroupMembers(id)
        deleteTabById(id)
    }
}
