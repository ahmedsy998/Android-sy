# Android Sentinel

**Consent-Based Remote Device Management & Transparent Monitoring Platform**

Android Sentinel is a native Android application built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Clean Architecture**, **MVVM**, **Room**, **DataStore**, **CameraX**, **MediaProjection**, **Android Storage Access Framework (SAF)**, **Firebase Authentication**, **Cloud Firestore**, **Firebase App Check**, and **Firebase AI Logic**.

All monitoring and sharing capabilities operate exclusively with **explicit, visible device-owner consent**. There is zero stealth execution, zero hidden recording, and zero silent storage scanning.

---

## Architecture

```
com.sentinel.android
├── core/
│   ├── common/         # UiState, NetworkMonitor, LocalizedStrings (EN LTR / AR RTL), structured Firestore error handler
│   ├── database/       # Room database (SentinelDatabase, Entities, DAOs) for offline cache & Media Studio library
│   ├── designsystem/   # Midnight futuristic theme (#080B12, #121C2A, #2878FF, #36C8F5), Dark/Light & RTL support, bundled fonts
│   ├── navigation/     # Auth-gated SentinelAppRoot, adaptive NavigationBar/NavigationRail, Navigation Compose routes
│   └── security/       # AppCheckConfig, Android Keystore AES-256-GCM crypto, ActionRateLimiter, BiometricPrompt helper
├── data/
│   ├── local/          # Jetpack DataStore preferences & Keystore-encrypted session tag
│   └── repository/     # SentinelFirestoreRepository & MediaStudioRepository (Firebase AI Logic)
├── domain/
│   └── model/          # @Keep @IgnoreExtraProperties @DocumentId models with safe enum parsing
├── feature/
│   ├── activity/       # Immutable Activity Audit Logs screen with category filtering
│   ├── auth/           # Animated SplashScreen, Google Credential Manager AuthScreen, BiometricLockScreen
│   ├── dashboard/      # Operations Dashboard & SentinelOperationsViewModel
│   ├── devices/        # Device Manager with live permission switches and permanent revocation
│   ├── enrollment/     # QR Code & Enrollment Code consent workflow + Enrollment Audit History
│   ├── media/          # Media Studio (Firebase AI Logic image synthesis, local PNG storage, safe FileProvider sharing)
│   ├── security/       # Security Center (Keystore status, token rotation, biometric lock, session timeout)
│   ├── sessions/       # Authorized Command Center (Photo Picker, SAF, CameraX, MediaProjection) & LiveSessionScreen
│   └── settings/       # Theme (Dark/Light/System) & Language (English LTR / Arabic RTL)
├── service/            # ScreenShareForegroundService (foregroundServiceType="mediaProjection") with ongoing Stop notification
└── di/                 # SentinelContainer manual DI graph
```

---

## Transparency & Security Guarantees

- **Zero-Trust Firestore Rules (`firestore.rules`)**: Default-deny catch-all, strict owner isolation under `/users/{userId}/...`, schema validation helpers on every `create`/`update`, terminal state locking on `enrollments` and `sessions`, immutable `activity_logs`, and verified via local unit tests (`firestore.test.js`).
- **Google Sign-In via Credential Manager**: Uses `GetSignInWithGoogleOption` for interactive sign-in and `GetGoogleIdOption` for silent restoration.
- **Hardware-Backed Local Crypto**: Uses `AndroidKeyStore` AES-256-GCM for local vault protection and `BiometricPrompt` for optional operator lock.
- **Google Play Policy Compliant**:
  - Uses zero-permission Android Photo Picker (`PickVisualMedia`) and Storage Access Framework (`OpenDocument`) — zero broad storage permissions.
  - Declares `android:foregroundServiceType="mediaProjection"` with a persistent, visible ongoing notification and immediate **Stop Session** action.
  - CameraX sessions display a live on-screen preview and active permission badges.
  - Media Studio generates standard clean PNG files via Firebase AI Logic with App Check attestation (no client API keys required).

---

## Firebase & Backend Configuration

1. Cloud Firestore Enterprise, Firebase Authentication (Google Sign-In), Firebase App Check, and Firebase AI Logic are provisioned via `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml`.
2. **Optional Extensions**: To enable push notifications via **Firebase Cloud Messaging (FCM)** or additional sign-in providers (such as Email/Password), enable those products directly in the Firebase Console for your provisioned project.
3. **Run Local Firestore Rules Tests**:
   ```bash
   FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
   ```

---

## Building the APK

```bash
gradle :app:assembleDebug
```
The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
