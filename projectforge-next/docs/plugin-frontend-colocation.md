# Colocating plugin frontends in `plugins/**` (design proposal)

Status: **proposal / not yet implemented.** This document records the idea, the constraint that
shapes it, and a recommended approach, so a later migration can pick it up.

## Context

ProjectForge plugins are standalone Gradle modules under `plugins/org.projectforge.plugins.*`,
each holding the complete **backend** of a feature (DOs, DAOs, `*Rest.kt`, Flyway migrations,
i18n bundles, and a `META-INF/services/...AbstractPlugin` descriptor for ServiceLoader discovery).
When the first plugin was migrated to projectforge-next (`ee1055bd3`, liquidity-planning), the
backend stayed in the plugin module but the **entire frontend landed in `projectforge-next/`**
(`app/(authenticated)/liquidity/` + `components/features/liquidity/` + `lib/rs/liquidity.ts`, …).

The question this document answers: can those Next.js pages be moved into the plugin directories,
or must they stay in the next module?

## The core asymmetry

The backend plugin model works because Spring discovers and loads plugins **at runtime**
(ServiceLoader → `PluginAdminService` → `PluginsRegistry`).

projectforge-next is, by contrast, a **single** Next.js App Router build with `output: "export"`
(`next.config.ts`, `turbopack.root: __dirname`): routes come exclusively from the one `app/` tree,
and everything is resolved **at build time** and compiled into one static bundle, which Gradle
(`npmBuild → copyNextBuild → nextAppJar`) packs into the boot jar under `/next`. There is **no
runtime frontend plugin loading**.

**Consequence:** a plugin frontend can never be as independent as the backend plugin. The most that
is achievable is to **place the source files physically under `plugins/**`** and pull them into the
one Next build at compile time. A thin route stub per page must remain in `app/` (App Router
requirement), and the migration switch (`NextMigration.kt`, menu, 2FA) stays in the backend anyway.

## Recommended approach: one npm workspace package per plugin + thin route stubs

Each plugin that ships Next pages gets a frontend directory under its module, wired into the single
projectforge-next build as a **local npm workspace package**. The domain frontend code (today's
`components/features/<noun>/` part) lives in the plugin; `projectforge-next` keeps only the thin
route stubs and the workspace wiring.

### Layout inside the plugin

```
plugins/org.projectforge.plugins.liquidityplanning/
  src/main/frontend/                     # new frontend root of the plugin
    package.json                         # name: "@projectforge/plugin-liquidity", private
    features/                            # = today's content of components/features/liquidity/
      liquidity.page.tsx  liquidity-forecast-view.tsx  …  types.ts
    rs/liquidity.ts                      # = today's lib/rs/liquidity.ts (plugin-specific calls)
```

- The package **imports** the host primitives from projectforge-next (`@/components/shared/*`,
  `@/components/data-table/*`, `@/lib/rs/client`, `@/lib/page-def/*`, `@/lib/format`, i18n). It is
  therefore **not** a standalone package but a module built against the app-internal singletons
  (React, TanStack Query, `lib/rs`, the locale provider) — this coupling is the unavoidable
  consequence of the static-export model.
- The plugin code obeys the same rules as the host (static-export-safe, no route handlers/SSR,
  root-relative `/rs` calls, texts via next-intl; see `projectforge-next/CLAUDE.md`).

### Wiring in projectforge-next

1. **Enable npm workspaces** in `projectforge-next/package.json`:
   `"workspaces": ["../plugins/org.projectforge.plugins.*/src/main/frontend"]` (glob), so
   `npm install` links the plugin packages. Raise `turbopack.root` to a shared root if needed, so
   the plugin directories fall inside the watch/compile scope.
2. **tsconfig `paths`** extended with `@projectforge/plugin-*` (or via workspace resolution), so
   `tsc`, ESLint and the bundler see the plugin sources.
3. **A thin route stub stays in `app/`** (Next requires routes in the app tree). Instead of feature
   logic it only composes and delegates to the plugin package, e.g.
   `app/(authenticated)/liquidity/page.tsx`:
   `export { LiquidityListPage as default } from "@projectforge/plugin-liquidity/features/liquidity.page"`
   including `generateStaticParams` for `[id]`. These stubs are a few lines and rarely change.

### Developer experience

- Through npm workspaces (symlinks in `node_modules/@projectforge/plugin-*`), `next dev` sees the
  plugin sources directly; HMR works as long as the directories are within the turbopack
  root/watch scope. No per-change copy step needed.
- If the watch scope causes problems: fallback = a Gradle/npm `prebuild` task that **symlinks**
  (not copies) `plugins/**/src/main/frontend/**` into `projectforge-next/.plugins/**`, followed by
  `next dev`. Rebuild only on a new plugin/new route.

### Build wiring (Gradle)

- `projectforge-next/build.gradle.kts`: `npmBuild` runs unchanged — the plugin packages are already
  part of the one build via workspaces. Adjust the `npmInstall` trigger so that a changed plugin
  `package.json` forces a re-install.
- **Gating optional plugins:** because everything is bundled at build time, a deactivated plugin
  (`pf.plugins.active`) is only "off" at runtime in that its menu entry and its `NextMigration`
  route are absent, or the REST endpoint answers 404/403 — the code is still in the bundle. Truly
  excluding it from the bundle would require conditional workspace inclusion + conditional stubs
  (see "Limits").

### Switch/2FA stay in the backend (unchanged)

`NextMigration.MIGRATED`, `HAND_BUILT_CATEGORIES` (kept in sync by `NextMigrationTest`),
`MenuItemDefId.url` and `ProjectForge2FAInitialization` (`NextMigration2FATest`) stay where they
are. They are not part of the move — only the TSX/TS sources migrate.

## Representative files (pilot: liquidity-planning)

To move (from `projectforge-next/` → `plugins/org.projectforge.plugins.liquidityplanning/src/main/frontend/`):

- `components/features/liquidity/*` → `features/*`
- `lib/rs/liquidity.ts` → `rs/liquidity.ts`

Remaining in / adapted in projectforge-next:

- `app/(authenticated)/liquidity/page.tsx`, `.../[id]/page.tsx`, `.../[id]/page-client.tsx`
  → reduced to thin re-export/composition stubs
- `package.json` (workspaces), `tsconfig.json` (paths), possibly `next.config.ts` (turbopack root)
- `lib/hand-built-categories.ts` stays (must stay in sync with the backend)

New in the plugin:

- `src/main/frontend/package.json` (+ the moved sources)

## Limits (honest)

- **No true runtime plugin frontend.** Static export has no dynamic loading; a third-party plugin
  could not add its frontend without a full Next rebuild of projectforge-next. For in-repo
  first-party plugins (the current case) this is a non-issue; for genuinely optional/third-party
  plugins the route would be Module Federation or similar — incompatible with `output: "export"`
  and therefore not recommended here.
- **The route stub stays in the app tree.** Being fully "plugin-only" is impossible because of the
  App Router requirement; ~2–3 lines of stub per page is the floor.
- **Coupling to host internals.** The plugin package is not standalone — it builds against the
  projectforge-next primitives. The cross-feature import rule still applies.

## Verification (when implemented)

1. Do the pilot with liquidity-planning only.
2. `cd projectforge-next && npm install` → check that `node_modules/@projectforge/plugin-liquidity`
   exists as a symlink.
3. `npm run typecheck` and `npm run lint` — zero errors (proves the workspace resolution works).
4. `npm run dev` (:3000), open `/next/liquidity` and `/next/liquidity/<id>`; make a change in
   `plugins/.../frontend/features/*` and confirm HMR.
5. `npm run build` — the static export must pass (CI gate for export compatibility).
6. `./gradlew :projectforge-next:build` (or `./gradlew build -x test`), then start the app
   (`bootRun`) and check `/next/liquidity` against the static export under `:8080`.
7. E2E: `E2E_BASE_URL=http://localhost:8080 npx playwright test` for the liquidity spec (if one
   exists), otherwise manually with `e2e-full-access` (credentials in
   `$PROJECTFORGE_HOME/testAccounts.txt`).
8. Backend tests untouched: `NextMigrationTest`, `NextMigration2FATest` must stay green.
