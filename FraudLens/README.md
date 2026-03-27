# FraudLens Android App

This is the native Android app module in this monorepo.

## 1) Clone and open

```bash
git clone <your-fork-or-repo-url>
cd FraudLens_Mobile_App/FraudLens
```

Open `FraudLens/` in Android Studio.

## 2) Configure secrets (required)

1. Copy `local.properties.example` to `local.properties`.
2. Keep your existing `sdk.dir` line.
3. Add your own keys:

```properties
GEMINI_API_KEY=your_gemini_key
IPDB_API_KEY=your_abuseipdb_key
RAZORPAY_KEY_ID=rzp_test_or_live_key_id
RAZORPAY_KEY_SECRET=your_razorpay_secret
```

The app injects these into `BuildConfig` at compile time.

## 3) Firebase setup (required for Firestore features)

1. In Firebase Console, create/select your Android app with package `com.example.fraudlens`.
2. Download `google-services.json`.
3. Place it at `FraudLens/app/google-services.json`.

This file is ignored by git and must not be committed.

## 4) Run

- From Android Studio: select the `app` configuration and click Run.
- From terminal:

```bash
./gradlew :app:installDebug
```

## 5) Key rotation guidance

If any key was ever committed or pushed:

1. Revoke/rotate in provider console (Gemini, AbuseIPDB, Razorpay, Firebase API restrictions).
2. Update your local `local.properties`.
3. Rebuild the app.
