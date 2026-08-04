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
| **projectforge-next** | `projectforge-next` | `/next/**` (Ziel) | Next.js 16 App Router, statisch exportiert |

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
- Die Mock-Route-Handler (`app/rs/book/*`) sind ein reines **Dev-Hilfsmittel** und
  existieren in Prod nicht.
- **i18n:** `i18n/request.ts` (heute server-seitig, hartkodiert `de`) muss auf
  **client-seitige** Locale-Ermittlung umgestellt werden (Cookie /
  `userStatus.locale`).
- **Routen:** dynamische Segmente (`[category]`, `[id]`) müssen statisch
  exportierbar sein – `generateStaticParams` oder Client-seitiges Catch-all-Routing.
- **CI-Gate:** Der Gradle-Build baut immer den Static Export – so werden Dev-only-
  Abhängigkeiten beim Build sichtbar, nicht erst in Prod.

## Phasen

### Phase 0 – Parallelbetrieb herstellen

1. `basePath: "/next"` in `next.config.ts` (statt `/react`); same-origin-Prefixe
   in `lib/rs/client.ts` entsprechend anpassen.
2. Spring-Serving für `/next`: View-Controller-Forward analog
   `WebApplicationConfig.java:43` → `/next/**` → `forward:/next-app.html`. Neue
   Konstante `NEXT_APP_PATH = "next/"` in `Constants.kt`.
3. Gradle-Packaging analog `projectforge-webapp/build.gradle.kts`: Node-Plugin,
   `npmBuild` (`next build` → `out/`), Copy nach `build/resources/main/static`,
   Einbindung in `projectforge-application/build.gradle.kts`
   (`processResources`/`bootJar` dependsOn). projectforge-next bleibt Build-
   Artefakt-Lieferant, kein Kotlin-Compile-Modul.
4. i18n client-seitig umstellen.
5. Static-Export-Kompatibilität herstellen (`output: 'export'`, dynamische Routen
   auf Client-Resolution), Dev-Server :3000 für HMR beibehalten.

**Verifikation.** Dev: `next dev` (:3000) + `bootRun` (:8080), Live-Reload, Login
geteilt. Prod-Simulation: `next build` (`export`) fehlerfrei, Boot-Jar mit
eingebetteten Assets, `/react` und `/next` erreichbar, ein Login authentifiziert
beide.

### Phase 1 – Menü-gesteuertes Routing pro Seite

`MenuItemDefId.kt` unterscheidet URLs bereits nach Wicket (`wa/...`) vs. React;
um `next/...`-Ziele erweitern. Der Menü-Client (`top-navigation.tsx`,
`MenuRest.kt`) öffnet `next/`-, `react/`- und `wa/`-Links als jeweiliges Frontend.
Pro migrierter Seite wird die Menü-URL auf `next/...` umgestellt – das ist der
Release-Schalter je Seite.

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
- `lib/api-client.ts` (veralteter Zweit-Client) entfernen; `lib/rs/` bleibt
  einzige Backend-Schnittstelle.

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

## Empfohlene Reihenfolge

1. Phase 0.1–0.2 (basePath `/next` + Spring-Forward) – kleinster Schritt zum
   echten Parallelbetrieb im Dev.
2. Phase 0.3 (Gradle-Packaging) + Static-Export-Kompatibilität.
3. Phase 1 (Menü-Schalter) – ab hier parallele Releases pro Seite möglich.
4. Danach parallel: Phase 2 (Renderer-Ausbau für die Masse) und Phase 3
   (Auftragsbuch als handgebauter Härtefall).
