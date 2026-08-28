# Migrationsplan: Kalenderseite (`/react/calendar` → `/next/calendar`)

Detailplan zu [MIGRATION.md](MIGRATION.md). Die Umsetzung ist **weitgehend
abgeschlossen**; dieses Dokument hält die ursprüngliche Analyse fest und
dokumentiert im folgenden Abschnitt den erreichten Stand.

**Stand:** Kalenderseite komplett; Timesheet-Edit **und Timesheet-Liste** fertig; TeamEvent-Edit
in den Phasen A (Grundgerüst), B (Reminder) und D (Recurrence/Serientermine) umgesetzt und verdrahtet.
**Offen** und bewusst zurückgestellt: TeamEvent-**Attendees** (Phase C, _später wenn überhaupt_),
die **TeamEvent-List-Page**, der Timesheet-**PDF-Export** (noch wicket-gekoppelt) und der
Timesheet-**Vorlagen**-Button sowie die Extraktion von `shared/async-entity-multi-select`.

## Todo-Liste (Umsetzungsstand)

Abgehakt = im Code vorhanden und dem Plan entsprechend umgesetzt.

### Renderer-Voraussetzungen (Blocker)

- [x] 2-Segment-Route `app/(authenticated)/[category]/[type]/` (`page.tsx` + `page-client.tsx`)
- [x] `route-params.ts` behandelt den No-Rest-Fall (`rest.length === 0`)
- [x] `fetchDynamic` in `lib/rs/client.ts` reicht `search?:` durch (startDate/endDate/firstHour)
- [x] Extraktion nach `components/dynamic/dynamic-form-page.tsx`

### Backend-Vertrag & RS-Client

- [x] `lib/rs/calendar-types.ts` (DTO-Spiegel)
- [x] `lib/rs/calendar.ts` (Wrapper über `request()`, alle `change*`- und Favoriten-Aufrufe)

### Kalenderseite & Feature-Komponenten

- [x] `app/(authenticated)/calendar/page.tsx` + `page-client.tsx`
- [x] `calendar-page.tsx`, `full-calendar-panel.tsx`, `types.ts`, `view-config.ts`
- [x] Hooks: `use-calendar-init`, `use-calendar-events`, `use-calendar-filter-mutations`,
      `use-calendar-favorites`, `use-calendar-state`, `use-calendar-action`,
      `use-goto-date`, `use-view-buttons`
- [x] `calendar-event-content.tsx`, `calendar-event-tooltip.tsx`
- [x] Toolbar-Familie: `calendar-toolbar`, `calendar-select`, `calendar-pill`,
      `calendar-style-popover`, `calendar-favorites-menu`, `calendar-favorite-entry`,
      `calendar-settings-dialog`, `calendar-vacation-selects`, `calendar-more-menu`
- [x] Tooltip-Parser `tooltip-html.ts` ohne `dangerouslySetInnerHTML`

### Shared / generischer Renderer

- [x] `components/shared/color-picker.tsx`
- [x] UICustomized-Registry mit `COLOR_CHOOSER` (`components/dynamic/customized/`,
      `color-chooser.tsx`)
- [ ] `components/shared/async-entity-multi-select.tsx` – **nicht als separater Shared-Baustein
      angelegt**; die Urlaubs-Auswahl steckt in `calendar-vacation-selects.tsx`. Bei einem
      zweiten Nutzer noch nach `shared/` zu extrahieren. Der einzige plausible zweite Nutzer
      (TeamEvent-Attendees, Phase C) ist selbst zurückgestellt – bleibt also offen.

### FullCalendar & Styling

- [x] FullCalendar v6 Deps (`@fullcalendar/*@^6.1.x`) in `package.json`
- [x] shadcn `hover-card` (`components/ui/hover-card.tsx`)
- [x] `.pf-calendar`-CSS-Block in `app/globals.css`

### Menü-Umschaltung & Backend

- [x] `NextMigration.kt`: `"calendar" to NextPage(route = "calendar")`
- [x] `MenuItemDefId.kt`: `CALENDAR` nutzt `getListUrl("calendar")` statt `getReactListUrl`
- [x] `lib/hand-built-categories.ts` enthält `"calendar"`
- [x] `app-sidebar.tsx`-Platzhalter (`href: "#"`) **absichtlich nicht** verdrahtet (plangemäß)

### Tests

- [x] `tooltip-html.test.ts`, `view-config.test.ts`
- [x] `e2e/calendar.spec.ts`
- [ ] Manuelle Verifikation & Live-Backend-Abgleich (siehe Abschnitt „Verifikation") – bei Bedarf durchführen

## Edit-Komponenten (Timesheet & TeamCal-Event)

Der Kalender navigiert zum Bearbeiten auf entity-Edit-Seiten. Ursprünglich war
dafür der generische UILayout-Renderer vorgesehen; stattdessen werden diese
Seiten **handgebaut** analog `book`/`order` (`definePage` + `EntityEditPage`,
`@tanstack/react-form` + Zod aus den generierten Metadaten). Speichern läuft über
das generische `lib/rs/entity.ts` (`saveOrUpdateEntity`), Validierung bleibt
serverseitig (HTTP 406).

### Timesheet – fertig

- [x] `components/features/timesheet/timesheet.page.tsx` (`definePage` mit Edit-Config,
      `returnTargets` zurück zum Kalender, `TemplatesRecentBar` als Banner)
- [x] `edit/timesheet-edit-schema.ts` (Zod aus Metadaten, Cross-Field-Refine start < stop),
      `edit/timesheet-edit-values.ts`, `edit/day-range.ts` (+Test)
- [x] Sections: `task-kost2-section`, `day-range-section`, `location-field`,
      `reference-field`, `templates-recent-bar`
- [x] Route `app/(authenticated)/timesheet/[id]/`, RS-Helper `lib/rs/timesheet.ts`
      (Recents/Favorites/Suggestions)
- [x] `TimesheetDetail`/Schema spiegeln **jedes** DTO-Feld – beim Speichern geht nichts verloren
- [x] **History-Tab.** Rendert automatisch, weil `TIMESHEET_METADATA.historizable === true`
      und `EntityEditPage` `history: page.metadata.historizable` durchreicht. **Keine**
      Redirect-Route `[id]/history/` nötig (anders als book/order): die Legacy-URLs zeigen
      auf `/react/…` bzw. `/wa/…`, nicht auf `/next/timesheet/[id]/history` – siehe unten.
- [x] **List-Page** (`app/(authenticated)/timesheet/page.tsx`) – migriert und geroutet
      (`next/timesheet`); `MenuItemDefId.TIMESHEET_LIST` löst über `listUrl` dorthin auf.
      Umgesetzt (volle Parität zur Wicket-Liste außer den beiden unten genannten Punkten):
  - [x] **Filter-Toggles** `recursive` (Task rekursiv, default an) und `onlyBillable` als
        `UIFilterBooleanElement` in `addMagicFilterElements`; `preProcessMagicFilter`
        fängt beide ab (`synthetic=true`): bei `recursive=false` ein nicht-rekursives
        `QueryFilter.taskSearch`, `onlyBillable` als `TimesheetBillableFilter`
        (`CustomResultFilter`, prüft `kost2.kost2Art.fakturiert` über `PfCaches`)
  - [x] **Footer** (Summe der Dauer + KI-Anteil) über `TimesheetListStatistics`
        (`ResultSet.statistics`, `AITimeSavings.buildStats`) → `TimesheetStatisticsLine`
        im `statistics`-Slot; KI-Zeile nur bei `timeSavingsByAIEnabled`
  - [x] **Export** – Excel (`exportAsExcel` → `downloadTimesheetExcel`) und ics-Abo-Dialog
        (`getIcsExportUrl` → `TimesheetIcsDialog`) im `listActions`-Slot (`TimesheetListActions`)
  - [x] **Mehrfachauswahl** – `massUpdate` (`timesheetSelected`, Route `/timesheet/mass-update`)
  - [ ] **PDF-Export** bewusst zurückgestellt – der Formatter ist an das
        `projectforge-wicket`-Modul gekoppelt; bleibt vorerst in Wicket
  - [ ] **Vorlagen-Button** bewusst nicht migriert (laut Vorgabe außen vor)

### TeamCal-Event – Phasen A/B/D umgesetzt, C (Attendees) offen

Aktiver Backend-Vertrag: `TeamEventPagesRest` + DTO `TeamEvent.kt`. `calEvent` ist nur ein
abgeschaltetes Flag (`calendar.useNewCalendarEvents=false`) **ohne** Controller/DTO – ignorieren.

**Phase A – lauffähiges Grundgerüst (fertig):**

- [x] Feature-Gerüst analog Timesheet: `teamEvent.page.tsx`, `types.ts`,
      `edit/team-event-edit-schema.ts`, `edit/team-event-edit-values.ts`,
      Route `app/(authenticated)/teamEvent/[id]/` (`page.tsx` + `page-client.tsx`)
- [x] Scalare Sections: `subject`, `location`, `note`, `startDate`/`endDate`, `allDay`
      (`date-range-section.tsx` schaltet start/end per allDay zwischen `DateTimeInput` und
      date-only `DateInput` und verankert die Enden beim Umschalten auf 00:00/23:59)
- [x] **`calendar`-Select** (Pflicht) – eigener `calendar-select-field.tsx`: lädt die
      schreibbaren Kalender per TanStack Query aus neuem Endpoint
      `TeamEventPagesRest.getCalendars` (`@GetMapping("calendars")`, `lib/rs/team-event.ts`),
      Pflichtfeld ohne Clear, hängt einen nicht-schreibbaren Event-Kalender vorne an
- [x] **Kein Datenverlust beim Speichern.** `EntityEditPage.save` postet die Formwerte _als_
      DTO, daher trägt Schema+`toFormValues` **jedes** nicht editierte DTO-Feld unverändert
      durch (recurrence*, attendees, reminder*, organizer\*, ownership, sequence, uid, …). Ein
      Serientermin wird dadurch beim Speichern **laut** abgelehnt (`validate` verlangt
      `seriesModificationMode`), statt still korrumpiert zu werden
- [x] **History-Tab** automatisch (`TEAM_EVENT_METADATA.historizable === true`)
- [x] Backend `newBaseDTO(request)` überschrieben – die hand-gebaute Add-Seite holt `newEntry`
      statt der UILayout-`edit`-Route; wiederverwendet `onBeforeGetItemAndLayout` für die
      startDate/endDate/calendar-Presets aus dem Kalender
- [x] Kalender-Routing: `team-event-route.ts` (`/teamEvent/edit`→`/teamEvent/new`,
      `/teamEvent/edit/<id>`→`/teamEvent/<id>`) + `case "teamEvent"` in `use-calendar-action.ts`
- [x] Registrierung: `lib/hand-built-categories.ts` + `NextMigration.MIGRATED["teamEvent"]`
      (Test `NextMigrationTest` grün), i18n-Katalog regeneriert (`GenerateNextI18nMessagesTest` grün)

Die anfangs zurückgestellten Custom-Widgets (jeweils **nicht** in den Metadaten;
Relationen/`UICustomized`) sind inzwischen bis auf Attendees umgesetzt:

- [x] **Phase B – Reminder** – nachgebaut als `edit/sections/reminder-section.tsx`, verdrahtet in
      `teamEvent.page.tsx` (`reminderDuration`/`reminderDurationUnit`/`reminderActionType`), ersetzt
      das Backend-`UICustomized("calendar.reminder")`
- [ ] **Phase C – Attendees** (`MutableSet<TeamEventAttendeeDO>`) – **bewusst offen, später wenn
      überhaupt.** Ein eigener Multi-Select (idealerweise über einen nach `shared/` extrahierten
      `async-entity-multi-select`, s. o.). Bis dahin trägt das Schema `attendees` verlustfrei als
      `z.array(z.unknown())` durch (`team-event-edit-schema.ts:90`, `team-event-edit-values.ts:50`) –
      bestehende Teilnehmer gehen beim Speichern also nicht verloren, nur editiert werden sie nicht.
- [x] **Phase D – Recurrence / Serientermine** – nachgebaut als `edit/recurrence-*.ts(x)` +
      `edit/sections/recurrence-section.tsx` (repliziert den Legacy-`react-rrule-generator`) und
      `series-modification-section.tsx` (`SeriesModificationMode` ALL/FUTURE/SINGLE, konditional je
      nach Master-Startdatum; rendert für Nicht-Serien nichts). `validate` verweigert weiterhin das
      Speichern eines Serientermins ohne gewählten Modus. Der Rückfrage-Dialog ist damit als
      In-Page-Section gelöst – **offenes Risiko 3** (MODAL-`ResponseAction` als eigene Seite statt
      Overlay) betrifft diesen Pfad daher nicht mehr.
- [x] **Drag/Resize bestehender Events** öffnet den Termin bereits verschoben. `toTeamEventRoute`/
      `toTimesheetRoute` behalten die Query bestehender Events; `calendarPrefill` macht daraus einen
      _dirtyPrefill_ (echte, speicherbare Änderung — im Gegensatz zum klickbasierten, nicht-dirtying
      `prefill`), Team-Event zusätzlich mit `selectedSeriesEvent` aus `origStartDate` für Single/Future.
      Datumsformate ISO (Drag/Resize) und Epoch-Sekunden (Klick) werden beide gelesen (`toIsoInstant`)
- [ ] **List-Page** (`app/(authenticated)/teamEvent/page.tsx`) – bewusst zurückgestellt; der
      Termin wird über den Kalender erreicht. Spalten sind in `teamEvent.page.tsx` bereits
      deklariert, sodass die Liste später nur eine Route ist

### History-Tab: nicht pro Entität anzulegen

Der History-**Tab** ist entity-unabhängig: `EntityEditPage` liest
`history: page.metadata.historizable` aus den generierten Metadaten. Jede
historisierbare Entität (`@WithHistory` → `historizable: true`) bekommt den Tab
automatisch, ohne eine Zeile Deklaration. Einzige Autorität ist das Backend-Flag –
bewusst kein zweiter Ort, an dem es driften könnte.

Die Route `[id]/history/` war **kein** Teil des Tabs, sondern nur ein
Redirect-Überbleibsel für alte **Next**-Deep-Links (`/next/<entity>/<id>/history` →
`?tab=history`) aus einer früheren Next-Iteration, in der die History eine eigene
Seite war. Solche Next-URLs kursieren nirgends – die echten Legacy-URLs zeigen auf
`/react/…` bzw. `/wa/…` (anderer Mechanismus, trifft diese Route nie). Die
Redirect-Routen sind daher toter Code und wurden für alle Entitäten gelöscht (book,
group/cost1, order, invoice, creditor-invoice, task); der Tab funktioniert ohne
sie, und frisch migrierte Entitäten wie Timesheet bekommen sie gar nicht erst. Siehe
den Nachtrag in [MIGRATION.md](MIGRATION.md) („die `[id]/history/`-Routen werden
gelöscht").

## Ausgangslage

Die Kalenderseite ist die Standard-Startseite nach dem Login
(`PagesResolver.getDefaultUrl()`) und liegt bislang ausschließlich im
Legacy-React-Frontend. In `projectforge-next` existiert dazu nichts außer
generierten Feld-Metadaten (`lib/metadata/team-cal.generated.ts`,
`team-event.generated.ts`, `timesheet.generated.ts`) und einem toten
Sidebar-Platzhalter (`components/shared/app-sidebar.tsx:47`, `href: "#"`).

Das Backend ist vollständig vorhanden und liefert bereits FullCalendar-DTOs –
die Migration ist deshalb fast reine Frontend-Arbeit.

### Grundsatzentscheidungen für diese Seite

- **Volle Parität in einem Schritt.** Alle Views, Kalenderauswahl mit Farbwahl,
  Filter-Favoriten, Einstellungen, Tooltips, Drag&Drop/Resize, Slot-Auswahl. Eine
  read-only Zwischenstufe würde die Seite nicht ablösen und den Menü-Schalter
  nicht rechtfertigen.
- **FullCalendar v6** wie im Legacy-Frontend (`@fullcalendar/react`), aber **ohne**
  Bootstrap-Theme: gestylt über FullCalendars eigene CSS-Variablen, gebunden an
  die shadcn-Tokens. Begründung: das Backend serialisiert `FullCalendarEvent` und
  die View-Keys (`dayGridMonth` …) direkt für diese Bibliothek; ein Eigenbau
  müsste Monats-/Wochen-Grid, Overlap-Layout, Drag&Drop und Resize nachbauen.
- **Event-Bearbeitung über den generischen UILayout-Renderer.**
  **⚠️ Überholt** – bei der Umsetzung verworfen. Timesheet und TeamEvent sind
  **handgebaut** (`definePage` + `EntityEditPage`, `@tanstack/react-form` + Zod)
  analog `book`/`order`, nicht UILayout-getrieben; nur Urlaub navigiert noch auf die
  Legacy-Edit-Seite. Begründung und Umfang siehe Abschnitt
  „[Edit-Komponenten (Timesheet & TeamCal-Event)](#edit-komponenten-timesheet--teamcal-event)".
  Der Kalender navigiert weiterhin dorthin; „kein neuer Formularcode" gilt nicht mehr.
- **UICustomized-Registry mit `COLOR_CHOOSER` wird mitgebaut**, weil das
  Kalender-Menü selbst auf `calendarSettings` verlinkt und diese Seite vier
  solche Felder hat. Sie ist zugleich das Fundament, das Phase 2 ohnehin braucht.

## Backend-Vertrag (vorhanden, nur lesen)

`projectforge-rest/src/main/kotlin/org/projectforge/rest/calendar/`

- **`CalendarServicesRest`** (`/rs/calendar`): `POST events`
  (`CalendarRestFilter` → `CalendarData`), `GET refresh`, `GET action`
  (`slotSelected|create|resize|dragAndDrop` → `ResponseAction(url)`),
  `POST storeState`.
- **`CalendarFilterServicesRest`** (`/rs/calendar`): `GET initial` →
  `CalendarInit`; `changeStyle`, `setVisibility`, `changeDefaultCalendar`,
  `changeTimesheetUser`, `changeShowBreaks`, `changeGridSize` (5/10/15/30/60),
  `changeFirstHour` (0–23), `POST changeVacationGroups`/`changeVacationUsers`
  (nackte Id-Arrays), `createNewFilter`/`updateFilter`/`renameFilter`/
  `deleteFilter`/`selectFilter`.

Drei Details, die den Frontend-Entwurf bestimmen:

1. **Mutierende Endpunkte antworten mit Teilmengen** von `CalendarInit`, Keys
   genau `filter | activeCalendars | teamCalendars | styleMap | filterFavorites |
isFilterModified`. `selectFilter` liefert ein komplettes `CalendarInit`.
   `changeStyle` liefert **kein** `isFilterModified`.
2. **`start`/`end` der Events sind flach serialisiert** (`EventDateSerializer`):
   entweder ISO-Instant oder `yyyy-MM-dd`. Unverändert an FullCalendar
   durchreichen – ein Parse-und-neu-Formatieren verschiebt All-Day-Termine um den
   Zeitzonen-Offset.
3. **`vacationGroupIds`/`vacationUserIds` im `events`-Body werden serverseitig
   überschrieben** (aus dem persistierten Filter). Sie gehören in den Query-Key,
   nicht in den Body.

Weiter: Bereichsgrenze >50 Tage in `events` → `BadRequestException`.
Filter/State/Styles/Settings liegen in den User-Prefs unter dem Bereich
`"calendar"`. `CalendarInit.translations` (für den Legacy-Client gebaut) wird von
next ignoriert, bleibt aber im Backend.

## Voraussetzungen im generischen Renderer (Blocker)

Der `action`-Endpunkt liefert für **jede** Neuanlage 2-segmentige URLs wie
`/timesheet/edit?startDate=…` (ohne id) – ebenso beim Klick auf eine
Arbeitszeit-Pause. Dafür gibt es heute keine Route: der Catch-All
`[category]/[type]/[...params]` verlangt mindestens einen Teil
(`lib/route-params.ts` gibt bei `rest.length === 0` `null` zurück).

1. **2-Segment-Route anlegen:** `app/(authenticated)/[category]/[type]/page.tsx`
   - `page-client.tsx`. Den Body des bestehenden `[...params]/page-client.tsx`
     nach `components/dynamic/dynamic-form-page.tsx` (`{category, type, id}`)
     ausziehen, damit beide Routen ~25 Zeilen bleiben und es eine Implementierung
     gibt. `generateStaticParams` liefern, damit
     `scripts/generate-spa-shell-map.mjs` einen Eintrag erzeugt.
2. **Query-String durchreichen:** `fetchDynamic` in `lib/rs/client.ts` verwirft
   heute alles außer `id`. Signatur um `search?: string` erweitern, der
   Page-Client übergibt `useSearchParams().toString()` (ohne `id`) und nimmt ihn
   in den Query-Key. Ohne das bekommt das erzeugte Timesheet falsche Zeiten:
   `TimesheetPagesRest` liest `startDate`/`endDate`/`firstHour` aus dem Request.

Beides ist Voraussetzung, keine Kür – ohne sie sind alle Neuanlage-Pfade tot.

## Dateien

```
app/(authenticated)/calendar/page.tsx          ~20  Wrapper + <Suspense> (useSearchParams)
app/(authenticated)/calendar/page-client.tsx   ~25  PageShell + <CalendarPage/>

lib/rs/calendar-types.ts   ~110  DTO-Spiegel: CalendarInit, CalendarInitPatch,
                                 CalendarFilter, StyledTeamCalendar,
                                 FullCalendarEventDto, CalendarData,
                                 CalendarViewKey, CALENDAR_GRID_SIZES
lib/rs/calendar.ts         ~140  dünne Wrapper über request(): fetchCalendarInit,
                                 fetchCalendarEvents, refreshSubscriptions,
                                 fetchCalendarAction, storeCalendarState,
                                 alle change*- und Favoriten-Aufrufe

components/features/calendar/
  types.ts                         ~60   EventsRequest, CalendarActionParams, TooltipRow
  calendar-page.tsx               ~120   Komposition, Skeleton, LegacyPageLink
  use-calendar-init.ts             ~70   useQuery(["calendar","init"]) + applyPatch
  use-calendar-events.ts           ~80   EventsRequest → useQuery, DTO → EventInput
  use-calendar-filter-mutations.ts ~150  change*-Mutations über applyPatch
  use-calendar-favorites.ts        ~90   create/update/rename/delete/select
  use-calendar-state.ts            ~70   datesSet → POST storeState (300 ms debounced)
  use-calendar-action.ts           ~80   /action + eventClick-Routingtabelle
  use-goto-date.ts                 ~60   ?gotoDate / ?hash gegen die FC-API
  view-config.ts                  ~110   PURE: views{} inkl. der beiden Working-Views,
                                         slotDuration(gridSize), scrollTime(firstHour),
                                         Monatsanfang-Normalisierung, headerToolbar
  use-view-buttons.ts              ~70   customButtons ("+", "5/7", Listen)
  full-calendar-panel.tsx         ~145   <FullCalendar> + Handler-Verdrahtung
  calendar-event-content.tsx       ~70   eventContent (Monat vs. Time-Grid)
  calendar-event-tooltip.tsx       ~80   HoverCard-Inhalt aus TooltipRow[]
  tooltip-html.ts                  ~70   PURE Parser (+ tooltip-html.test.ts)
  calendar-toolbar.tsx            ~100   Select + Favoriten + Zahnrad + "mehr"
  calendar-select.tsx             ~140   Multi-Select (Popover + Command)
  calendar-pill.tsx                ~90   ein gewählter Kalender als Pill
  calendar-style-popover.tsx      ~110   Sichtbarkeit + Farbwahl
  calendar-favorites-menu.tsx     ~140   gespeicherte Kalenderfilter
  calendar-favorite-entry.tsx      ~80   eine Zeile: select/rename/update/delete
  calendar-settings-dialog.tsx    ~140   Zahnrad-Dialog
  calendar-vacation-selects.tsx    ~90   Urlaubsgruppen/-user (async Multi-Select)
  calendar-more-menu.tsx           ~80   Einstellungen, Reload, Kalenderliste

components/shared/color-picker.tsx              ~90   NEU, wiederverwendbar
components/shared/async-entity-multi-select.tsx ~140  NEU, nutzt fetchAutoCompletion
components/dynamic/customized/                  ~60   UICustomized-Registry (COLOR_CHOOSER)
components/dynamic/dynamic-form-page.tsx        ~90   Refactor-Extraktion (s. Blocker)
```

Zwei bewusste Platzierungen in `shared/`: den **Color-Picker** braucht auch
`COLOR_CHOOSER` (aus der Feature-Ebene wäre das später ein Cross-Feature-Import),
den **Async-Multi-Select** brauchen Urlaubsgruppen (`/rs/group/autosearch`) und
Urlaubs-User (`/rs/vacation/users`). `components/shared/entity-autocomplete.tsx`
bleibt unverändert – ein Geschwister auf `fetchAutoCompletion` ist billiger als
eine API-Änderung für alle bisherigen Aufrufer.

`FilterFavoritesMenu` aus `components/data-table/` ist **nicht** wiederverwendbar
(auf `MagicFilter`/`useFilterFavorites` typisiert, andere Endpunkte) – bewusst
akzeptierte Duplikation. Kommt ein dritter Nutzer, wird ein präsentationales
`FavoritesMenu` nach `shared/` extrahiert.

## State: Query-Cache, kein zustand

```ts
const CALENDAR_INIT_KEY = ["calendar", "init"] as const;
const eventsKey = (r: EventsRequest) => ["calendar", "events", r] as const;
```

`EventsRequest = { start, end, view, activeCalendarIds, timesheetUserId,
showBreaks, vacationGroupIds, vacationUserIds, nonce }`. Die Id-Arrays vor dem
Key sortieren – sonst fragmentiert der Cache, wenn der Nutzer die Pills umsortiert.
`nonce` bedient `?hash` und den manuellen Refresh.

Ersatz für das Legacy-`saveUpdateResponseInState`:

```ts
const applyPatch = (qc, patch: CalendarInitPatch | CalendarInit) =>
  qc.setQueryData<CalendarInit>(CALENDAR_INIT_KEY, (prev) => ({
    ...prev,
    ...patch,
  }));
```

Spring serialisiert mit `JsonInclude.NON_NULL`, ein fehlender Key ist also gar
nicht im Objekt – der Spread reicht und verliert im Gegensatz zum Legacy-`||`
keine `false`/`0`-Werte (genau dafür existiert dort die
`isFilterModified`-Sonderbehandlung).

| Auslöser                                                                | Wirkung                                                                                                             |
| ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `changeStyle`, `setVisibility`                                          | `applyPatch` **+** `invalidateQueries(["calendar","events"])` (Farben und Sichtbarkeit stecken im Payload)          |
| `changeTimesheetUser`, `changeShowBreaks`, `changeVacationGroups/Users` | `applyPatch` + lokales `filter`-Patch (die Endpunkte liefern nur `isFilterModified`); Events refetchen über den Key |
| `changeGridSize`, `changeFirstHour`, `changeDefaultCalendar`            | nur `applyPatch` – rein darstellend, **kein** Events-Refetch                                                        |
| Favoriten create/update/rename/delete                                   | `applyPatch`                                                                                                        |
| `selectFilter`                                                          | ganzes `CalendarInit` setzen + Events invalidieren (activeCalendars, date und view ändern sich)                     |
| Kalender-Auswahl ±                                                      | lokales `activeCalendars`-Patch + `isFilterModified: true`; persistiert `storeState`                                |
| `refresh`                                                               | `refreshSubscriptions()` + `invalidateQueries(["calendar"])` statt `window.location.reload()`                       |

`staleTime`: Init 30 s, Events Default (60 s), dazu
`placeholderData: keepPreviousData` – sonst blitzt beim Monatswechsel ein leeres
Grid auf.

Nebenbei zu behebender Legacy-Bug: `CalendarFilterSettings.handleShowBreaksChange`
sendet den **alten** `showBreaks`-Wert an den Server und setzt lokal den neuen –
Client und Server divergieren. Wir senden `checked`.

## FullCalendar-Integration

Zwei bewusste Abweichungen vom Legacy-Panel:

1. **`events` ist ein Array aus dem Query-Cache**, nicht die
   `fetchEvents(info, cb)`-Callback-Form. Die Callback-Form erzwingt die
   Legacy-Spiegel-Refs (`activeCalendarsRef`, `timesheetUserIdRef`,
   `showBreaksRef`) und die manuellen `refetchEvents()`-Aufrufe, weil sie
   Props einfängt. Mit Query als Eigentümer kommt der Zeitraum aus `datesSet` in
   lokalen State, den Rest macht der Key. Das ist der größte Komplexitätsgewinn.
2. **Kein `useMemo`-eingefrorener Teilbaum.** Das Legacy-Panel friert
   `<FullCalendar>` auf `[gridSize, firstHour, alternateHoursBackground]` ein –
   deshalb wird dort das `views`-Objekt schal. Statt dessen: memoisiertes
   `viewConfig`, `useCallback`-Handler, `data-grid-size`-Attribut.

`initialDate` braucht die Legacy-Korrektur: bei `dayGridMonth` ist `init.date`
der erste _sichtbare_ Tag und kann zum Vormonat gehören – mit `date-fns` auf den
Monatsersten normalisieren (in `view-config.ts`, damit testbar).

`locale`, `firstDay` (`firstDayOfWeekSunday0`) und `hour12`
(`timeNotation !== "H24"`) kommen aus `useFormatContext()`, nie aus
Laufzeit-Defaults.

### CSS

Ein Block in `app/globals.css` unter `.pf-calendar` (~70 Zeilen, keine neue
Datei), der FullCalendars eigene Variablen an die shadcn-Tokens bindet:

```css
.pf-calendar {
  --fc-border-color: var(--border);
  --fc-page-bg-color: var(--card);
  --fc-neutral-bg-color: var(--muted);
  --fc-today-bg-color: color-mix(in oklab, var(--primary) 8%, transparent);
  --fc-now-indicator-color: var(--destructive);
  --fc-highlight-color: color-mix(in oklab, var(--primary) 14%, transparent);
  --fc-list-event-hover-bg-color: var(--accent);
  /* … Buttons auf transparent/--border/--accent */
}
```

Dark Mode kommt damit fast ohne Regel-Duplikate, weil die Tokens unter `.dark`
schon kippen. Die `.grid-size-NN`-Streifen werden zu
`.pf-calendar-alt[data-grid-size="NN"] .fc-timegrid-slots tr:nth-of-type(…)` –
`nth-of-type` nimmt keine Custom Property, also bleiben fünf explizite Regeln,
aber ohne Klassennamen-Bastelei. Farbe:
`color-mix(in oklab, var(--foreground) 3%, transparent)` statt des Legacy-`#00000007`,
das auf dunklem Grid unsichtbar ist. `.fc-holiday-weekend` und die
Legacy-`!important`-Patches (Event-Padding, `backdrop-filter: blur(1px)`, fette
Time-Grid-Titel) mitportieren – sie machen die transparenten Eventfarben lesbar.

`textColor`/`backgroundColor`/`borderColor` kommen als Daten aus dem
Event-Payload (`CalendarStyle.getTextColor/getBackgroundColor`, kontrastberechnet
und Farbschema-bewusst) und bleiben Inline-Styles – FullCalendars eigener
Mechanismus, kein Hex-Literal im Quellcode.

## Interaktion, Actions, Deep Links

Routingtabelle nach `extendedProps.category` (`use-calendar-action.ts`):

| Kategorie                                               | Ziel                                                          |
| ------------------------------------------------------- | ------------------------------------------------------------- |
| `timesheet-stats`                                       | nichts                                                        |
| `timesheet-break`                                       | `/timesheet/edit?startDate=…&endDate=…` (2-Segment-Route)     |
| `vacation`                                              | `/vacation/edit/<id>?returnToCaller=%2Fnext%2Fcalendar`       |
| `address` (Geburtstag)                                  | `/addressView/dynamic/<id>?returnToCaller=%2Fnext%2Fcalendar` |
| sonst (`timesheet`, `teamEvent`, `calEvent`, `holiday`) | `/<category>/edit/<id>?startDate=…&endDate=…`                 |

`id = extendedProps.uid ?? extendedProps.dbId`. `returnToCaller` jetzt
`/next/calendar`.

`select` → `action=slotSelected`, `+`-Button → `action=create` mit dem aktuellen
Datum der API, `eventResize`/`eventDrop` → erst `info.revert()`, dann
`action=resize|dragAndDrop` mit den Orig-Daten, abgesichert über
`event.startEditable === true`. Die zurückgegebene `ResponseAction.url` ist ein
**Frontend**-Pfad, geht also über `sanitizeRedirectUrl`/`resolveMenuUrl`
(`lib/menu-url.ts`) und `router.push`.

### Rückweg nach dem Speichern

`CalendarServicesRest.redirectToCalendarWithDate` (~Z. 369) kodiert
`Constants.REACT_APP_PATH` hart, während `PagesResolver.getDefaultUrl()` schon
`NextMigration.listUrl("calendar")` fragt – der Literal ist der Ausreißer.
Aufrufer sind `TimesheetPagesRest:196` und `TeamEventPagesRest:169`. Umstellen
auf `NextMigration.listUrl("calendar")` (beide Zweige); das ist die einzige
inhaltliche Backend-Änderung jenseits des Menü-Schalters.

Clientseitig `use-goto-date.ts`: liegt `gotoDate` außerhalb
`api.view.activeStart..activeEnd` → `api.gotoDate(...)` (löst `datesSet` → neuer
Key → Refetch), sonst bei geändertem `hash` den `nonce` hochzählen. Vergleich
gegen ein `useRef` des letzten Paars, nicht gegen einen Snapshot von
`window.location.search`.

### Einstellungs- und Abo-Seiten

Laufen über den generischen Renderer:
`/next/calendarSettings/dynamic/-1` und
`/next/calendarSubscription/dynamic/-1?type=HOLIDAYS` (drei Segmente, der
bestehende Catch-All greift; `getForm` ignoriert die id). `calendarSettings`
braucht die UICustomized-Registry mit `COLOR_CHOOSER` (nutzt
`shared/color-picker.tsx`).

## Menü-Umschaltung

`NextMigration.MIGRATED` ist nach **REST-Kategorie** geschlüsselt, nicht nach „ist
eine Entity-Liste“ – `calendar` _ist_ eine Kategorie (`/rs/calendar`), nur nie in
`PagesResolver.pagesRegistry` registriert. Der Eintrag ist daher unbedenklich und
gibt dem ganzen System eine konsistente Antwort. Die geerbten
`editRoute`/`newEntryRoute`-Defaults sind tot, aber harmlos – kommentieren, weil
die Klassendoku impliziert, dass jeder Eintrag eine Edit-Seite hat.

- `NextMigration.kt`: `"calendar" to NextPage(route = "calendar")`
- `MenuItemDefId.kt:53`: `CALENDAR(..., getListUrl("calendar"))` statt
  `getReactListUrl`
- `lib/hand-built-categories.ts`: `["book", "cost1", "calendar"]` – die konkrete
  Route beschattet `[category]` ohnehin; der Eintrag sorgt dafür, dass ein
  versehentliches `/next/calendar/foo` 404t statt `/rs/calendar/initialList` zu
  fragen.
- `<LegacyPageLink url="react/calendar" />` in `calendar-page.tsx` explizit
  rendern – es gibt kein `ui.legacyUrl`, weil das keine UILayout-Seite ist.
- Kontrolle: `grep -rn "REACT_APP_PATH" projectforge-rest projectforge-business`.
  Der Meldungstext `calendar.settings.colors.vacations.info` enthält einen
  hartkodierten `(/react/teamCal/edit)`-Link – bleibt, teamCal ist nicht migriert.
- `components/shared/app-sidebar.tsx:47` (toter `href: "#"`-Eintrag) **nicht**
  verdrahten. Navigation ist `top-navigation.tsx` + Backend-Menü; die Sidebar ist
  unbenutztes Platzhalter-Gerüst und soll keine zweite Navigation werden.

## Tooltip ohne `dangerouslySetInnerHTML`

`TooltipBuilder` erzeugt `<table><tr><th>Label:</th><td>Wert</td></tr>…`, Werte
meist escaped – aber an drei Stellen bewusst nicht (Teilnehmerliste in
`TeamCalEventsProvider:199`, Task-Pfad als `OutputType.HTML` in
`TimesheetEventsProvider:160`); `pre=true` umschließt mit `<pre>`. Der String
enthält also legitim Markup und lässt sich nicht einfach als Text rendern.

`dangerouslySetInnerHTML` wäre die Legacy-Lösung, `react-markdown` +
`rehype-raw` dieselbe Vertrauensentscheidung mit mehr Abhängigkeiten. Statt
dessen ein strukturbewusster Parser in `tooltip-html.ts`:

```ts
export function parseTooltipHtml(html: string): TooltipRow[] {
  const doc = new DOMParser().parseFromString(html, "text/html");
  return [...doc.querySelectorAll("tr")]
    .map((tr) => {
      const td = tr.querySelector("td");
      // <br> wird Zeilenumbruch (Teilnehmerliste), <pre> behält seinen eigenen.
      td?.querySelectorAll("br").forEach((br) => br.replaceWith("\n"));
      return {
        label: (tr.querySelector("th")?.textContent ?? "").replace(/:$/, ""),
        value: td?.textContent?.trim() ?? "",
        multiline:
          !!td?.querySelector("pre") || (td?.textContent ?? "").includes("\n"),
      };
    })
    .filter((r) => r.value);
}
```

`DOMParser` auf einem nicht angehängten Dokument führt nichts aus (keine Skripte,
keine Bild-Requests), und nur `textContent` gelangt nach React – die Ausgabe ist
beweisbar Text. Verloren geht Styling (Anker im Task-Pfad, Teilnehmer-Farben),
keine Information. Test mit einem echt abgegriffenen Payload; vitest läuft
`environment: "node"`, die Datei braucht also `// @vitest-environment jsdom`
(nicht global umstellen – die Config ist absichtlich `node`).

Darstellung: radix **HoverCard**, nicht `Tooltip` – der Inhalt ist eine
beschriftete Tabelle mit Fußzeile (`extendedProps.duration`), und die
`aria-describedby`-Semantik von `Tooltip` passt dafür nicht. Ein kontrollierter
HoverCard, dessen `HoverCardAnchor` bei `eventMouseEnter` auf `info.el` gesetzt
wird; das ersetzt den manuellen `createPopper`/`destroy`-Lebenszyklus und bringt
Kollisionsbehandlung mit. Schließen bei `eventMouseLeave`, `eventDragStart`,
`eventResizeStart`.

## Farbwahl und Kalender-Multi-Select

`components/shared/color-picker.tsx`: Swatch-Grid aus Tokens (`--chart-*` plus
einige gesättigte Töne, als Tokens definiert – kein Inline-Hex), dahinter ein
natives `<input type="color" class="sr-only">` hinter einem beschrifteten
Swatch-Button (null Abhängigkeiten, OS-nativ, tastaturfähig), plus ein
Hex-`<Input>`, validiert gegen die Backend-Regel `CalendarStyle.validateHexCode`
(`/^#([0-9a-f]{3}|[0-9a-f]{6})$/i`), damit `changeStyle` nie
`IllegalArgumentException` wirft. Commit bei Blur/Schließen – ein Request pro
gewählter Farbe, nicht pro Drag-Frame.

`calendar-select.tsx`: Popover + Command (`cmdk` ist da, `components/ui/command.tsx`
existiert) über `init.teamCalendars`, Farbpunkt pro Zeile, Suche nach Titel,
Mehrfachauswahl mit Häkchen, Menü bleibt offen. Gewählte Kalender als Pills in
der Triggerzeile, nach `title.localeCompare` sortiert (das Backend sortiert schon
mit dem Locale-Comparator; nach lokalem Hinzufügen stabil halten). Unsichtbare
Kalender `line-through italic opacity-50` – ersetzt die
`Calendar.module.js`-Overrides für react-select. Pill-Körper öffnet
`calendar-style-popover.tsx` (Checkbox → `setVisibility`, Picker →
`changeStyle`), das × entfernt lokal + `isFilterModified: true`. `aria-label` auf
jedem Icon-Button, der Trigger ist ein echter Button mit der Anzahl als
zugänglichem Namen.

## npm-Abhängigkeiten

```
npm i @fullcalendar/core@^6.1.20 @fullcalendar/react@^6.1.20 \
      @fullcalendar/daygrid@^6.1.20 @fullcalendar/timegrid@^6.1.20 \
      @fullcalendar/list@^6.1.20 @fullcalendar/interaction@^6.1.20
npx shadcn@latest add hover-card
```

V6 explizit pinnen (v7 ist inzwischen aktuell auf der Registry).
`@fullcalendar/react@6.1.20` erlaubt React 19 als Peer. FullCalendar 6 injiziert
sein CSS aus JS und bringt seine eigene `fc-icon`-Font mit – mit dem
Bootstrap-Theme fällt also auch der FontAwesome-Bedarf weg. Kein
`@popperjs/core` (radix), kein `react-color`, kein `react-select`.

## Verifikation

**Statische Gates:** `npm run typecheck`, `npm run lint -- --fix`,
`npm run format`, `npm test` (`tooltip-html.test.ts`, `view-config.test.ts`:
slotDuration, scrollTime, Monatsnormalisierung, 50-Tage-Grenze),
`npm run build` – das ist das eigentliche Gate, es beweist den Static Export mit
der neuen `[category]/[type]`-Route (auf die
`[spa-shell-map] no prerendered route`-Warnung achten). Dazu
`./gradlew :projectforge-application:test --tests "*GenerateNextI18nMessagesTest*"`.

**Gegen das laufende Backend** (`:8080` + `npm run dev`, Zugang aus
`~/ProjectForge/testAccont.txt`, lesend bevorzugt, Erzeugtes aufräumen):

- `GET /rs/calendar/initial` real abgreifen und gegen `lib/rs/calendar-types.ts`
  diffen. `JsonInclude.NON_NULL` heißt: „fehlt“ ≠ null – nur das klärt, welche
  Felder wirklich optional sind.
- `POST /rs/calendar/events` über einen Monat: die `start`/`end`-Stringformen
  (Instant vs. `yyyy-MM-dd`) prüfen und je ein Event pro Kategorie sehen
  (timesheet, break, stats, vacation, birthday, Feiertags-Background, teamEvent).
- `GET /rs/calendar/action?action=create&startDate=…` für beide Zweige
  (`defaultCalendarId > 0` vs. Timesheet).
- Jeden `change*`-Endpunkt einmal und den Key-Satz der Antwort gegen
  `CalendarInitPatch` prüfen.

**Manuell** (`localhost:3000/next/calendar`): alle sieben Views inkl. Persistenz
über Reload; today/prev/next; gridSize 5→60 (Slot-Höhe **und** Streifenrhythmus);
firstHour scrollt; Drag & Resize (revert, dann Formular mit richtigen Zeiten);
Slot-Auswahl → Timesheet vs. Team-Event je Standardkalender; Klick auf jede
Event-Kategorie; Tooltip bei Timesheet (Task-Pfad) und Team-Event (Teilnehmer);
Kalender hinzufügen/entfernen/verstecken/umfärben und Farbe nach Reload; Favorit
speichern/wählen/umbenennen/löschen inkl. Modified-Marker; Urlaubsgruppen und
-user; Refresh; Zahnrad- und Overflow-Menü; Timesheet aus dem Kalender speichern
und auf `/next/calendar?gotoDate=…` am richtigen Datum landen; **Dark Mode**
(Gitterlinien, Streifen, Feiertagsflächen, Event-Kontrast); **Mobil** (die
Toolbar muss umbrechen, nicht überlaufen).

**Playwright** `e2e/calendar.spec.ts` (Live-Backend, Texte und Daten über
`e2e/fixtures/format.ts`, nie hartes „Woche“ oder `dd.MM.yyyy`):

1. Seite lädt, Grid rendert, `?date=` wird beachtet;
2. View-Wechsel übersteht Reload (beweist `storeState`);
3. gridSize-Änderung → `data-grid-size` ändert sich und übersteht Reload;
4. Sichtbarkeit eines Kalenders umschalten → Pill durchgestrichen, übersteht Reload;
5. Hover über ein Event → HoverCard zeigt ein bekanntes Label;
6. Filter-Favorit anlegen und löschen;
7. Klick auf ein Timesheet-Event → `/next/timesheet/edit/<id>` mit geladenem
   Layout (Regression für die Query-Weitergabe);
8. Klick auf einen leeren Slot → 2-Segment-Route mit vorbelegter Startzeit
   (Regression für die neue Route).

## Offene Risiken

1. **Die beiden Renderer-Voraussetzungen sind Blocker.** Ohne sie sind alle
   Neuanlage-Pfade und Break-Klicks tot. Werden sie größer als skizziert, muss
   die Menü-Umschaltung warten – kein Kalender, dessen `+`-Button 404t.
2. **`calEvent` vs. `teamEvent`** entscheidet das Backend über
   `calendar.useNewCalendarEvents`. In diesem Stand findet sich kein Controller
   auf `calEvent`; mit gesetztem Flag liefert `action` eine URL auf eine
   Kategorie ohne Backend-Seite. Vorbestand, aber next zeigt das als leere Seite
   statt als Legacy-404 – vor der Umschaltung ansehen.
3. **Serientermine.** ~~Der Rückfrage-Dialog ist ein MODAL-`ResponseAction`; MODAL öffnet in
   next derzeit eine Seite statt eines Overlays.~~ **Gelöst:** Die Serien-Rückfrage ist als
   In-Page-`series-modification-section.tsx` umgesetzt (Phase D), nicht als MODAL. Der
   allgemeine MODAL-Overlay-Mangel bleibt für andere Pfade bestehen, trifft diesen aber nicht.
4. `changeStyle` liefert kein `isFilterModified`, und `styleMap` zählt nicht zu
   `CalendarFilter.isModified` – Umfärben markiert den Filter also nie als
   geändert. Backend-treu, kann Nutzer irritieren; so lassen.
5. **`view` im Events-Body** wird serverseitig nicht genutzt; im Query-Key
   bedeutet er einen Refetch bei reinem View-Wechsel innerhalb desselben
   Zeitraums. Akzeptabel, alternativ aus dem Key nehmen und nur im Body führen.
6. **50-Tage-Grenze:** ein ungewöhnlicher `firstDay` plus `listMonth` kann
   theoretisch einen breiteren Bereich erzeugen – clampen und den 400er als Toast
   zeigen, nicht als leeres Grid.
7. **`?gotoDate`-Race:** `api.gotoDate()` löst `datesSet` → `storeState`; eine
   schnelle Doppelnavigation kann ein Zwischendatum persistieren. Der
   300-ms-Debounce deckt es ab, aber so etwas findet sich nur manuell.
