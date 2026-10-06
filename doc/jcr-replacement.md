# JCR/Oak ablösen: Analyse und Vorschlag

## Context
Oak (1.92.0: oak-jcr, oak-segment-tar, oak-store-document, dazu transitiv oak-core/-commons/-blob/-spi,
jackrabbit-jcr-commons, oak-shaded-guava) ist eine schwere Abhängigkeit, die bei Updates mitgezogen werden
muss. Die Analyse zeigt: ProjectForge nutzt JCR **nur als Blob-Store mit etwas Metadaten**. Es gibt keine
Versionierung, keine JCR-Queries oder -Indizes, kein Locking und keine `nt:file`-Typen. Die Suche läuft
schon heute über Hibernate Search (`attachmentsNames` am Entity). JCR liefert also fast keinen Mehrwert
gegenüber einer einfachen Tabelle.

## Abwägung: ablösen oder JCR behalten?

**Für JCR behalten**
- Es läuft und ist abgedeckt: Sanity-Check, Backup/Restore-CLIs, Tests. Ablösen kostet etwa 1,5–2 Wochen
  und birgt ein Migrationsrisiko bei Produktivdaten.
- Der Update-Schmerz ist bisher überschaubar. Oak bringt seine eigene shaded Guava mit, und in
  `buildSrc` ist **keine** Version wegen Oak gepinnt (die Jackson-Pins kommen von ez-vcard, groovy-yaml
  und flyway). Das Abhängigkeitsproblem ist also eher latent als akut.
- Es gibt eine Alternative ohne Komplettumbau: auf `RDBStorage` umstellen, also Oak auf PostgreSQL.
  - Diese Variante ist aber als "UNDER CONSTRUCTION" markiert, hat keine Migration und behält Oak samt
    allen Abhängigkeiten. Die Blobs lägen dann in einem Oak-eigenen, undurchsichtigen Format in PostgreSQL.
  - Damit wäre das eigentliche Problem nicht gelöst.

**Für die Ablösung**
- **Betriebsproblem DataTransfer im Segment-Store:** `SegmentTarStorage` nutzt keinen externen BlobStore.
  Alle Binaries, auch die mehreren 100 GB DataTransfer-Dateien, liegen inline in den Tar-Segmenten.
  - Gelöschte und abgelaufene Dateien geben ihren Platz erst nach `compactFull()` und `cleanup()` frei.
    Das ist langsam und braucht zeitweise zusätzlichen Platz.
  - Im Dateisystem ist ein Löschen sofort ein Löschen.
  - Das ist der stärkste Einzelgrund.
- **Genutzter Funktionsumfang ist minimal:** Es sind nur Blob und Metadaten. Es gibt keine Versionierung,
  keine Queries, kein Locking und keine Suche (die läuft über Hibernate Search). Der Eigenbau ist deshalb
  klein (etwa 1 KLOC) und ersetzt rund 3,2 KLOC Oak-Integration plus 18 Oak/Jackrabbit-Artefakte.
- **Konsistenz und Betrieb:**
  - Metadaten und Blobs liegen transaktional in PostgreSQL. Verwaiste Nodes entfallen
    (`ToDo.adoc`: "Tool for removing or recovering orphaned nodes").
  - Backup und Restore laufen mit Standardwerkzeugen (pg_dump, getrennter Dump wie gewünscht) statt mit
    eigenem ZIP-Format und eigenen CLIs.
  - Risiko Segment-Korruption (`SegmentNotFoundException`) und Compaction-Jobs entfallen.
- **Transparenz:** Daten sind per SQL und im Dateisystem direkt einsehbar und reparierbar. Oak ist
  dagegen eine Blackbox, deren Know-how im Team schwindet.
- **Zukunftsrisiko:** Oak wird primär für Adobe AEM entwickelt. Release-Takt, Java-Baselines und
  Modulzuschnitt richten sich danach, nicht nach uns. Jeder Spring-Boot- oder JDK-Sprung muss Oak
  mitnehmen.

**Empfehlung: ablösen**, aber in zwei Releases und mit klarer Reihenfolge:
1. Release N (mit Oak):
   - Buttons "JCR-Backup-ZIP erzeugen" und "DataTransfer-Dateien ins Dateisystem verschieben".
   - Der neue Store wird schon eingebaut, zunächst nur für DataTransfer. So entschärft sich das
     Compaction-Problem sofort, mit dem kleinsten Risiko, denn die Daten sind kurzlebig.
2. Release N+1:
   - Entitäts-Dateien kommen in die DB (eine Zeile pro Datei, komprimiert, Schema `pf_files`), mit Import
     aus dem Backup-ZIP und Verifikation.
   - Oak wird entfernt.

Wenn der Aufwand gerade nicht passt, ist **Schritt 1 allein** (DataTransfer raus aus Oak) schon der
größte Gewinn. Die Entitäts-Dokumente (< 10 GB) könnten zur Not noch eine Weile in Oak bleiben, aber dann
bleibt auch die Abhängigkeit. Komplett beim Status quo zu bleiben empfehle ich nicht.

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

## Bewertung deines Vorschlags
**Entitäts-Dokumente in PostgreSQL: ja.** Das bringt
- Transaktionale Konsistenz mit dem Entity. Verwaiste Nodes, wie sie heute in der Sanity-Check- und
  Orphan-ToDo-Liste stehen, gibt es dann nicht mehr.
- Ein Backup statt zwei (pg_dump statt JCR-ZIP).
- Keine Segment-Korruption mehr (`SegmentNotFoundException`) und kein Compaction.

**Eine Zeile pro Datei, Inhalt komprimiert (entschieden).** Das passt zu den heutigen Einzeldatei-Features
(zipMode, Verschlüsselung, Download einzeln, Beschreibung). Der Multi-Download als ZIP existiert schon
(`AttachmentsServicesRest.multiDownload`).
- Komprimiert wird beim Schreiben als Stream mit `java.util.zip.Deflater`/GZIP (JDK, keine neue
  Abhängigkeit), dann in Chunks zerlegt. Spalte `compression` (NONE/GZIP), `size` (Original) und
  `stored_size`.
- Bereits komprimierte Inhalte bringen fast nichts: PDF, JPEG/PNG, ZIP, docx/xlsx (das ist ZIP), zip4j- oder
  AES-verschlüsselte Dateien. Heuristik: Bei diesen Formaten (Content-Type/Endung oder `zipMode`/
  `aesEncrypted`) wird NONE gespeichert. Sonst wird komprimiert. Ist das Ergebnis nicht mindestens ~10 %
  kleiner, wird beim Abschluss des Uploads auf NONE zurückgeschrieben (oder man nimmt den Verlust bei
  kleinen Dateien einfach hin).
- Die Checksumme (SHA-256) wird wie im JCR über den gespeicherten Inhalt gebildet, also bei verschlüsselten
  Dateien über den verschlüsselten Inhalt, aber vor der Kompression. So bleibt sie mit den Altdaten aus dem
  JCR vergleichbar, und die Migration kann jede Datei gegen die JCR-Checksumme prüfen.
- Reihenfolge beim Schreiben: Original → (AES, falls ein Passwort angegeben ist) → SHA-256 und Größe →
  komprimieren → Chunks. Verschlüsselte Dateien werden nicht komprimiert, denn verschlüsselte Daten lassen
  sich nicht mehr komprimieren.
- TOAST-Spalten bekommen `STORAGE EXTERNAL`, weil PostgreSQL nicht noch einmal komprimieren soll.

**Wie die Blobs speichern?** Bei bis zu 100 MB pro Datei kommt es darauf an, ob man streamen kann:
- `bytea` in einer Spalte: pgjdbc liest den ganzen Wert in den Speicher. Bei 100 MB × parallele Downloads
  ist das schlecht.
- Large Objects (`oid`): streamen gut, aber man braucht Orphan-Handling (`lo_unlink`/vacuumlo), und HSQLDB
  (Dev/Tests) hat keine Entsprechung.
- **Empfehlung: chunked bytea**, also `t_attachment_chunk(attachment_fk, seq, data)` mit Chunks von etwa
  1 MB. Das streamt in beide Richtungen, läuft auf PostgreSQL und HSQLDB (BLOB/VARBINARY), braucht kein
  Orphan-Handling (FK mit ON DELETE CASCADE) und funktioniert mit pg_dump. Die Spalten bekommen
  `STORAGE EXTERNAL`, damit TOAST nicht versucht, bereits komprimierte Dateien zu komprimieren.

**DataTransfer im Dateisystem: ja.** Die Dateien sind kurzlebig, groß und nicht im Backup. Sie in die DB zu
legen würde nur WAL und Backups aufblähen. Die Metadaten bleiben in der DB (dieselbe Tabelle
`t_attachment`, Flag `storage = FS`). Die Binaries liegen unter `<home>/datatransfer/<areaId>/attachments/<fileId>`.
Schreiben geht über eine Temp-Datei mit atomarem Rename. Der bestehende Cleanup-Job löscht Datei und Zeile.
Für die Gateway-Instanz passt das ebenfalls.

## Was wir selbst entwickeln müssten
1. **Schema (Flyway, postgresql + hsqldb):**
   - `t_attachment`: pk, file_id (bleibt als öffentliche ID, damit REST-URLs stabil bleiben), parent_path
     (z. B. `org.projectforge.fibu.RechnungDO/42`), rel_path (z. B. `attachments`), file_name, description,
     file_size, stored_size, chunk_count, compression_type, storage_type, checksum, aes_encrypted, zip_mode,
     created(_by), last_update(_by).
     - Wie im JCR zählt nur die Verkettung von parent_path und rel_path. Normalisiert wird so, dass das
       letzte Segment rel_path ist und die Segmente davor parent_path sind.
   - `t_attachment_chunk` wie oben.
   - Indizes auf (parent_path, rel_path) und unique auf file_id.
2. **Neue Storage-Schicht** statt `OakStorage` (geschätzt 600–900 LOC):
   - Interface `FileStore` (put als Stream, get als Stream, delete, list, changeFileInfo) mit den
     Implementierungen `DbFileStore` (Chunks) und `FsFileStore`.
   - Übernommen werden: SHA-256 beim Schreiben (DigestInputStream statt Nachberechnung), `FileSizeChecker`,
     AES über `CryptStreamUtils`, zipMode-Erkennung über `ZipUtils`.
   - `RepoService` behält möglichst seine API (FileObject/FileInfo), damit `AttachmentsService`,
     `EInvoiceExportService`, `ScriptFileAccessor`, `MerlinHandler` und DataTransfer kaum angefasst werden.
3. **Jobs:**
   - Der Sanity-Check bleibt (Checksummen prüfen) und wird einfacher.
   - `JCRBackupJob` und `RepoBackupService` entfallen, weil pg_dump das abdeckt. Optional bleibt ein
     "Export als ZIP" für einzelne Bereiche.
   - `DiskUsageStatisticsBuilder` wird angepasst (DB-Größe der Tabelle plus FS-Verzeichnis).
4. **Migration der Bestandsdaten über ein Backup-ZIP (entschieden):**
   - Schritt 1, noch im aktuellen Release mit Oak: Auf der System-Seite kommt ein Button
     "JCR-Backup-ZIP erzeugen" dazu. Dafür gibt es einen neuen Endpoint in
     `projectforge-rest/.../SystemRest.kt`, der `RepoBackupService.backupRepository` asynchron startet und
     die Zieldatei im Backup-Verzeichnis meldet. DataTransfer bleibt wie heute ausgeschlossen.
     - Nebenbei beheben: `RepoBackupService` liest Binaries komplett in den Speicher (`readBytes`) und
       soll stattdessen streamen.
   - Schritt 2, im Release ohne Oak: Ein Import (Button auf der System-Seite und/oder Auto-Import beim
     Start, falls `<home>/jcr-migration/*.zip` vorliegt) liest `repository.json` und die Binaries ohne Oak.
     Zielorte: Entitäts-Dateien in die DB, DataTransfer ins Dateisystem.
     - Idempotent über `file_id`, damit ein abgebrochener Import einfach erneut laufen kann.
   - **DataTransfer (mehrere 100 GB, weder in die DB noch ins Backup):** Ein ZIP kommt nicht infrage.
     Schon im Oak-Release nutzt DataTransfer den neuen Store (Metadaten in `t_attachment` mit storage = FS,
     Binaries im Dateisystem). Neue Dateien landen sofort dort.
     - Bestehende Dateien werden über `RepoService` weiter aus dem JCR gelesen, geändert und gelöscht,
       bis sie verschoben sind.
     - Der zweite Button "DataTransfer-Dateien ins Dateisystem verschieben" (`RepoMigrationService`) streamt
       jede Datei 1:1 (noch verschlüsselt) in den neuen Store und prüft die Checksumme gegen die des JCR.
       Erst dann wird die Datei aus dem JCR gelöscht. Am Ende gibt ein JCR-Cleanup den Platz frei.
     - Bereits verschobene Dateien werden übersprungen (über `file_id`). Ein erneuter Lauf ist also möglich.
     - Eine `files.json` ist nicht nötig, weil die Metadaten direkt in `t_attachment` landen.
     - Freier Plattenplatz: Bis zum Cleanup am Ende des Laufs liegen die verschobenen Dateien doppelt auf
       der Platte (JCR-Segmente plus FS).
     - Fallback: nicht verschieben. Die Nutzer werden informiert und DataTransfer startet mit dem Release
       ohne Oak leer. Die Dateien sind ohnehin nach höchstens 60 Tagen abgelaufen.
   - Danach Verifikation: Anzahl, Größe und SHA-256 pro Datei, außerdem ein Abgleich mit den
     `attachments_*`-Feldern der Entities. Das Ergebnis kommt als Report-Download.
5. **Getrennter Dump für die Dateien (Wunsch):**
   - `t_attachment` und `t_attachment_chunk` bekommen ein eigenes Schema `pf_files`, angelegt per Flyway,
     JPA mit `@Table(schema = "pf_files")`. Es gibt keine FKs auf Entity-Tabellen (wie heute bei JCR).
     So lassen sich beide Dumps unabhängig voneinander einspielen. Abweichungen findet der Sanity-Check.
   - Hauptdump: `pg_dump -N pf_files ...`
   - Datei-Dump: `pg_dump -n pf_files -Fc ...` (custom format, gut für große Blobs, `pg_restore -j`
     parallel).
   - Die Backup-Skripte in `site/_docs/adminguide.adoc` (ab ca. Zeile 1320) und die Docker-Doku werden
     angepasst. Die Restore-Doku beschreibt beide Dumps und den anschließenden Sanity-Check.
   - DataTransfer (`<home>/datatransfer`) wird explizit vom Datei-Backup ausgenommen und im adminguide
     dokumentiert.
6. **Tests:** `RepoTest` und `RepoBackupTest` werden auf den neuen Store umgeschrieben. Die DataTransfer-
   Tests (Cleanup, Access, PublicAccess, Notification) und die EInvoice-Tests laufen weiter. Neu kommt ein
   Migrationstest mit einem kleinen Backup-ZIP als Fixture.
7. **Aufräumen:**
   - `projectforge-jcr` wird zu einem schlanken Modul (oder geht in business auf).
   - Oak, jcr 2.0 und `oak.datasource.*` werden aus `libs.versions.toml`, `projectforge-application/build.gradle.kts`
     und `application.properties` entfernt.
   - Docs: adminguide, installation, technologies, development.

Gesamtaufwand grob: Storage und Schema 2–3 Tage, Migration und Verifikation 2–3 Tage, Tests, Jobs und
Docs 2 Tage. Insgesamt also etwa 1,5–2 Wochen inklusive e2e-Prüfung gegen eine Kopie des Prod-Repos.

## Risiken und Abwägungen
- **DB-Wachstum:** Die Größe des Prod-JCR (ohne DataTransfer) geht 1:1 in die DB, pg_dump und
  Restore-Zeiten. Vorher messen, z. B. mit `DiskUsageStatisticsBuilder` oder der Größe des letzten
  JCR-Backups.
- **Gewinn:** Ein Stack weniger (Oak-Updates, Segment-Store-Korruption, Compaction, eigene Backup-/Restore-
  Tooling-CLIs), und die "UNDER CONSTRUCTION"-RDB-Variante wird überflüssig.
- **Bei der Gelegenheit beheben:** den festen Salt bei der AES-Verschlüsselung (pro Datei einen Salt
  speichern; für Altdaten bleibt der feste Salt als Fallback), `createRandomId % 20` sowie das
  Klartext-`externalPassword` in DataTransfer (eigenes Thema).

## Verifikation (bei Umsetzung)
- `./gradlew :projectforge-jcr:test :projectforge-business:test` und DataTransfer-Plugin-Tests.
- Ein Prod-Backup-ZIP in eine lokale Instanz (pfDev-Slot, HSQLDB und PostgreSQL) importieren. Danach
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
