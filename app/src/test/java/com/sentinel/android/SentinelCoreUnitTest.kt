package com.sentinel.android

import com.sentinel.android.core.security.ActionRateLimiter
import com.sentinel.android.core.security.SentinelKeystoreCrypto
import com.sentinel.android.domain.model.ActivityCategory
import com.sentinel.android.domain.model.ActivityLog
import com.sentinel.android.domain.model.ConnectionQuality
import com.sentinel.android.domain.model.DeviceConnectionStatus
import com.sentinel.android.domain.model.EnrolledDevice
import com.sentinel.android.domain.model.EnrollmentStatus
import com.sentinel.android.domain.model.LogSeverity
import com.sentinel.android.domain.model.MonitoringSession
import com.sentinel.android.domain.model.SessionStatus
import com.sentinel.android.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SentinelCoreUnitTest {

    @Test
    fun `enrollment code follows SNTL format and avoids ambiguous characters`() {
        val crypto = SentinelKeystoreCrypto()
        val codeA = crypto.generateEnrollmentCode()
        val codeB = crypto.generateEnrollmentCode()

        assertTrue(codeA.matches(Regex("^SNTL-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$")))
        assertTrue(codeB.matches(Regex("^SNTL-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$")))
        assertNotEquals(codeA, codeB)
    }

    @Test
    fun `sha256 short fingerprint is deterministic and 8 colon-separated hex bytes`() {
        val crypto = SentinelKeystoreCrypto()
        val fp1 = crypto.sha256ShortFingerprint("sentinel-device-001")
        val fp2 = crypto.sha256ShortFingerprint("sentinel-device-001")
        val fp3 = crypto.sha256ShortFingerprint("sentinel-device-002")

        assertEquals(fp1, fp2)
        assertNotEquals(fp1, fp3)
        assertTrue(fp1.matches(Regex("^([0-9A-F]{2}:){7}[0-9A-F]{2}$")))
    }

    @Test
    fun `action rate limiter enforces window limit per key`() {
        val limiter = ActionRateLimiter(maxActionsPerWindow = 2, windowMillis = 60_000L)

        assertTrue(limiter.allow("enroll"))
        assertTrue(limiter.allow("enroll"))
        assertFalse(limiter.allow("enroll"))
        assertTrue(limiter.allow("session"))
    }

    @Test
    fun `domain models safely fall back on unknown enum values`() {
        val device = EnrolledDevice(connectionStatus = "UNKNOWN_STATUS")
        assertEquals(DeviceConnectionStatus.OFFLINE, device.statusEnum())

        val session = MonitoringSession(
            sessionType = "UNKNOWN_TYPE",
            status = "UNKNOWN_STATE",
            connectionQuality = "UNKNOWN_QUALITY"
        )
        assertEquals(SessionType.SCREEN_SHARE, session.typeEnum())
        assertEquals(SessionStatus.TERMINATED, session.statusEnum())
        assertEquals(ConnectionQuality.GOOD, session.qualityEnum())

        val log = ActivityLog(category = "UNKNOWN_CAT", severity = "UNKNOWN_SEV")
        assertEquals(ActivityCategory.SECURITY, log.categoryEnum())
        assertEquals(LogSeverity.INFO, log.severityEnum())
        assertEquals(EnrollmentStatus.PENDING, EnrollmentStatus.fromRaw("UNKNOWN"))
    }
}
