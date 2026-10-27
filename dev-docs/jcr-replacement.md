# JCR/Oak ablösen

> Release N (Datei-Store neben Oak) ist umgesetzt, offen ist Release N+1 (Oak entfernen). Die Abwägung
> „ablösen oder behalten?" und die Entwurfsdiskussion sind entschieden und stehen in der Git-Historie.

## Context
Oak (1.92.0: oak-jcr, oak-segment-tar, oak-store-document, dazu transitiv oak-core/-commons/-blob/-spi,
jackrabbit-jcr-commons, oak-shaded-guava) ist eine schwere Abhängigkeit, die bei Updates mitgezogen werden
muss. Die Analyse zeigt: ProjectForge nutzt JCR **nur als Blob-Store mit etwas Metadaten**. Es gibt keine
Versionierung, keine JCR-Queries oder -Indizes, kein Locking und keine `nt:file`-Typen. Die Suche läuft
schon heute über Hibernate Search (`attachmentsNames` am Entity). JCR liefert also fast keinen Mehrwert
gegenüber einer einfachen Tabelle.


## Ist-Zustand (kurz)
- `projectforge-jcr`: ca. 3.250 LOC. Der Kern ist `OakStorage.kt` (676), dazu `RepoService` (Fassade),
  `SegmentTarStorage`, `RDBStorage` ("UNDER CONSTRUCTION", Oak auf PostgreSQL), `RepoBackupService`
  (ZIP mit `repository.json` und Binaries), `JCRBackupJob`, `JCRCheckSanityCheckJob` und die CLIs
  Backup/Restore/SanityCheck.
- Datenmodell pro Datei: Pfad `/ProjectForge/<parentPath>/<id>/<listId>/__FILES/<fileId>` mit den Feldern
  content, fileName, fileDescription, size, created(By), lastUpdate(By), checksum (SHA-256, ab 50 MB
  asynchron berechnet), aesEncrypted, zipMode.
- `projectforge-business/.../framework/jcr/AttachmentsService.kt` (693) ist die eigentliche API für alle
  Nutzer. Sie pflegt die denormalisierten Felder `attachments_*` am Entity.
- Nutzer:
  - Contract, Auftrag, Rechnung, Book, Script, MerlinTemplate, DataTransferArea
  - `EInvoiceExportService` greift direkt auf `RepoService` zu
  - `ScriptFileAccessor` und `MerlinHandler` lesen Dateien
- DataTransfer: Dateien sind kurzlebig (7 bzw. 60 Tage), nicht im Backup, haben ein Kapazitätslimit, eigene
  Cleanup-, Notification-, Sanity- und Audit-Jobs und externen Zugriff per Token und Passwort.
- Gateway-Instanz: eigene JCR, nur DataTransfer.


## Risiken und Abwägungen
- **DB-Wachstum:** Die Größe des JCR (ohne DataTransfer) geht 1:1 in die DB, pg_dump und
  Restore-Zeiten. Vorher messen, z. B. mit `DiskUsageStatisticsBuilder` oder der Größe des letzten
  JCR-Backups.
- **Gewinn:** Ein Stack weniger (Oak-Updates, Segment-Store-Korruption, Compaction, eigene Backup-/Restore-
  Tooling-CLIs), und die "UNDER CONSTRUCTION"-RDB-Variante wird überflüssig.
- **Bei der Gelegenheit beheben:** den festen Salt bei der AES-Verschlüsselung (pro Datei einen Salt
  speichern; für Altdaten bleibt der feste Salt als Fallback), `createRandomId % 20` sowie das
  Klartext-`externalPassword` in DataTransfer (eigenes Thema).

## Verifikation (bei Umsetzung)
- `./gradlew :projectforge-jcr:test :projectforge-business:test` und DataTransfer-Plugin-Tests.
- Ein Backup-ZIP einer bestehenden Installation in eine lokale Instanz (pfDev-Slot, HSQLDB und PostgreSQL) importieren. Danach
  Checksummen-Report ohne Abweichungen, Stichproben für Download, Upload, Encrypt und Multi-Download in
  Auftrag, Rechnung (inkl. E-Invoice-Export), Vertrag, Script, Merlin und DataTransfer (inkl. externem
  Zugriff und Cleanup-Job).

## Umsetzungsstand
**Release N (mit Oak):**
- Store `projectforge-jcr/.../jcr/store` (`FileStore`, `DbBlobStore`, `FsBlobStore`, `FileMetaDao` per JDBC).
  Flyway-Skripte `V8.0.37__RELEASE-PfFiles.sql` (postgresql, hsqldb) in projectforge-business. Die Tests
  legen das Schema über dasselbe hsqldb-Skript an (`PfFilesTestSchema`), weil Flyway dort nicht läuft.
- `RepoService` leitet Pfade, die über `registerFileSystemPath` angemeldet sind (DataTransfer), an den
  `FileStore` weiter. Alles andere bleibt im JCR.
- System-Seite: Karte "Dokumenten-Repository (JCR)" mit den Jobs `JcrBackupZipJob`
  (`RepoBackupService.createBackupFile`, jetzt streamend) und `RepoMigrationJob`.
- Sanity-Check und `DiskUsageStatisticsBuilder` berücksichtigen den neuen Store.

- Schalter `projectforge.files.store=jcr|db`.
  - `jcr`: Alles bleibt im JCR wie bisher, auch DataTransfer. Die Migration ist gesperrt. Dateien, die
    während eines `db`-Zeitraums im neuen Store gelandet sind, werden dort weiter gefunden.
  - `db`: Alle neuen Dateien gehen an den `FileStore` (Entitäts-Dateien in die DB, DataTransfer ins
    Dateisystem). Noch nicht migrierte Dateien werden weiter aus dem JCR gelesen.
  - Ist nichts konfiguriert, wird beim Start erkannt: `db`, wenn der neue Store schon Dateien mit Storage
    DB enthält (`db` war also schon aktiv); `jcr`, wenn das JCR schon Daten enthält (bestehende
    Installation); sonst `db` (neue Installation). Im Modus `db` wird nichts ins JCR geschrieben, die
    Erkennung bleibt also stabil.
  - Wird der Code auf einer bestehenden Installation live genommen, ändert sich also nichts. Ein Rollback
    auf das vorherige Release ist ohne Mischbestand möglich (das Schema `pf_files` und die
    Flyway-Migration stören das alte Release nicht).
- Migration (nur im Modus `db`): Beim Start läuft sie automatisch (`FileStoreMigrationService`,
  `RepoMigrationJob`), und zwar bei jedem Start. Bereits migrierte Dateien werden per DB-Abfrage
  übersprungen, ein Lauf ohne Arbeit dauert nur Sekunden. Der Report `<home>/jcr-migration-report.txt`
  ist nur Information, keine Markierung: Nach einem Restore der DB (leeres `pf_files`) passt er nicht
  mehr zur DB. Auf der System-Seite lässt sich die Migration auch manuell starten.
  - Beim Shutdown hält sie nach der gerade laufenden Datei an (Report `Result: ABORTED`) und läuft beim
    nächsten Start weiter. Bereits kopierte Dateien werden übersprungen.
  - Jede Datei wird per SHA-256 und Größe gegen das JCR geprüft.
  - Alle Dateien werden **kopiert**, auch DataTransfer. Das JCR bleibt vollständig, ein Zurückschalten
    oder Rollback ist also möglich. Nach einem Rollback fehlen nur die Dateien, die unter `db` neu
    hochgeladen wurden.
  - Platzbedarf: Die DataTransfer-Dateien liegen bis zu ihrem Ablauf doppelt vor (JCR und Dateisystem).
    Gelöscht wird in beiden Stores, der Platz im JCR wird mit dem Compaction-Lauf des
    DataTransfer-Cleanup-Jobs frei.
- Löschen entfernt die Datei im Store und die Kopie im JCR.
- Das nächtliche JCR-Backup (`JCRBackupJob`) läuft in beiden Modi weiter, solange es das JCR gibt.

**Release N+1:** offen (Oak entfernen; Import aus dem Backup-ZIP als Fallback für Installationen, die in N
nicht auf `db` umgestellt und migriert haben).
