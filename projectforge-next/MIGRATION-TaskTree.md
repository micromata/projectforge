# Aufgabenbaum und Strukturelement-Seiten (`wa/taskTree` → `/next/taskTree`)

Phase 3, dritter handgebauter Fall zu [MIGRATION.md](MIGRATION.md). **Abgeschlossen:**
Baum, Listenperspektive, Edit-Seite und Assistent stehen, `task` ist in
`NextMigration.MIGRATED` umgeschaltet, `MenuItemDefId.TASK_TREE` zeigt auf
`next/taskTree`. Maßstab war durchgehend **Wicket** (`wa/taskTree`/`wa/taskEdit`);
`/react/task` ist nie fertig gebaut worden.

**Grundsatz:** erst alles aus Wicket nachbauen – Baum **und** Edit-Seite –, dann
umschalten. Ein Teilumstieg, dessen Zeilenklick nach `wa/taskEdit` zurückführt, hätte
Funktionalität verloren.

## Was gebaut wurde

- **Baum** (`components/shared/tasks/`, geteilt mit `TaskSelectField`): Einrückung,
  volle Spaltenliste inkl. Verbrauchsbalken/Kost2/Auftragspositionen (aus
  `TaskServicesRest`), debounced Suche, vier Status-Häkchen, serverseitiges Auf-/Zuklappen
  über User-Prefs (`TaskTree.USER_PREFS_KEY_OPEN_TASKS` – ein Request, kein Client-State),
  Wurzelknoten nur für Admin/FiBu, markierte Zeile + geöffnete Vorfahren nach dem
  Speichern (`highlightTaskId`, `savedId`-Param), Auswahlmodus, URL-basierte
  Spaltenzustands-Persistenz (`tree/setColumnStates`, eigenes `tree/`-Präfix wegen
  Kollision mit `TaskPagesRest`).
- **Listenperspektive** (`/next/task`): zehn Spalten als `PageDef`-Deklaration (nicht als
  `UITable` – die Spalten einer handgebauten Liste gehören ins `PageDef`; `createListLayout`
  bleibt einspaltig). Beide Perspektiven teilen sich Renderer und berechnete Werte über
  `Task.copyFrom4ListRow` (lean row via `NON_NULL`), Sichtbarkeitsregeln über
  `TaskColumnVisibility` + `addVariablesForListPage`. Umschaltknopf in beide Richtungen
  (`task-perspective-link.tsx`); die Liste ist das **zweite** `returnTarget`, der Baum das
  erste.
- **Edit-Seite** als `PageDef` (`components/features/task/`): drei Sektionen, zwei
  zugeklappt (Gantt, Finanzverwaltung), Kost2-Custom-Block, feldweise Rechte, History-Tab
  (aus `historizable`).
- **Aufgaben-Assistent** (`/next/taskWizard`, `components/features/task/wizard/`): handgebaut
  (schlichtes `useState`), Gruppen-Picker als `EntityAutocomplete`, Aufgabe über
  `TaskSelectControl`. Logik in `TaskWizardService.kt` (`projectforge-business`, damit
  prüfbar – `TaskWizardServiceTest`), dünnes `TaskWizardRest` (`POST execute`,
  Admin-geprüft). Der frühere `TaskWizardPageRest`-Torso ist gelöscht.

## Task-spezifische Gotchas (falls hier noch mal Hand angelegt wird)

- **`TaskDO`-Annotationen waren falsch** (auch in Wicket/React): fünf Gantt-Felder
  (`workpackageCode`, `ganttPredecessor(Offset)`, `ganttRelationType`, `ganttObjectType`)
  trugen alle `@PropertyInfo(i18nKey = "task.parentTask")`; `kost2IsBlackList` hatte gar
  keine `@PropertyInfo` und fehlte damit in den Metadaten. Behoben, neuer Bundle-Key
  `workpackage*`.
- **`Task.kt`: drei Datumsfelder waren `java.util.Date` statt `LocalDate`.** `BaseDTO.copy`
  kopiert nur bei Typgleichheit, sonst stumm (`log.debug("Unsupported field…")`) – die
  Felder (`startDate`/`endDate`/`protectTimesheetsUntil`) wurden **in keiner Richtung**
  übertragen (`/react/task` zeigte „Schutz bis" leer). Mit `LocalDate?` erledigt (globale
  Serializer schreiben `yyyy-MM-dd`, was `date-input.tsx` erwartet).
- **Kost2-Block lässt sich nicht in TS nachbauen.** `TaskHelper.addKost2` hängt je nach
  `id`/`parentTaskId` die zweistellige Art-Id oder die volle Nummer an und braucht
  `KostFormatter`, Projektauflösung über den Baum und `TaskTree.getKost2List` gegen den
  `KostCache`. Deshalb **ein** Server-Roundtrip: `POST /rs/task/kost2Preview` (Anhängen +
  Vorschau in einem), Werte serialisiert und debounced (Muster wie `use-order-sums.ts`).
  Picker über neuen Request-Parameter `projektId` an `cost2/autosearch` vorgefiltert.
- **`duration` ⇔ `endDate` schließen sich aus** – gehört ins Backend (`TaskPagesRest.validate`,
  war leer), nicht ins Zod-Schema. Zahlenbereiche (`progress` 0–100, `maxHours` 0–9999,
  `duration` 0–10000) stehen als `@PropertyInfo(min/max)` am `TaskDO` und werden generisch
  von `ValidationUtils.validateFields` geprüft (s. „Validierungs-Metadaten" in MIGRATION.md).
- **Feldweise Rechte** (`kost2AndBookingStatusWriteAccess`, `protectTimesheetsUntilWriteAccess`):
  ein Flag pro Regel (die DAO kennt zwei), in `transformFromDB` nur bei `editMode`. **Falle:**
  eine neue Aufgabe entscheidet am **Eltern**-Knoten – `newBaseDO(request)` muss `parentTaskId`
  übernehmen (dafür lernte `fetchNew`/`useEntityDetail` Parameter, deklariert über
  `EditDef.newEntryParams` als Whitelist). `causedByField` an den `TaskDao`-`AccessException`s
  ergänzt, damit der 406-Fehler am Feld statt allgemein landet.
- **Fünf Querverweise im Formularkopf** (`EditDef.crossLinks`/`CrossLinkDef`, generisch neu):
  Unteraufgabe/Zeitbuchung/Zeitbuchungen anzeigen/Gantt/Zugriffsrechte, als **ein** Menü neben
  der Überschrift, nur bei `id != null`. Vier zeigen noch auf unmigrierte Wicket-Seiten
  (`wa/timesheetEdit`, `wa/timesheetList`, `wa/ganttEdit`, `wa/accessList`).
- **Tippsuche im Auswahlfeld:** eigener Endpunkt `GET /rs/task/tree/autosearch`
  (`task/autosearch` ist unbenutzbar – keine `autoCompleteSearchFields`), sucht über `title`
  + indizierten `taskpath`, Label ist der ganze Pfad (`" | "`, Wurzel = `task.path.rootTask`).

## Querschnittliche Funde (einmal für alle Seiten behoben)

Diese sind in MIGRATION.md eingearbeitet; hier nur der Ursprung:

- **Undelete + „jedes Schreiben holte den Eintrag zurück".** Ein handgebautes Formular postet
  seine Werte als DTO; `deleted` steht in keinem Schema, `CandHMaster.copyValues` überträgt es
  → jedes `saveorupdate` stellte still wieder her. Behoben in der gemeinsamen Maschinerie:
  `entityAccess` kennt `deleted` (kein Schreib-/Löschrecht), `EntityUndeleteButton`
  (Bedingung `userAccess.insert`), gelöschte Felder als `<fieldset disabled>` + `readOnly`
  über den Formularkontext, `EntityDeletedBanner`, und das Formular postet `deleted: true`
  eines geladenen gelöschten Eintrags mit. Test: `e2e/deleted-entry.spec.ts`.
- **„+"/Zeilenklick ohne Rechteprüfung.** `useEditTargets` liest jetzt `userAccess.insert`
  (`canAdd`) und `.update` (`canOpen`). Dafür musste `getListMeta` aufhören, `update = true`
  für jeden zu setzen: `AbstractEntityRest.listUpdateAccess()` (Default `true`, `GroupPagesRest`
  überschreibt mit `isLoggedInUserMemberOfAdminGroup`). **Ausnahme Baum:** `TaskDao` hat keine
  `userRightId`, `hasInsertAccess(user)` ist immer wahr – echte Prüfung am Elternknoten beim
  Speichern; der „+" der Baumseite bleibt daher ungefiltert.
- **`newMagicFilter()`** (`AbstractEntityRest`, offen/leer für alle anderen): der
  Task-Statusfilter (`notOpened/opened`, nicht `closed`) steht als **vorbelegter
  Filtereintrag**, nicht als stille Abfragebedingung – eine Bedingung, die nur in der Abfrage
  lebt, wäre in der Filterzeile unerreichbar. `filterReset` legt die Vorgaben wieder ein.
- **`filterScope` am `ListGearMenu`** (`"stored"` vs. `"own"`): der Baum filtert mit einem
  eigenen `TaskFilter` in der Session, nicht mit dem gespeicherten `MagicFilter` – „Filter
  zurücksetzen" darf dort nicht den Endpunkt `resetListFilter` treffen.
- **`hoistDeletedFilter`** (`components/data-table/filter-groups.ts`): das `deleted`-Feld nach
  vorn ziehen (der einzige Weg zu einem Eintrag, den der Standardfilter verbirgt), in
  Feld-Picker und „Alle Filter"-Dialog.

## Bewusst nicht vorgesehen

**Strukturelement-Favoriten** (`UserPrefArea.TASK_FAVORITE`) – kein Rückstand, sondern
gegenstandslos: was sie in Wicket abkürzen, leisten in next die Auswahlfelder selbst (Baum +
Tippsuche). `TaskFavoritesRest` bleibt unbenutzt, `UserPrefListPage` wird nicht nachgezogen,
die Favoriten bleiben Wicket-Sache (über die Fluchtluke erreichbar).

## Verifikation

Gegen das laufende System (`E2E_BASE_URL=…`): `task-tree.spec.ts`,
`task-tree-actions.spec.ts`, `task-list.spec.ts`, `task-edit.spec.ts`,
`task-kost2-preview.spec.ts`, `task-wizard.spec.ts`; gemeinsame Handgriffe in
`e2e/fixtures/task-tree.ts`, Texte/Formate über `e2e/fixtures/format.ts`. Der Kost2-Block
wird gegen **Gleichstand mit Wicket** geprüft (`kost2Preview` vs. `TaskTree.getKost2List`,
ohne eine Aufgabe zu ändern). Ablehnungspfade der feldweisen Rechte sind mit `admin-user`
(Admin ohne Finanzrechte) prüfbar geworden, Insert/Update-Check mit `normalo-user` (s.
Testkonten in MIGRATION.md); Backend-Unit-Tests: `TaskTest`, `TaskPagesRestTest`,
`TaskWizardServiceTest`.
