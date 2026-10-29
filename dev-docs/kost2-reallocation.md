# Kost2-Reallokation von Buchungssätzen (rückwirkender Projektumzug)

Status: Phasen 1–3 sind umgesetzt (Migration `V9.2.0.3__RELEASE-Kost2Reallocation.sql`), Phase 4 und 5 sind offen.
Ziel-Release: das nächste (`version` in `gradle.properties`).

Dieses Dokument ist auf Deutsch geschrieben, weil es ein Buchhaltungsthema ist. Bezeichner im Code bleiben englisch.

## Ausgangslage
Zeitberichte werden rückwirkend von einem Sammeltask in ein eigenes Projekt mit eigener Kost2 umgezogen.
Ein Sammeltask ist z. B. „sonstige Projekte“ eines Geschäftsbereichs, gebucht auf die Sammel-Kost2 A.

Die aus DATEV importierten Buchungssätze tragen noch die Kost2 von damals:
- **Gehälter:** `EmployeeSalaryExportDao` verteilt das Bruttogehalt je Mitarbeiter und Monat auf die Kost2,
  gewichtet nach den Stunden der Zeitberichte (Konto 6000, Gegenkonto 3791, Kost1 = Mitarbeiter).
  - Die Aufteilung wird nicht gespeichert.
  - Jeder Export berechnet sie neu aus `TimesheetDO.kost2`.
  - Ergebnis: ein Buchungssatz je (Kost1, Kost2, Monat).
- **Erlöse:** auf Erlöskonten gebucht.
  - Die Kost2 kommt aus den Kostenzuweisungen der Debitorenrechnungen.
  - Die Rechnung lässt sich über `beleg` finden (`BookedInvoices.tokens`).

Die ursprüngliche Kost2 eines Buchungssatzes bleibt erhalten. ProjectForge rechnet stattdessen mit einer
reallokierten Zuordnung.

**Typischer Ablauf in der Praxis:**
- Der Task-Teilbaum des neuen Projekts wird im Strukturbaum unter das Zielprojekt gehängt.
- Zeitberichte ab einem Stichtag tragen die Kost2 des Zielprojekts.
- Zeitberichte davor bleiben bewusst auf der Sammel-Kost2, weil deren Gehaltsbuchungen schon erzeugt sind.
- Diese Zeitberichte liegen also im Teilbaum des Zielprojekts, tragen aber eine fremde Kost2. Genau das ist das Merkmal,
  über das die Reallokation die Gehaltsanteile findet.

**Entscheidungen:**
- **Anteilstabelle je Buchungssatz** statt eines einzelnen zweiten Felds, weil ein Satz oft aufgeteilt werden muss.
- **Gehaltsanteile aus den noch nicht umgestellten Zeitberichten:**
  - Die Zeitberichte werden vor der Berechnung nicht umgezogen. Ihr Task zeigt das Ziel, ihre Kost2 die damalige
    Aufteilung.
  - Umgestellt werden sie erst beim Anwenden des Laufs. Das Protokoll dient nur dem Revert.
- **Weitere Quellen** neben Gehältern und Erlösen:
  - Eingangsrechnungen, gefunden über `beleg`
  - pauschale Regeln „Kost2 A → B“

## Datenmodell
Flyway: `V<release>.<n>__RELEASE-Kost2Reallocation.sql` (Release = `version` aus `gradle.properties` beim Merge).
- Vor dem Merge die offenen Branches auf bereits vergebene Versionen im selben Release prüfen.

1. **`Kost2ReallocationDO`** (`t_fibu_kost2_reallocation`), der Reallokationslauf:
   - Felder: Beschreibung, Zielprojekt, Zeitraum (Monat von/bis), Status `DRAFT|APPLIED|REVERTED`, Kommentar.
   - `@WithHistory`.
   - Zugriff: nur FINANCE. Das DAO überschreibt `hasAccess` und `hasInsertAccess` (siehe Sicherheitsregeln).
2. **`Kost2ReallocationRuleDO`**: Lauf, Quell-Kost2, Ziel-Kost2, optionaler Kontenbereich.
   - Das sind die Regeln A → B.
   - Standardregeln werden aus dem Zielprojekt über die Kost2-Art erzeugt, wie in `TimesheetMassUpdateRest`
     (Z. 330-347).
3. **`Kost2ReallocationTimesheetDO`**, das Umstellungsprotokoll:
   - Felder: Lauf, Zeitbericht, Benutzer, Jahr, Monat, alter Task, neuer Task, alte Kost2, neue Kost2, Millis.
   - Wird beim Anwenden geschrieben, wenn die Zeitberichte umgestellt werden. Es ist keine Eingabe der Berechnung,
     sondern dient dem Revert und der Nachvollziehbarkeit.
4. **`BuchungssatzKost2ShareDO`** (`t_fibu_buchungssatz_kost2_share`):
   - Felder: `buchungssatz_fk`, `kost2_fk`, `betrag` (gleiches Vorzeichen wie `betrag` des Satzes),
     Quelle `SALARY|INVOICE|INCOMING_INVOICE|RULE`, `run_fk`.
   - Invariante: Die Anteile ergeben zusammen den `betrag` des Satzes.
   - Der letzte Anteil nimmt die Rundungsdifferenz auf, wie die Korrekturzeile des Gehaltsexports.
   - Ein Satz ohne Anteile zählt auf seine eigene `kost2`.
   - **Keine kaskadierende Collection in `BuchungssatzDO`.** Der DATEV-Import baut das DO neu auf und ruft
     `insertOrUpdate` (`DatevRecordImportJob.run`), das würde die Anteile löschen.
   - Die Anteile haben ein eigenes DAO. Sie werden gesammelt über `buchungssatz.id IN (...)` geladen, in Blöcken
     unterhalb des Limits für Bind-Parameter.

### Anteile pro Lauf versioniert (umgesetzt)
Ein späterer Lauf kann einen Satz erneut aufteilen, und ein Revert muss die früheren Anteile zurückholen. Deshalb gilt:
- Die **aktiven** Anteile eines Satzes sind die mit `deleted = false` und `superseded_by_run_fk IS NULL`.
- Teilt Lauf N einen Satz neu auf, markiert er die aktiven Anteile als `superseded_by_run_fk = N` und legt seine eigenen
  an (`run_fk = N`).
- Revert von N löscht die Anteile mit `run_fk = N` und setzt `superseded_by_run_fk = N` zurück. Er ist verweigert, wenn
  ein späterer Lauf Anteile von N überholt oder dieselben Zeitberichte umgezogen hat, oder wenn ein umgezogener
  Zeitbericht seitdem geändert wurde (anderer Task oder andere Kost2 als laut Protokoll). Gelöschte Zeitberichte bleiben,
  wie sie sind.
- Der DATEV-Reimport markiert die Anteile eines Satzes als gelöscht, wenn sich Kost2 oder Betrag ändern. Das entscheidet
  der Import-Job in seiner Transaktion aus der Datenbank; die Markierung in der Vorschau ist nur Anzeige.
  **Erneut anwenden** (`POST reapply`) rechnet sie aus dem Zeitberichtsprotokoll und den Regeln neu: Basis auf A sind die
  aktuellen Millis auf A plus die Millis, die dieser und spätere Läufe von A weggezogen haben. Die Zeitberichte bleiben
  unverändert. Frühere Läufe mit verworfenen Anteilen derselben Sätze sind zuerst erneut anzuwenden.
- Anwenden ist nur aus dem Entwurf möglich und läuft in einer Transaktion. Anwenden, erneut Anwenden und Revert sperren
  zuerst die Zeile des Laufs (bedingtes Update auf den erwarteten Status) und rechnen erst danach; ein gleichzeitiger
  zweiter Aufruf wartet und findet dann den neuen Status vor. Ein zweiter Aufruf mit denselben Parametern ändert nichts (die Zeitberichte zeigen das Merkmal nicht mehr, die Quellen der Regeln passen nicht mehr).

### Regeln und Gehaltssätze (umgesetzt)
Gehaltssätze (Konto 6000 / Gegenkonto 3791) werden nur über die Zeitberichte aufgeteilt. Eine Regel A → B erfasst sie nur,
wenn ihr Kontenbereich das Gehaltskonto ausdrücklich enthält; sonst würde sie die Zeitberichts-Aufteilung überstimmen.
Regeln verketten sich nicht: Sie lesen die Beträge vor den Regeln (A → B und B → C bringt A nur nach B), und je Quelle
gilt die erste passende Regel.

### Millis der Zeitberichte (umgesetzt)
Der Gehaltsexport teilt nach `kost2Durations[k].millis`. Der Arbeitsanteil (`workFraction`) ist je Kost2 gleich, kürzt
sich also heraus. Die Millis je Zeitbericht (nach der Überlappungsaufteilung) liefert
`MonthlyEmployeeReport.kost2TimesheetAttendances`. Die Aufteilung selbst steht in `EmployeeSalarySplit` und wird vom
Export und von der Plausibilitätsprüfung gemeinsam benutzt.

## Zentrale Wiederverwendung: die effektive Sicht
`BuchungssatzKost2ShareService.expand(records): List<BuchungssatzDO>`:
- Liefert je Anteil eine transiente Kopie mit `kost2` = Kost2 des Anteils, `betrag` = Anteil und einem Verweis auf
  den ursprünglichen Satz.
- Sätze ohne Anteile kommen unverändert zurück.
- `calculate()`/`isIgnore` bleiben am ursprünglichen Satz (die 1.0.0.0-Regel).

Verbraucher, die auf diese Sicht umgestellt werden:

| Verbraucher | Änderung |
|---|---|
| `ContributionMarginService.selectRecords` (Z. 688-711) | Lädt neben `isIn("kost2.id")` auch die Sätze, deren *Anteile* auf die Kost2 zeigen, und expandiert sie. `ContributionMarginCalculator.recordEntries` bleibt, wie es ist. |
| `ContributionMarginCache` | Bekommt einen Listener auf das Anteils-DAO. |
| `ReportDao.loadReport` / `Report.java` (Z. 388-408) | Der Kost2-Regex-Filter läuft auf den expandierten Sätzen. |
| `AccountingRecordEntityRest.reportRecords` (Drill-down) | Zeigt den ursprünglichen Satz mit seinen Anteilen. |
| Scripting: `AccountingRecord.kt`, `BuchungssatzImpl` | Bieten zusätzlich `effectiveKost2` und die Anteile an. |
| BWA | Keine Änderung. Sie arbeitet nur auf Konten. |

Die Zeitberichtskosten im Deckungsbeitrag (`ContributionMarginService.timesheetEntries`, Z. 663-671) laufen über
`timesheet.kost2`. Sie stimmen, sobald die Zeitberichte beim Anwenden umgestellt sind; eine eigene Anpassung ist nicht
nötig.

Anzeige in next (`components/features/accounting-record/`):
- Liste: Spalte „reallokiert“.
- Bearbeitungsformular: die Anteile schreibgeschützt, mit Link zum Lauf.
- Excel-Export (`AccountingRecordEntityRest` Z. 249-294): Spalten für die Anteile.

## DATEV-Reimport
Abgleich in `DatevRecordImportStorage` und im Job:
- Sind `kost2` und `betrag` eines zugeordneten Satzes unverändert, bleiben die Anteile. Sie hängen an der id des
  Satzes.
- Hat sich eins davon geändert, werden die Anteile verworfen.
  - Die Vorschau zeigt „Reallokation verworfen“. Der DTO-Diff bekommt dafür ein Flag.
  - Läufe sind idempotent und können erneut angewendet werden.

## Reallokationslauf (handgebauter next-Wizard, nur FINANCE)

1. **Lauf festlegen:**
   - Quelle: Sammel-Kost2 und/oder Tasks.
   - Ziel: Projekt, Zeitraum.
   - Kost2-Zuordnung je Kost2-Art: Standardregeln, änderbar.

2. **Zu reallokierende Zeitberichte:**
   - **Automatisch erkannt:** Task im Task-Teilbaum des Zielprojekts (`TaskTree`), Kost2 gehört nicht zum Zielprojekt,
     im Zeitraum.
   - **Umzug im Task-Baum ist nicht Teil des Laufs:** Zeitberichte, die direkt auf dem Sammeltask liegen, werden vorher
     und getrennt in den Teilbaum des Zielprojekts verschoben (z. B. mit der Massenänderung der Zeitberichte). Der Lauf
     erkennt sie danach automatisch. Der Task eines Zeitberichts ändert sich durch den Lauf nie.
   - Ziel-Kost2 je Zeitbericht: die Kost2 des Zielprojekts mit derselben Kost2-Art (Standardregeln aus Schritt 1).
   - Die Zuordnungslogik von `TimesheetMassUpdateRest` (taskAndKost2 / KOST2_REMAP) wandert in einen Service, den
     beide nutzen.

3. **Gehälter:**
   - Gehaltssätze sind die auf Konto 6000 / Gegenkonto 3791.
     - Diese Konstanten wandern aus `EmployeeSalaryExportDao` an eine gemeinsame Stelle.
   - Für jede Kombination (Kost1 des Mitarbeiters, Jahr, Monat, alte Kost2 A) der Zeitberichte aus Schritt 2 wird der
     Gehaltssatz mit (effektiver) Kost2 A genommen.
   - Anteil B = effektiver Anteil auf A × Millis der ausgewählten Zeitberichte auf A / aktuelle Millis auf A.
   - Die Millis werden je Mitarbeiter und Monat berechnet wie in `MonthlyEmployeeReport` (`MonthlyEmployeeReportWeek`
     Z. 107, `MonthlyEmployeeReportEntry.getMillis`). Der Arbeitsanteil (`workFraction`) kürzt sich heraus, weil
     beide Werte auf derselben Kost2 liegen.
   - Der Rest bleibt auf A.
   - Fehlt die Kost1 des Mitarbeiters oder ein Gehaltssatz auf A, ist das ein Fehler des Plans: Anwenden und erneutes
     Anwenden lehnen ab. Sonst würden die Zeitberichte ohne ihr Gehalt umziehen, und ein späterer Lauf würde den Rest
     auf A durch zu wenige Stunden teilen.
   - Ein späterer Lauf auf denselben Monat bleibt ohne Rückrechnung richtig:
     - Der erste Lauf hat seine Zeitberichte schon umgestellt und den Anteil auf A verringert.
     - Restanteil auf A / aktuelle Millis auf A ist also weiterhin der ursprüngliche Betrag je Stunde.
   - **Plausibilitätsprüfung in der Vorschau:**
     - Die Exportaufteilung (Logik aus `EmployeeSalaryExportDao`) wird aus den aktuellen Zeitberichten nachgerechnet.
     - Sie wird mit den gespeicherten Gehaltssätzen dieses Mitarbeiters und Monats verglichen.
     - Eine Abweichung bedeutet, dass Zeitberichte nach dem Export geändert wurden. Sie erscheint als Warnung je
       Mitarbeiter und Monat.

4. **Erlöse (Debitorenrechnungen):**
   - Betrifft Sätze auf Erlöskonten (`ContributionMarginConfig.isRevenueAccount`) mit einer Kost2 aus der Quelle.
   - `beleg` wird über `BookedInvoices.tokens` einer `RechnungDO` zugeordnet.
   - Ziel-Kost2 je Position:
     - ihre Kostenzuweisung, wenn diese schon auf das Zielprojekt zeigt;
     - sonst die Kost2 des Zielprojekts mit derselben Art, wenn `rechnung.projekt`,
       `auftragsPosition.auftrag.projekt` oder `auftragsPosition.task` (im umgezogenen Teilbaum) auf das Ziel zeigen.
   - Der Satz wird nach den Nettobeträgen der Positionen aufgeteilt.
   - Optional: auch die Kostenzuweisungen der Rechnungen anpassen.

5. **Eingangsrechnungen:** genauso, über `beleg` ↔ `EingangsrechnungDO` und deren Kostenzuweisungen.
   - **Offene Frage:** Was steht bei Kreditorensätzen in `beleg` (`referenz`? eine Belegnummer?)? Erst an echten
     Daten prüfen.

6. **Regeln A → B:** Alle übrigen Sätze der Quell-Kost2 im Zeitraum (optional ein Kontenbereich) gehen vollständig auf
   die Ziel-Kost2.

7. **Vorschau, Anwenden, Revert:**
   - Vorschau: ein Diff je Satz (Original, alte Anteile, neue Anteile), Summen je Kost2, die Fehler und die Warnungen aus Schritt 3.
   - Anwenden, in einer Transaktion:
     1. die Anteile speichern;
     2. danach die Zeitberichte aus Schritt 2 umstellen: Kost2 → B (der Task bleibt);
     3. je Zeitbericht einen Protokolleintrag schreiben.
   - Die Reihenfolge ist wichtig: Nach dem Umstellen ist das Merkmal aus Schritt 2 weg.
   - Privilegierter Pfad für FINANCE, in beide Richtungen getestet (`TimesheetDao.reallocate`):
     - Er umgeht `checkTimesheetProtection` (`protectTimesheetsUntil`), auch für FINANCEs eigene Zeitberichte.
     - `validateKost2` wird übersprungen (beendetes Projekt, Kost2 des Tasks); sonst stellt der Revert die alte Kost2
       unter dem neuen Task nicht wieder her.
   - Die Historie der Zeitberichte bleibt erhalten, weil das Umstellen über `timesheetDao.update` läuft.
   - Revert: löscht die Anteile des Laufs und stellt die Zeitberichte laut Protokoll auf alten Task und alte Kost2
     zurück. Nur erlaubt, wenn kein späterer Lauf dieselben Sätze oder Zeitberichte geändert hat.
   - Ein späterer Lauf arbeitet auf den effektiven Anteilen. Er kann z. B. den Restanteil auf A weiter aufteilen.

## Entscheidung: Zeitberichte werden wirklich umgestellt
Erwogen wurde ein zusätzliches Feld am Zeitbericht (wie die Anteile der Buchungssätze) statt einer Änderung von Task und
Kost2. Verworfen:
- Es änderte `t_timesheet`; die Migration ist heute rein additiv.
- Alle Verbraucher von `timesheet.kost2` (`MonthlyEmployeeReport`, `ContributionMargin*`, `EmployeeSalaryExportDao`,
  Export, Filter, Suchindex, Massenänderung, Kalender, Wicket) müssten auf eine effektive Kost2 umgestellt werden und
  liefen damit auch für Nutzer, die nie umschlüsseln, über neuen Code.
- Task-basierte Auswertungen (Ist-Stunden der Auftragspositionen) folgten dem Feld nicht.

Ein Feld bleibt ein möglicher Ausbau, falls das Zurücknehmen in der Praxis scheitert.

**Vor dem ersten produktiven Anwenden** einen Datenbank-Stand sichern: Er ist der einzige vollständige Rückweg, weil
das Zurücknehmen verweigert wird, sobald ein umgestellter Zeitbericht danach geändert wurde.

## Phasen
1. Datenmodell, Anteils-DAO, `expand`, die Verbraucher (Deckungsbeitrag, Reporting, Scripting), Anzeige und
   Excel-Export, Erhalt der Anteile beim DATEV-Reimport.
2. Lauf-Entität und Wizard mit Regeln A → B (Vorschau, Anwenden, Revert).
3. Zeitberichte erkennen, Gehaltsanteile, Umstellung der Zeitberichte mit Protokoll.
4. Erlöse über Debitorenrechnungen.
5. Eingangsrechnungen, sobald die `beleg`-Frage geklärt ist.

Zu jeder Phase gehören auch:
- i18n (deutsche Texte ohne „Sie“)
- `changelog/unreleased/yyyyMMdd-kost2-reallocation.json` (EN + DE)
- `RestEndpointAccessCheckTest` (`@AccessChecked`)
- Zustandsänderungen nur per POST

## Wichtige Dateien
- `projectforge-business/.../fibu/kost/BuchungssatzDO.kt`, `BuchungssatzDao.kt`
- `projectforge-rest/.../fibu/importer/DatevRecordImportJob.kt`, `DatevRecordImportStorage.kt`, `DatevRecordImportDTO.kt`
- `projectforge-business/.../fibu/contributionmargin/ContributionMarginService.kt`, `ContributionMarginCache.kt`
- `projectforge-business/.../fibu/kost/reporting/ReportDao.java`, `Report.java`
- `projectforge-business/.../fibu/datev/EmployeeSalaryExportDao.java`, `MonthlyEmployeeReportDao.java`,
  `MonthlyEmployeeReportWeek.kt`
- `projectforge-rest/.../TimesheetMassUpdateRest.kt`, `projectforge-business/.../timesheet/TimesheetDao.kt`,
  `projectforge-business/.../task/TaskTree.kt`
- `projectforge-business/.../scripting/support/BookedInvoices.kt`
- `projectforge-rest/.../orga/AccountingRecordEntityRest.kt`, `projectforge-next/components/features/accounting-record/`

## Prüfung
- Unit-Tests:
  - `expand`: Summen, Rundung, Vorzeichen bei Soll/Haben.
  - Gehaltsanteile gegen ein von Hand gerechnetes `EmployeeSalaryExportDao`-Beispiel: zwei Mitarbeiter in einem Monat,
    einer ganz im Teilbaum des Zielprojekts, einer nur teilweise, eine Kost2-Art.
  - Erkennung: Task im Teilbaum mit fremder Kost2 wird gefunden, Task im Teilbaum mit Kost2 des Zielprojekts nicht.
  - Zwei Läufe nacheinander auf denselben Monat und dieselbe Sammel-Kost2.
  - Plausibilitätsprüfung: ein nach dem Export geänderter Zeitbericht führt zur Warnung.
  - Zuordnung der Rechnungen über `beleg`.
  - Regeln.
  - Reimport: Anteile bleiben oder werden verworfen.
- Zugriffstests:
  - TEST_USER wird abgewiesen, FINANCE darf.
  - Nur FINANCE kann den Zeitberichtsschutz umgehen.
- Deckungsbeitrag eines Testprojekts vor und nach einem Lauf:
  - Die Summe über alle Projekte bleibt gleich.
  - Kosten (Gehälter und Zeitberichtskosten) und Erlöse sind vom Sammelprojekt zum Zielprojekt gewandert.
  - Nach dem Revert ist alles wie vorher.
- e2e auf einem Slot-Server (`bin/pfDev.sh e2e <n>`): ein Wizard-Lauf einschließlich Vorschau.
