# Deprecations for the published app

Fields, values and endpoints of the server that the **published** app still reads, although the server has a
better replacement. They stay until `AppCompatibility.MIN_APP_VERSION` is above the last app version that reads them.
The rules (and how to find out what the app reads) are in
[mobile-app.md](mobile-app.md#changing-an-api-the-app-uses); the release checklist is in `CLAUDE.md`.

| Field / endpoint | Replacement | Deprecated since (server) | Last app version that reads it | Remove when |
| ---------------- | ----------- | ------------------------- | ------------------------------ | ----------- |
| – | – | – | – | – |

Removed without an entry because the published app did not read them: `InitData.gatewayConfigured` of
`/rs/myBusinessCards` (9.2.6; the app parsed it but never used it; replaced by `photoUrlAvailable`, which only the
web version reads).
