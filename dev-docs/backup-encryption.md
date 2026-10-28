# Backup encryption with age

## Goal
- Backups (SQL dumps, files dumps) exist only in encrypted form on the server, at the rsync target and in
  the archive.
- The server holds only the public key. An attacker who takes over the server can't read old backups with it (the
  current data would be exposed anyway).
- Monthly backups are kept forever (year-over-year comparisons), so they must remain decryptable for years.

## Why age
- Public-key encryption (X25519): the server encrypts without knowing the secret.
- Authenticated encryption (ChaCha20-Poly1305), streamable, no keyring, agent or pinentry as with gpg.
- Fast: negligible even for a few GB per day (seconds). File size stays the same (age doesn't compress: the main dump is gzipped, the files dump uses `-Z 1`, because
  pg_dump writes bytea hex encoded, i.e. twice the size).
- Multiple recipients per file, e.g. a main key and an emergency key.
- ZIP with AES-256 was rejected: file names inside the ZIP stay readable, the password would have to be stored on the
  server, and the key derivation is weak (1000 rounds PBKDF2-SHA1).

## Keys
- Two key pairs created with `age-keygen`:
  - **Main key:** private part only with the admin (password manager), never on the server.
  - **Emergency key:** private part printed / stored offline in a safe.
- Every file is encrypted for both recipients, so losing one key doesn't lose the backups.
- The server holds only the public keys in `/home/projectforge/backup-recipients.txt`, one per line (`age1…`).
- Rotation: add a new line, remove the old one. Old backups stay readable with the old private key, which therefore
  must be kept.

## Where to encrypt

### In the backup script, no change to ProjectForge
`pforgebackup.sh` (adminguide, "Backup script") encrypts directly, without a plaintext intermediate file:

    pg_dump -U projectforge -N pf_files | gzip | age -R $RECIPIENTS -o $BACKUPFILE.gz.age
    pg_dump -U projectforge -n pf_files -Fc -Z 1 | age -R $RECIPIENTS -o $FILES_BACKUPFILE.age

- Use `set -o pipefail` so a failing pg_dump aborts the script. Write to a `.tmp` file first and rename afterwards.
- The public key is **not** stored in `projectforge.properties`.

## Purging (`DatabaseBackupPurgeJob`)
- `BackupFilesPurging` groups by the text in front of the date in the file name (series) and ignores everything
  after the date, so `.gz.age` and `.dump.age` stay in the same series as before. No change needed.
- `projectforge.cron.purgeBackupDir` must also cover the directory of the encrypted files.

## Converting an existing archive
- Script: per file `age -R recipients.txt -o f.age f`, verify with `age -d -i key.txt f.age | cmp - f`, only then
  delete `f`. File by file, if disk space is short.
- Duration: I/O-bound, minutes for a large archive.
- Include loose copies of dumps outside the archive, or delete them.
- Notes or scripts next to the backups must not contain passwords in plaintext; describe the restore procedure
  instead.

## Restore
    age -d -i <private-key-file> projectforge_2026-10-11_02-00.sql.gz.age | gunzip | psql …
    age -d -i <private-key-file> projectforge_files_….dump.age > files.dump && pg_restore -j 4 … files.dump

- Update the restore documentation in the adminguide ("Restoring a backup") and the Docker documentation.
- Do a test restore with the emergency key once per quarter, so a lost key is noticed while plaintext backups still
  exist.

## Checklist
- Decide who keeps the emergency key.
- Protect the rsync target additionally with disk encryption (it only protects the powered-off device).
