package com.sentinel.android

import android.app.Application
import com.sentinel.android.di.SentinelContainer

class SentinelApplication : Application() {
    // Note: Firebase App Check is intentionally configured in MainActivity.onCreate()
    // with the launch Intent, never here.
    lateinit var container: SentinelContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = SentinelContainer(this)
    }
}
