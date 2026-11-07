# Apple setup for the App Store

One-time steps in the Apple developer portal and App Store Connect, before the first upload. The build and upload
itself is described in [README.md](README.md#release).

| | |
|---|---|
| Bundle ID | `de.projectforge.mobileapp` (iOS and Android) |
| Team ID | `E4Q65AC7LP` |
| Capability | Push Notifications only |

## 1. Membership and agreements

- https://developer.apple.com/account: the Apple Developer Program membership (organization) is active.
- https://appstoreconnect.apple.com → **Business**: accept all open agreements (the Paid Apps agreement is only needed
  for paid apps or in-app purchases). An open agreement blocks uploads.
- Creating App IDs and keys needs the role Account Holder or Admin.

## 2. App ID

*Certificates, Identifiers & Profiles → Identifiers* (https://developer.apple.com/account/resources/identifiers/list):

1. Open `de.projectforge.mobileapp`, or create it with **+ → App IDs → App → Continue**. Platform iOS, Bundle ID
   *Explicit*.
2. Enable **Push Notifications** (no broadcast capability). Leave all other capabilities off:
   - *Associated Domains*: not needed, the login runs in the system browser with the custom scheme `projectforge://`
     (WebAuthn is done by the browser).
   - *Sign in with Apple*: not needed, the app has its own login.
   - *App Attest*, *iCloud*, *Communication Notifications*, *Time Sensitive Notifications*: not used (the push payload
     carries no message text, there is no notification service extension).
3. Save. Changing a capability later invalidates the provisioning profiles; with automatic signing Xcode renews
   them.

## 3. APNs authentication keys

The server sends pushes with token based keys (`.p8`, no yearly certificate renewal). A key belongs to the team, not to
one App ID, so changing the bundle ID doesn't require a new key. The bundle ID is sent as `apns-topic`.

Create two keys, one per environment, so a key on a development machine or server can't send production pushes
(Apple allows two APNs keys per account; check *Keys* for free slots first):

1. *Keys* (https://developer.apple.com/account/resources/authkeys/list) → **+**.
2. Name `ProjectForge APNs Sandbox`, enable **Apple Push Notifications service (APNs)**, then **Configure** and select
   the environment **Sandbox**. Continue, Register.
3. **Download** the `AuthKey_<KeyID>.p8`. It can be downloaded only once; store it in a password manager.
4. Repeat with the name `ProjectForge APNs Production` and the environment **Production**.
5. Note per key: the **Key ID** (10 characters, also in the file name) and the **Team ID** (`E4Q65AC7LP`, also under
   *Membership details*).

The server picks the key by the environment stored with the device token (`AppDeviceDO`): debug builds from Xcode get
sandbox tokens (`api.sandbox.push.apple.com`), TestFlight and App Store builds production tokens
(`api.push.apple.com`). A token sent to the wrong endpoint fails with `BadDeviceToken`. The `.p8` files belong to the
server configuration, never to the repository.

## 4. App record in App Store Connect

https://appstoreconnect.apple.com → **Apps**:

- An app record with the bundle ID `de.projectforge.mobileapp` already exists from an earlier attempt: use it
  instead of creating a new one (**+ → New App** needs a name that is unique in the store; the bundle ID of a record
  can only be changed until the first build is uploaded). The bundle ID drop-down lists only IDs registered in step 2.
- Update the metadata (below) before the first submission.

## 5. Store listing

- Name, subtitle, description, keywords, category, age rating.
- Screenshots: iPhone 6.9" (or 6.5") and iPad 13" (the app supports iPad).
- Support URL and privacy policy URL (the website's privacy policy).

## 6. App privacy ("nutrition labels")

Declare what the app collects and why (purpose *App Functionality*, no tracking): contact data (business cards), user
content (chat messages), identifiers (device id, push token).

## 7. App review information

- The app needs a ProjectForge server: provide a reachable test server with a demo user the reviewer can log in with,
  and say how the login works (system browser, then back to the app); a second factor can't be done by the reviewer: list the
  account in `projectforge.2fa.exemptUsers` of that server (no 2FA after login, none for registering the app) and
  remove it after the review. Give the account no rights beyond the defaults. The server URL goes into the review
  notes only, not into the repository.
- Contact data for the reviewer.
- A chat is user generated content (guideline 1.2): reporting a message (the menu of a message, shown by a click on
  it) and blocking a user (menu of the author, head of a direct message) exist; a report is mailed to the server's
  administrators, who deactivate the account of the author. Name this in the review notes, and give a contact for
  the operator (the support url).
- Export compliance: the app only uses standard HTTPS; `ITSAppUsesNonExemptEncryption` is already `false` in
  `ios/Runner/Info.plist`.

## 8. Upload and TestFlight

1. Build and upload as described in the README.
2. App Store Connect → the app → **TestFlight**: wait for the processing, add internal testers, later external ones.
   Production pushes can only be tested through TestFlight or the store.
3. Assign the build to the version on the app's page, **Add for Review** / **Submit for Review**, and release after
   the approval.
