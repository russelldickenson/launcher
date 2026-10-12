package com.russelldickenson.lelauncher.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.russelldickenson.lelauncher.helpers.Converters
import com.russelldickenson.lelauncher.interfaces.AppLaunchersDao
import com.russelldickenson.lelauncher.interfaces.DrawerFoldersDao
import com.russelldickenson.lelauncher.interfaces.HiddenIconsDao
import com.russelldickenson.lelauncher.interfaces.HomeScreenGridItemsDao
import com.russelldickenson.lelauncher.models.AppLauncher
import com.russelldickenson.lelauncher.models.DrawerFolder
import com.russelldickenson.lelauncher.models.HiddenIcon
import com.russelldickenson.lelauncher.models.HomeScreenGridItem

@Database(
    entities = [AppLauncher::class, HomeScreenGridItem::class, HiddenIcon::class, DrawerFolder::class],
    version = 8
)
@TypeConverters(Converters::class)
abstract class AppsDatabase : RoomDatabase() {

    abstract fun AppLaunchersDao(): AppLaunchersDao

    abstract fun HomeScreenGridItemsDao(): HomeScreenGridItemsDao

    abstract fun HiddenIconsDao(): HiddenIconsDao

    abstract fun DrawerFoldersDao(): DrawerFoldersDao

    companion object {
        private var db: AppsDatabase? = null

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE apps ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE apps ADD COLUMN custom_title TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // must match the Room-generated schema in app/schemas exactly (no NOT NULL after
                // AUTOINCREMENT, no DEFAULT on `order`, no DEFAULT on the nullable folder_id ALTER)
                // - Room validates the real resulting column defaults/nullability against its own
                // expected schema on next open, not just whether the migration "ran"
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `drawer_folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `title` TEXT NOT NULL, `order` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE apps ADD COLUMN folder_id INTEGER")
            }
        }

        fun getInstance(context: Context): AppsDatabase {
            if (db == null) {
                synchronized(AppsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(
                            context.applicationContext,
                            AppsDatabase::class.java,
                            "apps.db"
                        ).addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build()
                    }
                }
            }
            return db!!
        }
    }
}
