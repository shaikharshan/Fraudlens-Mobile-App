# Example

1. Generate platform folders if missing:
   ```bash
   cd fraudlens_flutter/example
   flutter create . --platforms=android,ios
   ```
2. Add **`include(":fraudlens-android")`** to **`android/settings.gradle.kts`** (see parent `fraudlens_flutter/README.md`). From this folder, use:
   ```kotlin
   include(":fraudlens-android")
   project(":fraudlens-android").projectDir =
       file("../../../fraudlens-sdk/fraudlens-android")
   ```
3. Add Firebase to **`android/app`** if you pass config that uses Firestore (same as native SDK).
4. `flutter run`
