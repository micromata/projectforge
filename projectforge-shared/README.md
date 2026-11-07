# @projectforge/shared

TypeScript shared by the app ([`projectforge-app`](../projectforge-app/README.md)) and the web frontend
([`projectforge-next`](../projectforge-next)). Consumed as `"@projectforge/shared": "file:../projectforge-shared"`
(a symlink, as a workspace would be) and imported by path: `@projectforge/shared/base64url`, `@projectforge/shared/rs/app-auth`.

- `src/rs/`: the types of the `/rs` api the clients read, named after the Kotlin classes (in the comment). The app reads
  server data only through them: `AppApiContractTest` (projectforge-application) checks every field of these
  interfaces against the Kotlin classes.
- Everything else: pure helpers (no React, no DOM, no React Native, no globals beyond ES2022 and `atob`/`btoa`),
  shipped as TypeScript sources and compiled by the consumer (Metro, Turbopack).
