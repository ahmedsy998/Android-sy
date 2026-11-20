package com.sentinel.android.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceCacheDao {
    @Query("SELECT * FROM cached_devices WHERE userId = :userId ORDER BY enrolledAtMillis DESC")
    fun observeDevices(userId: String): Flow<List<CachedDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(devices: List<CachedDeviceEntity>)

    @Query("DELETE FROM cached_devices WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

@Dao
interface ActivityLogCacheDao {
    @Query("SELECT * FROM cached_activity_logs WHERE userId = :userId ORDER BY createdAtMillis DESC LIMIT 100")
    fun observeLogs(userId: String): Flow<List<CachedActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(logs: List<CachedActivityLogEntity>)

    @Query("DELETE FROM cached_activity_logs WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

@Dao
interface StudioMediaDao {
    @Query("SELECT * FROM studio_media WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeMedia(userId: String): Flow<List<StudioMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StudioMediaEntity): Long

    @Query("DELETE FROM studio_media WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: Long, userId: String)
}
