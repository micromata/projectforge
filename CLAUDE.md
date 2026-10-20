# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build/Test Commands
- Build all: `./gradlew build`
- Build skipping tests: `./gradlew build -x test`
- Run application: `./gradlew bootRun`
- Single test: `./gradlew test --tests "org.projectforge.package.ClassName.methodName"`
- Package tests: `./gradlew test --tests "org.projectforge.package.*"`
- Run specific module tests: `./gradlew :projectforge-business:test`

## Language
- Write all code comments, KDoc/JavaDoc, commit messages and documentation in English
- German UI texts (`*_de.properties`, `messages/de.json`, mails) never use the formal "Sie". Where it fits,
  phrase them without direct address ("Hier kann dies und das gemacht werden" rather than "Hier kannst du
  dies und das machen"); otherwise use "du" ("ihr"/"euch" for a plural audience, e.g. a mail to all
  participants)

## Generated files
- Never edit `projectforge-next/messages/generated.*.json` or
  `projectforge-application/src/main/resources/i18nKeys.json` by hand. They are produced only by the
  generator (`bin/pfDev.sh gen`, i.e. `developmentMainForRelease`), which may be run after an i18n change;
  a manual change would be overwritten on the next run anyway.
- Merges and rebases don't conflict on the generated files (`.gitattributes`: merge driver `pf-generated`, which
  keeps the own side; registered by any `bin/pfDev.sh` call). Afterwards, run `bin/pfDev.sh gen` and commit the
  regenerated files.
- The changelog is written only in `changelog/changelog.json` (English, published — no names of persons,
  customers or internal teams), its German translation for the app in `changelog/changelog.de.json` (same
  structure, texts only, by version/id; every entry must be translated). A new change in a branch goes into
  its own file `changelog/unreleased/yyyyMMdd-<slug>.json` with English and German text (see the README
  there), not into these two files; `bin/pfDev.sh release` moves them into the release entry. `site/_changelogs/*.adoc`,
  `site/changelog-posts.adoc` and `projectforge-next/lib/generated/changelog{,.de}.json` are generated from
  them by the same `gen` run (`GenerateChangelogMain`) and never edited by hand.

## Website (`site/`)
- The website is English and German (see `site/README.adoc`). Every change of an English file goes into its
  German version in the same commit, with the same headings and anchors; `SiteTranslationTest` fails otherwise.
- Kept in both languages (English → German):
  - pages `site/*.adoc` → `site/de/*.adoc` (`index`, `features`, `about`, `services`, `download`, `search`),
    `site/misc/faqs.md` → `site/de/misc/faqs.md`; the German permalink is the English one prefixed with `/de`
  - docs `site/_docs/{userguide,adminguide,quickstart}.adoc` → `site/_docs/de/` (every heading with an explicit
    anchor `[[id]]`, the same in both; cross references by id, `<<id>>`, never by title)
  - FAQs `site/_faqs/*.md` → `site/_faqs/de/` (same categories)
  - data files `site/_data/*.yml` → `site/_data/de/` (same structure, German texts, `/de` urls), except
    `translation.yml`, which has the texts of layouts and includes for both languages (keys `en`, `de`)
  - texts in layouts and includes (`site/_layouts/`, `site/_includes/`): via `_data/translation.yml`, or
    `page.lang == 'de'` branches for whole sentences (`latest-binaries.html`, `release-pills.html`, …)
- Generated in both languages, never edited by hand: `site/_changelogs/` + `site/_changelogs/de/`,
  `site/changelog-posts.adoc` + `site/de/changelog-posts.adoc` (see Generated files).
- English only, marked `untranslated: true` in the front matter: the docs `installation`, `development`,
  `technologies`, `migration`, the legal pages `site/misc/{imprint,privacy-policy,terms-of-use}.md` and
  `site/404.html` (one page with both languages). German pages link them in English, marked (EN). A new page
  needs its German version or this marker.

## Flyway migrations
- New migrations are versioned by the release that ships them: `V<release>.<n>__RELEASE-<Name>.sql`, always
  four segments, e.g. `V9.0.4.1__…`, `V9.0.4.2__…` for two scripts in release 9.0.4. The release is the
  `version` in `gradle.properties` (without `-SNAPSHOT`) at merge time. Up to `V8.0.37` versions were
  counted up within the major version; those files keep their names.
- Scripts live in `projectforge-business/src/main/resources/flyway/migrate/common`, or under the same
  version in both `postgresql` and `hsqldb` if the SQL differs.
- A migration must never be lower than one already shipped (`outOfOrder` is off): if a branch misses its
  planned release, rename its scripts to the next release before merging. Check open feature branches for
  versions already taken in the same release.

## Security
Rules from security reviews; each one was broken somewhere before.
- **Access is checked on the server, per endpoint.** Hiding a page, menu entry or button in the client protects
  nothing: every `/rs` endpoint checks it itself (`@AccessChecked`; the baseline of `RestEndpointAccessCheckTest`
  may only shrink). This includes endpoints that only read a preset or metadata (`newEntry`, `listMeta`).
- **`BaseDao.hasLoggedInUser*Access()` answers, it doesn't throw.** Discarding its result checks nothing: throw
  yourself (`AbstractEntityRest.checkLoggedInUserInsertAccess()`), or use the variants with `throwException`.
- **A DAO without a `userRightId` allows everybody by default** (e.g. `BaseDao.hasInsertAccess(user)`). Such a DAO
  overrides `hasAccess` *and* `hasInsertAccess`; `hasAccess` never simply returns `true`.
- **Ownership comes from the database and the logged-in user, never from the request.** Take the owner, the parent
  (poll, task, …) and the id of the stored entry from the database; a posted id or `?owner=` would otherwise let
  anybody overwrite or act for somebody else. An update must not move an entry to another owner or parent.
- **Controllers and services are singletons:** no request or user state in fields, it is shared by all users.
- **State changing means POST/PUT/DELETE, never GET.** The CSRF token (`RestCsrfProtection`) is only checked for
  state changing methods, and a GET can be triggered by any link or `<img>`. Sending a mail or SMS, starting or
  cancelling a job, writing a user pref or a filter all count. In next: `request(path, { method: "POST" })`; in e2e:
  `headers: await writeHeaders(request)`.
- **Text stays text.** React escapes, so no `dangerouslySetInnerHTML` and no `rehypeRaw` without a sanitizer
  (`RichText` with DOMPurify, `rehype-sanitize`). Html built in the backend escapes every value (`HtmlElement`
  does); an endpoint answering an html fragment produces `text/plain` or JSON, never `text/html`. Mail templates
  escape user data (`pf.getString`, `HtmlSanitizer`).
- **SQL/HQL with bind parameters only.** What can't be bound (a table or column name) is checked against a
  whitelist or an identifier pattern.
- **The Content Security Policy has no `'unsafe-eval'` and allows no external hosts**
  (`projectforge.security.csp-header-value`). A new library that compiles code (`eval`, `new Function`) or loads
  scripts, fonts or images from elsewhere is refused by the browser: check the browser console for CSP violations
  after adding a dependency, and widen the policy only on purpose. Security headers are set by
  `SecurityHeaderFilter` with `setHeader` (not `addHeader`), consistent with the headers config of Spring Security.
- **A session that gains privileges gets a new id** (login: `LoginService.internalLogin`, second factor:
  `onSecondFactorSucceeded`).
- **An access rule comes with a test** that shows both halves: refused for an ordinary user (`TEST_USER`), allowed
  for the user it is meant for.

## Code Style Guidelines
- Use Kotlin JVM target 17 for all code; legacy code is in Java
- Follow standard Kotlin naming conventions (camelCase for variables/functions, PascalCase for classes)
- Include standard ProjectForge license header in all new files
- Organize imports with Kotlin stdlib first, followed by domain/project imports
- Use non-null types by default; use Kotlin's nullable types (Type?) when needed
- Use JUnit (Jupiter, JUnit 6) for tests with descriptive method names
- Prefer Kotlin extension functions for utility methods
- Use SpringBoot annotations for dependency injection
- Use Kotlin Coroutines for async operations
- Handle exceptions with appropriate logging using kotlin-logging
- Format code with 4-space indentation

## graphify

This project can use a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships. Both the graph and the tool are local and untracked: install graphify into a project-local venv, then run `graphify update .` to build graphify-out/. Until then the rules below simply do not apply — work from the sources as usual.

The graphify binary is installed here at `venv/bin/graphify` (project-local venv, never on `$PATH`); invoke it as `venv/bin/graphify query "..."` etc.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
