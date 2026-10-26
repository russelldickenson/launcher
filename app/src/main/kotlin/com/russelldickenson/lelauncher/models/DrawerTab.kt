package com.russelldickenson.lelauncher.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drawer_tabs")
data class DrawerTab(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "order") var order: Int = 0
) {
    constructor() : this(null, "", 0)
}
