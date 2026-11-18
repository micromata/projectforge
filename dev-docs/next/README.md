# projectforge-next — documentation

The Next.js frontend of ProjectForge, served by the Spring Boot application under `/next`. It replaced
the Wicket and React frontends, which are removed.

| Document                                                       | What it covers                                                                                        |
| -------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| [architecture.md](architecture.md)                             | How the package is built, served and routed; the layers (`lib/rs`, metadata, i18n, formatting, state) |
| [page-declarations.md](page-declarations.md)                   | The concept behind list and edit pages: one declaration per entity, rendered by two generic shells    |
| [entities.md](entities.md)                                     | The reference entities — `book`, `cost1`, `order` — and what each one proves about the concept        |
| [list-paging.md](list-paging.md)                               | Server-side paging of the large lists: why an id list, how it works, how to turn it on                |
| [open-points.md](open-points.md)                               | The one list of what is left: missing tests, list-paging follow-ups, deferred features                |
| [plugin-frontend.md](plugin-frontend.md)                       | Frontend plugins loaded at runtime: manifest, outlet, texts, bridge, build                            |
| [plugin-frontend-colocation.md](plugin-frontend-colocation.md) | Superseded proposal: plugin pages living in the plugin module at build time                           |

Related documents outside this folder:

- `projectforge-next/CLAUDE.md` — the binding conventions for writing code in this package (reuse rules, file
  tiers, styling, i18n, quality gates). Read it before changing anything.
