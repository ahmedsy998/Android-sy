package com.sentinel.android.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class DeviceConnectionStatus {
    ONLINE, OFFLINE, REVOKED;

    companion object {
        fun fromRaw(raw: String): DeviceConnectionStatus =
            entries.firstOrNull { it.name == raw } ?: OFFLINE
    }
}

enum class EnrollmentStatus {
    PENDING, APPROVED, REJECTED, CANCELLED, EXPIRED;

    companion object {
        fun fromRaw(raw: String): EnrollmentStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class SessionType {
    SCREEN_SHARE, FRONT_CAMERA, REAR_CAMERA, MEDIA_SHARE, FILE_SHARE;

    companion object {
        fun fromRaw(raw: String): SessionType =
            entries.firstOrNull { it.name == raw } ?: SCREEN_SHARE
    }
}

enum class SessionStatus {
    AWAITING_CONSENT, ACTIVE, PAUSED, TERMINATED;

    companion object {
        fun fromRaw(raw: String): SessionStatus =
            entries.firstOrNull { it.name == raw } ?: TERMINATED
    }
}

enum class ConnectionQuality {
    EXCELLENT, GOOD, FAIR, OFFLINE;

    companion object {
        fun fromRaw(raw: String): ConnectionQuality =
            entries.firstOrNull { it.name == raw } ?: GOOD
    }
}

enum class ActivityCategory {
    ENROLLMENT, CONSENT, PERMISSION, SESSION, DATA_SHARE, AUTH, SECURITY, MEDIA_STUDIO;

    companion object {
        fun fromRaw(raw: String): ActivityCategory =
            entries.firstOrNull { it.name == raw } ?: SECURITY
    }
}

enum class LogSeverity {
    INFO, SUCCESS, WARNING, CRITICAL;

    companion object {
        fun fromRaw(raw: String): LogSeverity =
            entries.firstOrNull { it.name == raw } ?: INFO
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val biometricEnabled: Boolean = false,
    val sessionTimeoutMinutes: Long = 30L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EnrolledDevice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceAlias: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val enrollmentCode: String = "",
    val connectionStatus: String = DeviceConnectionStatus.ONLINE.name,
    val consentVerified: Boolean = true,
    val cameraAllowed: Boolean = false,
    val screenShareAllowed: Boolean = false,
    val mediaPickerAllowed: Boolean = true,
    val filesAllowed: Boolean = true,
    @ServerTimestamp val enrolledAt: Timestamp? = null,
    @ServerTimestamp val lastActivityAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): DeviceConnectionStatus = DeviceConnectionStatus.fromRaw(connectionStatus)
}

@Keep
@IgnoreExtraProperties
data class EnrollmentRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val enrollmentCode: String = "",
    val targetAlias: String = "",
    val requestedPermissions: List<String> = emptyList(),
    val status: String = EnrollmentStatus.PENDING.name,
    val deviceModel: String = "",
    val androidVersion: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): EnrollmentStatus = EnrollmentStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class MonitoringSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val deviceAlias: String = "",
    val sessionType: String = SessionType.SCREEN_SHARE.name,
    val status: String = SessionStatus.AWAITING_CONSENT.name,
    val participantName: String = "",
    val connectionQuality: String = ConnectionQuality.EXCELLENT.name,
    val consentGranted: Boolean = false,
    val activePermissions: List<String> = emptyList(),
    val sharedItemSummary: String = "",
    @ServerTimestamp val startedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val endedAt: Timestamp? = null
) {
    fun typeEnum(): SessionType = SessionType.fromRaw(sessionType)
    fun statusEnum(): SessionStatus = SessionStatus.fromRaw(status)
    fun qualityEnum(): ConnectionQuality = ConnectionQuality.fromRaw(connectionQuality)
}

@Keep
@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = ActivityCategory.SECURITY.name,
    val action: String = "",
    val details: String = "",
    val severity: String = LogSeverity.INFO.name,
    val deviceId: String = "",
    val sessionId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun categoryEnum(): ActivityCategory = ActivityCategory.fromRaw(category)
    fun severityEnum(): LogSeverity = LogSeverity.fromRaw(severity)
}

data class StudioMediaItem(
    val id: Long = 0L,
    val prompt: String,
    val filePath: String,
    val createdAtMillis: Long
)

data class SharedFileDescriptor(
    val uriString: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sharedAtMillis: Long
)
