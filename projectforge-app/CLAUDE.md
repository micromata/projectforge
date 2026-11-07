# ProjectForge app (React Native, Expo) — conventions

See [README.md](README.md) for setup, [AGENTS.md](AGENTS.md) for Expo (versioned docs, never from memory) and
[dev-docs/mobile-app.md](../dev-docs/mobile-app.md) for the architecture (login, web session, SSO proxy, push).

- Structure: `src/app/` routes (expo-router) only, `src/core/` (api client, auth, i18n, cache, log) and
  `src/features/<noun>/` (screens, repository, hooks, models of one feature). No imports between features; shared code
  goes into `src/core/`, code shared with projectforge-next into `../projectforge-shared`.
- Server data only through the interfaces of `projectforge-shared/src/rs/` (each with `@server`/`@request` and its
  Kotlin class, checked by `AppTsApiContractTest`), every endpoint in `test/contract/app-api-contract.json`. Server
  calls only through a repository using `ApiClient` (`src/core/api/apiClient.ts`), never `fetch` in a component.
- Urls: never the global `URL` of React Native (not WHATWG conformant), always `parseUrl`/`SafeURL` of
  `src/core/url.ts`; requests only with `platformHttp` and `HTTP_DEFAULTS` (no cookie jar, no redirects). ESLint
  refuses the globals.
- Texts: `t('key')` with the key written out (the generator only finds literal keys; keys picked by value need a
  prefix in `GenerateAppI18nMessagesMain.TARGETS`). Prefer the keys of the server's I18nResources; app only texts in
  `assets/i18n/app.{en,de}.json`. German without "Sie". Run `bin/pfDev.sh gen` after adding a key; never edit
  `assets/i18n/generated.*.json`.
- Security: the app token stays in the `SessionStore` (key chain/keystore), nothing personal in logs (`appLog`
  masks). The WebView: every navigation is decided by `WebNavigation` (`originWhitelist={['*']}` stays), no
  JavaScript bridge beyond the commit and find messages, the files a page of the own server made itself
  (`BLOB_SCRIPT`, to the share sheet only) and next's status signal (no data, the user status is loaded again). Access is checked by the server; hiding a button protects nothing.
- License header of ProjectForge on top of every source file.
- Quality gates before reporting done: `bin/pfApp.sh check` (tsc for app and shared, eslint, jest). UI changes: try
  them in the simulator (`bin/pfApp.sh run <slot>`), or say that you couldn't.
