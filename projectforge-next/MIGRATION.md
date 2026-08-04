# Frontend-Migration nach projectforge-next

Dieses Dokument beschreibt das Zielbild und den schrittweisen Weg, das gesamte
ProjectForge-Frontend nach **projectforge-next** (Next.js, App Router) zu
migrieren – im Parallelbetrieb, so dass Releases pro Seite möglich sind.

## Ausgangslage: drei Frontends, ein Backend

Alle Frontends werden von der einen Spring-Boot-App auf `:8080` serviert:

| Frontend | Modul | Pfad | Technik |
|---|---|---|---|
| Wicket (Legacy) | `projectforge-wicket` | `/wa/*` | server-rendered, Servlet-Filter (`WebXMLInitializer.java`) |
| Alte React-App | `projectforge-webapp` | `/react/**` | backend-getriebener „Dynamic Renderer" (UILayout-JSON), CRA→Vite |
| **projectforge-next** | `projectforge-next` | `/next/**` | Next.js 16 App Router, statisch exportiert |

**Geteilte Authentifizierung.** Spring Security ist `permitAll`
(`SpringSecurityConfig.kt`); die Authentifizierung übernehmen PF-Servlet-Filter
(`WicketUserFilter`, `RestUserFilter`) über eine gemeinsame `HttpSession`
(`JSESSIONID`, `LoginService.SESSION_KEY_USER`). Alle drei Frontends teilen diese
Session per Cookie – das ist die Grundlage für den Parallelbetrieb.

**Backend-getriebener „Dynamic Renderer".** Die alte React-App rendert Seiten
generisch aus `UILayout`-JSON, das der Server pro Seite beschreibt
(`projectforge-rest/src/main/kotlin/org/projectforge/ui/`,
`rest/core/AbstractPagesRest.kt`). Interaktionen laufen über ein einheitliches
`ResponseAction`/`TargetType`-Protokoll. Rund 36 Entitäten (Adresse, Task,
Timesheet, Rechnung, Order-Liste …) sind bereits UILayout-basiert.

## Zielbild

projectforge-next bildet das **gesamte** Frontend ab. Die Dynamic-Renderer der
alten React-App **und** Wicket werden vollständig entfernt. Insbesondere komplexe
Wicket-Seiten (Auftragsbuch) werden durch handgebaute Next-Seiten ersetzt – dort
stößt der backend-gesteuerte Dynamic-Renderer prinzipiell an seine Grenzen.

## Grundsatzentscheidungen

1. **Koexistenz per eigenem Pfad.** projectforge-next bekommt `basePath: "/next"`.
   Alte React-App bleibt auf `/react`, Wicket auf `/wa`. Das Backend-Menü
   (`MenuItemDefId.url`) entscheidet pro Seite, welches Frontend geladen wird →
   echte parallele Releases pro Seite.

2. **Prod = Static Export.** `next build` mit `output: 'export'` erzeugt statische
   Assets, die per Gradle in die Spring-Boot-Jar gepackt und same-origin unter
   `/next` serviert werden (analog zur heutigen Vite-App).

3. **Dynamic-Renderer Dual-Track.** Der UILayout-Renderer in
   `components/dynamic/` wird zum vollwertigen Port ausgebaut, damit die ~36
   backend-getriebenen Seiten automatisch mitkommen. Komplexe Seiten werden
   zusätzlich handgebaut (Muster: `books`).

## Zwei Betriebsmodi (wichtig)

- **Dev:** Der Next-**Node-Server** läuft auf `:3000` (`next dev`) – volle
  HMR/Hot-Code-Replacement für schnelle Entwicklung. API-Calls gehen per
  `next.config.ts`-`rewrites()` (bzw. Spring-CORS) an das Backend auf `:8080`.
- **Prod:** **Kein Node-Server.** Reiner Static Export, von Spring unter `/next`
  ausgeliefert.

### Constraint: Dev-Komfort darf die Prod-Tauglichkeit nicht brechen

Weil Prod ein reiner Static Export ohne Node ist, darf Feature-Code **keine**
Features nutzen, die einen laufenden Node-Server voraussetzen – auch wenn der
Dev-Server sie anbietet:

- Keine SSR/Server-Runtime-Logik, keine Route Handlers als echte
  Prod-Backend-Endpunkte, keine `rewrites()` als Prod-Mechanismus (greifen nur im
  Dev-Node-Server).
- `/rs` + `/rsPublic`: Prod läuft same-origin unter `/next` → relative API-Calls
  treffen dieselbe Spring-Origin, kein Proxy nötig. `lib/rs/client.ts` muss in
  **beiden** Modi funktionieren (relativer Basis-Pfad, `credentials: "include"`).
- **Keine Route Handlers** (`route.ts`): mit `output: 'export'` grundsätzlich
  inkompatibel. Mocks gehören in MSW o.Ä., nicht in `app/`.
- **i18n** läuft client-seitig (Cookie / `userStatus.locale`) – siehe Phase 0.
- **Routen:** dynamische Segmente (`[category]`, `[id]`) müssen statisch
  exportierbar sein – `generateStaticParams` oder Client-seitiges Catch-all-Routing.
- **CI-Gate:** Der Gradle-Build baut immer den Static Export – so werden Dev-only-
  Abhängigkeiten beim Build sichtbar, nicht erst in Prod.

## Phasen

### Phase 0 – Parallelbetrieb herstellen ✅ erledigt

`basePath: "/next"`, `output: 'export'`, `trailingSlash: true`; `BASE_PATH` als
einzige Quelle in `lib/config.ts`. Backend: `NEXT`/`NEXT_APP_PATH` in
`Constants.kt`. Build: eigenes Gradle-Modul `projectforge-next`
(`npmBuild` → `out/` → `static/next/`), verdrahtet in `settings.gradle.kts` und
`projectforge-application` (`processResources`, `bootJar`).

**Abweichungen von der ursprünglichen Planung** – hier lagen die Fallstricke:

- **Kein View-Controller-Forward, sondern ein Resource-Handler.** Die alte
  React-App kommt mit einem simplen `/react/**`-Forward aus, weil ihre Assets an
  der **Root** liegen (`/assets/*`). Der Next-Export legt sie dagegen **unter**
  den basePath (`/next/_next/*`), ein Catch-all-Forward würde sie verschlucken.
  `WebApplicationConfig` nutzt daher einen `PathResourceResolver`: echte Dateien
  und Assets zuerst, dann `<route>/index.html` bzw. `<route>.html`, fehlende
  Assets → echter 404, und nur Page-Routen ohne eigene Datei → `404.html` als
  SPA-Shell. Damit sind Deep-Links/Bookmarks (`/next/books/5`) möglich.
  Der Wurzelpfad `/next/` braucht zusätzlich einen expliziten View-Controller
  (leerer Resource-Pfad wird vom Resolver nicht aufgelöst).
- **API-Calls sind root-relativ**, nicht mit basePath geprefixt: Spring serviert
  `/rs` + `/rsPublic` an der Origin-Root, nicht unter `/next`. Die Dev-`rewrites()`
  brauchen daher `basePath: false`.
- **Mock-Route-Handler entfernt** (`app/rs/book/*`): `route.ts`-Handler sind mit
  `output: 'export'` grundsätzlich inkompatibel. `mock-data.ts` bleibt liegen,
  wird aber nirgends mehr importiert (Kandidat für MSW oder Löschung).
- **Static-Export-Anpassungen:** `/login` braucht eine Suspense-Boundary
  (`useSearchParams`); `books/[id]` ist in Server-Wrapper (`generateStaticParams`
  mit Platzhalter) + Client-Component (`page-client.tsx`, ID via `useParams`)
  geteilt.
- **i18n** ist client-seitig (`i18n/config.ts`, `i18n/locale-provider.tsx`):
  Cookie → Browser-Sprache → `de`, übernimmt nach Login `userData.locale`. Beide
  Kataloge sind gebündelt; `NextIntlClientProvider` braucht eine explizite
  `timeZone`, sonst schlägt das Prerendering fehl. Das next-intl-**Plugin** und
  `i18n/request.ts` sind entfernt.

**Verifiziert** gegen die laufende App: `/next/books/`, Deep-Links
`/next/books/5` und `/next/order/edit/5` liefern die Shell (200), Assets laden
korrekt, fehlende Assets ergeben 404, `/react` unbeschädigt. Der Gradle-Build ist
inkrementell (kein Node-Build ohne Änderung).

### Phase 1 – Menü-gesteuertes Routing pro Seite ✅ erledigt

`getNextListUrl()` in `MenuItemDefId.kt`; **`BOOK_LIST` ist auf `next/books`
umgestellt** – der erste Release-Schalter. Alle anderen Einträge zeigen weiter auf
`react/...` bzw. `wa/...`.

Beide Frontends müssen das Präfix beachten:

- **projectforge-next:** `lib/menu-url.ts` löst Menü-URLs auf → `next/` als
  internes Client-Routing, `react/`/`wa/`/absolut als Hard-Navigation.
  `MenuLink` in `top-navigation.tsx` nutzt das.
- **Alte React-App:** neue Route `/next/*` → `RedirectToNext.tsx` (analog zum
  bestehenden `/wa/*` → `RedirectToWicket`), in **beiden** Routen-Listen
  (`AuthorizedRoutes.jsx`, `ProjectForge.jsx`). Ohne das würde die alte App
  `next/books` als eigene Kategorie interpretieren und ins Leere laufen.

**Wichtig für weitere Umstellungen:** Die Menü-URL muss die **Next-Route** nennen,
nicht die REST-Kategorie. Beispiel Bücher: Route `books` (Plural), REST-Kategorie
`book` (Singular). Zeigt die URL auf eine nicht existierende Route, liefert Spring
stillschweigend die SPA-Shell und die Seite bleibt leer.

### Phase 1.5 – `books` produktionsreif machen 🔶 in Arbeit

`books` ist die Referenz-Implementierung: Was hier nicht funktioniert, fehlt
später jeder migrierten Seite. Daher **vor** weiteren Seiten abschließen.

**Erledigt:** Der `MagicFilter`-Kontrakt. Jeder `/rs/{entity}/list`-Aufruf endete
mit HTTP 400, die Liste blieb leer. Ursachen:

- `paginationPageSize` wurde als Top-Level-Feld gesendet, ist im Backend aber ein
  **berechnetes read-only `val`**, das die Größe aus einem `entries`-Eintrag liest.
  Es wird in `ResultSet` zurückgegeben, aber nie akzeptiert. `MagicFilter` lehnt
  unbekannte Felder strikt ab (kein `@JsonIgnoreProperties`) → 400.
  → Jetzt via `paginationPageSizeEntry()` als Filter-Eintrag.
- `extended.page` war wirkungslos: **`AbstractPagesRest.getList` paginiert nicht
  serverseitig**, es liefert die ganze Liste (bis `maxRows`).
  → Pagination erfolgt clientseitig über das Ergebnis, wie in der alten React-App.

Beide Abweichungen entstanden für die Mock-Routen; seit deren Entfernung gilt der
echte Kontrakt. **Regel:** `lib/rs/types.ts` darf nur Felder enthalten, die die
Kotlin-Klasse deserialisieren kann.

**Offen:**

1. **Tabellen-Funktionen aus dem TanStack-Umbau übernehmen.** Die alte React-App
   hat Spalten-Filter, Resizing, Ein-/Ausblenden und Pinning bereits gelöst –
   Referenz: `projectforge-webapp/src/components/base/dynamicLayout/components/table/`
   (`DynamicTanStackGrid.tsx` sowie `tanstack/TanStackColumnPanel.tsx`,
   `TanStackColumnFilter.tsx`, `TanStackDateFilter.tsx`, `TanStackNumberFilter.tsx`,
   `TanStackPagination.tsx`, `FilterPortal.tsx`, `CellRendererDispatch.tsx`,
   `tableUtils.ts`). `projectforge-next/components/data-table/` hat davon bislang
   nur `data-table.tsx`, Column-Header und Pagination. Spaltenbreiten sind
   derzeit verrutscht.
   Spaltenzustand persistiert das Backend über `setColumnStates`
   (`AbstractPagesRest`, User-Prefs) – beim Port mitdenken.
2. **Filter-Panel ist Design-Attrappe.** `books-filter-panel.tsx` enthält
   hartkodierte Autorennamen, erfundene Zahlen (234/189/45) und gespeicherte
   Filter; nichts davon ist an einen Query gebunden. Deshalb ändert
   „Filter löschen" nichts. Ebenso sind die Chips in `books/page.tsx` bloßer
   `useState`-Initialwert. Entweder echt anbinden (Status/Autor als
   `MagicFilterEntry`, Zahlen vom Backend) oder entfernen.
3. **Konventions-Drift:** `books`-Edit nutzt `@tanstack/react-form`, `CLAUDE.md`
   schreibt `react-hook-form` vor. Vor der Bulk-Migration entscheiden.
4. Nicht browserseitig verifiziert: englischer Locale-Pfad und der vollständige
   Login-Flow mit echten Daten.

### Phase 2 – Dynamic-Renderer in Next vervollständigen (Bulk-Migration)

Port der Referenz `projectforge-webapp/src/components/base/dynamicLayout/` nach
`projectforge-next/components/dynamic/`. Lücken (Ist → Soll):

1. **`callAction`/`ResponseAction`-Interpreter** vervollständigen (alle
   `TargetType`: REDIRECT, UPDATE, DOWNLOAD, MODAL, CLOSE_MODAL, TOAST,
   CHECK_AUTHENTICATION, GET/POST/PUT/DELETE mit rekursivem Feedback,
   406→ValidationErrors).
2. **`watchFields`** verdrahten: Feldänderung → diff gegen `ui.watchFields` →
   `POST {category}/watchFields` → `UPDATE`-Merge.
3. **RHF + Zod** statt rohem `setData` (Projekt-Konvention). Hinweis: `books`-Edit
   nutzt abweichend `@tanstack/react-form` – vor dem Bulk-Track auf eine Form-Lib
   festlegen.
4. **`DataTable`-Integration** für Listen (heute statische HTML-Tabelle) an
   `components/data-table/` + `useMagicFilterQuery` (Server-Sort/Page/Search).
5. **Fehlende UIElement-Typen** aus `UIElementType.kt` (~40 Typen) ergänzen:
   Date/Time-Picker, Autocomplete/`DynamicObjectSelect`, RADIOBUTTON, RATING,
   EDITOR, ATTACHMENT_LIST, DROP_AREA, PROGRESS, FILTER_ELEMENT, NAMED_CONTAINER,
   `layoutBelowActions`, `pageMenu`.
6. **`UICustomized`-Escape-Hatch** (alt: ~30 String-IDs → bespoke Komponenten)
   als Registry nachbauen; die Komponenten selbst sind manuelle Ports
   (Adress-Bild/Telefon/VCard-Import, `book.lendOutComponent`,
   Kalender-Recurrency, Cost-Number, Invoice-Positionen, WebAuthn, `access.table`,
   …).

**Verifikation.** Eine UILayout-Seite (z.B. `address`, `timesheet`) über den
Next-Renderer laden, gegen die alte React-Seite vergleichen (Layout,
watchFields-Roundtrip, Speichern, Validierung, History).

### Phase 3 – Komplexe Wicket-Seiten handbauen (Beispiel Auftragsbuch)

Das **Auftragsbuch** ist der Referenz-Härtefall:

- Die **Liste** ist bereits REST-migriert (`AuftragPagesRest.createListLayout`,
  Filter in `addMagicFilterElements`/`preProcessMagicFilter`) → läuft über den
  Phase-2-Renderer.
- Das **Edit** ist über UILayout **nicht** abbildbar: `createEditLayout` ist
  read-only + Attachments; `_createEditLayoutUnderConstruction` ist ein toter, nie
  verdrahteter Versuch. Grund (`AuftragEditForm.kt`, ~826 Z.): unbegrenzt
  wachsende, geschachtelte Sub-Formulare (Auftragspositionen), zweite
  geschachtelte Collection (Zahlungspläne), Live-Currency-Berechnung
  (`AuftragsCache.getOrderInfo`), AJAX-Cross-Field-Autofill
  (Projekt→Kunde/PM/HOBM/SM), Forecast-Typen, Period-of-Performance. Dem
  `UILayout`-DSL fehlt das Primitive „wiederholbare, geschachtelte editierbare
  Sub-Entität mit Live-Server-Neuberechnung".
- **Vorgehen:** handgebaute Next-Seite (`components/features/orders/`) mit RHF+Zod
  gegen ein **echtes, geschachteltes Order-DTO** (heute `Auftrag.kt` `positionen`
  als rohe `MutableList<AuftragsPositionDO>`). Neue/erweiterte REST-Endpunkte für
  Positions-CRUD + Live-Kalkulation (Backend-Logik `AuftragDao`/`AuftragsCache`
  wiederverwenden). Menü-URL auf `next/` schalten.

**Verifikation.** Auftrag mit mehreren Positionen + Zahlungsplan
anlegen/ändern, Summen/Forecast gegen Wicket vergleichen, History prüfen.

### Phase 4 – Ablösung & Aufräumen

- Pro vollständig migrierter Seite: Menü auf `next/`, alte Route deaktivieren.
- Wenn alle Seiten migriert: `projectforge-webapp` und `projectforge-wicket` aus
  `settings.gradle.kts` + Build entfernen, `/react`- und `/wa`-Serving/Filter
  (`WebApplicationConfig`, `WebXMLInitializer`) entfernen, ggf. `NEXT_APP_PATH` →
  `/` als Default.
- `lib/api-client.ts` (veralteter Zweit-Client, unbenutzt) entfernen; `lib/rs/`
  bleibt einzige Backend-Schnittstelle.
- `components/features/books/mock-data.ts` (seit Entfernen der Mock-Routen
  unbenutzt) entfernen oder auf MSW umstellen.
- `_parked/[category]/` reaktivieren, sobald der Dynamic-Renderer trägt (Phase 2)
  – dort liegen die generischen List-/Form-Routen, die für den Static Export
  vorübergehend aus `app/` genommen wurden.
- `app/(authenticated)/page.tsx` ist noch die Starter-Vorlage; `demo/` ist eine
  Design-Referenz und kein Produktivziel.

## Kritische Dateien (Referenz)

- **Serving/Routing:** `projectforge-application/.../config/WebApplicationConfig.java`,
  `.../config/WebXMLInitializer.java`, `projectforge-business/.../Constants.kt`
- **Auth/Session:** `SpringSecurityConfig.kt`, `LoginService.kt`,
  `WicketUserFilter.kt`, `RestUserFilter.kt`
- **Menü:** `projectforge-business/.../menu/builder/MenuItemDefId.kt`,
  `MenuCreator.kt`; `projectforge-rest/.../MenuRest.kt`
- **Dynamic-Renderer Backend:** `projectforge-rest/src/main/kotlin/org/projectforge/ui/`
  (`UILayout.kt`, `UIElementType.kt`, `ResponseAction.kt`, `LayoutUtils.kt`,
  `UICustomized.kt`), `rest/core/AbstractPagesRest.kt`, `PagesResolver.kt`
- **Dynamic-Renderer alt (Port-Vorlage):**
  `projectforge-webapp/src/components/base/dynamicLayout/`, `src/actions/form.js`
- **Dynamic-Renderer neu (Ausbau):** `projectforge-next/components/dynamic/*`,
  `lib/rs/{client.ts,types.ts}`
- **Next-Packaging-Vorlage:** `projectforge-webapp/build.gradle.kts`,
  `projectforge-application/build.gradle.kts`
- **Auftragsbuch:** `projectforge-wicket/.../web/fibu/AuftragEditForm.kt`,
  `projectforge-rest/.../fibu/AuftragPagesRest.kt`, `rest/dto/Auftrag.kt`

## Stand & nächste Schritte

Erledigt: **Phase 0** (Parallelbetrieb, Static-Export-Packaging) und **Phase 1**
(Menü-Schalter pro Seite; `BOOK_LIST` zeigt auf `next/books`).

Als nächstes:

1. **Phase 1.5** – `books` fertigstellen: Tabellen-Funktionen aus dem
   TanStack-Umbau der alten React-App übernehmen (Filter, Resizing, Spalten
   ein-/ausblenden, Pinning), dann das Filter-Panel echt anbinden oder entfernen.
   Erst danach weitere Seiten umziehen: `books` ist die Vorlage, die jede weitere
   Seite erbt.
2. **Phase 2** – Dynamic-Renderer ausbauen (bringt die ~36 UILayout-Seiten in der
   Masse) – profitiert direkt von der fertigen `DataTable`.
3. **Phase 3** – Auftragsbuch als handgebauter Härtefall (parallel zu Phase 2
   möglich).
