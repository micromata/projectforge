# External plugins

A plugin whose backend is developed in a repository of its own, while its next pages and texts stay here. The
release jar (and so the Docker image) contains it as a binary. First (and so far only) one: the CVE-Clinic (formerly CVE-Radar: the display name, the next route `cveclinic` and the
docs anchors changed, the technical names did not: plugin id, tables, settings and package stay `cveradar`/`Cvr*`;
`OrphanedLinkFilter` redirects the old `next/cveradar/**` links).

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

## The plugin repository

- This repository as git submodule `projectforge/`, pinned by the release to its tag; composite build
  (`includeBuild`, ProjectForge's `buildSrc` as plugin build `pf-build-logic`, because `buildSrc` is a reserved build
  name; the version catalog from `projectforge/gradle/libs.versions.toml`). Core modules are used as
  `org.projectforge:<module>`, the texts at test time as `org.projectforge:org.projectforge.plugins.cveradar`.
- `-PpfCoreDir=<worktree>` builds against a local worktree instead of the submodule.
- Artifact `plugin/build/libs/projectforge-plugin-cveradar-<version>.jar`, the version is the one of ProjectForge.
- `bin/pf.sh <pfDev.sh command>` there builds the jar and runs this repository's `pfDev.sh` with it
  (`PF_EXTRA_PLUGINS`), e.g. `run 1` or `gen` (regenerates `lib/metadata/cvr-*` here, commit them here).

## Development and release

- Pages, texts and changelog entries (`changelog/unreleased/`, the feature is shipped) of a plugin change are made
  here, its backend in its repository; one change may need commits in both.
- Without `PF_EXTRA_PLUGINS`, a dev slot of this repository runs without the plugin (its pages are switched off).
- `bin/pfDev.sh release` needs the checkouts of the external plugins for a published release: comma separated in
  `PF_EXTERNAL_PLUGINS`, or one per line in `~/.config/projectforge/external-plugins`. It checks them first (clean,
  not behind, tag free), runs their tests with the ones here, builds them against the release commit into the
  jar (refused if one is missing in it), pins their submodule to the tag and tags them as well; `publish` pushes
  them after this repository. Docker (`docker/push-release.sh`) and the GitHub release take the jar, unchanged.
- The history of the plugin was extracted with `git filter-repo --paths-from-file` (paths
  `plugins/org.projectforge.plugins.cveradar/`, `dev-docs/requirements/PF-CVR*` with its images and
  `docker/compose/dependency-track/`, moved to `plugin/`, `docs/` and `docker/`).
