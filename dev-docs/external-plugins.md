# External plugins

A plugin whose backend is developed in a repository of its own, while its next pages and texts stay here. The
release jar (and so the Docker image) contains it as a binary. First (and so far only) one: the CVE-Radar.

## How it works

- **Register:** `ExternalPlugins` (`projectforge-business/.../plugins/core/ExternalPlugins.kt`) knows each such plugin:
  id, next routes, prefix of its generated field metadata, package of its entities.
- **Binary into the build:** `-Ppf.extraPlugins=<jar or directory>[,...]` (or the environment variable
  `PF_EXTRA_PLUGINS`) adds the jars to the runtime classpath of `projectforge-application`: boot jar
  (`BOOT-INF/lib`), `bootRun` and the generators. The plugin is found by the `ServiceLoader` as every other one
  (`META-INF/services/org.projectforge.plugins.core.AbstractPlugin`); its own libraries, if any, come as further jars.
- **Without the plugin** (e.g. a community build from the sources):
  - `FeaturePathIndex` blocks its next routes: 404 on loading, `FeatureGuard` while navigating, no menu entry anyway.
  - `GenerateNextFieldMetadataTest` keeps its `lib/metadata/<prefix>*.generated.ts`, no orphans.
- **Texts** stay in this repository (resource bundle of the plugin module), so `gen` exports them to next as before.
- **Integration settings** (timeouts, size limits) of the plugin: `IntegrationConfig.registerBuiltin`, called by the
  plugin; the `projectforge.plugin.<id>.*` block stays documented in `application.properties` (read by
  `PropertiesMetadata`, unknown keys would be flagged as obsolete).
- **Flyway:** the plugin migrates with its own history table (`t_flyway_<id>_schema_version`), the scripts live in
  its repository; versioning as in CLAUDE.md.

## Plan: moving the CVE-Radar backend

Status: preparation done (the items above), the code is still in `plugins/org.projectforge.plugins.cveradar`.

1. **Extract** the history of the plugin with `git filter-repo --paths-from-file` from a fresh clone of `develop`:
   ```
   plugins/org.projectforge.plugins.cveradar/
   glob:doc/requirements/PF-CVR*
   doc/requirements/images/cve-radar/
   glob:dev-docs/requirements/PF-CVR*
   dev-docs/requirements/images/cve-radar/
   glob:dev-docs/cve-radar-*
   docker/compose/dependency-track/
   plugins/org.projectforge.plugins.cveradar/==>plugin/
   doc/requirements/==>docs/requirements/
   dev-docs/requirements/==>docs/requirements/
   dev-docs/cve-radar-==>docs/cve-radar-
   docker/compose/dependency-track/==>docker/dependency-track/
   ```
2. **Plugin repository:** this repository as git submodule `projectforge/` (pinned to the release tag),
   composite build (`includeBuild`, `pluginManagement { includeBuild("projectforge/buildSrc") }`, version catalog
   from `projectforge/gradle/libs.versions.toml`), core modules as `org.projectforge:<module>` coordinates. Artifact
   `projectforge-plugin-cveradar-<version>.jar`, the version is the one of ProjectForge. CI: build and tests on
   every push, nightly against `develop`.
3. **Here:** reduce `plugins/org.projectforge.plugins.cveradar` to its resource bundle `CveRadarI18nResources*`;
   remove `dev-docs/requirements/PF-CVR_*` and `docker/compose/dependency-track/` (`bin/collect-libraries.sh` and the
   site docs stay).
4. **Release** (`bin/pfDev.sh release`): build the external plugins (`PF_EXTERNAL_PLUGINS`, paths of their
   checkouts) against the release commit, pass them as `pf.extraPlugins`, refuse a published release whose jar lacks
   one of them; tag the plugin repository with the same `X.Y.Z-RELEASE`. Docker (`docker/push-release.sh`)
   and the GitHub release need no change: they take the jar.
5. Changes of the CVE-Radar keep their changelog entries here (`changelog/unreleased/`), the feature is shipped.

Remove this plan section once the move is done.
