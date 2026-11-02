# ProjectForge app (Flutter) — conventions

See [README.md](README.md) for setup and [dev-docs/flutter-client.md](../dev-docs/flutter-client.md) for the
architecture.

- Structure: `lib/core/` (api client, auth, i18n, cache) and `lib/features/<noun>/` (screens, repository, Riverpod
  controller, models of one feature). No imports between features; shared code goes into `core/`.
- State with Riverpod (`flutter_riverpod`); server calls only through a repository using `ApiClient`
  (`lib/core/api/api_client.dart`), never `Dio` directly in a widget.
- Models keep the json map of the server (`BusinessCard.json`), so fields this app version doesn't know survive an
  edit. Mirror the server DTOs by name (Kotlin class in the comment).
- Texts: `context.t('key')` with the key written out (the generator only finds literal keys; keys picked by value
  need a prefix in `GenerateFlutterI18nMessagesMain.PREFIXES`). Prefer the keys of the server's I18nResources; app
  only texts in `assets/i18n/app.{en,de}.json`. German without "Sie". Run `bin/pfDev.sh gen` after adding a key.
- Security: the app token stays in `SessionStore` (key chain/keystore), nothing personal in logs. Access is checked
  by the server; hiding a button protects nothing.
- License header of ProjectForge on top of every Dart file.
- Quality gates before reporting done: `bin/pfApp.sh check` (dart format, flutter analyze without issues, flutter
  test). Format with `bin/pfApp.sh format`.
  UI changes: try them in the simulator, or say that you couldn't.
