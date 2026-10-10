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
- The changelog is written only in `changelog/changelog.json` (English, published — no names of persons,
  customers or internal teams), its German translation for the app in `changelog/changelog.de.json` (same
  structure, texts only, by version/id; every entry must be translated). A new change in a branch goes into
  its own file `changelog/unreleased/yyyyMMdd-<slug>.json` with English and German text (see the README
  there), not into these two files; `bin/pfDev.sh release` moves them into the release entry. `site/_changelogs/*.adoc`,
  `site/changelog-posts.adoc` and `projectforge-next/lib/generated/changelog{,.de}.json` are generated from
  them by the same `gen` run (`GenerateChangelogMain`) and never edited by hand.

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
