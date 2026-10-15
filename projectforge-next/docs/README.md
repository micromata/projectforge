# projectforge-next — documentation

The Next.js frontend of ProjectForge, served by the same Spring Boot application as the legacy
Wicket (`/wa`) and React (`/react`) frontends, under `/next`.

| Document                                     | What it covers                                                                                        |
| -------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| [architecture.md](architecture.md)           | How the package is built, served and routed; the layers (`lib/rs`, metadata, i18n, formatting, state) |
| [page-declarations.md](page-declarations.md) | The concept behind list and edit pages: one declaration per entity, rendered by two generic shells    |
| [entities.md](entities.md)                   | The three entities implemented so far — `book`, `cost1`, `order` — and what each of them proves       |

Related documents outside `docs/`:

- `../CLAUDE.md` — the binding conventions for writing code in this package (reuse rules, file
  tiers, styling, i18n, quality gates). Read it before changing anything.
- `../MIGRATION.md` — the migration plan and its phases (German).
- `../MIGRATION-list-paging.md` — server-side paging and the unified filter state.
- `../MIGRATION-calendar.md` — the plan for the calendar page (German).

Not documented here: `components/dynamic/` and `lib/dynamic/`, the renderer for server-laid-out
(`UILayout`) pages. It is provisional — a bridge that keeps un-migrated pages reachable under
`/next` until they get a real page of their own — and its shape is expected to change or disappear.
