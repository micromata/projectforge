# ProjectForge app

The mobile app of ProjectForge for iOS and Android (Flutter). Architecture, login flow and plans:
[dev-docs/flutter-client.md](../dev-docs/flutter-client.md).

## Setup (macOS)

- Flutter SDK (stable) on the `PATH`, e.g. cloned to `~/development/flutter`; `flutter doctor` shows what's missing.
- iOS: Xcode with an iOS simulator runtime (`xcodebuild -downloadPlatform iOS`), CocoaPods.
- Android: Android Studio with the Android SDK and an emulator.

## Run

```bash
bin/pfDev.sh run 1                     # a server on :8081 (slot 1, test data), from the repo root
projectforge-flutter/bin/pfApp.sh run 1   # boots an iOS simulator, flutter run, login preset to http://localhost:8081
projectforge-flutter/bin/pfApp.sh check   # dart format check + flutter analyze + flutter test
projectforge-flutter/bin/pfApp.sh help    # the other helpers: sim, devices, build-ios, build-android, xcode, gen, …
```

Without a slot the server is `:8080` (the main instance), as with `bin/pfDev.sh`. `bin/pfDev.sh app [slot]` and
`app-check` call the same script.

The login opens the server's page `/next/appLogin` in the system browser. The Android emulator reaches the Mac as
`10.0.2.2`, so enter `http://10.0.2.2:8081` there. WebAuthn needs https (or `localhost`): on the emulator test with
an OTP.

## Texts

`assets/i18n/generated.<locale>.json` is generated from the server's I18nResources by `bin/pfDev.sh gen`, never
edited by hand; app only texts go into `assets/i18n/app.<locale>.json`. Keys are always written out literally:
`context.t('businessCard.new')`.
