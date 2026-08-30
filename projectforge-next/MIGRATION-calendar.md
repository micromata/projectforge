# Migrationsplan: Kalenderseite (`/react/calendar` → `/next/calendar`)

Detailplan zu [MIGRATION.md](MIGRATION.md). Die Umsetzung ist **weitgehend
abgeschlossen**. Dieses Dokument ist auf den erreichten Stand, die offenen Punkte
und die für künftige Migrationen wiederverwendbaren Erkenntnisse eingekürzt; die
ausführliche Ausgangsanalyse ist in der Git-Historie.

## Stand

Fertig und verdrahtet:

- **Kalenderseite** komplett (alle Views, Kalenderauswahl mit Farbwahl,
  Filter-Favoriten, Einstellungen, Tooltips, Drag&Drop/Resize, Slot-Auswahl).
- **Timesheet** – Edit **und** Liste (Filter-Toggles, Footer/KI-Anteil, Export
  Excel/PDF/ics, Mehrfachauswahl/massUpdate). PDF-Export neu als Kotlin-Service
  `TimesheetListPdfExport` auf **OpenPDF** statt Apache-FOP.
- **TeamEvent** – Edit in den Phasen A (Grundgerüst), B (Reminder) und
  D (Recurrence/Serientermine); Drag/Resize öffnet den Termin verschoben.
- Menü-Umschaltung auf `next/calendar` bzw. `next/timesheet`, Backend-Rückweg
  nach dem Speichern über `NextMigration.listUrl("calendar")`.

## Offen

**Tatsächlich zu tun:**

- **Manuelle Verifikation & Live-Backend-Abgleich** – einziger echter Restpunkt.
  `GET /rs/calendar/initial` gegen die DTO-Spiegel (`lib/rs/calendar-types.ts`)
  diffen (`JsonInclude.NON_NULL`: „fehlt" ≠ null), `POST events` über einen Monat
  (je ein Event pro Kategorie; `start`/`end`-Stringformen prüfen), jeden
  `change*`-Endpunkt einmal, dann die manuelle Durchklick-Liste (7 Views inkl.
  Persistenz, gridSize, firstHour, Drag/Resize, Favoriten, Urlaub, Dark Mode,
  Mobil). E2E: `e2e/calendar.spec.ts` (Live-Backend, Texte/Daten über
  `e2e/fixtures/format.ts`).

**Bewusst zurückgestellt (hängt an Produktentscheidungen):**

- **TeamEvent-Attendees (Phase C)** – eigener Multi-Select. „Später, wenn
  überhaupt." Schema trägt `attendees` verlustfrei als `z.array(z.unknown())`
  durch – bestehende Teilnehmer gehen beim Speichern nicht verloren, sie sind nur
  nicht editierbar.
- **`shared/async-entity-multi-select.tsx`** – nicht extrahiert; die
  Urlaubs-Auswahl steckt in `calendar-vacation-selects.tsx`. Bei einem zweiten
  Nutzer nach `shared/` ziehen – der einzige plausible zweite Nutzer ist Phase C,
  also selbst offen.
- **TeamEvent-List-Page** (`app/(authenticated)/teamEvent/page.tsx`) – Termin
  wird über den Kalender erreicht. Spalten sind in `teamEvent.page.tsx` bereits
  deklariert; es fehlt nur die Route.
- **Timesheet-Vorlagen-Button** – laut Vorgabe außen vor.

## Erkenntnisse für künftige Migrationen

- **Edit-Seiten handgebaut, nicht UILayout.** Timesheet und TeamEvent wurden
  wie `book`/`order` gebaut (`definePage` + `EntityEditPage`,
  `@tanstack/react-form` + Zod aus den generierten Metadaten). Speichern über
  `lib/rs/entity.ts` (`saveOrUpdateEntity`), Validierung bleibt serverseitig
  (HTTP 406). Der ursprünglich vorgesehene generische UILayout-Renderer wurde
  dafür verworfen.
- **Kein Datenverlust beim Speichern.** `EntityEditPage.save` postet die Formwerte
  _als_ DTO – deshalb müssen Schema + `toFormValues` **jedes** nicht editierte
  DTO-Feld unverändert durchtragen (recurrence*, attendees, reminder*, …). Bei
  TeamEvent wird ein Serientermin dadurch beim Speichern _laut_ abgelehnt
  (`validate` verlangt `seriesModificationMode`), statt still korrumpiert zu
  werden.
- **History-Tab ist entity-unabhängig.** `EntityEditPage` liest
  `history: page.metadata.historizable` aus den generierten Metadaten; jede
  `@WithHistory`-Entität bekommt den Tab ohne eine Zeile Deklaration. Es braucht
  **keine** `[id]/history/`-Route – die war ein Redirect-Überbleibsel für alte
  Next-Deep-Links und wurde für alle Entitäten gelöscht.
- **Serien-Rückfrage als In-Page-Section**, nicht als MODAL-`ResponseAction`
  (`series-modification-section.tsx`). MODAL öffnet in next eine Seite statt eines
  Overlays – dieser Pfad umgeht das.
- **Menü-Umschaltung** ist nach REST-Kategorie geschlüsselt (`NextMigration.MIGRATED`),
  nicht nach „ist Entity-Liste". `calendar` ist eine Kategorie (`/rs/calendar`),
  daher ist der Eintrag `"calendar" to NextPage(route = "calendar")` unbedenklich.

## Backend-Vertrag (Kurzreferenz, nur lesen)

`projectforge-rest/.../calendar/`

- **`CalendarServicesRest`** (`/rs/calendar`): `POST events`, `GET refresh`,
  `GET action` (`slotSelected|create|resize|dragAndDrop` → `ResponseAction(url)`),
  `POST storeState`.
- **`CalendarFilterServicesRest`**: `GET initial` → `CalendarInit`; die
  `change*`-Endpunkte und `createNewFilter`/`updateFilter`/`renameFilter`/
  `deleteFilter`/`selectFilter`.

Drei Details, die den Frontend-Entwurf bestimmen (weiterhin gültig):

1. Mutierende Endpunkte antworten mit **Teilmengen** von `CalendarInit`
   (`filter | activeCalendars | teamCalendars | styleMap | filterFavorites |
   isFilterModified`); `selectFilter` liefert das komplette `CalendarInit`,
   `changeStyle` **kein** `isFilterModified`.
2. `start`/`end` der Events sind flach serialisiert (ISO-Instant oder
   `yyyy-MM-dd`) – **unverändert** an FullCalendar durchreichen, sonst verschieben
   sich All-Day-Termine um den Zeitzonen-Offset.
3. `vacationGroupIds`/`vacationUserIds` im `events`-Body werden serverseitig
   überschrieben – sie gehören in den Query-Key, nicht in den Body.

Bereichsgrenze >50 Tage in `events` → `BadRequestException` (clampen und den 400er
als Toast zeigen).

## Verbliebene Risiken

- **`calEvent` vs. `teamEvent`:** Bei gesetztem `calendar.useNewCalendarEvents`
  liefert `action` eine URL auf `calEvent` – dafür gibt es keinen Controller/DTO.
  next zeigt das als leere Seite statt Legacy-404. Vor einem Flag-Umschalten
  ansehen. (Flag ist aktuell `false`.)
- **Umfärben markiert den Filter nie als geändert** (`changeStyle` liefert kein
  `isFilterModified`, `styleMap` zählt nicht zu `CalendarFilter.isModified`).
  Backend-treu, so gelassen.
