# react-native-fraudlens

React Native native module that wraps the **FraudLens Android SDK** (`fraudlens-android`). Same idea as the Flutter plugin: **Android** runs Kotlin; **iOS** stubs reject (use HTTP from JS or add native code).

## Install (monorepo / local path)

```bash
yarn add file:../react-native-fraudlens
# or npm install ../react-native-fraudlens
```

## Android: include the Kotlin SDK module

In your app’s **`android/settings.gradle`** / **`settings.gradle.kts`**, add **`fraudlens-android`** (same as Flutter):

**If your `android/` folder is at the repo root** (`YourApp/android`):

```kotlin
include(":fraudlens-android")
project(":fraudlens-android").projectDir = file("../fraudlens-sdk/fraudlens-android")
```

Adjust the path so it points at **`fraudlens-sdk/fraudlens-android`** from your Gradle settings file.

Autolinking injects **`FraudLensPackage`** via **`react-native.config.js`**. If it doesn’t, add manually in **`MainApplication`**: `packages.add(FraudLensPackage())`.

## Firebase

If you use `initialize()` with features that touch Firestore, apply **`google-services`** on the **app** module and add **`google-services.json`** (same as native).

## JavaScript / TypeScript

```ts
import {
  initialize,
  audioHealth,
  audioDetect,
  checkIpReputation,
} from "react-native-fraudlens";

await initialize({
  audioBaseUrl: "https://your-audio.example/",
  audioApiKey: "secret",
  abuseIpDbApiKey: "...",
  fraudModelBaseUrl: "https://your-model.example/",
  geminiApiKey: "...",
});

const json = await audioHealth();
```

**Binary uploads** use **Base64** (not a raw byte array in the bridge):

```ts
import { audioDetect } from "react-native-fraudlens";

await audioDetect({
  base64: myBase64String,
  filename: "clip.wav",
  contentType: "audio/wav",
});
```

On **iOS**, `initialize` and other methods **no-op or reject** in native; the TS helpers return `null` where noted in `src/index.ts` for `Platform.OS !== "android"`.

## Publishing to npm

Point `package.json` `repository` / podspec `source` at your real git remote, then `npm publish`. Consumers still must wire **`:fraudlens-android`** in Gradle (or you publish the AAR to Maven and change this module’s `build.gradle` to use coordinates instead).

## Related

- Kotlin SDK: `../fraudlens-sdk/`
- Flutter plugin: `../fraudlens_flutter/`
