package org.fossify.home.models

import android.graphics.drawable.Drawable
import androidx.room.*

@Entity(tableName = "apps", indices = [(Index(value = ["package_name"], unique = true))])
data class AppLauncher(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "package_name") var packageName: String,
    @ColumnInfo(name = "activity_name") var activityName: String,   // some apps create multiple icons, this is needed at clicking them
    @ColumnInfo(name = "order") var order: Int,
    @ColumnInfo(name = "thumbnail_color") var thumbnailColor: Int,
    @ColumnInfo(name = "pinned", defaultValue = "0") var pinned: Boolean = false,
    @ColumnInfo(name = "custom_title") var customTitle: String? = null,
    @ColumnInfo(name = "folder_id") var folderId: Long? = null,

    @Ignore var drawable: Drawable?
) : Comparable<AppLauncher> {

    constructor() : this(null, "", "", "", 0, 0, false, null, null, null)

    override fun equals(other: Any?) = packageName.equals((other as AppLauncher).packageName, true)

    override fun hashCode() = super.hashCode()

    fun getBubbleText() = title

    fun getLauncherIdentifier() = "$packageName/$activityName"

    override fun compareTo(other: AppLauncher): Int {
        return if (order > 0 && other.order == 0) {
            -1
        } else if (order == 0 && other.order > 0) {
            1
        } else if (order > 0 && other.order > 0) {
            order.compareTo(other.order)
        } else {
            title.lowercase().compareTo(other.title.lowercase())
        }
    }
}
