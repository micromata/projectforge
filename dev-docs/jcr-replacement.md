# Replacing the JCR (Apache Oak)

## Status
The new file store is in place next to Oak (`projectforge-jcr/.../jcr/store`, schema `pf_files`): files of
entities in the database, data transfer files in the file system. `projectforge.files.store=jcr|db` selects the
store. Under `db`, the migration copies all JCR files on every start and verifies each one (size, SHA-256); the
JCR stays complete. Without configuration, existing installations stay on `jcr`. Admin guide:
`moving_the_files_out_of_the_jcr`.

## Plan
- **9.1.x:** last release with JCR support (and with Java 17). Every installation has to run 9.1.x and finish the
  migration there. 9.1.0 only documents this (changelog, admin guide, migration guide, download page); the to-dos
  below are still open and may follow in 9.1.x patch releases or 9.2.
- **9.2+:** no JCR anymore. Oak and the migration code are removed. There is no import from a JCR backup zip:
  an installation that skipped the migration has to go back to 9.1.x first.

## To do for 9.1.x (before 9.2)
- Default to `db` for existing installations as well. `jcr` stays possible only by explicit configuration, as a
  temporary fallback.
- A persistent completion marker in the database, written when a run finishes without errors (e.g. a row in
  `pf_files`). The report file can't be that marker: after a database restore it no longer matches the database.
  9.2 reads the marker without Oak.
- Show the migration status clearly (system page, admin notice while it is incomplete or `jcr` is configured).
- Mark 9.1.x as the last release with JCR support (done for 9.1.0: changelog, admin guide EN + DE, migration
  guide, download page). Upgrade path: install 9.1.x → finish the migration → then 9.2+.
- Rollout note: measure the JCR size first (without data transfer). It goes 1:1 into the database, pg_dump and
  restore times.
- The gateway instance (its own JCR, data transfer only) migrates with 9.1.x as well.

## To do for 9.2
- Start-up guard: if the JCR directory contains data and there is no completion marker, refuse to start and
  point to 9.1.x.
- Remove Oak: `OakStorage`, `SegmentTarStorage`, `RDBStorage`, `RepoBackupService`, `JCRBackupJob`,
  `JcrBackupZipJob`, `JCRCheckSanityCheckJob`, the backup/restore/sanity CLIs, `backupReadme.txt`,
  `FileStoreMigrationService`, `RepoMigrationJob`, the switch `projectforge.files.store`,
  `projectforge.dir.jcr`, and the Oak dependencies (`libs.versions.toml`, `projectforge-jcr`,
  `projectforge-application`). `RepoService` becomes a thin facade over `FileStore`, or goes away.
- Optional: rename the module `projectforge-jcr` and the package `framework/jcr`.
- Docs (EN + DE): remove the JCR sections (admin guide, installation, backup). Note that `<home>/jcr` can be
  deleted once 9.2 is running.

## Unrelated, still open
- Fixed salt in `CryptStreamUtils` (user encryption of attachments): store a salt per file, keep the fixed salt
  as a fallback for existing files.
- `externalPassword` of the data transfer areas is stored in plain text (separate topic).

## Verification
- `./gradlew :projectforge-jcr:test :projectforge-business:test` and the data transfer plugin tests.
- 9.1.x: migrate a copy of an existing installation (pfDev slot, HSQLDB and PostgreSQL). The report must show no
  differences. Spot checks for download, upload, encryption and multi-download in order, invoice (incl. e-invoice
  export), contract, script, Merlin and data transfer (incl. external access and the cleanup job).
- 9.2: start on the migrated copy without `<home>/jcr`, and check that it refuses to start on an unmigrated copy.
