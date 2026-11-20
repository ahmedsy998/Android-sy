package com.sentinel.android.di

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.sentinel.android.core.common.NetworkMonitor
import com.sentinel.android.core.database.SentinelDatabase
import com.sentinel.android.core.security.ActionRateLimiter
import com.sentinel.android.core.security.SentinelKeystoreCrypto
import com.sentinel.android.data.local.PreferencesRepository
import com.sentinel.android.data.repository.MediaStudioRepository
import com.sentinel.android.data.repository.SentinelFirestoreRepository

class SentinelContainer(context: Context) {
    private val appContext = context.applicationContext

    val crypto: SentinelKeystoreCrypto = SentinelKeystoreCrypto()
    val rateLimiter: ActionRateLimiter = ActionRateLimiter(maxActionsPerWindow = 8, windowMillis = 30_000L)
    val networkMonitor: NetworkMonitor = NetworkMonitor(appContext)
    val database: SentinelDatabase = SentinelDatabase.getInstance(appContext)
    val preferencesRepository: PreferencesRepository = PreferencesRepository(appContext, crypto)

    // Always bind to the single provisioned enterprise database ID from firebase_applet_config.xml
    val firestore: FirebaseFirestore by lazy {
        val databaseId = appContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }

    val firestoreRepository: SentinelFirestoreRepository by lazy {
        SentinelFirestoreRepository(
            db = firestore,
            deviceCacheDao = database.deviceCacheDao(),
            activityLogCacheDao = database.activityLogCacheDao(),
            crypto = crypto,
            rateLimiter = rateLimiter
        )
    }

    val mediaStudioRepository: MediaStudioRepository by lazy {
        MediaStudioRepository(
            context = appContext,
            studioMediaDao = database.studioMediaDao()
        )
    }
}
