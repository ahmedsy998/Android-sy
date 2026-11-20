package com.sentinel.android.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenShareRuntimeState(
    val active: Boolean = false,
    val paused: Boolean = false,
    val sessionId: String? = null,
    val previewFrame: Bitmap? = null,
    val frameCount: Int = 0,
    val errorMessage: String? = null
)

object ScreenShareRuntimeHolder {
    private val _state = MutableStateFlow(ScreenShareRuntimeState())
    val state: StateFlow<ScreenShareRuntimeState> = _state.asStateFlow()

    var pendingResultCode: Int = Activity.RESULT_CANCELED
    var pendingResultData: Intent? = null
    var onRemoteTerminateRequested: ((String) -> Unit)? = null

    fun updateState(transform: (ScreenShareRuntimeState) -> ScreenShareRuntimeState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        pendingResultCode = Activity.RESULT_CANCELED
        pendingResultData = null
        _state.value = ScreenShareRuntimeState()
    }
}

class ScreenShareForegroundService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                val activeId = ScreenShareRuntimeHolder.state.value.sessionId
                stopProjectionAndSelf()
                if (activeId != null) {
                    ScreenShareRuntimeHolder.onRemoteTerminateRequested?.invoke(activeId)
                }
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                ScreenShareRuntimeHolder.updateState { it.copy(paused = true) }
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                ScreenShareRuntimeHolder.updateState { it.copy(paused = false) }
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: ""
                ensureNotificationChannel()
                val notification = buildOngoingNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                startMediaProjection(sessionId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startMediaProjection(sessionId: String) {
        try {
            val resultCode = ScreenShareRuntimeHolder.pendingResultCode
            val resultData = ScreenShareRuntimeHolder.pendingResultData
            if (resultCode != Activity.RESULT_OK || resultData == null) {
                ScreenShareRuntimeHolder.updateState {
                    it.copy(active = false, errorMessage = "Screen share permission token missing.")
                }
                stopSelf()
                return
            }

            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(resultCode, resultData)
            if (projection == null) {
                ScreenShareRuntimeHolder.updateState {
                    it.copy(active = false, errorMessage = "MediaProjection could not be created.")
                }
                stopSelf()
                return
            }
            mediaProjection = projection
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopProjectionAndSelf()
                }
            }, mainHandler)

            val metrics = resources.displayMetrics
            val captureWidth = (metrics.widthPixels / 2).coerceIn(360, 720)
            val captureHeight = (metrics.heightPixels / 2).coerceIn(640, 1280)
            val densityDpi = metrics.densityDpi

            val reader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            reader.setOnImageAvailableListener({ r ->
                val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
                try {
                    if (!ScreenShareRuntimeHolder.state.value.paused) {
                        val plane = image.planes[0]
                        val buffer = plane.buffer
                        val pixelStride = plane.pixelStride
                        val rowStride = plane.rowStride
                        val rowPadding = rowStride - pixelStride * captureWidth
                        val rawBitmap = Bitmap.createBitmap(
                            captureWidth + rowPadding / pixelStride,
                            captureHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        rawBitmap.copyPixelsFromBuffer(buffer)
                        val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, captureWidth, captureHeight)
                        rawBitmap.recycle()
                        ScreenShareRuntimeHolder.updateState {
                            it.copy(
                                active = true,
                                sessionId = sessionId,
                                previewFrame = cropped,
                                frameCount = it.frameCount + 1,
                                errorMessage = null
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Frame capture skipped: ${e.message}")
                } finally {
                    image.close()
                }
            }, mainHandler)

            virtualDisplay = projection.createVirtualDisplay(
                "SentinelScreenShare",
                captureWidth,
                captureHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                mainHandler
            )

            ScreenShareRuntimeHolder.updateState {
                it.copy(active = true, paused = false, sessionId = sessionId, errorMessage = null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaProjection", e)
            ScreenShareRuntimeHolder.updateState {
                it.copy(active = false, errorMessage = e.localizedMessage ?: "Screen sharing failed.")
            }
            stopProjectionAndSelf()
        }
    }

    private fun stopProjectionAndSelf() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        ScreenShareRuntimeHolder.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        runCatching { mediaProjection?.stop() }
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_screen_share),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_screen_share_body)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildOngoingNotification(): Notification {
        val stopIntent = Intent(this, ScreenShareForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1001,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_screen_share_title))
            .setContentText(getString(R.string.notification_screen_share_body))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_stop_session),
                stopPendingIntent
            )
            .build()
    }

    companion object {
        private const val TAG = "SentinelScreenShareSvc"
        private const val CHANNEL_ID = "sentinel_screen_share_channel"
        private const val NOTIFICATION_ID = 7001

        const val ACTION_START = "com.sentinel.android.action.START_SCREEN_SHARE"
        const val ACTION_STOP = "com.sentinel.android.action.STOP_SCREEN_SHARE"
        const val ACTION_PAUSE = "com.sentinel.android.action.PAUSE_SCREEN_SHARE"
        const val ACTION_RESUME = "com.sentinel.android.action.RESUME_SCREEN_SHARE"
        const val EXTRA_SESSION_ID = "extra_session_id"

        fun start(context: Context, sessionId: String, resultCode: Int, data: Intent) {
            ScreenShareRuntimeHolder.pendingResultCode = resultCode
            ScreenShareRuntimeHolder.pendingResultData = data
            val intent = Intent(context, ScreenShareForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenShareForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun setPaused(context: Context, paused: Boolean) {
            val intent = Intent(context, ScreenShareForegroundService::class.java).apply {
                action = if (paused) ACTION_PAUSE else ACTION_RESUME
            }
            context.startService(intent)
        }
    }
}
