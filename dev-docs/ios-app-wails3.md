# iOS chat app with Wails v3 and Claude Code

Status: proposal, nothing built yet. This note estimates the effort of shipping an iOS app with a chat and push
notifications on top of the ProjectForge chat (see [requirements/PF-CHAT_Chat_Discussion.md](requirements/PF-CHAT_Chat_Discussion.md)).

Statements about Wails v3 internals (task names, generated project layout) come from the Wails documentation as
remembered and from a research answer; they are **to be verified** against the current Wails v3 docs before the
spike starts.

## Short answer

**Medium effort, roughly 1-3 days for a first working build**, because the prerequisites are in place:

- A Wails v3 desktop app already builds.
- An Apple developer account, code signing, notarization and App Store deployment are mastered.
- The server is under our control, so the push sender can be added freely.

The only real risk is **remote push (APNs)**: it needs native iOS code that Wails does not provide, and it cannot
be fully tested in the simulator. Wails v3 mobile support is also still **experimental**, so plan extra test time
before calling the result production-grade.

## Effort by component

| Component            | Effort        | Tool                       | Main challenge                                                          |
| -------------------- | ------------- | -------------------------- | ----------------------------------------------------------------------- |
| Build for iOS        | low           | Wails v3 CLI, Xcode        | macOS and Xcode required; mobile support experimental                   |
| Signing, store       | none          | existing account           | already done                                                            |
| UI                   | medium        | CSS / frontend             | responsive layout, safe areas (notch, Dynamic Island), keyboard handling |
| Chat connection      | low to medium | SSE / REST as in the chat  | connection is dropped when the app goes to the background               |
| Push notifications   | high-ish      | native Swift/Obj-C, APNs   | capability, token hand-over, server sender, real device needed          |

### Build for iOS

Wails v3 compiles the same Go code and the same frontend for iOS; bindings and events work as on the desktop.
The expected workflow is a task to run the app in the iOS simulator (`wails3 task ios:run`) and a task to open the
generated Xcode project (`wails3 task ios:xcode`) for signing and capabilities. The frontend runs inside a
`WKWebView`, i.e. like a single-page app.

### UI

The existing chat frontend of `projectforge-next` is mobile first (NFR-02), but the Wails shell needs its own
checks: safe-area insets (`env(safe-area-inset-*)`), the on-screen keyboard, pull-to-refresh/overscroll and
tap targets. Estimated at about half a day.

### Chat connection

The chat specification already fits the iOS lifecycle: live updates via SSE with event ids and a polling
fallback (NOT-06), no WebSocket (NOT-08). Nothing changes while the app is in the foreground. When iOS suspends the
app, the connection is dropped; on resume the client reconnects with the last event id and receives the missed
events (or falls back to polling).

## Push notifications

### Why it is the hard part

In the background iOS suspends the Go runtime and the `WKWebView`. No SSE/WebSocket connection survives, so
"live" is impossible; only APNs can wake the user. Local notifications (a Wails event) do not help, because
nothing runs that could create them.

### Design

1. **Capability**: add *Push Notifications* (and *Remote notifications* background mode, if silent pushes are
   wanted) in the Xcode project; the entitlement `aps-environment` is created with it.
2. **Native hooks** in the app delegate of the generated wrapper:
   `application(_:didRegisterForRemoteNotificationsWithDeviceToken:)`, `...didFailToRegisterForRemoteNotificationsWithError:`
   and `userNotificationCenter(_:didReceive:withCompletionHandler:)` for taps. Call
   `registerForRemoteNotifications()` after the user granted permission (`UNUserNotificationCenter.requestAuthorization`).
3. **Hand-over to the app**: the native layer forwards the device token to Go (or directly to the web view) as a
   Wails event; the frontend registers it at the server with a state-changing request
   (`POST`, CSRF token, access checked on the server, see the Security rules in `CLAUDE.md`). A tap on a push opens
   the conversation (deep link) and triggers a resync.
4. **Server**: store *per user and device* the token, the environment (sandbox/production), the app bundle id and
   the time of last use. Send with a token-based APNs key (`.p8`, HTTP/2, no yearly certificate renewal) via a
   Java APNs client library. Remove a token when APNs answers `410 Unregistered` or `BadDeviceToken`.
5. **When to push**: only when the recipient does not watch the conversation via SSE; this is the same rule as for
   the existing notifications ("who has the conversation open counts as watching", NOT-06).
6. **Payload without content**: the push carries only a generic text, the conversation id and a counter, never the
   message text. This keeps push data out of Apple's infrastructure and is required for encrypted
   conversations anyway (CONF-02). The app fetches the content from the server when opened. If the message text
   should be visible on the lock screen, a *Notification Service Extension* (`mutable-content`) is needed, which
   decrypts or fetches the text on the device; that is an additional native target and not part of the minimum.

### Limits of testing

- The simulator accepts local test payloads (`xcrun simctl push`), but real APNs delivery needs a **physical
  device**.
- Sandbox and production APNs differ by environment and token; test via TestFlight before the store release.

## Role of Claude Code

Claude Code is well suited for the mechanical native part: reading the generated Xcode project, adding the
delegate methods, the entitlement and the permission request, wiring the token event into Go and the frontend,
and writing the server side (token endpoint, APNs sender, cleanup). It can also drive the simulator build.

Points to watch:

- The generated iOS project may be **regenerated** by Wails. Native patches must live in the Wails template or in a
  task that applies them, not as one-off hand edits in the generated files, otherwise they are lost.
- Code signing, provisioning and the capability switch in the developer portal stay manual.
- A physical device test cannot be done by Claude Code; the user has to run and report it.

## Risks and open points

- **Experimental mobile support** in Wails v3: API and project layout may change between releases.
- **Background behaviour**: any feature that assumes a running app (uploads, long polls) needs a resume path.
- **TLS and CSP**: the app talks to the server over HTTPS only; check the Content Security Policy
  (`projectforge.security.csp-header-value`) and the session/cookie handling in the `WKWebView` (login, second
  factor, session cookie lifetime); a native shell may need token-based auth instead of the session cookie.
- **App Store review**: a chat is user-generated content (report/block functions, terms) and needs a privacy
  declaration for the push token.
- **Alternative**: an installed PWA supports web push since iOS 16.4 and needs no native code, but depends on
  "Add to Home Screen", has limited background reliability and no store presence. Capacitor is the other
  mature route if Wails mobile turns out to be too unstable.

## Suggested steps

1. Spike: run the existing Wails app in the iOS simulator and log in against a test server.
2. Make the chat layout safe-area and keyboard clean.
3. Add the native token hook and print the token in the app.
4. Server: token registration endpoint, table, APNs sender (sandbox first), cleanup on `410`.
5. Test on a physical device (push received, tap opens the conversation, resume resync).
6. TestFlight, then the store release.

## Open decisions

- Native shell session handling: cookie vs. dedicated token for the app.
- Generic push text only, or a Notification Service Extension for the message text.
- Where the native patches live (Wails template vs. task script).
