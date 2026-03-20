# fraudlens_flutter

Flutter plugin that talks to the **same FraudLens logic** as the native Android SDK by calling **`fraudlens-android`** through a **MethodChannel**.

| Platform | Behavior |
|----------|-----------|
| **Android** | Uses `FraudLensSdk` from `../fraudlens-sdk/fraudlens-android` (you must wire the Gradle module). |
| **iOS** | **Stub** — every method returns `UNIMPLEMENTED`. Use `package:http` (or similar) to your backends from Dart, or add Swift code later. |

## 1. Add the Android library to the host Gradle project

Flutter’s Android build must know about `:fraudlens-android`. In **`android/settings.gradle.kts`** (for your app or the **`example/`** app), **before** plugins resolve the Flutter embedding, add:

```kotlin
include(":fraudlens-android")
project(":fraudlens-android").projectDir =
    file("../../fraudlens-sdk/fraudlens-android") // adjust path from your android/ folder to the SDK module
```

Path cheat sheet (this monorepo):

| Your `android/` folder | Use |
|------------------------|-----|
| `fraudlens_flutter/example/android` | `file("../../../fraudlens-sdk/fraudlens-android")` |
| `YourApp/android` at repo root | `file("../fraudlens-sdk/fraudlens-android")` |

Then `flutter pub get` / build; the plugin’s `android/build.gradle` already has `implementation project(':fraudlens-android')`.

## 2. Firebase (if you use `FraudLensSdk.initialize` with Firestore)

Same as the native SDK: host **`android/app`** needs **`com.google.gms.google-services`** and **`google-services.json`**, because `initialize` touches `FirebaseFirestore.getInstance()`.

## 3. Dart usage

```dart
import 'package:fraudlens_flutter/fraudlens_flutter.dart';

await FraudLensFlutter.initialize({
  'audioBaseUrl': 'https://your-audio-api.example/',
  'audioApiKey': 'your-key',
  'abuseIpDbApiKey': '...',
  'fraudModelBaseUrl': 'https://your-model.example/',
  'geminiApiKey': '...',
  'enableHttpLogging': false,
});

final health = await FraudLensFlutter.audioHealth();
final ip = await FraudLensFlutter.checkIpReputation('8.8.8.8');
```

Large files: prefer keeping uploads reasonable; the channel passes **byte arrays** in memory.

## 4. Example app

From repo root:

```bash
cd fraudlens_flutter/example
flutter pub get
# Ensure android/settings.gradle.kts includes :fraudlens-android (see example snippet in this folder if present)
flutter run
```

If `example/` is incomplete, run `flutter create . --platforms=android,ios` inside `fraudlens_flutter/example`, re-add the `fraudlens_flutter` path dependency, then re-apply the **`include(":fraudlens-android")`** snippet.

## Publishing

`publish_to: none` in `pubspec.yaml` is set for local/monorepo use. Remove it and add a license when you publish to pub.dev.

## Related

- **React Native:** [`../react-native-fraudlens`](../react-native-fraudlens/)
