package com.sentinel.android.feature.media

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sentinel.android.data.repository.MediaStudioRepository
import com.sentinel.android.data.repository.SentinelFirestoreRepository
import com.sentinel.android.domain.model.ActivityCategory
import com.sentinel.android.domain.model.LogSeverity
import com.sentinel.android.domain.model.StudioMediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface StudioGenerationUiState {
    data object Idle : StudioGenerationUiState
    data object Loading : StudioGenerationUiState
    data class Success(val bitmap: Bitmap, val prompt: String, val savedPath: String? = null) : StudioGenerationUiState
    data class Error(val message: String) : StudioGenerationUiState
}

class MediaStudioViewModel(
    private val mediaStudioRepository: MediaStudioRepository,
    private val auditRepository: SentinelFirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _genState = MutableStateFlow<StudioGenerationUiState>(StudioGenerationUiState.Idle)
    val genState: StateFlow<StudioGenerationUiState> = _genState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val savedMedia: StateFlow<List<StudioMediaItem>> =
        mediaStudioRepository.observeSavedMedia(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun clearStatus() {
        _statusMessage.value = null
    }

    fun generateImage(prompt: String) {
        viewModelScope.launch {
            _genState.value = StudioGenerationUiState.Loading
            val result = mediaStudioRepository.generateOriginalImage(prompt)
            result.fold(
                onSuccess = { bitmap ->
                    _genState.value = StudioGenerationUiState.Success(bitmap = bitmap, prompt = prompt.trim())
                    auditRepository.recordAuditLog(
                        category = ActivityCategory.MEDIA_STUDIO,
                        action = "Original Image Generated",
                        details = "Synthesized clean PNG artwork via Firebase AI Logic for prompt '${prompt.trim().take(80)}'.",
                        severity = LogSeverity.SUCCESS
                    )
                },
                onFailure = { err ->
                    _genState.value = StudioGenerationUiState.Error(
                        err.localizedMessage ?: "Image generation failed."
                    )
                }
            )
        }
    }

    fun saveCurrentBitmap() {
        val current = _genState.value as? StudioGenerationUiState.Success ?: return
        viewModelScope.launch {
            val result = mediaStudioRepository.saveBitmapLocally(
                userId = currentUserId,
                prompt = current.prompt,
                bitmap = current.bitmap
            )
            result.fold(
                onSuccess = { saved ->
                    _genState.value = current.copy(savedPath = saved.filePath)
                    _statusMessage.value = "Image saved locally as standard PNG."
                    auditRepository.recordAuditLog(
                        category = ActivityCategory.MEDIA_STUDIO,
                        action = "Studio Image Saved Locally",
                        details = "Saved standard PNG asset to encrypted app storage.",
                        severity = LogSeverity.INFO
                    )
                },
                onFailure = { err ->
                    _statusMessage.value = err.localizedMessage ?: "Failed to save image."
                }
            )
        }
    }

    fun getShareUri(filePath: String): Uri? = mediaStudioRepository.getShareableUri(filePath)

    fun deleteItem(item: StudioMediaItem) {
        viewModelScope.launch {
            mediaStudioRepository.deleteSavedMedia(currentUserId, item)
            _statusMessage.value = "Removed image from local Media Studio library."
        }
    }

    companion object {
        fun provideFactory(
            mediaStudioRepository: MediaStudioRepository,
            auditRepository: SentinelFirestoreRepository,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MediaStudioViewModel(mediaStudioRepository, auditRepository, currentUserId)
            }
        }
    }
}
