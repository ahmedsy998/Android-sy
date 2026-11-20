package com.sentinel.android.core.navigation

object SentinelRoutes {
    const val ROUTE_SPLASH = "splash"
    const val ROUTE_AUTH = "auth"
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_DEVICES = "devices"
    const val ROUTE_ENROLLMENT = "enrollment"
    const val ROUTE_COMMAND = "command"
    const val ROUTE_LIVE_SESSION = "live_session"
    const val ROUTE_MEDIA_STUDIO = "media_studio"
    const val ROUTE_SECURITY = "security"
    const val ROUTE_ACTIVITY_LOGS = "activity_logs"
    const val ROUTE_SETTINGS = "settings"

    fun liveSessionRoute(sessionId: String): String = "$ROUTE_LIVE_SESSION/$sessionId"
}
