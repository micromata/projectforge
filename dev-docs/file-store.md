# File store

All files (attachments of entities, Merlin templates, invoice PDFs, DataTransfer files) are stored by `FileStore`
(module `projectforge-files`, schema `pf_files`): metadata in `pf_files.t_attachment`, content chunked in the
database or, for registered paths (DataTransfer, `<home>/datatransfer`), in the file system (compressed, optionally
encrypted by `projectforge.files.fileSystemEncryptionKey`). Attachments of entities go through `AttachmentsService`
(`org.projectforge.framework.files`); an entity rest enables them by `AbstractEntityRest.enableAttachments()`.

The JCR (Apache Oak) was removed with 9.2; 9.1.x was the last release with it and migrated its files.
`LegacyJcrGuard` refuses the start, if the former JCR (`<home>/jcr`, `projectforge.dir.jcr` or `oak.datasource.url`)
contains data and there is no successful migration report of 9.1.x (`<home>/jcr-migration-report.txt`, or the report
says files were migrated, but the file store is empty: database restored). `projectforge.files.ignoreLegacyJcr=true`
forces the start.

## Open points
- Fixed salt in `CryptStreamUtils` (user encryption of attachments): store a salt per file, keep the fixed salt
  as a fallback for existing files.
- `externalPassword` of the data transfer areas is stored in plain text (separate topic).
- Remove `LegacyJcrGuard` and the mentions of 9.1.x in the docs, once no installation can come from 9.1.x any more.
- `zipMode` of `.zip` files is never backfilled (the former JCR sanity check set it with `ZipUtils.determineZipMode` for
  files without one): add it to `FileStore.checkSanity` (`changeFileInfo(newZipMode = ...)`).
- `ZipUtilsTest` lives in `projectforge-files`, but `ZipUtils` in `projectforge-common` (left over from
  `projectforge-jcr`): move the test with its test resources.
