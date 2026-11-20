package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.sentinel.android.core.navigation.SentinelAppRoot
import com.sentinel.android.core.security.configureAppCheck

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch Intent BEFORE any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge insets and launch Sentinel root UI
        enableEdgeToEdge()
        setContent {
            SentinelAppRoot()
        }
    }
}
