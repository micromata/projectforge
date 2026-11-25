# Android push setup (Firebase Cloud Messaging)

One-time steps for the pushes of the Android app. The server sends them itself (`FcmPushSender`,
[dev-docs/mobile-app.md](../dev-docs/mobile-app.md)); the app only needs the Firebase configuration at build time.

| | |
|---|---|
| Application ID | `de.projectforge.mobileapp` (the dev client: `de.projectforge.mobileapp.dev`) |
| Service | Firebase Cloud Messaging (HTTP v1) |

## 1. Firebase project

1. https://console.firebase.google.com → create a project (Analytics isn't needed).
2. *Project settings → General → Your apps → Add app → Android*: package name `de.projectforge.mobileapp`. Add the
   dev client `de.projectforge.mobileapp.dev` as a second app of the same project, so both builds get tokens.
3. Download `google-services.json` (it holds both apps). It contains no secret of the server, but keep it out of the
   repository (`.gitignore`).
4. *Project settings → Cloud Messaging*: **Firebase Cloud Messaging API (V1)** must be enabled (the legacy API is
   gone).

## 2. Build of the app

`app.config.ts` reads the path of the file from `GOOGLE_SERVICES_JSON`:

- locally: `GOOGLE_SERVICES_JSON=./google-services.json` in `.env.local`;
- on EAS: an environment variable of the project of type *file* (`eas env:create --name GOOGLE_SERVICES_JSON --type file`).

Without it the app builds, but `getDevicePushTokenAsync()` fails and nothing is registered. Changing the file changes
the native code: a new build, not an update (the fingerprint changes).

## 3. Service account for the server

1. *Project settings → Service accounts → Generate new private key*, or in the Google Cloud console a service account
   with the role **Firebase Cloud Messaging API Admin** (`roles/firebasecloudmessaging.admin`) and a JSON key.
2. Store the JSON on the server, readable by ProjectForge only, and configure it:

   ```properties
   projectforge.push.fcm.serviceAccountFile=/path/to/firebase-service-account.json
   # projectforge.push.fcm.projectId=...   (default: project_id of the file)
   ```

   The key belongs to the publisher of the app; an installation with its own build of the app needs the key of its own
   Firebase project.

## 4. Test

- Install a build on a device or emulator with Google Play services, log in, allow notifications (Android 13+ asks).
- `T_PF_USER_APP_DEVICE.push_token` holds the token (no `push_environment`).
- "Test to me" of a notification rule, app in the background: the push shows, the tap opens its link.
- Uninstall the app and send again: the server logs `UNREGISTERED` and forgets the token.
