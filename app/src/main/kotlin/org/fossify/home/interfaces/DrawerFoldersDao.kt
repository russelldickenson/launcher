package org.fossify.home.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import org.fossify.home.models.DrawerFolder

@Dao
interface DrawerFoldersDao {
    @Query("SELECT * FROM drawer_folders")
    fun getFolders(): List<DrawerFolder>

    @Insert
    fun insert(folder: DrawerFolder): Long

    @Query("UPDATE drawer_folders SET title = :title WHERE id = :id")
    fun renameFolder(id: Long, title: String)

    @Query("UPDATE apps SET folder_id = NULL WHERE folder_id = :folderId")
    fun ungroupMembers(folderId: Long)

    @Query("DELETE FROM drawer_folders WHERE id = :id")
    fun deleteFolderById(id: Long)

    // deleting a folder ungroups its members back to the top-level app list - it never deletes
    // the apps themselves, mirroring HomeScreenGridItemsDao's deleteById/deleteItemsWithParentId
    @Transaction
    fun deleteFolder(id: Long) {
        ungroupMembers(id)
        deleteFolderById(id)
    }
}
