# FraudLens Android SDK

Standalone Gradle project that produces the **`fraudlens-android`** library (AAR) — voice/image/video integrity, payment-risk (IP + ML), Gemini SMS-style analysis, Firestore helpers, optional Compose UI, and Hilt modules.

**Flutter:** [`../fraudlens_flutter`](../fraudlens_flutter/) — method channel on **Android** (iOS stub).

**React Native:** [`../react-native-fraudlens`](../react-native-fraudlens/) — `NativeModules` / TS API on **Android** (iOS stub).

## Module

| Module | Description |
|--------|-------------|
| **fraudlens-android** | Full SDK surface (see **Capabilities** below) |

## Capabilities

| Area | What’s included |
|------|------------------|
| **Audio** | `FraudLensSdk.audio()` → `GET /health`, `POST /api/voice-detection` (multipart, default field `file`), optional `x-api-key` |
| **Image** | `FraudLensSdk.image()` → `GET /health`, `POST /api/image-detection` |
| **Video** | `FraudLensSdk.video()` → `GET /health`, `POST /api/video-detection` |
| **Payment / IP / ML** | `FraudLensSdk.paymentRisk()` → AbuseIPDB (`Key` header), fraud model `GET /health` + `POST /predict` |
| **Scam (Gemini)** | `FraudLensSdk.scamAnalysis()` → REST `gemini-2.5-flash`, JSON verdict (`ScamAnalysisResult`) |
| **Location** | `FraudLensLocation.getLastLocation` / `getLastLatLng` (needs runtime permission) |
| **Risk helpers** | `FraudLensRiskConstants`, `isLocationDeviationRisky` (default 100 km) |
| **Firebase** | `FraudLensFirestoreRepository` — sign-in by email/password, load transactions, VPA prefix search (demo schema) |
| **Compose** | `FraudLensHighRiskPaymentDialog` (Material3) |
| **Hilt** | `FraudLensFirebaseModule`, `FraudLensSdkHiltModule` (see **Hilt**) |

### `FraudLensConfig` fields

Set only what you use; leave others empty. **Do not commit production API keys.**

| Field | Used for |
|-------|-----------|
| `audioBaseUrl`, `audioApiKey` | Voice API (`x-api-key` when set) |
| `imageBaseUrl`, `imageApiKey` | Image API |
| `videoBaseUrl`, `videoApiKey` | Video API |
| `abuseIpDbBaseUrl` (default AbuseIPDB v2), `abuseIpDbApiKey` | IP reputation |
| `fraudModelBaseUrl` | FastAPI-style fraud model host |
| `geminiApiKey` | Google AI (`x-goog-api-key`) |
| `enableHttpLogging` | OkHttp body logging (**debug only**) |

### `FraudLensSdk` API

Call **`FraudLensSdk.initialize(FraudLensConfig(...))` once** (typically in `Application.onCreate`).

| Method | When it works |
|--------|----------------|
| `audio()` | `audioBaseUrl` is non-blank |
| `image()` | `imageBaseUrl` is non-blank |
| `video()` | `videoBaseUrl` is non-blank |
| `paymentRisk()` | Always after `initialize` (IP/model calls fail with a clear error if not configured) |
| `scamAnalysis()` | `geminiApiKey` is non-blank |
| `firestoreRepository()` | After `initialize` (uses `FirebaseFirestore.getInstance()`) |
| `firebaseFirestore()` | Raw Firestore instance |
| `clear()` | Drops SDK-held clients |
| `isInitialized()` | Whether `initialize` ran |

### HTTP paths (customize if your backends differ)

Default Retrofit paths under each media **base URL**:

- Voice: `health`, `api/voice-detection`
- Image: `health`, `api/image-detection`
- Video: `health`, `api/video-detection`

Change the internal `*DetectionApi` / `VoiceDetectionApi` interfaces if your Hugging Face Space or BFF uses different routes.

---

## Setup (build the AAR)

1. **Android SDK** — Copy `local.properties.example` to `local.properties` and set `sdk.dir` to a **real** path (or set `ANDROID_HOME`). In Android Studio: **Settings → Android SDK → Android SDK Location**.
2. **Terminal** — Do not run lines that start with `#` as commands; they are comments only.
3. From the **`fraudlens-sdk`** directory (if your shell prompt already shows `fraudlens-sdk`, skip `cd`):

   ```bash
   cd fraudlens-sdk
   ./gradlew :fraudlens-android:assembleRelease
   ```

   Output: `fraudlens-android/build/outputs/aar/fraudlens-android-release.aar`

---

## Use in an app

**`settings.gradle.kts`** (example — adjust paths):

```kotlin
include(":fraudlens-android")
project(":fraudlens-android").projectDir = file("../fraudlens-sdk/fraudlens-android")
```

**`app/build.gradle.kts`**

```kotlin
dependencies {
    implementation(project(":fraudlens-android"))
}
```

**Firebase** — If you use Firestore helpers, apply the Google Services plugin on the **app** module and add `google-services.json`.

---

## Example: `initialize` + coroutines

```kotlin
FraudLensSdk.initialize(
    FraudLensConfig(
        audioBaseUrl = "https://arshan123-vnitx-audio.hf.space",
        audioApiKey = BuildConfig.AUDIO_API_KEY, // never commit real keys
        imageBaseUrl = "https://arshan123-vnitx-image.hf.space",
        imageApiKey = BuildConfig.IMAGE_API_KEY,
        videoBaseUrl = "https://arshan123-vnitx-video.hf.space",
        videoApiKey = BuildConfig.VIDEO_API_KEY,
        abuseIpDbApiKey = BuildConfig.ABUSEIPDB_KEY,
        fraudModelBaseUrl = "https://fraudlens-fastapi.onrender.com/",
        geminiApiKey = BuildConfig.GEMINI_API_KEY,
        enableHttpLogging = BuildConfig.DEBUG,
    ),
)

// Audio (suspend)
val healthJson = FraudLensSdk.audio().health().getOrNull()
val voiceJson = FraudLensSdk.audio().detectVoice(
    VoiceDetectionRequest(audioBytes, "clip.wav", "audio/wav"),
).getOrNull()

// Payment risk
val ipResult = FraudLensSdk.paymentRisk().checkIpReputation("8.8.8.8")
val modelResult = FraudLensSdk.paymentRisk().predictFraud(modelInput)

// Scam text
when (val r = FraudLensSdk.scamAnalysis().analyzeMessage("Sender: X\nMessage: ...")) {
    is ScamAnalysisResult.Success -> { /* r.analysis */ }
    is ScamAnalysisResult.Error -> { /* r.message */ }
    else -> { /* RateLimited, RawText */ }
}

// Firestore (suspend)
val user = FraudLensSdk.firestoreRepository().signInWithEmailPassword(email, password)
```

---

## Hilt

Two modules ship in **`com.fraudlens.sdk.di`**:

1. **`FraudLensFirebaseModule`** — provides `FirebaseFirestore` and `FraudLensFirestoreRepository` (no `FraudLensSdk.initialize` required for these two).
2. **`FraudLensSdkHiltModule`** — provides `AudioIntegrityClient`, `ImageIntegrityClient`, `VideoIntegrityClient`, `PaymentRiskClient`, `ScamAnalysisClient` via **`FraudLensSdk`**.

**Important:** Call **`FraudLensSdk.initialize(...)` before `super.onCreate()`** in your `@HiltAndroidApp` `Application` if you inject anything from `FraudLensSdkHiltModule`. Injecting a client that was **not** configured in `FraudLensConfig` will fail at runtime.

---

## Compose

```kotlin
import com.fraudlens.sdk.ui.risk.FraudLensHighRiskPaymentDialog

FraudLensHighRiskPaymentDialog(
    visible = showDialog,
    title = "Transaction blocked",
    bodyLines = listOf("Reason one", "Reason two"),
    countdownSeconds = 15,
    confirmLabel = "Verify",
    dismissLabel = "Cancel",
    onDismiss = { showDialog = false },
    onConfirm = { /* step-up */ },
)
```

---

## Merged manifest (library)

The SDK library declares **`INTERNET`** and **location** permissions so host apps merge them automatically. You still need **runtime** location permission before calling `FraudLensLocation`.

---

## Security

- Keep keys in **`local.properties` → BuildConfig**, remote config, or your BFF — not in Git.
- Treat **`enableHttpLogging = true`** as **debug-only** (may log bodies).

---

## Next steps

- Publish the AAR with **`maven-publish`** (Maven Central or private registry).
- Align image/video route names with your deployed Spaces or BFF OpenAPI.
