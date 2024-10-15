package com.hz_apps.autowificonnector.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WifiConfig::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "locked_files"
                ).addMigrations(MIGRATION_2_3).addMigrations(MIGRATION_3_4).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }


}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE wifi_configurations ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object: Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // add a column of domain
        database.execSQL("ALTER TABLE wifi_configurations ADD COLUMN `domain` TEXT NOT NULL DEFAULT ''")
        // put the domain value in the new column
    }
}