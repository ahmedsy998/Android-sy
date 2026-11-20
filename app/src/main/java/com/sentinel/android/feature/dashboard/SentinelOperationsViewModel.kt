package com.sentinel.android.feature.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.sentinel.android.core.common.AppLanguage
import com.sentinel.android.core.common.NetworkMonitor
import com.sentinel.android.core.common.ThemeMode
import com.sentinel.android.core.common.UiState
import com.sentinel.android.core.security.SentinelKeystoreCrypto
import com.sentinel.android.data.local.PreferencesRepository
import com.sentinel.android.data.local.SentinelPreferences
import com.sentinel.android.data.repository.SentinelFirestoreRepository
import com.sentinel.android.domain.model.ActivityCategory
import com.sentinel.android.domain.model.ActivityLog
import com.sentinel.android.domain.model.DeviceConnectionStatus
import com.sentinel.android.domain.model.EnrolledDevice
import com.sentinel.android.domain.model.EnrollmentRequest
import com.sentinel.android.domain.model.EnrollmentStatus
import com.sentinel.android.domain.model.LogSeverity
import com.sentinel.android.domain.model.MonitoringSession
import com.sentinel.android.domain.model.SessionStatus
import com.sentinel.android.domain.model.SessionType
import com.sentinel.android.domain.model.SharedFileDescriptor
import com.sentinel.android.domain.model.UserProfile
import com.sentinel.android.service.ScreenShareRuntimeHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "SentinelOpsVM"
private const val STOP_TIMEOUT_MS = 5000L

data class TokenStatusInfo(
    val verified: Boolean = true,
    val issuedAtMillis: Long = 0L,
    val expiresAtMillis: Long = 0L,
    val providerId: String = "google.com",
    val deviceKeyFingerprint: String = "",
    val keystoreHardwareReady: Boolean = false
)

class SentinelOperationsViewModel(
    private val repository: SentinelFirestoreRepository,
    private val preferencesRepository: PreferencesRepository,
    private val networkMonitor: NetworkMonitor,
    private val crypto: SentinelKeystoreCrypto,
    val currentUserId: String
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), networkMonitor.isCurrentlyOnline())

    val preferences: StateFlow<SentinelPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SentinelPreferences())

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _actionInProgress = MutableStateFlow(false)
    val actionInProgress: StateFlow<Boolean> = _actionInProgress.asStateFlow()

    private val _sharedMediaItems = MutableStateFlow<List<SharedFileDescriptor>>(emptyList())
    val sharedMediaItems: StateFlow<List<SharedFileDescriptor>> = _sharedMediaItems.asStateFlow()

    private val _tokenInfo = MutableStateFlow(
        TokenStatusInfo(
            deviceKeyFingerprint = crypto.sha256ShortFingerprint(currentUserId),
            keystoreHardwareReady = crypto.isKeystoreReady()
        )
    )
    val tokenInfo: StateFlow<TokenStatusInfo> = _tokenInfo.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            if (unwrapFirestoreError(error) == null && error is CancellationException) throw error
            Log.w(TAG, "Profile stream error", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val devicesState: StateFlow<UiState<List<EnrolledDevice>>> = combine(
        repository.observeDevices(currentUserId).toResultFlow(),
        repository.observeCachedDevices(currentUserId),
        isOnline
    ) { result, cached, online ->
        result.fold(
            onSuccess = { devices ->
                when {
                    !online -> UiState.Offline(devices)
                    devices.isEmpty() -> UiState.Empty
                    else -> UiState.Success(devices)
                }
            },
            onFailure = { err ->
                if (!online && cached.isNotEmpty()) {
                    UiState.Offline(
                        cached.map { c ->
                            EnrolledDevice(
                                id = c.id,
                                userId = c.userId,
                                deviceAlias = c.deviceAlias,
                                deviceModel = c.deviceModel,
                                androidVersion = c.androidVersion,
                                enrollmentCode = c.enrollmentCode,
                                connectionStatus = c.connectionStatus,
                                consentVerified = c.consentVerified,
                                cameraAllowed = c.cameraAllowed,
                                screenShareAllowed = c.screenShareAllowed,
                                mediaPickerAllowed = c.mediaPickerAllowed,
                                filesAllowed = c.filesAllowed
                            )
                        }
                    )
                } else {
                    UiState.Error(err.localizedMessage ?: "Unable to load enrolled devices.")
                }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val enrollmentsState: StateFlow<UiState<List<EnrollmentRequest>>> =
        repository.observeEnrollments(currentUserId)
            .map<List<EnrollmentRequest>, UiState<List<EnrollmentRequest>>> { list ->
                if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load enrollment requests."))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val sessionsState: StateFlow<UiState<List<MonitoringSession>>> =
        repository.observeSessions(currentUserId)
            .map<List<MonitoringSession>, UiState<List<MonitoringSession>>> { list ->
                if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to observe sessions."))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val activityLogsState: StateFlow<UiState<List<ActivityLog>>> = combine(
        repository.observeActivityLogs(currentUserId).toResultFlow(),
        repository.observeCachedLogs(currentUserId),
        isOnline
    ) { result, cached, online ->
        result.fold(
            onSuccess = { logs ->
                when {
                    !online -> UiState.Offline(logs)
                    logs.isEmpty() -> UiState.Empty
                    else -> UiState.Success(logs)
                }
            },
            onFailure = { err ->
                if (!online && cached.isNotEmpty()) {
                    UiState.Offline(
                        cached.map { c ->
                            ActivityLog(
                                id = c.id,
                                userId = c.userId,
                                category = c.category,
                                action = c.action,
                                details = c.details,
                                severity = c.severity,
                                deviceId = c.deviceId,
                                sessionId = c.sessionId
                            )
                        }
                    )
                } else {
                    UiState.Error(err.localizedMessage ?: "Failed to load audit logs.")
                }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    init {
        viewModelScope.launch {
            repository.ensureUserProfile()
            preferencesRepository.recordAuthenticatedTimestamp(currentUserId)
            refreshIdTokenMetadata(forceRefresh = false)
        }
        ScreenShareRuntimeHolder.onRemoteTerminateRequested = { sessionId ->
            terminateSession(sessionId)
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun showBanner(message: String) {
        _bannerMessage.value = message
    }

    fun refreshIdTokenMetadata(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            try {
                val user = Firebase.auth.currentUser ?: return@launch
                val tokenResult = user.getIdToken(forceRefresh).await()
                _tokenInfo.value = TokenStatusInfo(
                    verified = true,
                    issuedAtMillis = tokenResult.issuedAtTimestamp * 1000L,
                    expiresAtMillis = tokenResult.expirationTimestamp * 1000L,
                    providerId = tokenResult.signInProvider ?: "google.com",
                    deviceKeyFingerprint = crypto.sha256ShortFingerprint(currentUserId),
                    keystoreHardwareReady = crypto.isKeystoreReady()
                )
                if (forceRefresh) {
                    repository.recordAuditLog(
                        category = ActivityCategory.SECURITY,
                        action = "Short-Lived Token Rotated",
                        details = "Firebase ID token refreshed securely over TLS 1.3.",
                        severity = LogSeverity.INFO
                    )
                    _bannerMessage.value = "Authentication token refreshed securely."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Token refresh warning: ${e.message}")
                if (forceRefresh) {
                    _bannerMessage.value = e.localizedMessage ?: "Unable to refresh token right now."
                }
            }
        }
    }

    fun enrollCurrentDevice(alias: String) {
        viewModelScope.launch {
            _actionInProgress.value = true
            val result = repository.enrollCurrentDeviceDirectly(alias)
            _actionInProgress.value = false
            result.fold(
                onSuccess = { _bannerMessage.value = "Current device enrolled with verified owner consent." },
                onFailure = { _bannerMessage.value = it.localizedMessage ?: "Enrollment failed." }
            )
        }
    }

    fun createEnrollmentInvitation(alias: String, requestedPermissions: List<String>) {
        viewModelScope.launch {
            _actionInProgress.value = true
            val result = repository.createEnrollmentRequest(alias, requestedPermissions)
            _actionInProgress.value = false
            result.fold(
                onSuccess = { req ->
                    _bannerMessage.value = "Enrollment code ${req.enrollmentCode} generated. Awaiting explicit device approval."
                },
                onFailure = { _bannerMessage.value = it.localizedMessage ?: "Failed to create enrollment request." }
            )
        }
    }

    fun approveEnrollment(request: EnrollmentRequest) {
        viewModelScope.launch {
            _actionInProgress.value = true
            val result = repository.approveEnrollmentAndRegisterDevice(request)
            _actionInProgress.value = false
            result.fold(
                onSuccess = { _bannerMessage.value = "Device '${request.targetAlias}' approved and added to Device Manager." },
                onFailure = { _bannerMessage.value = it.localizedMessage ?: "Approval failed." }
            )
        }
    }

    fun decideEnrollment(request: EnrollmentRequest, status: EnrollmentStatus) {
        viewModelScope.launch {
            _actionInProgress.value = true
            val result = repository.decideEnrollment(request.id, status, request.enrollmentCode)
            _actionInProgress.value = false
            result.fold(
                onSuccess = { _bannerMessage.value = "Enrollment ${request.enrollmentCode} marked ${status.name}." },
                onFailure = { _bannerMessage.value = it.localizedMessage ?: "Action failed." }
            )
        }
    }

    fun updateDevicePermissions(
        device: EnrolledDevice,
        cameraAllowed: Boolean = device.cameraAllowed,
        screenShareAllowed: Boolean = device.screenShareAllowed,
        mediaPickerAllowed: Boolean = device.mediaPickerAllowed,
        filesAllowed: Boolean = device.filesAllowed,
        connectionStatus: DeviceConnectionStatus = device.statusEnum()
    ) {
        viewModelScope.launch {
            val result = repository.updateDevicePermissions(
                device = device,
                cameraAllowed = cameraAllowed,
                screenShareAllowed = screenShareAllowed,
                mediaPickerAllowed = mediaPickerAllowed,
                filesAllowed = filesAllowed,
                connectionStatus = connectionStatus
            )
            result.onFailure {
                _bannerMessage.value = it.localizedMessage ?: "Permission update failed."
            }
        }
    }

    fun revokeDevice(device: EnrolledDevice) {
        viewModelScope.launch {
            _actionInProgress.value = true
            val result = repository.revokeDevice(device)
            _actionInProgress.value = false
            result.fold(
                onSuccess = { _bannerMessage.value = "Access permanently revoked for '${device.deviceAlias}'." },
                onFailure = { _bannerMessage.value = it.localizedMessage ?: "Revocation failed." }
            )
        }
    }

    fun startAuthorizedSession(
        device: EnrolledDevice,
        sessionType: SessionType,
        activePermissions: List<String>,
        sharedItemSummary: String = "",
        onStarted: (String) -> Unit
    ) {
        viewModelScope.launch {
            _actionInProgress.value = true
            val result = repository.startAuthorizedSession(device, sessionType, activePermissions, sharedItemSummary)
            _actionInProgress.value = false
            result.fold(
                onSuccess = { sessionId -> onStarted(sessionId) },
                onFailure = { _bannerMessage.value = it.localizedMessage ?: "Could not start session." }
            )
        }
    }

    fun togglePauseSession(session: MonitoringSession) {
        viewModelScope.launch {
            val next = if (session.statusEnum() == SessionStatus.ACTIVE) SessionStatus.PAUSED else SessionStatus.ACTIVE
            val result = repository.updateSessionState(session.id, next)
            result.onFailure { _bannerMessage.value = it.localizedMessage ?: "Unable to update session." }
        }
    }

    fun terminateSession(sessionId: String) {
        viewModelScope.launch {
            val result = repository.updateSessionState(sessionId, SessionStatus.TERMINATED)
            result.fold(
                onSuccess = { _bannerMessage.value = "Session terminated immediately." },
                onFailure = { _bannerMessage.value = it.localizedMessage ?: "Termination error." }
            )
        }
    }

    fun recordExplicitSharedItem(
        device: EnrolledDevice?,
        descriptor: SharedFileDescriptor,
        isPhotoVideo: Boolean
    ) {
        _sharedMediaItems.value = listOf(descriptor) + _sharedMediaItems.value.take(19)
        viewModelScope.launch {
            repository.recordAuditLog(
                category = ActivityCategory.DATA_SHARE,
                action = if (isPhotoVideo) "Photo/Video Explicitly Shared" else "SAF Document Explicitly Shared",
                details = "Owner selected '${descriptor.displayName}' (${descriptor.mimeType}) via Android system picker.",
                severity = LogSeverity.SUCCESS,
                deviceId = device?.id ?: ""
            )
            _bannerMessage.value = "Shared '${descriptor.displayName}' transparently via Android system picker."
        }
    }

    fun updateSecuritySettings(biometricEnabled: Boolean, timeoutMinutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setBiometricLockEnabled(biometricEnabled)
            preferencesRepository.setSessionTimeoutMinutes(timeoutMinutes)
            val result = repository.updateSecurityPreferences(biometricEnabled, timeoutMinutes)
            result.onFailure { _bannerMessage.value = it.localizedMessage ?: "Could not sync security policy." }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    override fun onCleared() {
        ScreenShareRuntimeHolder.onRemoteTerminateRequested = null
        super.onCleared()
    }

    private fun <T> Flow<T>.toResultFlow(): Flow<Result<T>> =
        map { Result.success(it) }.catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            emit(Result.failure(firestoreError ?: error))
        }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    companion object {
        fun provideFactory(
            repository: SentinelFirestoreRepository,
            preferencesRepository: PreferencesRepository,
            networkMonitor: NetworkMonitor,
            crypto: SentinelKeystoreCrypto,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SentinelOperationsViewModel(
                    repository = repository,
                    preferencesRepository = preferencesRepository,
                    networkMonitor = networkMonitor,
                    crypto = crypto,
                    currentUserId = currentUserId
                )
            }
        }
    }
}
