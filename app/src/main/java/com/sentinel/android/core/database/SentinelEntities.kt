package com.sentinel.android.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_devices")
data class CachedDeviceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val deviceAlias: String,
    val deviceModel: String,
    val androidVersion: String,
    val enrollmentCode: String,
    val connectionStatus: String,
    val consentVerified: Boolean,
    val cameraAllowed: Boolean,
    val screenShareAllowed: Boolean,
    val mediaPickerAllowed: Boolean,
    val filesAllowed: Boolean,
    val enrolledAtMillis: Long,
    val lastActivityAtMillis: Long
)

@Entity(tableName = "cached_activity_logs")
data class CachedActivityLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: String,
    val action: String,
    val details: String,
    val severity: String,
    val deviceId: String,
    val sessionId: String,
    val createdAtMillis: Long
)

@Entity(tableName = "studio_media")
data class StudioMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,
    val prompt: String,
    val filePath: String,
    val createdAtMillis: Long
)
