<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.

<!-- END:nextjs-agent-rules -->

# ProjectForge Next.js — Project Conventions

This is the in-progress migration of the legacy `projectforge-webapp` (CRA + React) into a modern Next.js App Router app. The legacy code is a **reference for capabilities and domain logic only** — never copy its structure, styling, or patterns. Everything written here must be clean, structured, and maintainable for the long term.

## Reuse first — always

**Before writing any new component, hook, util, or type, you MUST search the existing codebase.** Reusability is the single most important rule of this project.

1. `grep`/`find` `components/`, `lib/`, `hooks/` for similar names and purposes. Read at least one match.
2. For any UI primitive (button, input, dialog, dropdown, etc.) check if shadcn provides it. If yes, install via `npx shadcn@latest add <name>` — never hand-roll. Never edit anything in `components/ui/`.
3. For domain features, check `projectforge-webapp/` (legacy) to understand existing capabilities and naming. Do **not** port its code; reimplement cleanly.
4. If you find a near-match that needs extension, extend or generalize it rather than forking. Big or duplicated components should be picked up and split.

If you create something that could plausibly be reused, put it in the right tier (see below) so the next agent finds it.

## File structure (3-tier + lib)

```
components/ui/              shadcn primitives — managed by CLI, never edit
components/shared/          app-wide reusables (AppSidebar, BrandStripe, MicromataIcon)
components/data-table/      generic primitive sibling to ui/ (DataTable, hooks, headers)
components/features/<noun>/ domain-specific (books/, invoices/, ...). No cross-feature imports.
lib/                        pure TS utilities, API client, types
lib/rs/                     SOLE entry point for Spring backend calls (fetchList, types, ...)
hooks/                      cross-cutting React hooks (use-mobile, ...)
store/                      Zustand stores (client-only global state)
app/                        routes only — pages compose features, no business logic here
i18n/  messages/            next-intl config and locale JSON
```

Cross-feature imports are forbidden. If two features need the same thing, it belongs in `shared/`, `data-table/`, `lib/`, or `hooks/`.

## Component rules

- **Hard cap ~150 lines per file.** If you cross it, split. Subcomponents used in only one place may live in the same file but should be extracted the moment they're reused.
- **One responsibility per file.** Naming should make this obvious.
- **Co-locate types with the feature** in `types.ts`. No global types dump.
- **Server Components by default.** Add `"use client"` only when state, effects, or browser APIs are needed. Keep client islands small — push state down, not up.
- **No barrel files** (`index.ts` re-exports) except at primitive boundaries like `components/data-table/`. Import direct paths.

## Naming

- Files: `kebab-case.tsx` (e.g. `books-table.tsx`).
- Components: `PascalCase` (e.g. `BooksTable`).
- Feature folders: plural domain noun (`books/`, `invoices/`). Singular allowed for single-entity edit pages.
- Hooks: `use-*.ts`, prefixed `use*` in code. Cross-cutting in `hooks/`; feature-specific co-located.
- Types files: `types.ts`. Schemas: `schema.ts`.

## Styling

- **Tailwind utilities only.** No `styled-components`, no `emotion`, no inline `style={...}` except for CSS-var-driven dynamic colors (e.g. `style={{ background: "var(--status-loaned-bg)" }}`).
- **No inline hex colors.** All brand colors via `--brand-*`, `--primary`, `--status-*` tokens defined in `app/globals.css`. If you need a new color, add a token first.
- **Always use `cn()`** from `lib/utils` for conditional / composed classes. Never string concatenation or template literals for class composition.
- **Mobile-first.** Base styles target mobile; layer up with `sm:` / `md:` / `lg:`. No desktop-only components without an explicit mobile fallback.
- Complex non-utility styles go into `globals.css` under a semantic class, not inline.

## Icons

- **Hugeicons only.** `@hugeicons/react` + `@hugeicons/core-free-icons`. No `lucide`, no other icon libs, no inline SVG (except brand marks like `MicromataIcon`).

## Data fetching & state

- **All server state goes through TanStack Query** (`useQuery` / `useMutation`). No `useEffect` + `fetch`. No manual loading flags for server state.
- **All Spring backend calls go through `lib/rs/`.** Feature code never calls `fetch` directly — extend `lib/rs/client.ts` (e.g. add `fetchOne`, `save`, `delete`) before reaching for raw fetch. The mock route handlers under `app/rs/<entity>/...` mirror Spring Boot contracts exactly so they can be swapped via Next.js rewrite later.
- **Local `useState` first.** Lift to **Zustand** (under `store/`) only when state is shared across unrelated trees.
- React Query keys must include the full filter object (or a stable derivation) so refetch is automatic.

## Forms, validation, i18n

- **`react-hook-form` + Zod** for all forms via the shadcn `Form` primitive (`zodResolver`). No raw `<form onSubmit>` handlers.
- **Zod schemas mirror Spring Boot DTOs exactly** when wiring real endpoints. Schema lives next to the feature.
- **All user-facing strings via `next-intl`.** Add to `messages/<locale>.json`; reference via `useTranslations` / `getTranslations`. No hardcoded German (or any natural language) in JSX. When you touch a file with hardcoded strings, migrate them.

## Accessibility

- **All interactive elements need accessible names.** Icon-only buttons require `aria-label`. Form inputs need labels (visible or `sr-only`). Dynamic labels should reflect the row/item (e.g. `aria-label={`Buch ${row.titel} bearbeiten`}`).

## Quality gates (must pass before reporting done)

1. `npm run typecheck` — zero errors.
2. `npm run lint -- --fix` — auto-fix trivial issues; zero remaining errors. Warnings should be discussed if present.
3. `npm run format` — clean.
4. **Never edit `components/ui/`.** If a shadcn primitive needs updating, re-run `npx shadcn@latest add <name>`.
5. For UI changes, manually verify in the browser at `localhost:3000`. If you can't test in a browser, say so explicitly — type/lint passing is not the same as feature-correct.

## Communication

- **Plan mode for any non-trivial task.** Use `ExitPlanMode` to get approval before implementing anything beyond a one-line edit.
- **Ask before assuming.** When scope, location, or naming is ambiguous, use `AskUserQuestion` rather than guessing.
- **No end-of-task summaries unless asked.** State what changed, nothing more.
- **Flag deferred work explicitly** so the user can confirm or push back. Do not silently expand or contract scope.

## Reusable components — where to look

The agent must `grep`/`find` before writing new code. The directories below are the canonical homes; scan them in this order:

1. `components/ui/` — shadcn primitives (button, input, table, dialog, sidebar, dropdown-menu, popover, collapsible, badge, checkbox, radio-group, separator, skeleton, ...). Don't edit; install via shadcn CLI.
2. `components/data-table/` — generic data table primitive. The `DataTable` here is the **only** way to render tabular data; do not hand-roll tables. Includes `useMagicFilterQuery` for server-driven sort/page/search bridging TanStack ↔ Spring `MagicFilter`.
3. `components/shared/` — app-wide chrome (sidebar, brand stripe, brand mark).
4. `components/features/<noun>/` — domain code. Look for prior solutions to similar problems before inventing new ones.
5. `lib/rs/` — Spring backend client and contract types (`MagicFilter`, `ResultSet`, `fetchList`, `RsError`).
6. `hooks/` — cross-cutting hooks.
7. `store/` — Zustand stores.

When you add a new reusable, place it in the matching tier. Do not duplicate functionality across tiers.

## Migrating pages

To migrate an entity page from Wicket or the old React webapp to this Next.js app, use the `/migrate-page` skill (`.claude/skills/migrate-page/SKILL.md`). It documents the full workflow: locating source pages and DTOs in the Java backend, using the reusable shell components (`PageShell`, `ListPageShell`, `EditPageShell`), wiring `lib/rs/`, and following all conventions. The books entity is the reference implementation.
