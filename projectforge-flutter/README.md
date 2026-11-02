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

## App icon

`assets/icon/app-icon.png` (1024 × 1024, opaque) is the ProjectForge wordmark with the flame, rendered from the parts of
the website logo (`site/_includes/logo.html`: Archivo Narrow bold italic, `projectforge-flame.svg`). The platform icons
are scaled from it:

```bash
cd ios/Runner/Assets.xcassets/AppIcon.appiconset
for f in Icon-App-*.png; do
  px=$(sed -E 's/Icon-App-([0-9.]+)x[0-9.]+@([0-9])x\.png/\1 \2/' <<<"$f" | awk '{printf "%d", $1*$2}')
  magick ../../../../assets/icon/app-icon.png -filter Lanczos -resize ${px}x${px} -alpha off -strip "$f"
done
cd ../../../../android/app/src/main/res
for d in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  magick ../../../../../assets/icon/app-icon.png -filter Lanczos -resize ${d#*:}x${d#*:} -strip mipmap-${d%%:*}/ic_launcher.png
done
```

## Release (iOS, App Store / TestFlight)

Bundle ID `de.projectforge.mobileapp` (iOS and Android), team `E4Q65AC7LP`, automatic signing. Push Notifications is the
only capability of the App ID (`ios/Runner/Runner.entitlements`; Xcode switches `aps-environment` to `production` for
distribution builds).

```bash
projectforge-flutter/bin/pfApp.sh check
projectforge-flutter/bin/pfApp.sh build-ios --release   # flutter build ipa; raise the build number in pubspec.yaml
```

One-time portal steps (App ID, APNs keys, store listing): [APPLE-SETUP.md](APPLE-SETUP.md).

Upload `build/ios/ipa/*.ipa` with Transporter or the Xcode Organizer, then test via TestFlight before submitting. The
APNs auth keys (`.p8`, one for sandbox and one for production) belong to the server configuration, never to the repo.
