package org.fossify.home.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.home.models.AppLauncher

@Dao
interface AppLaunchersDao {
    @Query("SELECT * FROM apps")
    fun getAppLaunchers(): List<AppLauncher>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(appLaunchers: List<AppLauncher>)

    @Query("DELETE FROM apps WHERE package_name = :packageName")
    fun deleteApp(packageName: String)

    @Query("DELETE FROM apps WHERE id = :id")
    fun deleteById(id: Long)

    @Query("UPDATE apps SET pinned = :pinned WHERE package_name = :packageName")
    fun updatePinned(packageName: String, pinned: Boolean)

    @Query("SELECT package_name FROM apps WHERE pinned = 1")
    fun getPinnedPackageNames(): List<String>

    @Query("UPDATE apps SET title = :title, custom_title = :customTitle WHERE package_name = :packageName")
    fun updateCustomTitle(packageName: String, title: String, customTitle: String?)

    @Query("UPDATE apps SET folder_id = :folderId WHERE package_name = :packageName AND activity_name = :activityName")
    fun updateFolderId(packageName: String, activityName: String, folderId: Long?)
}
