package com.sentinel.android.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.generationConfig
import com.sentinel.android.core.database.StudioMediaDao
import com.sentinel.android.core.database.StudioMediaEntity
import com.sentinel.android.domain.model.StudioMediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream

private const val TAG = "MediaStudioRepo"

class MediaStudioRepository(
    private val context: Context,
    private val studioMediaDao: StudioMediaDao
) {
    // Lazy initialization prevents eager Firebase.ai startup issues
    private val imageModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

    fun observeSavedMedia(userId: String): Flow<List<StudioMediaItem>> =
        studioMediaDao.observeMedia(userId).map { list ->
            list.map {
                StudioMediaItem(
                    id = it.id,
                    prompt = it.prompt,
                    filePath = it.filePath,
                    createdAtMillis = it.createdAtMillis
                )
            }
        }

    suspend fun generateOriginalImage(prompt: String): Result<Bitmap> {
        val sanitizedPrompt = prompt.trim()
        if (sanitizedPrompt.length < 3) {
            return Result.failure(IllegalArgumentException("Please describe the image you want to create (at least 3 characters)."))
        }
        return try {
            val bitmap = withTimeout(60_000L) {
                val response = imageModel.generateContent(sanitizedPrompt)
                response.candidates.firstOrNull()
                    ?.content?.parts
                    ?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                    ?.image
            }
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("The image generation model returned no image output."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            Result.failure(IllegalStateException("Image generation timed out after 60 seconds. Please try again."))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image generation quota exceeded", e)
            Result.failure(IllegalStateException("Rate limit or billing quota reached for image generation. Please check Firebase Blaze plan quota."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun saveBitmapLocally(
        userId: String,
        prompt: String,
        bitmap: Bitmap
    ): Result<StudioMediaItem> = withContext(Dispatchers.IO) {
        try {
            val mediaDir = File(context.filesDir, "studio_media").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val fileName = "sentinel_studio_$timestamp.png"
            val outFile = File(mediaDir, fileName)
            FileOutputStream(outFile).use { stream ->
                // Clean standard PNG encoding only; no hidden payloads or executable metadata
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }
            val entity = StudioMediaEntity(
                userId = userId,
                prompt = prompt.trim().take(240),
                filePath = outFile.absolutePath,
                createdAtMillis = timestamp
            )
            val insertedId = studioMediaDao.insert(entity)
            Result.success(
                StudioMediaItem(
                    id = insertedId,
                    prompt = entity.prompt,
                    filePath = entity.filePath,
                    createdAtMillis = timestamp
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save PNG locally", e)
            Result.failure(e)
        }
    }

    fun getShareableUri(filePath: String): Uri? {
        val file = File(filePath)
        if (!file.exists()) return null
        val allowedDir = File(context.filesDir, "studio_media").canonicalFile
        if (!file.canonicalFile.path.startsWith(allowedDir.path + File.separator)) {
            return null
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun deleteSavedMedia(userId: String, item: StudioMediaItem) = withContext(Dispatchers.IO) {
        studioMediaDao.deleteById(item.id, userId)
        val file = File(item.filePath)
        val allowedDir = File(context.filesDir, "studio_media").canonicalFile
        if (file.exists() && file.canonicalFile.path.startsWith(allowedDir.path + File.separator)) {
            file.delete()
        }
    }
}
