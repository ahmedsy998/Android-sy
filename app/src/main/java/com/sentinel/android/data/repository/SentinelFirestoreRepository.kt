package com.sentinel.android.data.repository

import android.os.Build
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.sentinel.android.core.common.OperationType
import com.sentinel.android.core.common.handleFirestoreError
import com.sentinel.android.core.database.ActivityLogCacheDao
import com.sentinel.android.core.database.CachedActivityLogEntity
import com.sentinel.android.core.database.CachedDeviceEntity
import com.sentinel.android.core.database.DeviceCacheDao
import com.sentinel.android.core.security.ActionRateLimiter
import com.sentinel.android.core.security.SentinelKeystoreCrypto
import com.sentinel.android.domain.model.ActivityCategory
import com.sentinel.android.domain.model.ActivityLog
import com.sentinel.android.domain.model.ConnectionQuality
import com.sentinel.android.domain.model.DeviceConnectionStatus
import com.sentinel.android.domain.model.EnrolledDevice
import com.sentinel.android.domain.model.EnrollmentRequest
import com.sentinel.android.domain.model.EnrollmentStatus
import com.sentinel.android.domain.model.LogSeverity
import com.sentinel.android.domain.model.MonitoringSession
import com.sentinel.android.domain.model.SessionStatus
import com.sentinel.android.domain.model.SessionType
import com.sentinel.android.domain.model.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await

class SentinelFirestoreRepository(
    private val db: FirebaseFirestore,
    private val deviceCacheDao: DeviceCacheDao,
    private val activityLogCacheDao: ActivityLogCacheDao,
    private val crypto: SentinelKeystoreCrypto,
    private val rateLimiter: ActionRateLimiter,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "User must be signed in with Google before accessing Sentinel data." }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun devicesCol(uid: String) = userDoc(uid).collection("devices")
    private fun enrollmentsCol(uid: String) = userDoc(uid).collection("enrollments")
    private fun sessionsCol(uid: String) = userDoc(uid).collection("sessions")
    private fun logsCol(uid: String) = userDoc(uid).collection("activity_logs")

    fun observeCachedDevices(userId: String): Flow<List<CachedDeviceEntity>> =
        deviceCacheDao.observeDevices(userId)

    fun observeCachedLogs(userId: String): Flow<List<CachedActivityLogEntity>> =
        activityLogCacheDao.observeLogs(userId)

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Cross-user profile observation rejected." }
        val path = "users/$uid"
        emitAll(
            userDoc(uid).snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeDevices(userId: String): Flow<List<EnrolledDevice>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Cross-user device observation rejected." }
        val path = "users/$uid/devices"
        emitAll(
            devicesCol(uid)
                .orderBy("enrolledAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(EnrolledDevice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .onEach { list ->
                    val entities = list.map { d ->
                        CachedDeviceEntity(
                            id = d.id,
                            userId = uid,
                            deviceAlias = d.deviceAlias,
                            deviceModel = d.deviceModel,
                            androidVersion = d.androidVersion,
                            enrollmentCode = d.enrollmentCode,
                            connectionStatus = d.connectionStatus,
                            consentVerified = d.consentVerified,
                            cameraAllowed = d.cameraAllowed,
                            screenShareAllowed = d.screenShareAllowed,
                            mediaPickerAllowed = d.mediaPickerAllowed,
                            filesAllowed = d.filesAllowed,
                            enrolledAtMillis = d.enrolledAt?.toDate()?.time ?: System.currentTimeMillis(),
                            lastActivityAtMillis = d.lastActivityAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                    runCatching {
                        deviceCacheDao.clearForUser(uid)
                        if (entities.isNotEmpty()) deviceCacheDao.upsertAll(entities)
                    }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeEnrollments(userId: String): Flow<List<EnrollmentRequest>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Cross-user enrollment observation rejected." }
        val path = "users/$uid/enrollments"
        emitAll(
            enrollmentsCol(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(EnrollmentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeSessions(userId: String): Flow<List<MonitoringSession>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Cross-user session observation rejected." }
        val path = "users/$uid/sessions"
        emitAll(
            sessionsCol(uid)
                .orderBy("startedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MonitoringSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeActivityLogs(userId: String): Flow<List<ActivityLog>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Cross-user activity log observation rejected." }
        val path = "users/$uid/activity_logs"
        emitAll(
            logsCol(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .onEach { list ->
                    val entities = list.map { log ->
                        CachedActivityLogEntity(
                            id = log.id,
                            userId = uid,
                            category = log.category,
                            action = log.action,
                            details = log.details,
                            severity = log.severity,
                            deviceId = log.deviceId,
                            sessionId = log.sessionId,
                            createdAtMillis = log.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                    runCatching {
                        activityLogCacheDao.clearForUser(uid)
                        if (entities.isNotEmpty()) activityLogCacheDao.upsertAll(entities)
                    }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val user = auth.currentUser ?: error("User not authenticated.")
            val uid = user.uid
            path = "users/$uid"
            val docRef = userDoc(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val displayName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Sentinel Operator").take(80)
                val email = (user.email ?: "").take(120)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "email" to email,
                    "biometricEnabled" to false,
                    "sessionTimeoutMinutes" to 30L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                recordAuditLog(
                    category = ActivityCategory.AUTH,
                    action = "Operator Profile Provisioned",
                    details = "Authenticated via Google Credential Manager ($email).",
                    severity = LogSeverity.SUCCESS
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSecurityPreferences(
        biometricEnabled: Boolean,
        sessionTimeoutMinutes: Int
    ): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val clampedTimeout = sessionTimeoutMinutes.coerceIn(5, 1440).toLong()
            userDoc(uid).update(
                mapOf(
                    "biometricEnabled" to biometricEnabled,
                    "sessionTimeoutMinutes" to clampedTimeout,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAuditLog(
                category = ActivityCategory.SECURITY,
                action = "Security Policy Updated",
                details = "Biometric lock=${biometricEnabled}, session expiry=${clampedTimeout}m.",
                severity = LogSeverity.INFO
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createEnrollmentRequest(
        targetAlias: String,
        requestedPermissions: List<String>
    ): Result<EnrollmentRequest> {
        var path = "users/unauthenticated/enrollments"
        return try {
            val uid = requireUserId()
            if (!rateLimiter.allow("create_enrollment")) {
                return Result.failure(IllegalStateException("Rate limit active. Please wait a moment before generating another code."))
            }
            val cleanAlias = targetAlias.trim().ifBlank { "${Build.MANUFACTURER} ${Build.MODEL}" }.take(60)
            val safePerms = requestedPermissions.distinct().take(10)
            val code = crypto.generateEnrollmentCode()
            val model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".take(80)
            val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})".take(40)

            val docRef = enrollmentsCol(uid).document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "enrollmentCode" to code,
                "targetAlias" to cleanAlias,
                "requestedPermissions" to safePerms,
                "status" to EnrollmentStatus.PENDING.name,
                "deviceModel" to model,
                "androidVersion" to osVersion,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            recordAuditLog(
                category = ActivityCategory.ENROLLMENT,
                action = "Enrollment Invitation Created",
                details = "Code $code generated for '$cleanAlias' with ${safePerms.size} requested scopes.",
                severity = LogSeverity.INFO
            )
            Result.success(
                EnrollmentRequest(
                    id = docRef.id,
                    userId = uid,
                    enrollmentCode = code,
                    targetAlias = cleanAlias,
                    requestedPermissions = safePerms,
                    status = EnrollmentStatus.PENDING.name,
                    deviceModel = model,
                    androidVersion = osVersion
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun approveEnrollmentAndRegisterDevice(request: EnrollmentRequest): Result<String> {
        var path = "users/unauthenticated/enrollments/${request.id}"
        return try {
            val uid = requireUserId()
            require(request.id.isNotBlank()) { "Enrollment ID cannot be blank." }
            val enrollmentRef = enrollmentsCol(uid).document(request.id)
            val deviceRef = devicesCol(uid).document()
            path = enrollmentRef.path

            val model = request.deviceModel.ifBlank { "${Build.MANUFACTURER} ${Build.MODEL}" }.take(80)
            val osVersion = request.androidVersion.ifBlank { "Android ${Build.VERSION.RELEASE}" }.take(40)
            val alias = request.targetAlias.ifBlank { model }.take(60)
            val perms = request.requestedPermissions

            val devicePayload = mapOf(
                "userId" to uid,
                "deviceAlias" to alias,
                "deviceModel" to model,
                "androidVersion" to osVersion,
                "enrollmentCode" to request.enrollmentCode.take(32),
                "connectionStatus" to DeviceConnectionStatus.ONLINE.name,
                "consentVerified" to true,
                "cameraAllowed" to perms.contains("CAMERA"),
                "screenShareAllowed" to perms.contains("SCREEN_SHARE"),
                "mediaPickerAllowed" to perms.contains("MEDIA_PICKER"),
                "filesAllowed" to perms.contains("FILES_SAF"),
                "enrolledAt" to FieldValue.serverTimestamp(),
                "lastActivityAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.runBatch { batch ->
                batch.update(
                    enrollmentRef,
                    mapOf(
                        "status" to EnrollmentStatus.APPROVED.name,
                        "deviceModel" to model,
                        "androidVersion" to osVersion,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(deviceRef, devicePayload)
            }.await()

            recordAuditLog(
                category = ActivityCategory.CONSENT,
                action = "Device Enrolled with Consent",
                details = "Owner explicitly approved '$alias' ($model, $osVersion) via code ${request.enrollmentCode}.",
                severity = LogSeverity.SUCCESS,
                deviceId = deviceRef.id
            )
            Result.success(deviceRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun decideEnrollment(requestId: String, status: EnrollmentStatus, code: String): Result<Unit> {
        var path = "users/unauthenticated/enrollments/$requestId"
        return try {
            val uid = requireUserId()
            require(requestId.isNotBlank()) { "Enrollment ID cannot be blank." }
            require(status in listOf(EnrollmentStatus.REJECTED, EnrollmentStatus.CANCELLED, EnrollmentStatus.EXPIRED))
            val docRef = enrollmentsCol(uid).document(requestId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAuditLog(
                category = ActivityCategory.ENROLLMENT,
                action = "Enrollment ${status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                details = "Enrollment request $code marked ${status.name} by device owner.",
                severity = LogSeverity.WARNING
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun enrollCurrentDeviceDirectly(alias: String): Result<String> {
        var path = "users/unauthenticated/devices"
        return try {
            val uid = requireUserId()
            if (!rateLimiter.allow("enroll_direct")) {
                return Result.failure(IllegalStateException("Too many enrollment attempts. Please wait briefly."))
            }
            val model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".take(80)
            val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})".take(40)
            val cleanAlias = alias.trim().ifBlank { "$model (Local)" }.take(60)
            val code = crypto.generateEnrollmentCode()

            val docRef = devicesCol(uid).document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "deviceAlias" to cleanAlias,
                "deviceModel" to model,
                "androidVersion" to osVersion,
                "enrollmentCode" to code,
                "connectionStatus" to DeviceConnectionStatus.ONLINE.name,
                "consentVerified" to true,
                "cameraAllowed" to true,
                "screenShareAllowed" to true,
                "mediaPickerAllowed" to true,
                "filesAllowed" to true,
                "enrolledAt" to FieldValue.serverTimestamp(),
                "lastActivityAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            recordAuditLog(
                category = ActivityCategory.ENROLLMENT,
                action = "Current Device Enrolled",
                details = "Enrolled '$cleanAlias' ($model, $osVersion) with explicit local owner approval.",
                severity = LogSeverity.SUCCESS,
                deviceId = docRef.id
            )
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDevicePermissions(
        device: EnrolledDevice,
        cameraAllowed: Boolean = device.cameraAllowed,
        screenShareAllowed: Boolean = device.screenShareAllowed,
        mediaPickerAllowed: Boolean = device.mediaPickerAllowed,
        filesAllowed: Boolean = device.filesAllowed,
        connectionStatus: DeviceConnectionStatus = device.statusEnum()
    ): Result<Unit> {
        var path = "users/unauthenticated/devices/${device.id}"
        return try {
            val uid = requireUserId()
            require(device.id.isNotBlank()) { "Device ID cannot be blank." }
            val docRef = devicesCol(uid).document(device.id)
            path = docRef.path
            docRef.update(
                mapOf(
                    "cameraAllowed" to cameraAllowed,
                    "screenShareAllowed" to screenShareAllowed,
                    "mediaPickerAllowed" to mediaPickerAllowed,
                    "filesAllowed" to filesAllowed,
                    "connectionStatus" to connectionStatus.name,
                    "lastActivityAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAuditLog(
                category = ActivityCategory.PERMISSION,
                action = "Permissions Synchronized",
                details = "Updated '${device.deviceAlias}': camera=$cameraAllowed, screen=$screenShareAllowed, media=$mediaPickerAllowed, files=$filesAllowed, status=${connectionStatus.name}.",
                severity = LogSeverity.INFO,
                deviceId = device.id
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun revokeDevice(device: EnrolledDevice): Result<Unit> {
        var path = "users/unauthenticated/devices/${device.id}"
        return try {
            val uid = requireUserId()
            require(device.id.isNotBlank()) { "Device ID cannot be blank." }
            val docRef = devicesCol(uid).document(device.id)
            path = docRef.path
            docRef.update(
                mapOf(
                    "connectionStatus" to DeviceConnectionStatus.REVOKED.name,
                    "cameraAllowed" to false,
                    "screenShareAllowed" to false,
                    "mediaPickerAllowed" to false,
                    "filesAllowed" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAuditLog(
                category = ActivityCategory.SECURITY,
                action = "Device Revoked",
                details = "Permanently revoked management access and cleared all permissions for '${device.deviceAlias}'.",
                severity = LogSeverity.CRITICAL,
                deviceId = device.id
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun startAuthorizedSession(
        device: EnrolledDevice,
        sessionType: SessionType,
        activePermissions: List<String>,
        sharedItemSummary: String = ""
    ): Result<String> {
        var path = "users/unauthenticated/sessions"
        return try {
            val uid = requireUserId()
            if (!rateLimiter.allow("start_session")) {
                return Result.failure(IllegalStateException("Rate limit active for session requests."))
            }
            require(device.id.isNotBlank()) { "Valid enrolled device required." }
            val participant = (auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.email
                ?: "Verified Owner").take(80)
            val docRef = sessionsCol(uid).document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "deviceId" to device.id,
                "deviceAlias" to device.deviceAlias.take(60),
                "sessionType" to sessionType.name,
                "status" to SessionStatus.ACTIVE.name,
                "participantName" to participant,
                "connectionQuality" to ConnectionQuality.EXCELLENT.name,
                "consentGranted" to true,
                "activePermissions" to activePermissions.take(10),
                "sharedItemSummary" to sharedItemSummary.take(240),
                "startedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            recordAuditLog(
                category = ActivityCategory.SESSION,
                action = "Session Started (${sessionType.name})",
                details = "Visible owner-approved ${sessionType.name} session started on '${device.deviceAlias}'. ${sharedItemSummary.take(120)}",
                severity = LogSeverity.SUCCESS,
                deviceId = device.id,
                sessionId = docRef.id
            )
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSessionState(
        sessionId: String,
        status: SessionStatus,
        sharedItemSummary: String? = null
    ): Result<Unit> {
        var path = "users/unauthenticated/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            require(sessionId.isNotBlank()) { "Session ID required." }
            val docRef = sessionsCol(uid).document(sessionId)
            path = docRef.path
            val updates = if (status == SessionStatus.TERMINATED) {
                mapOf(
                    "status" to SessionStatus.TERMINATED.name,
                    "connectionQuality" to ConnectionQuality.OFFLINE.name,
                    "activePermissions" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "endedAt" to FieldValue.serverTimestamp()
                )
            } else {
                buildMap<String, Any> {
                    put("status", status.name)
                    put("connectionQuality", if (status == SessionStatus.ACTIVE) ConnectionQuality.EXCELLENT.name else ConnectionQuality.GOOD.name)
                    put("updatedAt", FieldValue.serverTimestamp())
                    if (sharedItemSummary != null) {
                        put("sharedItemSummary", sharedItemSummary.take(240))
                    }
                }
            }
            docRef.update(updates).await()
            recordAuditLog(
                category = ActivityCategory.SESSION,
                action = "Session ${status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                details = "Monitoring session $sessionId transitioned to ${status.name}.",
                severity = if (status == SessionStatus.TERMINATED) LogSeverity.WARNING else LogSeverity.INFO,
                sessionId = sessionId
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordAuditLog(
        category: ActivityCategory,
        action: String,
        details: String,
        severity: LogSeverity = LogSeverity.INFO,
        deviceId: String = "",
        sessionId: String = ""
    ): Result<Unit> {
        var path = "users/unauthenticated/activity_logs"
        return try {
            val uid = requireUserId()
            val docRef = logsCol(uid).document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "category" to category.name,
                "action" to action.trim().ifBlank { "Audit Event" }.take(100),
                "details" to details.trim().ifBlank { "Recorded event" }.take(400),
                "severity" to severity.name,
                "deviceId" to deviceId.take(128),
                "sessionId" to sessionId.take(128),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: (error as? Exception) ?: return
        if (auth.currentUser?.uid == uid) {
            handleFirestoreError(toLog, op, path)
        }
    }
}
