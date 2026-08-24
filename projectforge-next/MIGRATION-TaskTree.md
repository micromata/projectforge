# Aufgabenbaum und Strukturelement-Seiten (`wa/taskTree` → `/next/taskTree`)

Detailplan und Bestandsaufnahme zu [MIGRATION.md](MIGRATION.md), Phase 3, dritter
handgebauter Fall. Anders als [MIGRATION-calendar.md](MIGRATION-calendar.md) hält dieses
Dokument keine reine Planung fest: Baum, Listenperspektive, Edit-Seite und Assistent
**stehen**, umgeschaltet ist die Kategorie `task` auch schon. Was hier festgehalten wird,
ist deshalb zweierlei – erstens, was in fünf Schritten entstanden ist und warum es so
entstanden ist (der untere, gewachsene Teil), und zweitens, was gegen die **Vorlage**
Wicket noch fehlt (der Abschnitt „Was noch offen ist", entstanden aus einer Prüfung
Wicket ↔ next am 23.08.2026).

Der Maßstab ist durchgehend **Wicket**, nicht das Legacy-React-Frontend: die
Strukturelement-Seiten liegen produktiv unter `wa/taskTree`/`wa/taskEdit`, und
`/react/task` ist nie fertig gebaut worden.

## Was noch offen ist

Prüfung Wicket ↔ next vom 23.08.2026, Zeile für Zeile gegen `TaskTreePage`/`TaskTreeForm`,
`TaskListPage`/`TaskListForm`, `TaskEditPage`/`TaskEditForm` und `TaskSelectPanel`.
Ergebnis: **Feldsätze, Spalten und Filter stimmen** – die Lücken liegen bei _Aktionen,
Vorbelegungen und Rechteprüfungen_. Die Reihenfolge unten ist Aufwand gegen Nutzen, nicht
Wichtigkeit allein.

### 1. Die Listenperspektive zeigte geschlossene Strukturelemente – erledigt

Wickets Liste hat drei Häkchen (`TaskListForm.onOptionsPanelCreate`: `notOpened`,
`opened`, `closed`) und eine Voreinstellung, die keins davon ausdrückt:
`TaskFilter` steht auf `notOpened = true, opened = true, closed = false`, und
`TaskDao.select` macht daraus ein `status in (…)`. Die next-Liste läuft über den
`MagicFilter`, und `TaskPagesRest` brachte dort **keine** Statusvorgabe mit – `/next/task`
zeigte also geschlossene Strukturelemente, die Wicket ausblendet, und bot keinen Weg, sie
auszublenden.

**Umgesetzt, serverseitig** – der Status ist eine Abfragebedingung, und der `MagicFilter`
ist das Vokabular, in dem diese Liste ihre Bedingungen ausdrückt (die React-Liste bekommt
es damit mit):

- `TaskPagesRest.addMagicFilterElements` bietet `status` als
  `UIFilterListElement`-Mehrfachauswahl mit `defaultFilter = true`, gefüllt aus
  `TaskStatus` – dieselben drei Worte, die die Häkchen des Baums tragen
  (`task-tree-filter.tsx`), weil beide die i18n-Keys des Enums lesen. Ein
  `CustomResultFilter` ist unnötig: `status` ist eine echte Enum-Property, und
  `MagicFilterProcessor` macht aus den Werten von sich aus ein `status in (…)`.
- Die Voreinstellung steht als **vorbelegter Filtereintrag**, nicht als stille Bedingung
  beim Abfragen: dafür ist `AbstractEntityRest.newMagicFilter()` neu (offen, leer für alle
  anderen Seiten) und tritt an die Stelle der drei `MagicFilter()`-Aufrufe, mit denen der
  gespeicherte Filter angelegt und zurückgesetzt wird. Der Client baut seine Filterzeile
  aus genau diesem Filter – eine Bedingung, die nur in der Abfrage lebt, würde die Liste
  filtern, ohne irgendwo in ihr aufzutauchen, und das dritte Häkchen wäre unerreichbar.
  `filterReset` setzt den gespeicherten Filter jetzt zurück **und** legt die Vorgaben
  wieder ein (`MagicFilter.reset` löscht nur), behält aber die Instanz, weil sie ein
  benannter Favorit sein kann.
- Zusicherung: `e2e/task-list.spec.ts` „starts with closed tasks hidden …" – der
  ausgelieferte Filter trägt `["N","O"]`, unter ihm kommt keine geschlossene Aufgabe
  zurück, unter `["C"]` nur geschlossene, und die Pille steht ohne Zutun in der
  Filterzeile.

### 2. Die fünf Querverweise im Kopf des Formulars – erledigt

`TaskEditPage.addTopMenuPanel`, nur bei bestehendem Datensatz: Unteraufgabe anlegen
(`PARAM_PARENT_TASK_ID`), Zeitbuchung anlegen, Zeitbuchungen anzeigen
(`task.menu.showTimesheets`), Gantt-Diagramm anlegen und im erweiterten Menü die
Zugriffsrechte. In next gab es davon **nichts**: `lib/page-def/types.ts` kannte keinen
Slot für Aktionen am Formularkopf. „Unteraufgabe anlegen" existierte nur als Zeilenaktion
des Baums, „Zeitbuchungen anzeigen" nur über die Consumption-Bar.

**Umgesetzt, zur Hälfte Gerüstarbeit** – und als _Querverweise_ deklariert, nicht als
Aktionen: jeder Eintrag ist ein Link, der den geöffneten Datensatz in seiner URL nennt,
und nur das kann eine Seite in einer Zeile hinschreiben.

- `CrossLinkDef<Data>` (`labelKey` + `href: (data) => string | null`) und
  `EditDef.crossLinks` in `lib/page-def/types.ts`, generisch wie `listActions` es für die
  Liste ist. Ein `null` fällt weg, damit ein Eintrag an eine Bedingung geknüpft werden
  kann, ohne dass die Seite die Menülogik nachbaut.
- `components/shared/edit/entity-cross-links.tsx` rendert sie als **ein** Menü neben der
  Überschrift (neuer `crossLinks`-Slot an `EntityEditHeader`, gefüllt von
  `EntityEditPage` – nur bei `id != null`, wie Wickets `isNew() == false`). Ein Menü und
  keine Knopfreihe: das sind Abzweigungen und nicht die Handlung, für die die Seite da ist
  – Wicket sagt dasselbe, indem es den fünften Eintrag in sein erweitertes Menü schiebt.
  Über welchen Weg ein Ziel geöffnet wird, entscheidet `resolveMenuUrl` wie beim
  Hauptmenü: eine Route dieser App über `GuardedLink` (fragt vor dem Verwerfen), ein
  `wa/…` als vollständiger Seitenwechsel.
- Die fünf Einträge stehen in `task.page.tsx`, in Wickets Reihenfolge und mit seinen
  Texten. Der erste ist eine next-Route (`/task/new?parentTaskId=…`, dieselbe URL, die die
  Zeilenaktion des Baums baut), die vier anderen zeigen auf unmigrierte Wicket-Seiten:
  `wa/timesheetEdit`, `wa/timesheetList`, `wa/ganttEdit` (Basisname `gantt`, und sein
  Parameter heißt `task`, nicht `taskId`), `wa/accessList`. Damit wächst die Zahl der
  Stellen, die eine Legacy-URL selbst bilden, von zwei (`task-edit-link.tsx`,
  `consumption-cell.tsx`) auf drei; sie fallen mit der Migration der Zeitberichte bzw. von
  Gantt/Access gemeinsam um (s. „Offen: …" in [MIGRATION.md](MIGRATION.md)).
- Zusicherung: `e2e/task-edit.spec.ts` prüft die fünf Einträge samt ihrer URLs (Endung, da
  die eigene Route den Base-Path davorträgt) und dass ein noch nicht gespeichertes
  Strukturelement das Menü gar nicht anbietet.

### 3. Im Auswahlfeld fehlte die Tippsuche – erledigt

`TaskSelectPanel` hat neben dem Baum-Dialog ein Type-ahead auf Strukturelemente
(`initAutoCompletePanels`) – in einem Baum mit tausenden Knoten der schnellere Weg. next
bot nur den Dialog und den Pfad.

**Umgesetzt**, und der Endpunkt-Gleichstand war der eigentliche Posten:

- `task/autosearch` ist **unbenutzbar**: `TaskPagesRest` erbt es von
  `AbstractDTOPagesRest`, deklariert aber keine `autoCompleteSearchFields`, und
  `AbstractEntityRest.getAutoCompleteObjects` wirft dann eine `TechnicalException`.
  Deshalb ein eigener Endpunkt `GET /rs/task/tree/autosearch` auf `TaskServicesRest` – mit
  dem `tree/`-Präfix, das dort schon die Kollision mit `TaskPagesRest` vermeidet (wie beim
  Grid-Zustand). Antwort ist ein gewöhnliches `DisplayObject`, also braucht der Client
  nichts Eigenes.
- Gesucht wird mit **Wickets** Mitteln: `taskDao.select(BaseSearchFilter)` über die zwei
  Felder von `TaskSelectAutoCompleteFormComponent.SEARCH_FIELDS` – `title` und den
  indizierten `taskpath` (Class-Bridge über die Titel aller Vorfahren), also trifft ein
  Begriff auch über ein Oberelement. Der DAO sortiert nach Titel, entfernt, was der Nutzer
  nicht sehen darf, und lässt – über den `TaskFilter`, in den er einen einfachen Filter
  wickelt – geschlossene Strukturelemente weg (dieselbe Voreinstellung wie Punkt 1).
- Das Label eines Treffers ist der **ganze Pfad**, mit `" | "` verbunden wie Wickets
  `createPath`; die Wurzel allein heißt `task.path.rootTask`. In einem tiefen Baum sind
  zwei „Entwicklung" durch nichts anderes zu unterscheiden.
- Client: `components/shared/tasks/task-search-popover.tsx` – ein Lupenknopf neben dem
  Baumknopf in `TaskSelectControl`, im Popover das geteilte `EntitySearchList`. Damit hat
  jedes Aufgabenfeld die Tippsuche, auch das des Assistenten (beide gehen über dieses
  Control). Ein leerer Begriff wird beantwortet, weil `useEntityLookup` beim Öffnen so
  fragt – Wickets Feld mit `withMinChars(2)` sieht diesen Fall nie.
- Kein neuer i18n-Schlüssel: der Knopf heißt `search._` plus der Feldname. Das hat die
  e2e-Suite kurz gekostet – `getByLabel` trifft Teilzeichenketten, also fand
  `getByLabel("Suchen")` auch den neuen Knopf; die Baum-Fixture sucht jetzt exakt (und
  `"/rs/task/tree?"` statt `"/rs/task/tree"`, damit `tree/autosearch` keine Wartebedingung
  erfüllt).
- Zusicherung: `e2e/task-edit.spec.ts` tippt den Titel des Unterelements des Seeds, prüft,
  dass der Treffer `Oberelement | Unterelement` heißt, wählt ihn und findet ihn im
  Pfad-Breadcrumb wieder.
- Nicht mitgenommen: `autocompleteOnlyTaskBookableForTimesheets` (Wicket schränkt die
  Trefferliste damit auf buchbare Strukturelemente ein). In next hat das keinen Aufrufer,
  bis die Zeitberichte migriert sind – dann ist es ein Parameter am Endpunkt und ein
  `params`-Eintrag am Popover.

### 4. Kleinigkeiten, je ein Handgriff – erledigt

Vier Punkte, jeder für sich klein, aber zwei davon brauchten eine Antwort des Servers:
welches Strukturelement die Wurzel ist und welche Rechte an einem noch nicht gespeicherten
Element gelten, weiß nur das Backend.

- **Keine markierte Zeile nach dem Speichern.** Die Kette stand schon (`highlightTaskId`
  durch `TaskTreePanel` → `TaskTreeTable`, der Server öffnet die Vorfahren des markierten
  Knotens), es fehlte die Weitergabe: `task.page.tsx` gibt sein erstes Rückkehrziel jetzt
  ein `savedIdParam` mit, `app/(authenticated)/taskTree/page.tsx` liest es aus der URL und
  übergibt es dem Panel. Der Parameter heißt für alle Aufrufer gleich (`SAVED_ID_PARAM` in
  `task-routes.ts`, vorher `WIZARD_SAVED_ID_PARAM`) – der Assistent macht mit demselben
  Wert weiter, der Baum markiert damit. Die Seite braucht dafür eine `<Suspense>`-Grenze um
  ihren Rumpf, wie jede Seite des statischen Exports, die `useSearchParams` liest.
- **Der Assistent belegt sein „Strukturelement anlegen" nicht vor.** Wicket setzt dort die
  Wurzel als Elternelement (`TaskWizardForm` → `PARAM_PARENT_TASK_ID`). Dafür gibt es
  jetzt `GET /rs/task/tree/root` (`TaskServicesRest.getRoot`, Antwort ein
  `DisplayObject` – Id plus der Pfad, den `formatPath` baut); `wizard-task-step.tsx` fragt
  ihn per `useQuery` und hängt den `parentTaskId` an den Link. Bewusst ein Endpunkt und
  kein Zauberwort in der URL (`parentTaskId=root`) und kein Baumlauf im Client: die Id ist
  „1 nur nach Konvention", und `hibernate_sequence` vergibt in Schritten von 50. Bezahlt
  hat sich das gleich zweimal – `e2e/fixtures/seed.ts` kommt damit ohne seinen
  Baum-Abruf und den auf 50 Schritte begrenzten Lauf über die Elternkette aus.
- **Die Finanzfelder einer neuen Unteraufgabe sahen editierbar aus.** `TaskPagesRest`
  überschreibt jetzt `newBaseDTO` und ruft `transformFromDB(newBaseDO(request), editMode = true)`
  – die Vorbelegung ist das Einzige, was ein Formular anfragt, also sind die feldweisen
  Flags genau dort auszufüllen. `edit/finance-section.tsx` liest sie unbedingt, nicht mehr
  nur bei vorhandener Id, und sperrt, solange die Antwort aussteht: ein Feld wird durch ein
  Recht freigegeben, nicht durch eine offene Anfrage. Entschieden wird damit am
  **Eltern**-Knoten, den `newBaseDO` bis dahin gesetzt hat – dieselbe Frage, die Wicket in
  `TaskEditForm.onBeforeRender` stellt.
- **„Filter zurücksetzen" im Baum traf auch die Liste.** `ListGearMenu` hat jetzt ein
  `filterScope`: `"stored"` (Voreinstellung) ruft wie bisher `resetListFilter(entity)`,
  `"own"` überlässt das Zurücksetzen allein dem `onFilterReset` des Aufrufers. Die
  Baumseite deklariert `"own"`, weil ihr Filter ein `TaskFilter` in der Session ist – der
  Endpunkt hätte den gespeicherten `MagicFilter` **und** den Spaltenzustand der
  \_Listen_perspektive gelöscht und den Filter des Baums gar nicht angefasst. Nicht als
  gewollte Doppelwirkung vermerkt, sondern beseitigt; die Notiz in Schritt 3 unten
  („`ListGearMenu.onFilterReset` ist dafür optional geworden") ist damit vollständig.
- Zusicherungen: `e2e/task-edit.spec.ts` prüft, dass ein Speichern auf
  `/taskTree?savedId=<id>` zurückkommt und die Zeile dort `row-highlighted` trägt und im
  Blickfeld liegt, und – als `admin-user`, dem Konto mit Admin-Gruppe ohne Finanzrechte –
  dass die drei Felder der Finanzverwaltung an einer **neuen** Unteraufgabe gesperrt sind
  und die Ablehnung des Backends als Hinweis nennen. `e2e/task-wizard.spec.ts` prüft die
  Wurzel-Id im `href` des Links und dass das geöffnete Formular sein Elternfeld gefüllt
  bekommt. `e2e/task-tree-actions.spec.ts` prüft, dass beim Zurücksetzen im Baum **kein**
  Aufruf an `task/filter/reset` geht und die eigenen Filterwerte trotzdem stehen.

### 5. Querschnittlich, hier aufgefallen – erledigt

Beides betrifft **jede** handgebaute Seite, nicht nur die Strukturelemente – gefunden bei
den Strukturelementen, umgesetzt einmal für alle. Die offene Frage aus der Planung hat der
Test beantwortet, und zwar mit „ja": es war das Loch, nicht nur der fehlende Knopf.

- **Es gab keinen Weg zurück aus dem Löschen – und jedes Schreiben holte den Eintrag
  zurück.** `lib/rs/entity.ts` hatte `undeleteEntity` und keinen Aufrufer; ein gelöschter
  Datensatz bot erneut „als gelöscht markieren". Der Beweis am laufenden System (ein Buch,
  `markAsDeleted`, danach `saveorupdate` mit einem DTO **ohne** `deleted`): der Datensatz
  kam als `deleted: false` mit dem geänderten Titel zurück. Ein handgebautes Formular postet
  seine Werte _als_ DTO, `deleted` steht in keinem Schema, und `CandHMaster.copyValues`
  überträgt die Eigenschaft des geposteten Objekts auf die Zeile – also stellte **jedes**
  Schreiben still wieder her, das Ausleihen eines Buches genauso (`BookServicesRest.lendOut`
  gibt das gepostete DTO direkt an `saveOrUpdate`).

  Umgesetzt in der gemeinsamen Maschinerie, in drei Lagen:
  - `entityAccess` (`lib/rs/entity-access.ts`) kennt jetzt `deleted` – kein Recht, sondern
    ein Zustand, der dieselbe Frage entscheidet: ein gelöschter Eintrag hat weder
    Schreib- noch Löschrecht. Damit verschwinden „Speichern", das Return-Kürzel und das
    Kommentarfeld der Historie von sich aus.
  - An ihre Stelle tritt `EntityUndeleteButton` (`useUndeleteEntity`, `runUndelete` in
    `EntityEditPage`) – ohne Rückfrage, wie `UIButton.createUndeleteButton`, und unter der
    Bedingung, die Legacy stellt: `userAccess.insert`, nicht das Schreibrecht eines
    Eintrags, den es so nicht mehr gibt (`LayoutUtils.processEditPage`).
  - Die Felder eines gelöschten Eintrags sind **nur noch Anzeige**: ein `<fieldset disabled>`
    um die deklarierten Abschnitte sperrt jedes Bedienelement darin, Felder wie die eigenen
    Aktionsknöpfe einer Entität (das Ausleihen des Buches). Das war die Alternative zum
    stillen Verwerfen – Wiederherstellen ignoriert die Formulareingaben, und ein Formular,
    das Eingaben annimmt und dann wegwirft, sagt das nirgends. Erst nach dem
    Wiederherstellen ist der Eintrag wieder ein Eintrag. Das `fieldset` sperrt, ansehen kann man
    das den Feldern aber nicht: dieselbe Auskunft geht deshalb als `readOnly` durch den Formularkontext
    (`useFormReadOnly`, gelesen von `DeclaredFormField`), damit die Felder auch _aussehen_ wie
    Anzeige – ein Auswahlfeld behielt sonst sein „×" zum Leeren, und die Objektsuche ihren
    „mich auswählen"-Knopf.
  - Und es steht dabei, statt erschlossen werden zu müssen: `EntityDeletedBanner` über den
    Abschnitten („Dieser Eintrag ist als gelöscht markiert. Zum Bearbeiten zuerst
    wiederherstellen.", `entityEdit.deletedInfo` in `messages/{de,en}.json`) und im Kopf der
    Titel durchgestrichen mit einem `deleted`-Abzeichen. Vorher war der Zustand an genau einer
    Sache ablesbar – dass die Seite Wiederherstellen anbietet statt Speichern –, und das ist
    ein Schluss, keine Aussage; die gesperrten Felder sahen nach einem fehlenden Recht aus. Der
    Hinweis ersetzt das Banner einer Entität nicht, er steht darüber.
  - Darunter bleibt die Absicherung auf Datenebene: ist der geladene Eintrag gelöscht,
    postet das Formular `deleted: true` mit. Nur dann, die Nutzlast jedes normalen
    Speicherns ist unverändert. Sie bleibt, weil sie auch für eine Seite gilt, die diese
    Hooks selbst zusammensetzt.

- **„+" und Zeilenklick fragten nicht nach dem Recht.** Legacy hängt den Anlege-Eintrag nur
  bei `userAccess.insert` ans Menü (`AbstractPagesRest.createListLayout`) und verdrahtet den
  Zeilenklick nur bei `userAccess.update` (`AGGridSupport`, weshalb die servergelayoutete
  `DynamicGrid` schon richtig lag). `useEditTargets` liest jetzt beides: `canAdd` lässt
  `AddEntryButton` samt `N`-Kürzel weg, `canOpen` lässt den Zeilenklick weg – kein Handler,
  kein Zeigefinger, so wie Wickets Liste den Namen dann als schlichtes Label statt als Link
  zeigt (`GroupListPage`).

  Dafür musste das Backend erst aufhören zu lügen: `AbstractEntityRest.getListMeta` setzte
  `userAccess.update = true` für jeden, mit dem Argument, das Schreibrecht sei eine Frage am
  einzelnen Eintrag und reise auf seinem DTO. Das stimmt für die meisten Entitäten und für
  ein paar eben nicht – eine Gruppe darf jeder lesen und nur ein Administrator ändern. Die
  Antwort steht jetzt in `AbstractEntityRest.listUpdateAccess()` (Voreinstellung `true`,
  überschreibbar), `GroupPagesRest` überschreibt sie mit
  `accessChecker.isLoggedInUserMemberOfAdminGroup` – genau der Wert, den es vorher nur in
  seinem `createListLayout` gesetzt hat, jetzt für beide Listenseiten an einer Stelle.

- **Und man muss gelöschte Einträge auch finden können.** Das `deleted`-Feld hängt
  `LayoutListFilterUtils` an jede Liste, sortiert dann aber alle Filterfelder nach Beschriftung
  – womit „gelöscht" mitten unter den Eigenschaften der Entität landet, obwohl es die eine Frage
  ist, die jede Liste kennt, und der einzige Weg zu einem Eintrag, den der Standardfilter
  verbirgt. `hoistDeletedFilter` (`components/data-table/filter-groups.ts`) zieht es nach vorn,
  an beiden Stellen, an denen ein Feld gewählt wird: in der Auswahlliste des „+"-Chips direkt
  hinter „geändert" (die Änderungshistorie), und im „Alle Filter"-Dialog an die Spitze der
  eigenen Felder der Entität.

- Zusicherungen: `e2e/deleted-entry.spec.ts` (neu, an einem eigenen Buch, das es hinterher
  gelöscht zurücklässt) prüft, dass der gelöschte Eintrag den Hinweis und `undelete` anbietet
  und weder „Speichern" noch „als gelöscht markieren", dass der Knopf ihn zurückholt und auf
  die Liste führt, und dass Titelfeld, Auswahlfelder **und** Ausleihknopf gesperrt sind (die
  Auswahlfelder über ihren eigenen Zustand, nicht nur über das `fieldset` um sie herum).
  `e2e/filter-all-dialog.spec.ts` prüft die Reihenfolge der beiden Feldauswahlen,
  `filter-groups.test.ts` das Umsortieren selbst. `e2e/group.spec.ts` prüft
  als `normalo-user` – dem Konto, das Gruppen lesen, aber nicht anlegen darf – dass die
  Liste keinen „+"-Knopf trägt, und dass `listMeta` `update: false` und `insert: false`
  antwortet; der Zeilenklick selbst ist dort nicht zu klicken, weil `GroupDao` diesem Konto
  keine Gruppe zeigt. Gegenprobe in derselben Datei mit dem Vollzugriffskonto, das die
  gesäte Gruppe per Zeile öffnet.

- Bewusst nicht angefasst: der „+"-Knopf der Baumseite (`task-tree-action-bar.tsx`).
  `TaskDao` hat keine `userRightId`, `hasInsertAccess(user)` ist damit für jeden wahr – die
  echte Prüfung läuft am Elternknoten beim Speichern (`hasInsertAccess(user, obj)`). Eine
  Abfrage wäre wirkungslos und würde der Baumseite eine `listMeta`-Anfrage aufhalsen, die
  sie sonst nicht braucht. `DynamicGrid` bleibt ebenfalls, wie sie ist: dort entscheidet der
  Server über `clickable`.

### Wie das zu prüfen ist

Gegen das laufende System, wie alles hier: Dev-Server dieses Arbeitsbaums starten und
`E2E_BASE_URL=… npx playwright test` über die Specs fahren, die die Seiten schon haben –
`task-tree.spec.ts`, `task-tree-actions.spec.ts`, `task-list.spec.ts`,
`task-edit.spec.ts`, `task-kost2-preview.spec.ts`, `task-wizard.spec.ts`. Neue
Zusicherungen gehören in genau diese Dateien, Texte und Formate immer über
`e2e/fixtures/format.ts`.

Neu und für zwei der Punkte entscheidend: es gibt **mehrere Testkonten** statt einem
(`e2e/fixtures/credentials.ts`, s. „Mehrere Testkonten statt einem" in
[MIGRATION.md](MIGRATION.md)). Damit sind die Ablehnungspfade prüfbar, die im unteren
Teil dieses Dokuments noch mehrfach als „nicht lokal prüfbar" stehen: die feldweisen
Schreibrechte des Formulars (`kost2AndBookingStatusWriteAccess`,
`protectTimesheetsUntilWriteAccess`) mit `admin-user`, der Insert-/Update-Check von „+"
und Zeilenklick mit `normalo-user`. Diese Vermerke sind also überholt, nicht die
Unit-Tests, die statt ihrer geschrieben wurden.

### Bewusst nicht vorgesehen

**Die Strukturelement-Favoriten** (`UserPrefArea.TASK_FAVORITE`) – Wicket hat sie an drei
Stellen: Baumseite, Listenseite und Auswahlfeld. Sie sind **kein Rückstand, sondern
gegenstandslos**: was sie in Wicket abkürzen, leisten in React und next die Auswahlfelder
selbst – beim Strukturelement der Baum samt Tippsuche im Auswahlfeld, beim Benutzer
`EntityAutocomplete`. `TaskFavoritesRest` bleibt also in next unbenutzt, die
Verwaltungsseite (`UserPrefListPage`) wird für next nicht nachgezogen, und die Favoriten
bleiben eine Sache der klassischen Version (erreichbar über die Fluchtluke). Am
Menüeintrag `TASK_TREE` hängt damit nichts mehr (s. Schritt 5 unten).

## Bestandsaufnahme: wie die Seiten entstanden sind

Der **Aufgabenbaum** liegt produktiv unter `wa/taskTree` (Wicket, nicht React) und
ist der dritte Fall, der handgebaut werden muss: die Baumdarstellung selbst kennt
`UILayout` nicht (`TaskPagesRest.createListLayout` besteht aus einer einzigen
Spalte `title`), die Spalten kommen stattdessen aus einem eigenen Endpunkt
`TaskServicesRest`. Anders als bei Auftragsbuch und Kalender ist die Baumseite
in next aber **schon weitgehend gebaut** (Commits `d9c0e6a2c`, `522f3bbc3`,
`0e7f9fe44`, `ecf3e544c`).

**Grundsatz für diese Seite: erst alles aus Wicket nachbauen – Baum _und_
Edit-Seite –, dann umschalten.** Kein Teil-Umstieg, bei dem der Zeilenklick nach
`wa/taskEdit` zurückführt: die Wicket-Edit-Seite hat Felder und Rechte-Logik, die
`TaskPagesRest.createEditLayout` heute nicht abbildet, ein Teilumstieg würde also
Funktionalität verlieren statt gewinnen.

### Was in next steht (`/next/taskTree`, ~1030 Z. in 12 Dateien)

`app/(authenticated)/taskTree/page.tsx` ist eine konkrete Route, die den
`[category]`-Catch-all überschattet – `taskTree` ist **keine** REST-Kategorie, die
Entität heißt `task`. Der Baum selbst liegt bewusst in
`components/shared/tasks/`, weil ihn `TaskSelectField` (Auftragsposition) im
Auswahlmodus mitbenutzt: `task-tree-panel.tsx`, `task-tree-table.tsx`,
`task-tree-filter.tsx`, `use-task-tree.ts`, `use-task-tree-columns.tsx`,
`types.ts`, dazu `lib/rs/task.ts` und die Picker-Familie (`task-select-field`,
`task-select-modal`, `task-select`, `task-chip`, `task-path`).

Damit sind aus Wicket bereits nachgebaut: Baum mit Einrückung, die vollständige
Spaltenliste inkl. Verbrauchsbalken, Kost2-Label und Auftragspositionen (aus
`TaskServicesRest.createDefaultColumnDefs`, mit denselben Sichtbarkeitsregeln –
Kost2 nur bei `isCostConfigured`, Aufträge/„Schutz bis" nur für FiBu), Suchfeld
(debounced), die vier Status-Häkchen (`opened`/`notOpened`/`closed`/`deleted`),
serverseitiges Auf-/Zuklappen über die User-Prefs (`TaskTree.USER_PREFS_KEY_OPEN_TASKS`
– Aufklappen ist ein Request, kein Client-State), Wurzelknoten nur für Admin/FiBu,
Klickverhalten „Titelspalte klappt auf, andere Spalte wählt", markierte Zeile
inkl. geöffneter Vorfahren, gelöschte durchgestrichen, Auswahlmodus und die
URL-basierte Spaltenzustands-Persistenz (`tree/setColumnStates`,
`tree/resetGridState`). Gegen das laufende System geprüft: `e2e/task-tree.spec.ts`
(4 Fälle).

### Lücke 1 – Routing/Menü (in Schritt 5 erledigt)

Ausgangslage war: `MenuItemDefId.TASK_TREE` stand auf `"wa/taskTree"`, `task` fehlte in
`NextMigration.MIGRATED`, `task` fehlte in `lib/hand-built-categories.ts`
(`NextMigrationTest` liest die `.ts` per Regex und erzwingt den Gleichstand).

**Die Entscheidung, so umgesetzt:** der Baum bleibt auf `/next/taskTree` (keine REST-Kategorie,
eine eigene konkrete Route), und `task` ist als **normale Entität** eingetragen –
`route = "task"` für die Listenperspektive wie bei jeder anderen Seite,
`editRoute = "task/:id"`, `newEntryRoute = "task/new"`. Damit zeigt kein
`PagesResolver`-Redirect für `task` auf den Baum, und der Konflikt „Baum kapert die
Listen-URL" entsteht nicht – ein Redirect nach dem Speichern darf nicht im Baum landen.

Der Menüeintrag `TASK_TREE` zeigt weiter auf den Baum, aber nicht hart verdrahtet: dafür
gibt es neu `NextMigration.nextRouteUrl(category, route, legacyUrl)` – die **zweite
Perspektive** einer schon migrierten Entität, unter einer Route, die die Kategorie nicht
hergibt. Solange `task` nicht migriert ist, antwortet die Funktion mit der Legacy-URL, die
Menü-URL bleibt also an `MIGRATED` gekoppelt (die Invariante, auf die die Klassen-Doku
besteht) statt auf `next/taskTree` festgenagelt zu sein.

Die frühere Notiz, `legacyRoute` müsse explizit auf `taskTree` gesetzt werden, ist
damit hinfällig – und war auch so nicht richtig: Wicket mountet `taskList`/`taskEdit`,
also genau das, was `LegacyApp.WICKET.listRoute`/`editRoute` aus der Kategorie bauen.
Für einen `task`-Eintrag braucht es **kein** `legacyRoute`.

### Lücke 2 – die Aktionen der Baumseite (`TaskTreePage.init()`, `TaskTreeForm`)

| Aktion                                                                                           | Backend                                                                                                                                                                                                                                                          |
| ------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Neue Aufgabe** (`+`, Access-Key)                                                               | `PUT /rs/task/saveorupdate` vorhanden – es fehlt die Zielseite (s. Lücke 3)                                                                                                                                                                                      |
| **Favoriten** (`UserPrefArea.TASK_FAVORITE`)                                                     | `TaskFavoritesRest` (`/rs/task/favorites/list\|create\|select\|delete\|rename`) vollständig, in next unbenutzt. Achtung: auch die schreibenden Aufrufe sind dort `@GetMapping`                                                                                   |
| **Aufgaben-Assistent** (nur Admin)                                                               | `TaskWizardPageRest` ist ein Torso: `UIAlert("To-do: watchfields, create new entities, show no action")`, Abbrechen redirectet hart auf `/wa/taskTree`. Vorlage ist `TaskWizardForm.java` (Aufgabe + Manager-Gruppe/Team/externe Gruppe, Gruppe/Aufgabe anlegen) |
| **Reindex** (nur Admin)                                                                          | `AbstractEntityRest.reindexFull` vorhanden                                                                                                                                                                                                                       |
| **Listenansicht** (Umschalten Baum ↔ Tabelle, `TaskListForm` mit `task.tree.perspective` zurück) | `createListLayout` hat **eine** Spalte, die Wicket-Liste zehn – erst das Listen-Layout füllen                                                                                                                                                                    |
| **Filter zurücksetzen**                                                                          | kein Endpunkt; `TaskFilter.reset()` ist Wicket-intern. Hängt an der offenen `filter/reset`-Lücke aus Phase 1.5                                                                                                                                                   |
| **Lucene-Hilfe am Suchfeld** (`tooltip.lucene.link` → `Constants.WEB_DOCS_LINK_HANDBUCH_LUCENE`) | reine Frontend-Arbeit                                                                                                                                                                                                                                            |

Kleinigkeit dazu: der Hinweistext unten nutzt `task.selectPanel.info` (Text des
Auswahl-Panels); die Baumseite hat mit `task.tree.info` einen eigenen.

Diese Tabelle ist die Bestandsaufnahme und bleibt so stehen; abgearbeitet ist sie in
Schritt 3 der Reihenfolge unten – bis auf **Favoriten** (bewusst ausgelassen). Der
**Aufgaben-Assistent** ist in Schritt 4b nachgezogen (`/next/taskWizard`).
„Filter zurücksetzen" hängt _nicht_ an
der `filter/reset`-Lücke, wie hier vermutet: der Baum hat seinen eigenen Filter, s. Schritt 3.

Die **Listenansicht** ist in Schritt 4a erledigt – und die Vermutung „erst das Listen-Layout
füllen" war falsch. Das `UITable`-Layout bleibt einspaltig; die zehn Spalten sind im `PageDef`
deklariert, wo die Spalten einer handgebauten Liste hingehören. Der Umschaltknopf steht in
beiden Richtungen.

### Lücke 3 – die Edit-Seite, handgebaut als `PageDef`

Der Zeilenklick geht heute auf `listMeta.legacyEditPage` → `wa/taskEdit?id=…`.
Ziel ist eine handgebaute Seite wie `book`/`order` (`docs/page-declarations.md`),
**nicht** der Dynamic-Renderer: dessen Entity-Picker sind nicht migriert
(`dynamic-input-resolver.tsx:15`), und `createEditLayout` deckt nur
`parentTask, title, status, priority, responsibleUser, shortDescription,
reference, description, protectTimesheetsUntil` ab – also weder den Gantt- noch
den Kost2-Block.

Was schon trägt: `lib/metadata/task.generated.ts` hat alle 26 Felder mit
`required`/`maxLength`/`enumValues` (nie im Frontend wiederholen),
`historizable: true` bringt den History-Reiter mit, `DeclaredFormField` deckt
`STRING`/`INT`/`DECIMAL`/`DATE`/`BOOLEAN` und die Enums ab, `SEARCH_ENTITY`
kennt `TASK`/`USER`/`COST2`, und `TaskSelectField` (Modal-Baum) steht für
`parentTask`/`ganttPredecessor` bereit.

Die Metadaten sind allerdings an sechs Stellen falsch – nicht in next, sondern in den
Annotationen des `TaskDO`, und damit auch in Wicket und `/react/task`:
`workpackageCode`, `ganttPredecessorOffset`, `ganttRelationType`, `ganttObjectType`
und `ganttPredecessor` tragen alle fünf `@PropertyInfo(i18nKey = "task.parentTask")`,
würden also als „Übergeordnete Aufgabe" beschriftet. `kost2IsBlackList` hat gar keine
`@PropertyInfo` und fehlt dadurch in `lib/metadata/task.generated.ts` vollständig.
Vier der fünf richtigen Keys existieren im Bundle (`gantt.objectType`,
`gantt.predecessor`, `gantt.predecessorOffset`, `gantt.relationType`); für
`workpackageCode` gibt es in **keinem** der beiden Bundles einen `workpackage*`-Key,
der muss neu angelegt werden.

Was fehlt, gegen `TaskEditForm.java` (481 Z.) und `TaskEditPage.java`:

1. **DTO unvollständig und in drei Feldern stumm defekt** (`rest/dto/Task.kt`): kein
   `projekt`/`kost2List` für den Kost2-Block. `startDate`/`endDate`/
   `protectTimesheetsUntil` sind dort `java.util.Date`, im `TaskDO` aber `LocalDate` –
   und das ist schlimmer als ein falsches Draht-Format: `BaseDTO.copy` kopiert nur bei
   `srcField.type == destType`, sonst landet das Feld im Zweig
   `log.debug("Unsupported field to copy …")`. Die drei Felder werden also **in
   keiner Richtung übertragen**; `/react/task` zeigt „Schutz bis" leer und speichert
   es leer. Mit `LocalDate?` ist nichts weiter zu tun: `LocalDateSerializer`/
   `-Deserializer` sind global registriert (`JsonUtils.initializeMapper`) und
   schreiben `yyyy-MM-dd` – genau was `components/shared/date-input.tsx` erwartet
   (wie `Book.lendOutDate`, `Vacation`, `Auftrag`).
2. **Feldweise Schreibrechte.** `TaskDao.hasAccessForKost2AndTimesheetBookingStatus`
   schaltet in Wicket `kost2BlackWhiteList`, `kost2IsBlackList` und
   `timesheetBookingStatus` read-only, `protectTimesheetsUntil` und
   `protectionOfPrivacy` nur für die FiBu-Gruppe. Das Muster steht seit dem
   Auftragsformular (`vollstaendigFakturiertWriteAccess`, s. „Erledigt:
   Zugriffsrechte im Formular"): ein Flag pro **Regel**, nicht pro Feld – die DAO
   kennt hier genau zwei. Die Ablehnung selbst kommt korrekt als **406** an
   (`AbstractPagesRestUtils.handleException`), der frühere 200-mit-Toast-Befund
   betraf nur die Endpunkte hinter `postEntityAction`. Was fehlt, ist das
   `causedByField` an den `AccessException`s der `TaskDao` – ohne das landet der
   Fehler im allgemeinen Bereich der Form statt am Feld. Weiterhin gilt: `PageDef`
   hat kein `readOnlyWhen` und `readOnly` erreicht nur `NumberField`, die
   betroffenen Felder müssen also handgerendert werden wie im Auftragsformular.
3. **Cross-Feld-Validierung:** `duration` und `endDate` schließen sich aus
   (`gantt.error.durationAndEndDateAreMutuallyExclusive`) – in Wicket ein
   `IFormValidator`, im Backend **nirgends** (`TaskPagesRest.validate` ist leer).
   Gehört ins Backend, nicht ins Zod-Schema.
4. **Zwei zugeklappte Sektionen:** `task.gantt.settings` (ganttObjectType,
   startDate, endDate, progress 0–100 %, duration 0–10000, ganttPredecessorOffset,
   ganttRelationType, ganttPredecessor) und `financeAdministration`.
   `SectionCard` braucht dafür einen collapsed-Zustand.
5. **Kost2-Block ist ein Custom-Fall:** Anzeige `projekt.kost + ".*"` mit Tooltip
   über die aufgelösten Kost2-Nummern, Freitextfeld `kost2BlackWhiteList`,
   Weiß/Schwarz-Umschalter und ein auf das Projekt vorgefilterter Kost2-Picker, der
   Nummern **anhängt** (`TaskHelper.addKost2`). Braucht einen Server-Roundtrip; heute
   gibt es keinen Endpunkt dafür – und nachbauen in TypeScript ist keine Option:
   `addKost2` hängt nur die zweistellige `Kost2ArtDO`-Id an, wenn die Nummer mit der
   Projekt-Kost beginnt – **außer** bei `id == null && parentTaskId != null`, dann die
   vollständige formatierte Nummer. Eine TS-Kopie müsste zusätzlich `KostFormatter`,
   die Projektauflösung über den Baum und das Suffix-Matching von
   `TaskTree.getKost2List` gegen den `KostCache` duplizieren.
6. **Kleinteile:** `maxHours` mit Warn-Tooltip bei zugeordneten
   Auftragspositionen (`task.edit.maxHoursIngoredDueToAssignedOrders`),
   `progress` als Prozentfeld, JIRA-Hinweis an `shortDescription`/`description`,
   `parentTask` nur außerhalb des Wurzelknotens pflichtig.
7. **Fünf Querverweise im Kopfmenü** (`TaskEditPage.addTopMenuPanel`, nur bei
   bestehendem Datensatz): Unteraufgabe anlegen (`PARAM_PARENT_TASK_ID`),
   Zeitbuchung anlegen, Zeitbuchungen anzeigen, Gantt-Diagramm anlegen und im
   erweiterten Menü Zugriffsrechte. Vier davon zeigen auf **unmigrierte** Seiten
   (`wa/timesheetEdit`, `wa/timesheetList`, `wa/ganttEdit`, `wa/accessList`)
   – bleiben also zunächst Links ins Alt-Frontend.

### Reihenfolge

1. **Erledigt.** Backend-Vorarbeit für die Edit-Seite, sechs eigenständige Commits:
   1. die fünf falschen `@PropertyInfo`-Keys im `TaskDO`, `@PropertyInfo` für
      `kost2IsBlackList`, die neuen Bundle-Keys – zusammen mit den regenerierten
      Dateien im _selben_ Commit (`GenerateNextFieldMetadataTest` /
      `GenerateNextI18nMessagesTest` sind Drift-Tests);
   2. `Task.kt`: die drei Datumsfelder auf `LocalDate?`, dazu der tote
      `AddressbookDao` und der `UIInput(… DATE)`-Workaround in `TaskPagesRest` weg
      (den brauchte es nur, weil `lc` den Typ aus dem DTO ableitete);
   3. `causedByField` an den vier readonly-`AccessException`s der `TaskDao`;
   4. die Zugriffs-Flags am `Task`-DTO. `writeAccess`/`deleteAccess` sind nicht Sache
      dieser Rest-Klasse: sie kommen aus `EntityAccessSupport` +
      `AbstractEntityRest.getById` und gelten für jede Entität, die das Interface
      implementiert (siehe „Zugriffsrechte im Formular"). Eigen sind nur die beiden
      feldweisen `kost2AndBookingStatusWriteAccess` und
      `protectTimesheetsUntilWriteAccess`, in `transformFromDB` nur bei `editMode`
      gefüllt – die Methode läuft auch pro Listenzeile, und der Kost2-Check kostet eine
      Projektauflösung plus Gruppen-Lookup. **Falle:** Wicket fragt bei einer neuen Aufgabe mit dem
      _Eltern_-Knoten; `hasAccessForKost2AndTimesheetBookingStatus` fällt auf
      `obj.parentTaskId` zurück, also muss `newBaseDO(request)` die Eltern-Id aus dem
      Request übernehmen (`parentTaskId`), sonst sieht ein Projektassistent die
      Kost2-Felder gesperrt, obwohl die DAO speichern würde;
   5. `TaskPagesRest.validate`: `duration` ⇔ `endDate` plus die Bereichsregeln, die
      Wicket nur als Feld-Validator hat (`progress` 0–100, `maxHours` 0–9999,
      `duration` 0–10000) – eigener Commit, weil `validate` auch für `/react/task`
      läuft und alleine rückrollbar sein muss;
   6. der Kost2-Kontrakt: `GET /rs/task/info/{id}` um das aufgelöste Projekt und
      `costConfigured` erweitern, dazu **ein** `POST /rs/task/kost2Preview`
      (`{id, parentTaskId, kost2BlackWhiteList, kost2IsBlackList, addKost2Id?}` →
      `{kost2BlackWhiteList, projektKost, kost2WildCard, kost2ListAsLines}`), das
      Anhängen und Vorschau in einem Roundtrip macht – nach einer Auswahl braucht der
      Client die neue Vorschau ohnehin. Serverseitig dieselben drei Aufrufe, die
      `addKost2List` schon macht. Auf `TaskServicesRest`, nicht auf `TaskPagesRest`
      (das bleibt ein reines `AbstractDTOPagesRest`). Der Picker wird über einen neuen
      Request-Parameter `projektId` an `cost2/autosearch` vorgefiltert
      (`Kost2PagesRest.queryAutocompleteObjects` liest mit `onlyActiveEntries` schon
      genau so einen Extra-Parameter) – nicht über Wickets `"nummer:<kost>.*"`, das das
      Nummernformat in die URL zieht und serverseitig neu geparst wird. Für Schritt 2
      gemerkt: `EntityAutocompleteField` hat `${entity}/autosearch?search=` fest
      verdrahtet und braucht eine Extra-Parameter-Möglichkeit.

   Unterwegs mitgefunden: `AbstractTestBase` ersetzte `PfCaches.instance` durch
   `internalSetupForTestCases()`, also durch Caches ohne jede Injektion – wer danach an
   `PfCaches.instance` kam, lief in ein nicht initialisiertes `persistenceService`
   (`Kost2DO.effectiveKostentraegerStatus` tut das). Dass es nur in manchen Modulen auffiel, lag
   an der Reihenfolge der Bean-Erzeugung. In Spring-Tests bleibt jetzt die verdrahtete Bean
   stehen; `internalSetupForTestCases()` bleibt für `TestSetup`, das keinen Kontext hat.

   Nicht live geprüft: die _Ablehnungen_ aus 1.3/1.4. Der Testaccount ist Admin und in den
   FiBu-Gruppen, dort gibt es nichts abzulehnen – dafür stehen die Unit-Tests (`TaskTest`,
   `TaskPagesRestTest`).

2. **Erledigt.** Handgebaute Edit-Seite als `PageDef` inkl. der beiden zugeklappten
   Sektionen und des Kost2-Custom-Felds; Zeilenklick des Baums darauf umlenken.

   Die drei Gerüst-Ergänzungen, die dabei entstanden sind, stehen in [MIGRATION.md](MIGRATION.md), Phase 1.5, unter
   „Erledigt: das Edit-Gerüst für Seiten mit mehreren Aufrufern", die Bereichsregel als
   Nachtrag zu „Validierungsregeln nicht duplizieren". Im Feature selbst:
   `components/features/task/` mit `task-schema.ts` (jede Regel aus
   `lib/metadata/task.generated.ts`, keine einzige davon hier wiederholt),
   `task-values.ts` (`emptyTaskValues()` ist `toFormValues({id: null})` – eine zweite
   Feldliste wäre genau die Drift, die die Normalisierung verhindern soll),
   `task.page.tsx` mit den drei Sektionen und `edit/` mit `finance-section.tsx`,
   `kost2-block.tsx` und `use-kost2-preview.ts`.

   Der Kost2-Block rechnet nichts selbst: die Formularwerte gehen serialisiert und
   entprellt an `POST /rs/task/kost2Preview` (dasselbe „erst serialisieren, dann
   entprellen"-Muster wie `use-order-sums.ts` – ein Selektor, der ein frisches Objekt
   liefert, kommt sonst nie zur Ruhe), und eine Auswahl aus dem auf `projektId`
   vorgefilterten `cost2/autosearch` schickt `addKost2Id` mit und schreibt die
   _geantwortete_ Liste ins Feld. Die fünf zugriffsgeschützten Felder werden gerendert
   und ohne Recht gesperrt, mit der Ablehnungsmeldung des Backends als Hinweis – die
   bewusste Abweichung von Wicket, die schon der „vollständig fakturiert"-Schalter des
   Auftrags eingeführt hat.

   `/next/task` hatte in diesem Schritt noch keine Route (die Liste ist Schritt 4a); `route: "/task"`
   ist trotzdem richtig, es ist die Basis von `${route}/${id}`. Der Zeilenklick des Baums
   geht auf `/task/:id?returnTo=/taskTree`, der `legacyEditPage`-Umweg über
   `window.location` ist weg.

   **Gleichstand mit Wicket ist geprüft** – der Maßstab, den die Verifikation unten für den
   Kost2-Block nennt, und zwar ohne eine Aufgabe der Produktionskopie zu ändern:
   `e2e/task-kost2-preview.spec.ts` vergleicht `kost2ListAsLines` des Baums mit der Antwort
   von `POST /rs/task/kost2Preview` zur _gespeicherten_ Liste derselben Aufgabe. Beide
   Zahlen kommen aus `TaskTree.getKost2List` über `KostHelper.getFormattedNumberLines` –
   genau die drei Aufrufe, aus denen Wicket seinen Tooltip baut –, nur eben über den ganzen
   Baum statt über die eine Aufgabe, die jemand aufgeschlagen hätte. Stand heute: 128
   Aufgaben, 8 mit aufgelösten Kostenträgern, 8-mal gleich, 0 Abweichungen. Dazu Runden über
   dieselbe Aufgabe: leere Liste = alle Einheiten, `"*"` = alle, Schwarzliste = das Komplement
   der Weißliste derselben Einträge, `"  17,  02 ; 02  "` normalisiert zu `"02,17"`, und eine
   unbekannte Id antwortet eine leere Vorschau statt eines Fehlers.

   `e2e/task-edit.spec.ts` deckt die drei Dinge ab, die es nur auf dieser Seite gibt: die
   beiden zugeklappten Sektionen (samt `#sectionId` beim Ankommen), `?returnTo=` in beide
   Richtungen inkl. eines nicht deklarierten Ziels, und den Kost2-Block einer Aufgabe _ohne_
   Projekt – der Zustand, den er überleben muss.

   Nicht lokal prüfbar bleiben die Read-only-Pfade der fünf Felder – das Testkonto ist Admin
   und in den FiBu-Gruppen, `kost2AndBookingStatusWriteAccess` und
   `protectTimesheetsUntilWriteAccess` sind dort immer true. Dafür stehen die Unit-Tests
   aus Schritt 1.

3. **Erledigt.** Aktionsleiste des Baums: Reindex, Neu, Filter zurücksetzen,
   Lucene-Hilfe, `task.tree.info`.

   Vorweg erledigt und eigener Commit: **`fetchNew` kannte keine Parameter.**
   `lib/rs/client.ts` rief `GET /rs/{entity}/newEntry` ohne Query, `TaskPagesRest.newBaseDO`
   liest aber `parentTaskId` (s. die Falle in Schritt 1.4). `fetchNew`/`useEntityDetail`
   nehmen jetzt ein Parameter-Objekt, und welche URL-Parameter überhaupt ans Backend
   dürfen, deklariert die Seite: `EditDef.newEntryParams` (`task`:
   `["parentTaskId"]`), gelesen von `hooks/use-new-entry-params.ts`. Eine Weißliste aus
   demselben Grund wie bei `returnTargets` – dieselbe URL trägt auch `returnTo`, und das
   ist Sache der Seite, nicht des Backends. Die Parameter gehören zum Query-Key, sonst
   liefert der Cache den Datensatz ohne Voreinstellung.

   Die Leiste selbst sitzt **im Panel** (`pageMode`), nicht im Seitenkopf: sie wirkt auf
   Filter und Zeilen, also auf den Zustand des Panels
   (`components/shared/tasks/task-tree-action-bar.tsx`). Inventar ist das Content-Menü von
   `TaskTreePage` plus der Rücksetz-Knopf des Formulars. Der `+`-Knopf ist aus der
   `ListToolbar` nach `components/shared/add-entry-button.tsx` herausgezogen, damit der Baum
   denselben Knopf und dasselbe `N`-Kürzel bekommt statt einer zweiten Schreibweise. Dazu
   pro Zeile eine „Unteraufgabe anlegen"-Aktion (`DataTable.rowActions`, bis dahin ohne
   Aufrufer) – die hat Wicket **nicht**, dessen Baum kann nur unter der Wurzel anlegen; die
   Wurzelzeile bietet sie nicht an, denn das tut der `+` der Leiste schon.

   **Zwei Einträge fehlten in diesem Schritt bewusst** (entschieden: „beide erst mal
   auslassen"), weil next sie nicht selbst bedienen konnte: die **Favoriten**
   (`UserPrefListPage` für `UserPrefArea.TASK_FAVORITE`, `TaskFavoritesRest` ist vollständig,
   aber es fehlt die Verwaltungsseite) und der **Aufgaben-Assistent** (`TaskWizardPageRest`
   war ein Torso, s. Lücke 2). Der Assistent ist in Schritt 4b nachgezogen und steht wieder
   im Zahnrad-Menü; die Favoriten bleiben über den Legacy-Link im Seitenkopf erreichbar.
   Wickets
   **Listenansicht**-Knopf fehlte in diesem Schritt noch – er ist in Schritt 4a ein Link
   und ein zweites `returnTarget` geworden.

   **„Filter zurücksetzen" ist nicht `filter/reset`.** Der Endpunkt
   (`AbstractEntityRest.resetListFilter`) räumt den gespeicherten `MagicFilter` der Entität
   samt Grid-State auf; der Baum filtert aber mit einem eigenen `TaskFilter` unter eigenem
   Session-Key (`ListFilterService.getSearchFilter(…, filterKeySuffix(selectMode))`). Also
   setzt der Baum seinen Filter clientseitig auf `DEFAULT_TASK_TREE_FILTER`
   (`useTaskTree.resetFilter`), und das Backend übernimmt ihn beim nächsten
   nicht-initialen Aufruf, weil `TaskServicesRest.getTree` dessen Parameter als neuen
   Benutzerfilter liest. `ListGearMenu.onFilterReset` ist dafür optional geworden: eine
   Seite ohne gespeicherten `MagicFilter` bekommt nur die Reindex-Einträge.

   **Notiert, nicht geändert:** Wickets Hilfe-Icon am Suchfeld trägt `tooltip.lucene.link`
   und verspricht damit mehr als das Feld kann – `TaskFilter.isVisibleBySearchString` ist ein
   `StringUtils.containsIgnoreCase` über sieben Spalten (Titel, Referenz, Kurzbeschreibung,
   Beschreibung, Anzeigename, Name/Kennung des Verantwortlichen, Workpackage-Code), keine
   Lucene-Abfrage. Gleichstand ist gewollt (das verlinkte Kapitel erklärt die Volltextsuche
   der Listenseiten), die Formulierung zu korrigieren wäre eine Änderung am Bundle, das
   Wicket mitbenutzt – im Code vermerkt, `lib/docs-links.ts` spiegelt
   `Constants.WEB_DOCS_*`.

   Kleinigkeit aus Lücke 2 mit erledigt: der Hinweis unten liest auf der Seite jetzt
   `task.tree.info` und nur im Auswahl-Panel `task.selectPanel.info`.

   `e2e/task-tree-actions.spec.ts` deckt die Leiste ab: `+` → `/task/new?returnTo=%2FtaskTree`,
   Zeilenaktion → `/task/new?parentTaskId=…` **und** das vom Backend voreingestellte
   Elternteil im Formular (die URL allein würde nichts beweisen), keine Aktion an der
   Wurzelzeile, das Zahnradmenü mit Reindex + Rücksetzen, das Rücksetzen über einen Reload
   (also serverseitig angekommen), der Handbuch-Link, und das Auswahl-Panel _ohne_ all das.
   Die gemeinsamen Baum-Handgriffe liegen jetzt in `e2e/fixtures/task-tree.ts`.

4. **a) Erledigt.** Listenperspektive: die zehn Spalten von `TaskListPage.createColumns`
   als `PageDef`-Deklaration, plus die Route `/next/task` und die beiden
   Perspektiv-Knöpfe.

   Sieben Spalten sind Felder des DTOs und tragen **keinen** `labelKey` – die Beschriftung
   ist der `i18nKey` des Feldes aus `lib/metadata/task.generated.ts` (`labelKeyFor` in
   `use-declared-columns.tsx`). Nur die drei berechneten müssen einen nennen; die
   Titelspalte nennt `task._` („Strukturelement"), weil beide Wicket-Seiten diese Spalte
   mit dem Namen der Entität überschreiben, nicht mit dem des Feldes.

   **Drei Werte stehen nicht am `TaskDO`** und kommen aus demselben Baum, aus dem die
   Baumperspektive sie rechnet – `Task.copyFrom4ListRow` ruft `Consumption.create`,
   `TaskServicesRest.addKost2List` (ohne die Kostenträger-Objekte, die nur der Picker des
   Baums braucht) und `addOrderList`. Dieselben Funktionen, also können beide
   Perspektiven nie verschiedene Zahlen zeigen (auch nicht denselben Link: die
   Consumption-Bar führt in beiden auf die Zeitberichte des Strukturelements, s. eigener
   Abschnitt „Offen: Die Consumption-Bar zeigt noch auf Wicket" in
   [MIGRATION.md](MIGRATION.md)); und es kostet keine Abfrage, weil der
   Baum im Speicher liegt und die Auftragspositionen nach Task-Id cacht. Die lean row
   lässt alles weg, was nur das Formular braucht (`description`, die geschachtelten
   Tasks, die Zugriffs-Flags) – `JsonInclude.Include.NON_NULL` hält es damit pro Zeile
   von der Leitung. Die Renderer selbst sind die des Baums, über `renderCell` aus
   `components/data-table` adaptiert (`task-list-cells.tsx`), damit es pro Zelle eine
   Implementierung bleibt.

   **Die Sichtbarkeitsregeln sind die des Baums**, nicht eine Kopie:
   `TaskServicesRest.columnVisibility()` ist als `TaskColumnVisibility` herausgezogen, und
   `TaskPagesRest.addVariablesForListPage` beantwortet die drei Flags, auf die die Liste
   gattert (`kost2Configured`, `orders`, `protectTimesheetsUntil`). Serviert statt im
   Client abgeleitet, aus demselben Grund wie die Zugriffs-Flags: Gruppenmitgliedschaft
   und Systemkonfiguration weiß das Backend.

   **Sortierbar sind nur die sieben Feldspalten** (`sortable: false` an den drei
   berechneten) – die Liste läuft über den `MagicFilterProcessor`, sortiert also nach
   Entity-Property, und Wickets Liste sagt dasselbe, indem sie diesen drei Spalten keine
   Sort-Property mitgibt. Die Verantwortlichen-Spalte sortiert nach
   `responsibleUser.lastname`: der angezeigte Name ist zusammengesetzt
   (`PFUserDO.displayName` ist `@Transient`), und Wickets eigene Sortierung nach
   `responsibleUserId` ordnet nach etwas, das niemand sieht.

   **Abweichung zwischen den beiden Wicket-Seiten, nicht hier entschieden:** der _Baum_
   versteckt `reference` und `priority` zusätzlich, wenn keine Aufgabe im ganzen Baum
   einen Wert hat; Wickets _Liste_ zeigt beide immer. Die next-Liste folgt der Liste.

   Zwei neue Vokabeln im `ColumnBase` (in Phase 1.5 von [MIGRATION.md](MIGRATION.md) registriert): `visible` (die Seite
   _hat_ die Spalte nicht – anders als eine im Spaltenpanel abgewählte) und
   `sortable: false`.

   Wickets **Listenansicht**-Knopf ist jetzt beidseitig da:
   `components/shared/tasks/task-perspective-link.tsx` in beide Richtungen, in der Leiste
   des Baums und über `PageDef.listActions` in der Toolbar der Liste. Die Rückrichtung ist
   mit `task.title.list` beschriftet – Wickets eigener Knopf liest das unübersetzte Modell
   `"listView"`, ein Fehler dort und kein Text zum Abschreiben. Dazu die Liste als
   **zweites** `returnTarget`; der Baum bleibt das erste und damit das Ziel einer
   Add-URL ohne `returnTo`. Genau deshalb musste `useEditTargets` lernen, den Parameter
   selbst zu setzen (siehe Phase 1.5 in [MIGRATION.md](MIGRATION.md)): sonst schickte ein Abbruch in der aus der Liste
   geöffneten Aufgabe den Nutzer in den Baum.

   `createListLayout` bleibt einspaltig, mit Begründung im Code: die Filterzeile kommt aus
   `baseDao.searchFields` über `LayoutListFilterUtils` und nicht aus dem Layout, und die
   Spalten einer handgebauten Liste gehören ins `PageDef` – sie tragen eigene Zellen,
   eigene Sichtbarkeit und eigene Breiten, was keine `UITable`-Spalte ausdrücken kann.
   Zehn `UITable`-Spalten würden nur `/react/task` dienen, wohin das Menü nicht mehr
   zeigt.

   `e2e/task-list.spec.ts` prüft die Spalten **gegen die Deklaration und die Metadaten**
   statt gegen abgeschriebene Beschriftungen, und jede bedingte Spalte gegen ihr Flag aus
   `listMeta.variables` (nicht gegen die Annahme, dieses Konto habe alle drei) – dazu die
   drei berechneten Werte gegen die Antwort des Baums für dieselbe Aufgabe, das Fehlen des
   Sortier-Angebots an genau den drei Spalten, `returnTo` in beide Richtungen und die beiden
   Perspektiv-Links. Die Spalten werden dabei über das Element gefunden, das
   `DataTableColumnHeader` als seinen Text markiert (`data-overflow-text`), nicht über den
   Accessible Name der Zelle: die trägt auch Filterknopf und Breiten-Griff, deren Namen in
   ihren eingehen. Und „sortierbar“ ist hier kein Knopf – diese Tabelle sortiert per Klick
   auf die _ganze_ Kopfzelle (ein Knopf um die Beschriftung würde dem Filtersymbol den Platz
   nehmen), also wird das Anzeichen geprüft, das der Kopf zeigt, solange er sortieren kann. Auf `TaskPagesRestTest`-Seite eine Runde über `copyFrom4ListRow`: die
   zehn Spalten samt `created`/`lastUpdate` sind da, `description`, die Kost2-Liste, das
   Elternteil und die Zugriffs-Flags nicht.

   Nicht lokal prüfbar bleibt der _versteckte_ Zustand der drei bedingten Spalten – das
   Testkonto ist Admin und in den FiBu-Gruppen, und die Installation hat Kostenträger
   konfiguriert, also sind alle drei Flags wahr. Deshalb prüft der Spec jede Spalte gegen
   ihr Flag; die Regeln selbst sind die des Baums (dieselbe Funktion, keine Kopie).

   Unterwegs mitgefunden und mit erledigt: `TaskPagesRestTest.validate refuses what the
Wicket form refuses` war seit `ac46dda0b` rot. Die Bereichsregeln sind dort von
   `TaskPagesRest.validate` an `@PropertyInfo(min/max)` am `TaskDO` gewandert und werden
   generisch von `ValidationUtils.validateFields` über das **DO** geprüft – der Test hat
   sie weiter am DTO-Override erwartet. Die Ränder stehen jetzt als eigener Test gegen
   `validateFields`, der Override-Test nur noch für die eine Regel, die Wicket exklusiv
   hatte.

   **b) Erledigt: Aufgaben-Assistent** (`/next/taskWizard`). `TaskWizardPageRest` war
   schlimmer als ein Torso – es hatte nur `getForm`, und den `execute`-Endpunkt, auf den der
   eigene Fertig-Knopf postete, **gab es nicht**; nichts rief `/rs/taskWizard/dynamic` auf,
   also ist die Klasse gelöscht statt umgebaut. Die eigentliche Arbeit stand in
   `TaskWizardPage.create` und steht jetzt in
   `projectforge-business/.../task/TaskWizardService.kt`: den Baum hochlaufen und
   `GroupTaskAccessDO`-Zeilen schreiben (Leiter/Mitarbeiter/Extern rekursiv auf dem gewählten
   Element, Gast ohne Rekursion an jedem Vorfahren, an der Wurzel nichts). Als Service in
   `projectforge-business`, weil die Regel damit prüfbar ist –
   `TaskWizardServiceTest` deckt die fünf Fälle ab (Vorfahren, Wurzel, zweiter Lauf,
   gelöschter Eintrag, keine Gruppe). Darüber ein dünnes `TaskWizardRest`
   (nur `POST execute`; das frühere `GET info` lieferte allein die Legacy-URL des
   „Gruppe anlegen"-Links und ist mit ihm gelöscht), über
   `accessChecker.checkIsLoggedInUserMemberOfAdminGroup()` – Wicket zeigte den Eintrag nur
   Admins, prüfte es aber serverseitig nie.

   Kein `UILayout`: die Seite ist handgebaut
   (`components/features/task/wizard/`, Zustand als schlichtes `useState` wie beim Login und
   beim Passwort-Reset, s. Formular-Regeln in `projectforge-next/CLAUDE.md`). Ein neues
   Primitiv war nicht nötig – die drei Gruppen-Picker sind `EntityAutocomplete` auf
   `group/autosearch`, der Aufgaben-Picker ist der aus `TaskSelectField` herausgezogene
   `TaskSelectControl` samt `TaskSelectModal`. Alle Texte lagen als `task.wizard.*` schon im
   Bundle.

   **Zwei bewusste Abweichungen von Wicket.** (1) Der Hinweis
   `task.wizard.action.taskAndgroupsGiven` erscheint erst, wenn Element **und** mindestens
   eine Gruppe gesetzt sind; Wickets `actionRequired()` prüft nur das Element und
   widerspricht damit seinem eigenen `noactionRequired`-Text. (2) Der Gruppenname ist
   **wirklich vorbelegt** (`<Titel>`, `<Titel>-pm`, `<Titel>-external`), nicht bloß zum
   Abschreiben angezeigt – Wicket gab `GroupEditPage.PARAM_GROUP_NAME` mit, das
   React-Formular liest keinen solchen Parameter.

   **Beide „anlegen"-Wege kehren zum Assistenten zurück** – gemeldet war, dass eine im
   Assistenten angelegte Gruppe hinterher nicht ausgewählt ist (der Link ging in einen neuen
   Tab der Legacy-Seite):
   - **Gruppe:** im Dialog auf der Seite selbst, nicht auf der Gruppenseite –
     `components/shared/edit/entity-edit-dialog.tsx` mit `page={GROUP_PAGE}`,
     `prefill={{ name: suggestGroupName(…), localGroup: true }}` und einem `onSaved`, das die
     neue Gruppe zum Wert des Schritts macht. Es ist also das handgebaute Gruppenformular
     selbst, nicht mehr das `UILayout` des Servers (s. „Gruppen" in [MIGRATION.md](MIGRATION.md)):
     zwischenzeitlich stand hier ein `DynamicFormDialog` um `GET /rs/group/edit`, weil die
     Gruppenseite noch Legacy war – der ist mit der Migration von `group` gelöscht.
   - **Strukturelement:** weiter auf der eigenen Seite – das Formular ist handgebaut, also
     kein `UILayout` und kein Dialog –, aber der Rückweg trägt jetzt die ID mit
     (`/taskWizard?savedId=…`, s. `EditReturn.savedRoute` und `savedIdParam` am dritten
     `returnTarget` des Formulars), und die bereits gewählten Gruppen überleben den Umweg in
     einem Modul-Wert (`wizard-handover.ts`, dasselbe Muster und dieselbe Begründung wie
     `use-pending-clone.ts`: die URL trägt die Tatsache, das Modul die Nutzlast).

   **Dafür nötig, und darüber hinaus nützlich:** `DynamicInputResolver` rendert
   `INPUT`-Elemente mit `dataType` `USER`/`GROUP`/`EMPLOYEE`/`COST1`/`COST2`/`KONTO` jetzt
   über `dynamic-entity-input.tsx` (auf `EntityAutocomplete`, Endpunkt aus dem Typ:
   `user/autosearch` …) statt als `DynamicFallback` – wie Legacy es mit seinem `ObjectSelect`
   tut. Aufgefallen ist die Lücke am `groupOwner` des damals noch server-gelayouteten
   Gruppenformulars; die Elementtypen bleiben für die ~36 UILayout-Kategorien nötig, auch
   wenn dieser eine Aufrufer inzwischen handgebaut ist. Offen bleiben `TASK`, `LOCALE`,
   `TIMEZONE`, `PICTURE` und `CUSTOMIZED`.

5. **Erledigt: Umschaltung, Menüeintrag inklusive.**
   `NextMigration.MIGRATED["task"]` und `lib/hand-built-categories.ts` sind zusammen
   umgeschaltet (`NextMigrationTest` erzwingt es), also gehen Redirects und Editier-URLs der
   Kategorie `task` nach next. `MenuItemDefId.TASK_TREE` steht auf
   `NextMigration.nextRouteUrl("task", "taskTree", "wa/taskTree")` – der Baum und nicht die
   Liste der Kategorie, Einzelheiten in Lücke 1 oben. Die Aufgaben-Favoriten halten das
   nicht auf – sie sind für next gegenstandslos, s. „Bewusst nicht vorgesehen" oben. Wicket
   bleibt über die Fluchtluke am Seitentitel erreichbar (`NextMigration.legacyListUrl`).

   Gegen das laufende System geprüft (nach `npm run build` +
   `:projectforge-next:copyNextBuild`, mit damals umgelegtem Menüeintrag): der Menüeintrag
   antwortete mit `next/taskTree`,
   `POST /rs/task/list` liefert die **schlanke** Zeile in 3,9 s statt der vollen DTOs in
   32 s – 14 Felder pro Zeile, `description`/`kost2BlackWhiteList`/`parentTask` sind nicht
   darunter –, und die drei berechneten Spalten sind gefüllt (`consumption` in 18010,
   `kost2WildCard`/`kost2ListAsLines` in 10100, `orderList` in 3351 von 19717 Zeilen).
   `listMeta.variables` antwortet mit allen drei Flags wahr, wie für dieses Konto erwartet.

**Verifikation.** Aufgabe anlegen/verschieben/schließen, Gantt- und
Kost2-Felder mit _und_ ohne FiBu-Recht (die Ablehnung muss als Fehler ankommen,
nicht als Erfolg), Suche und Klappzustand über einen Reload, Auswahlmodus aus der
Auftragsposition, History. Maßstab für den Kost2-Block ist **Gleichstand mit Wicket** –
für Schritt 2 erledigt und als `e2e/task-kost2-preview.spec.ts` festgehalten, das den
Vergleich gegen `kost2ListAsLines` des Baums führt statt gegen abgeschriebene
Tooltip-Zeilen (dieselben drei Aufrufe, und keine Änderung an einer Aufgabe nötig). Die
*Ablehnungs*pfade sind lokal nicht prüfbar (das Testkonto ist Admin und in den
FiBu-Gruppen) – dafür Unit-Tests.
