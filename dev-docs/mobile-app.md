# Mobile app (React Native)

The app lives in [`projectforge-app/`](../projectforge-app/README.md): React Native (Expo), iOS and Android, not part
of the Gradle build. Dev helpers (run against a slot server, simulator, builds, checks):
`projectforge-app/bin/pfApp.sh help`. It talks to the `/rs` api of the server like projectforge-next does; nothing of
the next frontend is reused but the texts. It replaced the earlier Flutter app (same store app
`de.projectforge.mobileapp`).

## Chat

The chat of next (editor, threads, mentions, reactions, files, moderation, search, confidential chats) is big and
keeps growing: native screens would have to repeat every feature, and app and web would drift apart. So the tab
"Chat" shows `/next/chat` in a second WebView (`src/features/web/`), with the live updates of
next (SSE on the web session's cookie). Native is only what a page can't do:

- **Badge:** the unread messages (`chatUnread` of `GET /rs/userStatus`) at the tab (`features/web/webTabs.ts`), loaded
  every 60 s in the foreground, on resuming the app and on switching tabs, and at once when next changes its own
  badges (a chat read: `{"pf":"status"}` of `hooks/use-app-status-signal.ts`, no data; the app loads the status now
  and 3 s later, when the server has resolved the chat's notifications on its own thread). The app token has no
  session to keep alive, `core/status/userStatus.ts` caches the count. The tab is missing if the chat is switched off (`disabledFeatures`
  contains `CHAT`).
- **System message:** the message of the administrators (`alertMessage` of `GET /rs/userStatus`, e.g. maintenance)
  is shown by `AlertBanner` (`features/home/`) above the native pages (the web tab shows it itself, the chat
  page doesn't). It can't be dismissed and follows the 60 s polling of the user status. The field is part of the published api (contract test).
- **Files:** uploads through the file input of the page (WKWebView and the Android WebView open the system picker;
  iOS needs `NSCameraUsageDescription`, `NSPhotoLibraryUsageDescription`), downloads see "Web pages in the app".
- **Push** (see "Notifications and push"): a tap opens `/next/chat/{id}` in the chat tab.

## Time sheets

A phone books in a few taps, so the tab "Time sheets" (`src/features/timesheets/`) is not the form of next but a day view:

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
  for reading offline (`core/cache/jsonCache.ts`, deleted on logout).
- **Form:** *what* first (recent bookings, templates — they fill task, cost unit, location, reference, tag, text, never
  the times), then *when* in one row (wheel in 5-minute steps, "+15 min", "until now"; a stop before the start means
  the next day), then the text. The only cost unit of a task is chosen automatically. On a wide screen the form is a
  side panel instead of a bottom sheet.
- **Timer:** local, kept in a file of the cache so it survives closing the app; stopping opens the form with the start
  rounded down and the stop rounded up to 5 minutes. Not on the server, so another device doesn't see it.
- **Times** are the device's clock (local time, sent as UTC instants like next does); days begin at the device's
  midnight. The daily target of the week strip is a constant (8 h), not the working day of the installation.
- The list of next (`/next/timesheet`) switches to this tab (`core/web/nativePage.ts`, switchable in the settings of the
  app); a single booking opened by a link (`/next/timesheet/5`) stays in the web pages.

## Login

Native passkeys would need the domain of every installation associated with the app (Associated Domains,
`assetlinks.json`), which a store app can't have. So the app logs in in the system browser
(`ASWebAuthenticationSession`, Chrome Custom Tabs), where password, WebAuthn and OTP work as
in the web — an authorization code flow with PKCE (`AppAuthService`):

1. The app opens `/next/appLogin?challenge=<S256>&state=…&device=…&platform=…[&deviceId=…]`.
2. The page (`components/features/app-login/`) logs in as usual (`AuthGuard` keeps the query), asks the user to
   confirm the app and calls `POST /rs/appAuth/authorize`. Only for a browser session and only after a second factor
   of at most 10 minutes: otherwise the answer is the 403 `twoFactorRequired`, projectforge-next opens its 2FA dialog
   and repeats the call.
3. The browser is redirected to `projectforge://auth?code=…&state=…` (fixed, built by the server). The code is valid
   for 2 minutes and consumed by the first attempt.
4. The app exchanges code and verifier at `POST /rsPublic/appAuth/token` (throttled per ip address) for its token.

Only https in release builds (`core/auth/appLogin.ts`), so code, verifier and token are never sent in clear. Debug
builds allow http for the development servers.

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
files, WebView data), starts the tabs anew (keyed by device) and fails the request.

**Behind an SSO proxy** (e.g. Authentik as forward auth in nginx): the proxy lets only requests with its session
cookie through, which the browser has, but not the app. The cookies named in `projectforge.app.proxyCookies`
(e.g. `authentik_proxy_*`; a bare `*` is ignored) are taken by `POST /rs/appAuth/authorize` from the browser login
(never the cookies of ProjectForge itself, nor its session cookie under another name, nor a name the browser sends
twice, e.g. one set by another subdomain), kept with the code and handed
to the app with its token (`TokenResponse.proxyCookies`). The app keeps them with the token, sends them with every
request and puts them into the WebView. A refusal of the proxy (a 401 without a json content type, a redirect) isn't a
logout: the app logs in again in the browser, which brings fresh cookies, and repeats the request once. If that doesn't
satisfy the proxy either, it isn't tried again by itself while the app has these cookies (`app.proxy.refused`; a
login that brings others may, and the "retry" of the pages logs in anyway). Requests the app makes on
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
(base64url of a json object, `packParams` in `core/auth/appLogin.ts`), which `readAppLoginParams` of next prefers to the single ones
(still sent for older servers). Some proxies bring the browser back without any query (seen with Authentik: the page
arrived as a bare `/next/appLogin`); a server from 9.2.4 on therefore takes the packed parameter in the path as well,
`/next/appLogin/<login>` (the route `appLogin/[login]`, preferred to the query). It says so by `appLoginPath` in the
system status (first login) and the token answer (kept in `Session.appLoginPath` for the renewals; a session of an
older server asks the system status again). Older servers answer that path with a 404, so the app sends the query only
to them. The path segment is masked in the client log.

## Web pages in the app

Everything without a native screen is shown as the normal web pages (next, and the legacy pages it links to) in a
WebView, the tab "ProjectForge" (`src/features/web/`). A WebView needs a session cookie, the
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
- **SSO proxy refusing the pages:** when the proxy session expires (e.g. Authentik in forward auth mode), the proxy
  redirects a page load to its login on another host (`/outpost.goauthentik.io/start` → `sso…/authorize`). The app
  tells it from other navigations to another site by `WebNavigation.isProxyLogin`: a redirect (iOS: navigation type
  other than link or form, Android: `isRedirect` or no gesture) while a page of the server is still loading (until it
  is committed, `onPageCommitVisible`; marked for the app's own loads and pull to refresh too, which Android doesn't
  pass through `shouldOverrideUrlLoading`), behind a proxy only (the server sent proxy cookies at the login). A script of a loaded
  page (an external menu entry) and a tapped link open the other site in the system browser.
  - The navigation is cancelled, and the app asks the proxy itself (`ApiClient.proxyRefuses`: `GET /next/` with the
    proxy cookies only, no token, no session, redirects not followed; `/next/` is static, the server never redirects
    it, and a proxy that lets `/rs` through but guards the pages is told apart too): only if the proxy refuses it, it
    is its login. Otherwise the other site (e.g. an
    open redirect of the server) opens in the system browser, without a login of the app and never in the WebView.
  - The login of the app is renewed in the system browser
    (`AuthController.reauthenticate`): `ASWebAuthenticationSession` shares the proxy's login session with Safari
    (often no password), the token exchange brings fresh proxy cookies, also into the WebView, and the page asked for
    is loaded again. The password of the proxy's login isn't typed into a WebView of the app (RFC 8252).
  - A tab not shown (e.g. the chat reloading in the background) doesn't renew: it shows the refusal, so no browser
    login opens that the user didn't cause.
  - If that is cancelled or fails, or the proxy refuses again within 30 s, the refusal is shown (`app.proxy.refused`).
    Its "retry" renews the login in the system browser itself and then loads the refused url again, as it was (a path
    rebuilt of it could resolve to another site, `//other.host/`); the web session opened then doesn't renew once more
    (`renewLogin: false`). The `ApiClient` doesn't start a new browser login while it has the proxy cookies a login
    brought without satisfying the proxy (only once others arrive), which would otherwise leave the retry without a
    login. The proxy's login never happens in the WebView, which never shows another site.
- **Only pages of the server in the WebView:** the app loads only urls of the server's origin (`WebNavigation.pageUrl`;
  its own loads don't pass `shouldOverrideUrlLoading` on Android, this includes the `startUrl` of the server). As a
  last guard, a page of another site that is committed anyway (`onPageCommitVisible`, e.g. a back navigation on
  Android) is stopped, replaced by the error and opened in the system browser.
- **App mode of next:** the user agent carries `ProjectForgeApp/<version>` (`lib/app-mode.ts` of projectforge-next): next hides its
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
- **Browser functions** (the WebView has no address bar): the WebView tabs (pages, chat) have browser entries in the
  "more" menu (⋮) of the navigation, above the account entries, so the bar keeps its slots on every tab (`browserMenuEntries`, driven by a
  `WebScreenController` per tab): back, forward, reload, find in the page, share the link, open the page in the system browser (no web session there, it may ask for the login) and
  "Restart page" (a new WebView with a new web session).
  - **Pull to refresh:** next scrolls in columns inside a box of the window's height (`page-shell.tsx`), the document
    never. The WebView's own control would never fire there (iOS) or on every downward swipe (Android), so the app
    switches it off on `/next` pages (`WebNavigation.isNextPage`) and next does it itself in app mode
    (`hooks/use-pull-to-refresh.ts`: only if no column up from the touched element is scrolled down, not in fields,
    dialogs and `[data-no-pull-refresh]`, e.g. the chat's message list). The legacy pages keep the WebView's.
  - **Find in the page:** iOS 16+ the native find navigator of WebKit, Android the `WebFindBar` on the WebView's
    `findAllAsync`. Not the plugin's JavaScript search (iOS without the navigator, so none on iOS 15): it wraps the
    matches in elements of its own in the DOM of next, which React then can't update. Virtualized lists find only
    the rows rendered.
  - **No connection** (a main frame failing with a network error): the app's error with a retry of the failed page,
    not the WebView's own error page.

Open: the 2FA dialog of next offers WebAuthn, which doesn't work in a WebView
for self-hosted servers (OTP does): in app mode it should offer "confirm in the browser" (the app's renewal, then a
new web session).

## Client log

`core/log/appLog.ts` (`appLog`) records what is needed to find out why something fails, in particular behind
an SSO proxy: the login (system status with its redirects, browser login, callback, token exchange, names of the
received proxy cookies), every request of `ApiClient` (failures with status, content type, `Location`, names of the
sent proxy cookies; successful polling isn't logged), the renewal of the login, the web session, every main frame
navigation of the WebView with its decision and how it came about (link or redirect), load errors, and uncaught
errors (`core/log/uncaughtErrors.ts`).

- Only in memory, at most one day and 2000 entries; nothing is written to a file. In a debug build also on the
  console. Cleared by the logout and by a renewal that logged in another user (it names server, pages and ids), kept
  for an expired login (it tells why) until somebody else (another user or server) logs in.
- Shown, copied and cleared on the page "Log" (`core/log/LogScreen.tsx`), reachable from the settings and from the
  login page (a failed login happens before the settings can be opened).
- No secrets: urls through `LogSanitizer.url` (query values masked, names kept; tokens in the path masked, e.g.
  `/next/public/datatransfer/<token>`), cookies only by name, no bodies, no
  user or device name (only ids). `LogSanitizer.text` masks every message again (`PFApp`/`Bearer` values, JWTs,
  `token=`, `code=`, `state=`, … and their json fields, the proxy cookie names of the server, mail addresses, words
  of 20 and more letters and digits, urls within a text through `LogSanitizer.url`), replaces control characters,
  further line breaks (U+0085, U+2028/2029) and marks of the text direction, and indents further lines of an entry. Texts of the server (error messages) may still name
  persons: the log stays on the device unless the user copies it.

## Compatibility of app and server

The app comes from the store and connects to servers of different versions. Only the newer side knows about a
break, so each side declares the minimum it needs from the other:

- `AppCompatibility.MIN_APP_VERSION` (server, projectforge-business): raised in the commit that removes or changes
  an api older apps use. Published in `SystemData.minAppVersion` (public too); "My account" marks outdated app
  devices (`AppDevice.outdated`, from their `X-PF-App-Version`).
- `MIN_SERVER_VERSION` (app, `core/compat/compatibility.ts`): raised in the commit that needs a feature of a newer
  server. Compared with `systemData.version` of the user status: the public system status hides the version, so this
  is checked only after the login. A snapshot counts as its release.

Only a warning (banner on the home screen once per server and app start, a line in the account menu), nothing is
refused; unknown or unparseable versions never warn. Prefer a feature flag in the system status (e.g.
`appLoginPath`) wherever the app can fall back: the minimums are for hard breaks only.

### Changing an api the app uses

The store version of the app is old the day after a server release, so the server keeps what the published app reads:

1. **Adding is free.** New fields and endpoints need no care; the app reads tolerantly (`== true`, `?? default`,
   unknown fields are ignored), and so must new app code.
2. **The contract test guards it:** the app reads the server's data only through the TypeScript interfaces in
   `projectforge-shared/src/rs/`, each naming its Kotlin class (`@server`, `@request`, `@optionalRead`,
   `@noServerClass`), and `projectforge-app/test/contract/app-api-contract.json` lists the endpoints it calls, each
   with its controller. `AppTsApiContractTest` (projectforge-application) fails when the server loses a field of such an
   interface, a field of a request class the app sends, or an endpoint. A new app feature adds its interface and
   endpoint entry in the same commit.
   **Before removing or renaming a field, an endpoint or a value, ask what the published app reads:**
   `git grep -n <name> <tag or commit of the published app version> -- projectforge-app/src projectforge-shared/src/rs`
   (the app version of `app.config.ts`). Every store build is tagged `app-<version>+<build>` (`bin/pfApp.sh publish`):
   `git tag -l 'app-*' --sort=-v:refname`. Search the name in the models and in the tests' fixtures.
3. **Not read** (or only parsed, never used): remove it at once, as for any server-only change; the contract test
   then doesn't name it.
4. **Read** (the contract names it, the test fails when it goes): the server keeps delivering it, marked `@Deprecated("app <= x.y.z reads it, remove with MIN_APP_VERSION > x.y.z")`
   in the KDoc/annotation, with the replacement beside it, and the entry goes into
   [app-api-deprecations.md](app-api-deprecations.md). The next app release stops reading it.
5. **Clean-up:** an entry goes when `AppCompatibility.MIN_APP_VERSION` is above the last app version that reads it; the
   release checklist (CLAUDE.md, "Compatibility of app and server") looks through the register.
6. **A hard break without a fallback** raises `MIN_APP_VERSION` in the same commit (see above) and says so in the
   changelog; where the app can fall back, add a feature flag to the system status instead.

The same holds the other way round for `MIN_SERVER_VERSION`: the app only calls what servers from that version on
deliver, or checks a flag.

## Texts

`GenerateAppI18nMessagesMain` (run by `bin/pfDev.sh gen`) writes `assets/i18n/generated.<locale>.json` from the
I18nResources bundles: the keys the TypeScript sources name literally (`t('businessCard.new')`) and the keys of its
`prefixes`. Flat, placeholders as `{0}`. App only texts are hand-written in `assets/i18n/app.<locale>.json`.
`GenerateAppI18nMessagesTest` fails if the generated files are out of date.

## Notifications and push

The notifications of the web version (`NotificationDO`, rules and chat, see `business/notification/`) are shown in the
app natively (`src/features/notifications/`): the bell in the navigation, on every tab, next to the gear of the account menu (count and colour from the
summary in `GET /rs/userStatus`), the list with the archive, "confirm" and "mark as done" as in the web, and "open",
which shows the link in the chat or web tab (`webPathOfLink`: only paths of the own server). The list is loaded again
when the summary (count, newest id) of the user status changes; the user status (`src/core/status/`) is polled every
60 s in the foreground and refreshed by a push (the bell takes the push's badge at once, the status is loaded now and
3 s later) and by the status signal of next (see Badge above).

In the background iOS suspends the app and no SSE connection survives, so only APNs can wake the user (Android with
FCM follows).

- **Registration:** after login the app asks for the permission, registers for remote notifications and hands the token
  to the server with `POST /rs/appAuth/pushToken` (token, `SANDBOX` for debug and `PRODUCTION` for release builds),
  stored at its `AppDeviceDO` (`push_token`, `push_environment`). The device is the one of the app token, a token known
  on another device is moved. The logout deletes the device and with it the token. Code: `core/push/pushService.ts`, `core/push/expoPush.ts`.
- **Sending:** the push accompanies the in-app step of the delivery cascade: when a notification becomes visible for
  the recipient (`NotificationService.deliverDueSteps`, "test to me" of a rule), `PushNotifier` sends in the background
  to every registered app of the recipient (`PushSender`, `ApnsPushSender`: HTTP/2 to APNs, ES256 provider token
  of the `.p8` key, `projectforge.push.apns.*`). A token refused with 410/`BadDeviceToken` is forgotten. Without
  `keyFile` nothing is sent. The chat doesn't push for a conversation the recipient watches or has muted, as for the
  other channels (`ChatNotifier`).
- **Payload:** the title and the beginning of the notification (`PushNotifier`: the preview the bell shows,
  `NotificationPreview`, e.g. the chat message; else the SMS text if the rule has one, else the html body as plain
  text, one line per block, at most 240 characters; the chat bodies don't repeat their title, `ChatNotifier.body`), the link, the notification id and the
  badge (count of the visible notifications), a sound only for IMPORTANT and URGENT. Apple sees it: the user may switch
  the content off (`PushContentPrefService`, a user pref, `GET/POST /rs/pushSettings`, in "My account" → Apps and in
  the app's settings, `UserStatusStore.choosePushContent`), the server for everybody (`projectforge.push.content=false`,
  the switch is hidden then); a generic text (`notification.push.*`, in the language of the recipient) instead. Chat
  content is in the notification only where mails may have it (`ChatHost.mailContentAllowed`).
- **Tap:** the link is opened in the chat or web tab, also when the push started the app (`launchLink`).
- **Several servers:** all stored logins push, not only the one shown. The payload names the device (`device`, the
  `AppDeviceDO` id = `Session.deviceId`) and the server (`server`, the host of `projectforge.domain`, also the banner's
  `subtitle`); `pushTarget` (`core/push/pushService.ts`) tells the login by them (the ids are counted per server, the
  host tells two logins with the same id apart). The token goes to every stored login on each start; a server
  answering `pushToken` without `deviceTagged` (older) gets `pushToken/remove` while it isn't shown, its untagged push
  would look like the active server's. A switch keeps the token at the server left (`authController.leave`); logout
  and removing a server end it. A tap on another login's push asks to switch (`features/home/switchServer.ts`), then
  opens the link there (`webTabs.afterReset`). The open notifications of the other logins (`core/status/otherServers.ts`,
  loaded on start and resume, set by their pushes) are shown at the "more" menu, in the server list, below the
  notification center and in the icon badge (the sum).
- Real APNs delivery needs a physical device; sandbox and production differ, test via TestFlight.
- Open: who sends for a self-hosted installation. The store app has one APNs key: for now the server sends itself with
  keys from its configuration (installations with the publisher's keys or an own build of the app); a push relay of the
  app's publisher, to which each server sends only token and ids (as Nextcloud does), would be a second `PushSender`.

## Open points

- Store presence: app icons, privacy declarations, review notes (the chat is user generated content: reporting a message and blocking a user exist, see PF-CHAT MSG-12 and DM-07).
- User and admin guide (EN+DE) once the app is distributed, with the SSO proxy (`projectforge.app.proxyCookies`).
- Behind an SSO proxy, background polling (`renewLogin: false`) stays refused after the proxy session expired until
  the user opens a web page or does something in a native tab (both renew the login in the browser).
