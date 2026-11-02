# Mobile app (Flutter)

Status: phase 1 in implementation (app login, digital business cards and the web pages in a WebView done), the chat
tab done (WebView with badge), the time sheets implemented (see "Time sheets"), the manual tests on a device open.

The app lives in [`projectforge-flutter/`](../projectforge-flutter/README.md): Flutter, iOS and Android, not part of
the Gradle build (own CI job `.github/workflows/flutter.yml`). Dev helpers (run against a slot server, simulator,
builds, checks): `projectforge-flutter/bin/pfApp.sh help`. It talks to the `/rs` api of the server like
projectforge-next does; nothing of the next frontend is reused but the texts.

## Phases

1. Login (WebAuthn or OTP as second factor) and the own digital business cards (`/rs/myBusinessCards`), shown only:
   they are created and edited in the web version, which a link opens in the web tab. Done, apart from the manual tests on a device.
2. Time sheets: week and day view, booking, timer (see "Time sheets"). Done, apart from the manual tests on a device.
3. Chat: the chat of next in a WebView (see below), then push notifications (see below).

## Chat

The chat of next (editor, threads, mentions, reactions, files, moderation, search, confidential chats) is big and
keeps growing: native screens would have to repeat every feature, and app and web would drift apart. So the tab
"Chat" shows `/next/chat` in a second WebView (`HomeScreen`, `WebScreen(initialPath:)`), with the live updates of
next (SSE on the web session's cookie). Native is only what a page can't do:

- **Badge:** the unread messages (`chatUnread` of `GET /rs/userStatus`) at the tab (`lib/features/chat/`), loaded
  every 60 s in the foreground, on resuming the app and on switching tabs. The app token has no session to keep
  alive, `ChatUnreadCounter` caches the count. The tab is missing if the chat is switched off (`disabledFeatures`
  contains `CHAT`).
- **Files:** uploads through the file input of the page (WKWebView and the Android WebView open the system picker;
  iOS needs `NSCameraUsageDescription`, `NSPhotoLibraryUsageDescription`), downloads see "Web pages in the app".
- **Push** (see "Notifications and push"): a tap opens `/next/chat/{id}` in the chat tab.

## Time sheets

A phone books in a few taps, so the tab "Time sheets" (`lib/features/timesheets/`) is not the form of next but a day view:

- **Day and week:** a week strip (Monday to Sunday) with the total of each day; swiping the strip changes the week,
  swiping the list the day. The day shows its bookings and the **gaps** between them (today up to now, at most 20:00);
  a tap on a gap books exactly that time. Swipe right duplicates a booking (next free time), swipe left deletes it
  (with "undo", `undelete`); a long press offers both, edit and the timer.
- **Server:** `GET /rs/timesheet/appList?from&to` (epoch millis, at most 62 days) answers the user's own sheets in the
  row shape of the next list plus the configured tags (`TimesheetEntityRest.getAppList`). Everything else is the
  existing api: `GET {id}` before an edit or delete (the whole DTO goes back, so fields the app doesn't show, e.g. the
  AI time savings, survive), `PUT insert`/`update`, `DELETE markAsDeleted`, `PUT undelete`, `GET recentList`,
  `/rs/timesheet/favorites` (templates), `/rs/task/tree/autosearch?onlyBookable=true`, `/rs/task/recent/list`,
  `/rs/task/info/{id}` (the cost units of a task). An app token needs no CSRF token.
- **Online only for writing:** the checks of the server (overlap, closed or not bookable tasks, cost unit required,
  protected period, 5-minute steps, 1 minute to 14 hours) can't be repeated on the device, so the form sends and shows
  the refusals (`ApiException.validationErrors`: `task`, `kost2`, anything else at the time). The weeks last seen are kept
  for reading offline (`JsonCache`, deleted on logout).
- **Form:** *what* first (recent bookings, templates — they fill task, cost unit, location, reference, tag, text, never
  the times), then *when* in one row (wheel in 5-minute steps, "+15 min", "until now"; a stop before the start means
  the next day), then the text. The only cost unit of a task is chosen automatically. On a wide screen the form is a
  side panel instead of a bottom sheet.
- **Timer:** local, kept in a file of the cache so it survives closing the app; stopping opens the form with the start
  rounded down and the stop rounded up to 5 minutes. Not on the server, so another device doesn't see it.
- **Times** are the device's clock (`DateTime` local, sent as UTC instants like next does); days begin at the device's
  midnight. The daily target of the week strip is a constant (8 h), not the working day of the installation.
- The list of next (`/next/timesheet`) switches to this tab (`NativePage.timesheets`, switchable in the settings of the
  app); a single booking opened by a link (`/next/timesheet/5`) stays in the web pages.

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

Only https in release builds (`AppLogin.isInsecure`): Dio (`dart:io`) isn't bound by iOS ATS nor by the cleartext
policy of Android, so plain http would send code, verifier and token in clear. Debug builds allow http for the
development servers.

Every request then carries `Authorization: PFApp <token>` (`RestAuthenticationUtils.appTokenAuthentication`): no
session, no cookies, no CSRF token. One row per app in `T_PF_USER_APP_DEVICE` (`AppDeviceDO`, SHA-256 of the token
only), shown and logged out in "My account" (`MyAccountRest.appDevices`, `logoutAppDevice`). The token expires 90
days after its last usage (`projectforge.app.tokenExpiryDays`); a password change and the admin's "log out all
devices" delete the apps of the user.

**Second factor:** the time of the 2FA of the browser login is stored with the app (`last_2fa`) and taken as
`UserContext.lastSuccessful2FA` of each request, so the 2FA protected urls (`projectforge.2fa.expiryPeriod.*`) work
as in the web. Too old, the server answers 403 `twoFactorRequired` (unlike a REST client token, which is refused
for good); the app logs in again in the browser with its `deviceId`, which keeps the row and renews token and
`last_2fa`, and repeats the request (`ApiClient`). If the browser is logged in as another user, the server registers the app
anew for that user: the app then logs the old user out (`/rs/appAuth/logout`), forgets their data (cache, shared
files, WebView data), starts the tabs anew (`HomeScreen` keyed by device) and fails the request.

**Behind an SSO proxy** (e.g. Authentik as forward auth in nginx): the proxy lets only requests with its session
cookie through, which the browser has, but not the app. The cookies named in `projectforge.app.proxyCookies`
(e.g. `authentik_proxy_*`; a bare `*` is ignored) are taken by `POST /rs/appAuth/authorize` from the browser login
(never the cookies of ProjectForge itself, nor its session cookie under another name, nor a name the browser sends
twice, e.g. one set by another subdomain), kept with the code and handed
to the app with its token (`TokenResponse.proxyCookies`). The app keeps them with the token, sends them with every
request and puts them into the WebView. A refusal of the proxy (a 401 without a json content type, a redirect) isn't a
logout: the app logs in again in the browser, which brings fresh cookies, and repeats the request once. If that doesn't
satisfy the proxy either, it isn't tried again until the next login (`app.proxy.refused`). Requests the app makes on
its own (the badge of the chat) never open the browser (`renewLogin: false`), only something the user does. After each
expiry of the proxy session (the token validity of the Authentik provider) the user logs in again in the browser, with
a second factor of ProjectForge.

The app gets the proxy session of the browser itself, not one of its own: with forward auth on domain level it lets
the app through to every application behind the proxy, logging out the app in "My account" doesn't end it, and a
logout at the proxy in the browser shuts the app out too (it then logs in again in the browser). The admin guide has
to say so once the app is distributed.

**On the device:** the token is in the keychain (`first_unlock_this_device`, not in backups) or the keystore; Android
has no backups at all (`allowBackup="false"`). The logout deletes the cache, the files handed to the share sheet
(`SharedFiles`: downloads, vCards) and the cookies, cache and web storage of the WebView.

**SSO proxy and the parameters of `/next/appLogin`:** a proxy redirecting a browser without a proxy session to its
login passes the requested url on as `rd`; nginx configurations of Authentik put it in unencoded
(`rd=$scheme://$http_host$request_uri`), so everything after the first `&` of the query got lost and the page said
"The link of the app is invalid". The app therefore sends its parameters also packed into one, first parameter `login`
(base64url of a json object, `AppLogin.packParams`), which `readAppLoginParams` of next prefers to the single ones
(still sent for older servers).

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
- **Navigation:** the own server stays in the WebView; other sites, `mailto:`, `tel:` open in the system. `data:`
  pages are refused (without an address bar they would look like pages of the server), as are unknown schemes; no
  file or content access.
- **Login of an SSO proxy in the WebView:** when the proxy session expires (e.g. Authentik in forward auth mode), the
  proxy redirects the pages to its login on another host (`/outpost.goauthentik.io/start` → `sso…/authorize`). Such
  a redirect (iOS: navigation type other than link or form, Android: `isRedirect` or no gesture) stays in the WebView,
  and so does everything on that site until the pages are back (`WebNavigation.decide`, `redirect`,
  `fromForeignSite`); only a link the user taps opens another site in the system browser. Back on the server, the
  fresh proxy cookies of the WebView go into the session (`AuthController.takeProxyCookies`: only the names the
  server sent at the login), so the native tabs and the next web session get through again, and
  `ApiClient.proxyCookiesRenewed` allows a browser renewal again. A site a script of the pages navigates to also
  stays in the WebView. While the WebView shows another site, a bar above it names its host (there is no address
  bar, the site mustn't pass for the pages) and offers "open in the browser" (the current page) and "back to
  ProjectForge" (the last page of the server).
- **App mode of next:** the user agent carries `ProjectForgeApp/<version>` (`lib/app-mode.ts`): next hides its
  logout, the app has its own. Only for the UI, never a security check.
- **No JavaScript handlers** in the WebView: the pages never get hold of the app token.
- **One session for all tabs:** the WebViews (pages, chat) share the cookie jar, so `WebSessionRepository` opens one
  web session (`ensure`) and renews it once for all tabs (`renew`), otherwise each tab would replace the other's
  cookie.
- **Downloads** (`onDownloadStartRequest`, e.g. a chat file): `WebDownload` loads the file with the WebView's session
  cookie, never with the app token (the url is chosen by the page), only from the own server and without following
  redirects, and offers it in the share sheet (also "save to files"). Open: downloads built in the page as a `blob:`
  url (the Excel exports, `lib/rs/download.ts`) can't be loaded by the app and fail with a message; they need the
  blob's bytes from the page (`onDownloadStartRequest` gives only the url).

Open: the 2FA dialog of next offers WebAuthn, which doesn't work in a WebView
for self-hosted servers (OTP does): in app mode it should offer "confirm in the browser" (the app's renewal, then a
new web session).

## Client log

`core/log/app_log.dart` (`appLog`) records what is needed to find out why something fails, in particular behind
an SSO proxy: the login (system status with its redirects, browser login, callback, token exchange, names of the
received proxy cookies), every request of `ApiClient` (failures with status, content type, `Location`, names of the
sent proxy cookies; successful polling isn't logged), the renewal of the login, the web session, every main frame
navigation of the WebView with its decision and how it came about (link or redirect), load errors, and uncaught
errors (`main.dart`).

- Only in memory, at most one day and 2000 entries; nothing is written to a file. In a debug build also on the
  console.
- Shown, copied and cleared on the page "Log" (`core/log/log_screen.dart`), reachable from the settings and from the
  login page (a failed login happens before the settings can be opened).
- No secrets: urls through `LogSanitizer.url` (query values masked, names kept), cookies only by name, no bodies, no
  user or device name (only ids). `LogSanitizer.text` masks every message again (`PFApp`/`Bearer` values, JWTs,
  `token=`, `code=`, `state=`, … and their json fields, mail addresses).

## Texts

`GenerateFlutterI18nMessagesMain` (run by `bin/pfDev.sh gen`) writes `assets/i18n/generated.<locale>.json` from the
I18nResources bundles: the keys the Dart sources name literally (`t('businessCard.new')`) and the keys of its
`PREFIXES`. Flat, placeholders as `{0}`. App only texts are hand-written in `assets/i18n/app.<locale>.json`.
`GenerateFlutterI18nMessagesTest` fails if the generated files are out of date.

## Notifications and push

The notifications of the web version (`NotificationDO`, rules and chat, see `business/notification/`) are shown in the
app natively (`lib/features/notifications/`): the bell in the bar of the business cards tab (count and colour from the
summary in `GET /rs/userStatus`), the list with the archive, "confirm" and "mark as done" as in the web, and "open",
which shows the link in the chat or web tab (`webPathOfLink`: only paths of the own server). The list is loaded again
when the summary (count, newest id) of the user status changes; the user status (`lib/core/status/`) is polled every
60 s in the foreground and refreshed by a push.

In the background iOS suspends the app and no SSE connection survives, so only APNs can wake the user (Android with
FCM follows).

- **Registration:** after login the app asks for the permission, registers for remote notifications and hands the token
  to the server with `POST /rs/appAuth/pushToken` (token, `SANDBOX` for debug and `PRODUCTION` for release builds),
  stored at its `AppDeviceDO` (`push_token`, `push_environment`). The device is the one of the app token, a token known
  on another device is moved. The logout deletes the device and with it the token. Native part: `AppDelegate.swift`,
  channel `projectforge/push`; Dart: `core/push/push_service.dart`.
- **Sending:** the push accompanies the in-app step of the delivery cascade: when a notification becomes visible for
  the recipient (`NotificationService.deliverDueSteps`, "test to me" of a rule), `PushNotifier` sends in the background
  to every registered app of the recipient (`PushSender`, `ApnsPushSender`: HTTP/2 to APNs, ES256 provider token
  of the `.p8` key, `projectforge.push.apns.*`). A token refused with 410/`BadDeviceToken` is forgotten. Without
  `keyFile` nothing is sent. The chat doesn't push for a conversation the recipient watches or has muted, as for the
  other channels (`ChatNotifier`).
- **Payload:** no content, as Apple sees it: a generic text (`notification.push.*`, in the language of the recipient),
  the link, the notification id and the badge (count of the visible notifications), a sound only for IMPORTANT and
  URGENT. A Notification Service Extension for text on the lock screen isn't in the first version.
- **Tap:** the link is opened in the chat or web tab, also when the push started the app (`launchLink`).
- Real APNs delivery needs a physical device; sandbox and production differ, test via TestFlight.
- Open: who sends for a self-hosted installation. The store app has one APNs key: for now the server sends itself with
  keys from its configuration (installations with the publisher's keys or an own build of the app); a push relay of the
  app's publisher, to which each server sends only token and ids (as Nextcloud does), would be a second `PushSender`.

## Open points

- Manual tests on a device: login with WebAuthn (passkey of the platform and security key), OTP, renewal on a 2FA
  protected url, logout in the web, QR code offline,
  booking, editing and deleting time sheets against a real server (the app was tested with fake responses only).
- Android: plain http to the development server (`10.0.2.2`) needs a debug network security config.
- Release signing: the Android release build is still signed with the debug key (`android/app/build.gradle.kts`).
- Store presence: app icons, privacy declarations, review notes (a chat is user generated content: report and block).
- User and admin guide (EN+DE) once the app is distributed, with the SSO proxy (`projectforge.app.proxyCookies`).
- Behind an SSO proxy, background polling (`renewLogin: false`) stays refused after the proxy session expired until
  the user opens a web page (login of the proxy in the WebView) or does something in a native tab (browser login).
