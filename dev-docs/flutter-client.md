# Mobile app (Flutter)

Status: phase 1 in implementation (app login, digital business cards and the web pages in a WebView done), timesheets
and chat open.

The app lives in [`projectforge-flutter/`](../projectforge-flutter/README.md): Flutter, iOS and Android, not part of
the Gradle build (own CI job `.github/workflows/flutter.yml`). Dev helpers (run against a slot server, simulator,
builds, checks): `projectforge-flutter/bin/pfApp.sh help`. It talks to the `/rs` api of the server like
projectforge-next does; nothing of the next frontend is reused but the texts.

## Phases

1. Login (WebAuthn or OTP as second factor) and the own digital business cards (`/rs/myBusinessCards`). Done, apart
   from the manual tests on a device.
2. Timesheets: week list (`POST /rs/timesheet/listPage`), new and edited bookings (`newEntry`, `insert`/`update`),
   task (`/rs/task/tree/autosearch?onlyBookable=true`, `/rs/task/recent/list`), kost2 (`/rs/task/info/{id}`),
   recent sheets and templates (`recentList`, `/rs/timesheet/favorites`).
3. Chat: conversations (`/rs/chatDirect`, `/rs/chatRoom`, `/rs/chat`), live updates via SSE with `Last-Event-ID`
   and `/changes` as polling fallback, then push notifications (see below).

## Login

Native passkeys would need the domain of every installation associated with the app (Associated Domains,
`assetlinks.json`), which a store app can't have. So the app logs in in the system browser
(`ASWebAuthenticationSession`, Chrome Custom Tabs via `flutter_web_auth_2`), where password, WebAuthn and OTP work as
in the web — an authorization code flow with PKCE (`AppAuthService`):

1. The app opens `/next/appLogin?challenge=<S256>&state=…&device=…&platform=…[&deviceId=…]`.
2. The page (`components/features/app-login/`) logs in as usual (`AuthGuard` keeps the query), asks the user to
   confirm the app and calls `POST /rs/appAuth/authorize`. Only for a browser session and only after a second factor
   of at most 10 minutes: otherwise the answer is the 403 `twoFactorRequired`, projectforge-next opens its 2FA dialog
   and repeats the call.
3. The browser is redirected to `projectforge://auth?code=…&state=…` (fixed, built by the server). The code is valid
   for 2 minutes and consumed by the first attempt.
4. The app exchanges code and verifier at `POST /rsPublic/appAuth/token` (throttled per ip address) for its token.

Every request then carries `Authorization: PFApp <token>` (`RestAuthenticationUtils.appTokenAuthentication`): no
session, no cookies, no CSRF token. One row per app in `T_PF_USER_APP_DEVICE` (`AppDeviceDO`, SHA-256 of the token
only), shown and logged out in "My account" (`MyAccountRest.appDevices`, `logoutAppDevice`). The token expires 90
days after its last usage (`projectforge.app.tokenExpiryDays`); a password change and the admin's "log out all
devices" delete the apps of the user.

**Second factor:** the time of the 2FA of the browser login is stored with the app (`last_2fa`) and taken as
`UserContext.lastSuccessful2FA` of each request, so the 2FA protected urls (`projectforge.2fa.expiryPeriod.*`) work
as in the web. Too old, the server answers 403 `twoFactorRequired` (unlike a REST client token, which is refused
for good); the app logs in again in the browser with its `deviceId`, which keeps the row and renews token and
`last_2fa`, and repeats the request (`ApiClient`).

## Web pages in the app

Everything without a native screen is shown as the normal web pages (next, and the legacy pages it links to) in a
WebView, the tab "ProjectForge" (`lib/features/web/`, `flutter_inappwebview`). A WebView needs a session cookie, the
app has only its token:

1. The app calls `POST /rs/appAuth/webSession` with its token (`AppAuthRest.webSession`). Only for an app token,
   neither for a browser session nor a REST client token; user and device are the ones of the token.
2. `LoginService.appWebLogin` opens a new session (new id, `internalLogin`) with the second factor of the app's
   browser login as `lastSuccessful2FA`. Is that one too old for a login (`projectforge.2fa.loginExpiryDays`), the
   answer is the 403 `twoFactorRequired` and the `ApiClient` renews the login in the browser.
3. The app hands the `Set-Cookie` of the answer to the WebView (`WebCookies`) and loads the start page `/next/`.

- **Bound to the app:** the session carries the device id; `LoginService.checkLogin` checks once a minute that the
  device still exists, so logging out the app (here, in "My account", by a password change, expiry) ends its web
  sessions too. The app's logout deletes the cookies of the WebView.
- **No further apps:** such a session is refused by `POST /rs/appAuth/authorize`. A stolen token would otherwise
  register new apps that outlive its revocation.
- **Session lost** (timeout, server restart): next shows `/next/login?returnUrl=…`; the app sees it
  (`shouldOverrideUrlLoading`, `onUpdateVisitedHistory` for client side navigations), opens a new session and returns
  to `returnUrl` (only a path of the own server). At most once per 30 s, otherwise an error with "retry".
- **Navigation:** the own server stays in the WebView; other sites, `mailto:`, `tel:` open in the system.
- **App mode of next:** the user agent carries `ProjectForgeApp/<version>` (`lib/app-mode.ts`): next hides its
  logout, the app has its own. Only for the UI, never a security check.
- **No JavaScript handlers** in the WebView: the pages never get hold of the app token.

Open: downloads and uploads in the WebView; the 2FA dialog of next offers WebAuthn, which doesn't work in a WebView
for self-hosted servers (OTP does): in app mode it should offer "confirm in the browser" (the app's renewal, then a
new web session).

## Texts

`GenerateFlutterI18nMessagesMain` (run by `bin/pfDev.sh gen`) writes `assets/i18n/generated.<locale>.json` from the
I18nResources bundles: the keys the Dart sources name literally (`t('businessCard.new')`) and the keys of its
`PREFIXES`. Flat, placeholders as `{0}`. App only texts are hand-written in `assets/i18n/app.<locale>.json`.
`GenerateFlutterI18nMessagesTest` fails if the generated files are out of date.

## Push notifications (phase 3)

In the background iOS suspends the app and no SSE connection survives, so only APNs (and FCM on Android) can wake
the user.

- The app asks for the permission after login, registers for remote notifications and hands the device token to
  the server with a `POST` (stored at `AppDeviceDO`: token, environment sandbox/production).
- The server sends with a token based APNs key (`.p8`, no yearly certificate renewal) and FCM; it removes a token on
  `410 Unregistered`/`BadDeviceToken`.
- Only when the recipient doesn't watch the conversation via SSE (the rule of the existing notifications).
- The payload carries no message text: a generic text, the conversation id and a counter. The app loads the content
  when opened. A text on the lock screen would need a Notification Service Extension: not in the first version.
- Real APNs delivery needs a physical device; sandbox and production differ, test via TestFlight.

## Open points

- Manual tests on a device: login with WebAuthn (passkey of the platform and security key), OTP, renewal on a 2FA
  protected url, logout in the web, QR code offline.
- Android: plain http to the development server (`10.0.2.2`) needs a debug network security config.
- Store presence: app icons, privacy declarations, review notes (a chat is user generated content: report and block).
- User and admin guide (EN+DE) once the app is distributed.
