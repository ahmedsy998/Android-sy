package com.sentinel.android.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedDeviceEntity::class,
        CachedActivityLogEntity::class,
        StudioMediaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SentinelDatabase : RoomDatabase() {
    abstract fun deviceCacheDao(): DeviceCacheDao
    abstract fun activityLogCacheDao(): ActivityLogCacheDao
    abstract fun studioMediaDao(): StudioMediaDao

    companion object {
        @Volatile
        private var INSTANCE: SentinelDatabase? = null

        fun getInstance(context: Context): SentinelDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SentinelDatabase::class.java,
                    "sentinel_local_vault.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}
