# Google Play setup

One-time steps in the Google Play Console, before the first upload. The build and upload itself is described in
[README.md](README.md#android-google-play). The counterpart for iOS is [APPLE-SETUP.md](APPLE-SETUP.md).

| | |
|---|---|
| Package name | `org.projectforge.mobileapp` (`android.package` in `app.config.ts`; the iOS bundle ID stays `de.projectforge.mobileapp`: the Flutter app's Android id `de.projectforge.mobileapp` is taken by an inactive Play account) |
| Account | Google Play Console, organization account (verified, so the test requirement of personal accounts doesn't apply) |
| First release | without push (see "Push" below) |

## 1. Create the app

https://play.google.com/console → **Create app**:

- App name `ProjectForge`, default language English (United States) or German, type **App**, **Free**. Free can't be changed
  to paid later.
- Accept the declarations (developer program policies, US export laws).
- Package name `org.projectforge.mobileapp`, checked with *Check availability*. It can't be changed afterwards and
  must equal `android.package` of `app.config.ts`, or the first bundle is refused.

## 2. Play App Signing and the upload key

Play App Signing is mandatory for new apps: Google keeps the app signing key, the build only carries an **upload key**.

- EAS creates and keeps the upload keystore with the first Android build (`bin/pfApp.sh eas build --platform android
  --profile production`; answer "Generate new keystore"). Back it up once: `bin/pfApp.sh eas credentials` →
  Android → production → download the keystore, store it with its passwords in the password manager. A lost upload
  key can be reset by Google support, an app signing key can't.
- Leave "Use Play App Signing" on when the first bundle is uploaded.

## 3. Service account for the automatic submit

`eas submit` needs a Google service account to upload, *after* the first bundle was uploaded by hand (Google's API can
only update an app that has a release already).

1. Google Cloud Console (the project linked to the Play Console, or a new one): **IAM & Admin → Service Accounts →
   Create**, name `eas-submit`, no roles. **Keys → Add key → JSON**, download.
2. Enable the **Google Play Android Developer API** for that project (APIs & Services → Library).
3. Play Console → **Users and permissions → Invite new users**: the service account's email, permission
   **Release to production, exclude devices, and use Play App Signing** (or *Release apps to testing tracks* only) for the app.
4. Give the key to EAS: `bin/pfApp.sh eas credentials` → Android → Google Service Account → upload. It stays out of
   the repository (`eas.json` has no `serviceAccountKeyPath`).

## 4. Store listing

*Grow → Store presence → Main store listing*:

- Short description (80 characters), full description (4000), app icon 512×512 PNG (from `assets/icon.png`,
  `dev-docs/logo/render-logos.py`), feature graphic 1024×500, at least 2 phone screenshots; 7" and 10" tablet
  screenshots are recommended (the app supports tablets).
- Category *Business* (or *Productivity*), contact email, website; **privacy policy URL** (the website's privacy
  policy).

## 5. App content (*Policy → App content*)

All of these must be completed before a release can go to production:

- **Privacy policy:** the URL of the website.
- **App access:** the app needs a ProjectForge server, so the content isn't open: give the reviewer a reachable test
  server with a demo user and how the login works (system browser, then back to the app). The second factor can't be
  done by the reviewer: as for the App Store, list the account in `projectforge.2fa.exemptUsers` of that server and
  remove it after the review, with no rights beyond the defaults (see APPLE-SETUP.md, 7). The server URL goes only
  into this form, not into the repository.
- **Ads:** none.
- **Content rating:** the questionnaire; the chat is user generated content, answer that users can communicate.
- **Target audience:** adults (18+), not aimed at children.
- **Data safety:** collected and linked to the user, purpose *App functionality*, no sharing, no ads: personal
  info (name, email, contact data of business cards), messages (chat), files (uploads), device or other ids (device id).
  Everything is encrypted in transit (HTTPS only); the user can have the data deleted by the operator of their server.
- **Account deletion:** the app has no sign-up; accounts are created and deleted by the operator of the server. Say so.
- **User generated content:** the app has report and block functions for chat messages (APPLE-SETUP.md, 7); name them
  where asked.
- **Government apps, financial features, health, news:** no.
- **Advertising ID:** not used (answer "No").

## 6. First release

1. Build the bundle: `bin/pfApp.sh eas build --platform android --profile production` (an `.aab`, ~10 min). Raise
   `android.versionCode` in `app.config.ts` first (see README).
2. Download the `.aab` from the build page.
3. Play Console → **Test and release → Testing → Internal testing → Create new release**, upload the `.aab`, add testers
   (an email list, **Testers** tab), save, roll out. Testers install through the opt-in link; wait a few minutes.
   Start with internal testing: it needs no review and checks login, push-free operation and the WebView on a real device.
4. When it works: **Production → Create new release** (or promote the internal release), release notes per language,
   *Send for review*. The first review of a new app takes a few days.
5. Later releases: `bin/pfApp.sh eas submit --platform android --profile production --latest` uploads as a draft
   to the track of `eas.json`; open the draft in the Play Console and roll it out.

## 7. Push

The first Android release has no push: `expo-notifications` needs Firebase (`google-services.json`) and the server only
sends through APNs (`ApnsPushSender`). On Android the user status is polled every 60 s while the app is open (dev-docs/mobile-app.md,
"Notifications and push"). FCM needs: a Firebase project with the app, `google-services.json` in the app, an
FCM `PushSender` in the server, and the Android registration in `core/push`.
