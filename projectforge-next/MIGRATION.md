# Frontend-Migration nach projectforge-next

Zielbild und Weg, das gesamte ProjectForge-Frontend nach **projectforge-next**
(Next.js, App Router) zu migrieren – im Parallelbetrieb, mit Releases pro Seite.

Dieses Dokument hält das **Zielbild, die tragenden Constraints und die für weitere
Migrationen wichtigen Regeln** fest. Bereits erledigte Arbeit ist bewusst nur so weit
beschrieben, wie eine künftige Seitenmigration sie kennen muss; die Details stehen im
Code und in den Tests. Zwei Detailpläne liegen daneben:
**[MIGRATION-calendar.md](MIGRATION-calendar.md)** (Kalenderseite),
**[MIGRATION-list-paging.md](MIGRATION-list-paging.md)** (serverseitiges Paging).
**[MIGRATION-TaskTree.md](MIGRATION-TaskTree.md)** dokumentiert den abgeschlossenen
Aufgabenbaum.

## Ausgangslage: drei Frontends, ein Backend

Alle Frontends werden von der einen Spring-Boot-App auf `:8080` serviert:

| Frontend              | Modul                 | Pfad        | Technik                                                          |
| --------------------- | --------------------- | ----------- | ---------------------------------------------------------------- |
| Wicket (Legacy)       | `projectforge-wicket` | `/wa/*`     | server-rendered, Servlet-Filter (`WebXMLInitializer.java`)       |
| Alte React-App        | `projectforge-webapp` | `/react/**` | backend-getriebener „Dynamic Renderer" (UILayout-JSON), CRA→Vite |
| **projectforge-next** | `projectforge-next`   | `/next/**`  | Next.js 16 App Router, statisch exportiert                       |

**Geteilte Authentifizierung.** Spring Security ist `permitAll`
(`SpringSecurityConfig.kt`); die Authentifizierung übernehmen PF-Servlet-Filter
(`WicketUserFilter`, `RestUserFilter`) über eine gemeinsame `HttpSession`
(`JSESSIONID`). Alle drei Frontends teilen diese Session per Cookie – das ist die
Grundlage für den Parallelbetrieb.

**Backend-getriebener „Dynamic Renderer".** Die alte React-App rendert Seiten generisch
aus `UILayout`-JSON, das der Server pro Seite beschreibt (`projectforge-rest/.../ui/`,
`rest/core/AbstractPagesRest.kt`); Interaktionen laufen über
`ResponseAction`/`TargetType`. Rund 36 Entitäten sind UILayout-basiert.

## Zielbild

projectforge-next bildet das **gesamte** Frontend ab. Dynamic-Renderer der alten
React-App **und** Wicket werden vollständig entfernt. Komplexe Wicket-Seiten
(Auftragsbuch, Kalender, Aufgabenbaum) werden handgebaut – dort stößt der
backend-gesteuerte Renderer prinzipiell an seine Grenzen.

## Grundsatzentscheidungen

1. **Koexistenz per eigenem Pfad.** projectforge-next hat `basePath: "/next"`, React
   bleibt auf `/react`, Wicket auf `/wa`. Das Backend-Menü (`MenuItemDefId.url`)
   entscheidet pro Seite, welches Frontend geladen wird → parallele Releases pro Seite.
2. **Prod = Static Export.** `next build` mit `output: 'export'` erzeugt statische
   Assets, per Gradle in die Spring-Boot-Jar gepackt, same-origin unter `/next`.
3. **Dynamic-Renderer Dual-Track.** Der UILayout-Renderer in `components/dynamic/` wird
   zum vollwertigen Port ausgebaut (bringt die ~36 Seiten in der Masse); komplexe Seiten
   werden zusätzlich handgebaut (Muster: `book`).

## Zwei Betriebsmodi (tragender Constraint)

- **Dev:** Next-**Node-Server** auf `:3000` (`next dev`), volle HMR. API-Calls per
  `next.config.ts`-`rewrites()` (bzw. Spring-CORS) ans Backend auf `:8080`.
- **Prod:** **Kein Node-Server.** Reiner Static Export, von Spring unter `/next`.

`output: 'export'` ist deshalb **nur in Prod** gesetzt (`isProd` in `next.config.ts`):
der Dev-Server lehnt mit aktivem Export jeden dynamischen Param ab, den
`generateStaticParams()` nicht auflistet (Deep-Links → 500). In Prod fällt Spring auf die
SPA-Shell (`404.html`) zurück und der Client liest die Params zur Laufzeit. Das CI-Gate
bleibt scharf, weil der Gradle-Build (`npmBuild`) immer mit Export baut.

### Constraint: Dev-Komfort darf die Prod-Tauglichkeit nicht brechen

Feature-Code darf **keine** Features nutzen, die einen laufenden Node-Server voraussetzen:

- Keine SSR/Server-Runtime-Logik, keine Route Handlers (`route.ts` ist mit
  `output: 'export'` inkompatibel; Mocks gehören in MSW o.Ä.), keine `rewrites()` als
  Prod-Mechanismus.
- **API-Calls sind root-relativ**, nicht mit basePath geprefixt: Spring serviert `/rs` +
  `/rsPublic` an der Origin-Root. `lib/rs/client.ts` muss in **beiden** Modi funktionieren
  (relativer Basis-Pfad, `credentials: "include"`; Dev-`rewrites()` mit `basePath: false`).
- **i18n** läuft client-seitig (Cookie / `userStatus.locale`, `i18n/config.ts`,
  `i18n/locale-provider.tsx`). `NextIntlClientProvider` braucht eine explizite `timeZone`.
- **Routen:** dynamische Segmente (`[category]`, `[id]`) müssen statisch exportierbar sein
  – `generateStaticParams` (Platzhalter) + Client-Component, ID via `useParams`. Wer
  `useSearchParams` nutzt, braucht eine `<Suspense>`-Grenze.

**Serving** (`WebApplicationConfig`): ein `PathResourceResolver` liefert echte Dateien
und Assets zuerst, dann `<route>/index.html` bzw. `<route>.html`, fehlende Assets → echter
404, Page-Routen ohne eigene Datei → `404.html` als SPA-Shell (Deep-Links/Bookmarks). Der
Wurzelpfad `/next/` braucht einen expliziten View-Controller.

## Bei jeder Seitenmigration mitzuziehen: die 2FA-Kurzbefehle

Wicket und React sichern den zweiten Faktor über die **URL der Seite** ab (geprüft vom
`WicketUserFilter`). Eine Next-Seite ist eine statische Datei – kein Filter sieht ihre
URL. Abzusichern ist der **REST-Aufruf**, den die Seite macht. Zu **jeder** migrierten
Seite gehört deshalb ein Eintrag in `ProjectForge2FAInitialization`, dort wo die
Legacy-URL stand:

- **lesend:** die `*Rest`-Klasse der Kategorie über `registerShortCutClasses`
  (→ `/rs/<kategorie>` und darunter),
- **schreibend:** `WRITE:<kategorie>` im passenden `*_WRITE`-Kurzbefehl (→
  `saveorupdate`, `markAsDeleted`, `edit`, …). Die Kategorie ist der REST-Pfad, nicht der
  Dao-Identifier (`order`, nicht `auftrag`).

Beides ist nötig, weil ein Kurzbefehl für sich konfiguriert werden kann (Installation mit
`FINANCE_WRITE`, aber ohne `FINANCE`). `NextMigration2FATest` erzwingt die Regel: verlangt
die Legacy-URL einen zweiten Faktor, muss die REST-URL es auch – der Test schlägt fehl,
sobald eine Seite nach `MIGRATED` wandert, ohne dass ihr 2FA-Teil mitgezogen wurde.

## Der Umschalt-Mechanismus (eine Stelle)

`projectforge-business/.../NextMigration.kt` (`MIGRATED`) ist die **einzige** Stelle, die
`/react` vs. `/next` entscheidet. `PagesResolver`, `AbstractPagesRest`,
`MenuItemDefId` fragen dort. Eine Seite umschalten = ein Eintrag in `MIGRATED`. Kategorie
und next-Route dürfen abweichen (handgebaut: absichtlich gleich); die Menü-URL muss die
**Next-Route** nennen. `HAND_BUILT_CATEGORIES` (`lib/hand-built-categories.ts`) hält die
konkreten Routen synchron mit `MIGRATED` (Route-Shadowing: konkrete Routen vor dem
Catch-all). `NextMigrationTest` erzwingt die Gleichheit. Für eine zweite Perspektive einer
Entität unter abweichender Route gibt es `NextMigration.nextRouteUrl(category, route,
legacyUrl)` (Beispiel: Aufgabenbaum `next/taskTree` neben der Liste `next/task`).

## Querschnittliche Fundamente (erledigt)

Diese Schichten tragen jede migrierte Seite; neue Endpunkte/Seiten erben sie ohne Zutun.
Detail steht im Code – hier nur die Regeln und die Fallen, die wiederkehren.

### Backend-Kontrakt (`lib/rs/`)

- **`MagicFilter`** (`lib/rs/types.ts`): nur Felder, die die Kotlin-Klasse deserialisieren
  kann (kein `@JsonIgnoreProperties` → sonst HTTP 400). Seitengröße via
  `paginationPageSizeEntry()` als Filter-Eintrag, **nicht** als Top-Level-Feld.
- **Paging ist clientseitig.** `AbstractPagesRest.getList` paginiert nicht serverseitig,
  es liefert die ganze Liste (bis `maxRows`). Spalten-Filter und Paging macht die Tabelle;
  Sortierung und Suchstring gehen an Spring. (Serverseitiges Paging: eigener Plan, s.
  [MIGRATION-list-paging.md](MIGRATION-list-paging.md).)
- **Schreiben** (`lib/rs/entity.ts`, getrennt von `client.ts`): `PUT
  /rs/{entity}/saveorupdate` für Anlegen **und** Ändern (unterschieden an `data.id`), Body
  immer der `PostData`-Umschlag `{ data }`. `DELETE …/markAsDeleted` + `PUT …/undelete`.
  `RestPaths.DELETE`/`FORCE_DELETE` (zerstören auch Historie) sind absichtlich nicht
  angebunden. Antwort ist eine `ResponseAction`, **nicht** die Entität – die id eines neuen
  Datensatzes kommt als `variables.id`; die Hooks **invalidieren** danach und lesen neu.
- **HTTP 406 ist eine reguläre Antwort** und trägt `validationErrors`
  (`EntityWriteResult` = `ok | validationErrors`, nur echte Transportfehler werfen).
  Mapping auf die Felder: `lib/validation/server-errors.ts` legt sie per `fieldId` in den
  `onServer`-Slot von `@tanstack/react-form` (form-core leert ihn beim nächsten
  `change`/`blur`). Fehler ohne `fieldId` → `unassigned` (Toast) + `onServer.form`.
- **Fallen der Antwortform:** ein abgelaufenes CSRF-Token antwortet **HTTP 200** mit
  `ResponseAction` + `validationErrors` (`errorpage.csrfError`) – `write()` prüft
  `validationErrors` auch im Erfolgsfall. Eine `AccessException` sieht als **HTTP 200 mit
  reinem Toast** wie ein Erfolg aus: die vier CRUD-Endpunkte machen daraus über
  `handleException` ein **406**; eigene Endpunkte (`postEntityAction`) liefern `kind:
  "rejected"` (unterschieden vom Erfolg-mit-Warnung am `targetType`: Ablehnung ist `TOAST`,
  Erfolg trägt `REDIRECT`); lesende Aufrufe bekommen für next-Clients **403** + `RestError`
  (`useReadAccessGuard`). `isNextClient` wählt nur die Antwortform, ist keine
  Vertrauensgrenze.
- **`FormData`-Body:** `Content-Type` **nicht** setzen (der Browser setzt die Boundary).

### CSRF (querschnittlich, `RestCsrfProtection.kt`)

Zwei Schranken in `RestAuthenticationUtils.doFilter`, zentral für alle `/rs/*`; neue
Endpunkte erben den Schutz:

1. **`Sec-Fetch-Site`** (alle Clients, alle Methoden): nur `same-origin`/`none`; deckt auch
   zustandsändernde `@GetMapping`s. `same-site` wird bewusst abgelehnt.
2. **Session-Token im Header `X-PF-CSRF-Token`** (next-Clients, nicht-`GET`): aus
   `userStatus`, im Client in Modul-State (nicht `localStorage`), zentral in `rawRequest`
   gesetzt. Veraltet (Stay-logged-in rotiert die Session) → `403 {csrfTokenRequired}` →
   `client.ts` holt `userStatus` und wiederholt **einmal**.

`server.servlet.session.cookie.same-site=Lax` steht explizit in `application.properties`
(Defense in Depth; `Strict` bräche den Reset-Mail-Link).

### i18n (generiert aus dem Backend)

`I18nResources.properties` (EN) + `I18nResources_de.properties` (DE) sind die **Quelle**,
nicht `messages/*.json`. Neue Texte dort anlegen, den Key-Prefix in `PREFIXES`
(`GenerateNextI18nMessagesMain`) aufnehmen, dann `DevelopmentMainForRelease` laufen lassen
→ `messages/generated.<locale>.json`. **Nie** die generierten Dateien von Hand ändern
(`GenerateNextI18nMessagesTest` erzwingt es). `messages/de.json`/`en.json` halten nur
Texte ohne Backend-Pendant (Zwischenzustände). Ein Backend-Key, der Text **und**
Namensraum ist, liegt als `<key>._` im Katalog – `lib/leaf-key.ts` (`leafKeyOf`) fängt das
ab (`INSUFFICIENT_PATH` in next-intl sonst).

### Validierungs-Metadaten (generiert, nie im Frontend dupliziert)

Feldlängen, Typen, `required`, Zahlenbereiche und Enum-Werte sind im Backend genau einmal
deklariert – JPA `@Column(length, nullable)` + `@PropertyInfo(i18nKey, required, type,
min, max)`, zusammengeführt in `ElementsRegistry`. Frontend-Validierung ist reine
UX-Vorwegnahme; Autorität bleibt der Server (406).

- **Dynamische Seiten:** kommt zur Laufzeit im `UILayout` mit (`maxLength`/`required`/
  `dataType`) – durchreichen, nie eigene Grenzen erfinden.
- **Handgebaute Seiten:** generiert. `GenerateNextFieldMetadataMain` scannt alle
  `@Entity`-Klassen und schreibt `lib/metadata/<entity>.generated.ts` (kein Barrel,
  tree-shakebar; `GenerateNextFieldMetadataTest` vergleicht byteweise + meldet Waisen).
  `lib/validation/from-metadata.ts` macht daraus Zod-Bausteine, Meldungen über Marker
  (`lib/validation/markers.ts`, weil nur das rendernde Feld sein Label kennt). Ein
  Feldname, den die Metadaten nicht kennen, ist dank Literal-Union ein `tsc`-Fehler.

Grenze: geerbte Properties (`id`/`created`/`lastUpdate`/`deleted`) haben keine
`maxLength`/`nullable`; Collections und Fremd-DO-Referenzen sind ausgelassen (stehen in der
Datei der jeweiligen Entität).

### Zugriffsrechte im Formular

Wicket blendet Buttons aus, die der Nutzer nicht drücken darf; next baut sein Formular
selbst. Zwei Flags am DTO (`EntityAccessSupport`: `writeAccess`/`deleteAccess`), zentral in
`AbstractEntityRest.getById` gefüllt (aus denselben DAO-Aufrufen wie `UILayout.UserAccess`).
Gelesen in `lib/rs/entity-access.ts`: **fehlendes Flag = erlaubt** (`!== false`), `isNew`
separat (Insert-Recht ist Sache der Liste). Speichern-/Löschen-Button werden ohne Recht
**weggelassen** (nicht ausgegraut), Tastatur-Abkürzung mitgeprüft. Feldweise Rechte
(Beispiel `vollstaendigFakturiertWriteAccess`): Feld immer gerendert, ohne Recht read-only,
Hinweis = die Ablehnungsmeldung des Backends. Die Flags stoppen den ehrlichen Client, die
DAO den unehrlichen (kein Vorab-Check im `validate()`). **Grenze:** `page-def` kennt kein
`readOnlyWhen`; `readOnly` in `DeclaredField` ist statisch und wird von Select/Input/
TextArea/Checkbox ignoriert – solche Felder handrendern.

### DataTable, Filter, Persistenz

`components/data-table/` ist das **einzige** Tabellen-Primitiv (kein Hand-Rollen). Trägt
Resize, Spalten-Panel (Ein-/Ausblenden, Pinning, Drag-Reorder), Header-Filter (client,
Vergleichs- statt Auswahlfilter ab 20 Werten) und Zustands-Persistenz. Fallen:
`table-fixed` + `colgroup` + explizite Breite (`getTotalSize()`, `minWidth: 100%`) sind
zwingend; kein Extra-`<td>` pro Zeile; `meta.label` (nicht `columnDef.header`) für
Klartext.

- **Spaltenzustand** (Breiten/Sichtbarkeit/Pinning/Reihenfolge/Sortierung) liegt
  serverseitig in User-Prefs, geladen per GET (`getColumnStates`, `RestPaths.COLUMN_STATES`,
  natives TanStack-`GridState`). Tabelle montiert erst, wenn er da ist. `columnFilters` wird
  bewusst **nicht** wiederhergestellt.
- **Listen-Filter als Pillen-Zeile** (`filter-pills.tsx` u.a.): Felder kommen vom Backend
  (`UINamedContainer("searchFilter")` mit `FILTER_ELEMENT`-Kindern, aus `baseDao.searchFields`),
  wirken serverseitig über `MagicFilter.entries`. Fallen: STRING wird `LIKE` über das ganze
  Feld → Zeile setzt Wildcards; Bereichsgrenzen heißen auf dem Draht `from`/`to`
  (`@JsonProperty`). Feldtypen: STRING/DATE/TIMESTAMP/BOOLEAN/OBJECT (Entitätssuche gegen
  `autoCompletion.url`, speichert `{id, displayName}`)/LIST. TIMESTAMP hat `PeriodStepper`
  (Kalenderperioden) und `IntervalPresetsSelect` (rollende Fenster, nur bei `UNTIL_NOW`).
- **Gespeicherte Filter** = Backend-Favoriten (`filter/select|create|update|rename|delete`).
  Fallen: `create` **ohne** `id`; `update` ersetzt den ganzen Favoriten (Name muss
  mitreisen), Antwort leere Map; nur `select` liefert volle `InitialListData`. Die
  Filter-Referenz (`id`+`name`) muss bei **jedem** Listenaufruf mitgehen (`getList` speichert
  ihn als „aktuellen"). Eine Quelle der Wahrheit: der `["initialList", entity]`-Cache wird
  gepatcht (`useInitialList`), nicht eigener State. Änderungsmarker über `filterFingerprint`.
- **Gemerkter Filter:** `getList` ruft `saveCurrentFilter`; `initialList.filter` gibt ihn
  zurück (`useRememberedFilter`/`useRememberFilter`). Werte müssen beim ersten Render dastehen
  (Liste hinter Spinner bis `initialList` **und** `columnStates` da sind). Nicht
  wiederhergestellt: Seitengröße, Sortierung (kommt aus `columnStates`), Spalten-Filter.
- **Spalten-Vokabeln** am `ColumnBase`: `visible?: (ctx) => boolean` (Spalte existiert für
  diesen Nutzer/diese Installation nicht – Kontext ist `listMeta.variables`; **nicht**
  dasselbe wie eine ausgeblendete Spalte), `sortable: false` (berechneter Wert).
  `transient`-Flag für Sprung-Filter, die nicht als gemerkter Filter zurückbleiben sollen
  (`storeFilter=false`-Äquivalent, `ListPageRequest.doNotStore`).

### Datumseingabe international

Genau eine Komponente (`components/shared/date-input.tsx` + `date-input-calendar.tsx`),
bedient alle Aufrufstellen (`DynamicDateInput`, `RangeField`, `ComparisonFilter`,
`InputField type="date"`). **Es gibt kein `type="date"` mehr** – das ist die Prüfung. Wert
ist immer der ISO-String `yyyy-MM-dd`, nie ein `Date` (`Date`↔ISO nur in `dateOf`/`isoOf`,
`lib/date-parse.ts`, aus den drei Zahlen in lokaler Zone). Feldreihenfolge, Trennzeichen,
`weekStartsOn` aus `FormatContext`/`Intl.DateTimeFormat.formatToParts` (dieselbe Quelle wie
`formatDate`). **Offen:** `TIME`/`TIMESTAMP` bleiben native Inputs (keine Seite gibt
Uhrzeiten ein).

### Alles Locale-Abhängige durch einen Helfer

Dates, Zahlen, Währung, Texte sind **die des Nutzers** (`userData`). Formatierung:
`lib/format.ts` mit `useFormatContext()` – kein `toLocaleString()`/`Intl.*` an der
Aufrufstelle. Texte: `useTranslations` only. Vom Backend bereits formatierte Werte
(`sizeHumanReadable`, `lastUpdateFormatted`, …) unverändert übernehmen. Gilt in Tests
(`e2e/fixtures/format.ts` leitet aus `userStatus` ab).

### Geteiltes Edit-Gerüst (`PageDef`)

Submit-Ablauf, 406-Mapping, „gespeichert"-Toast, URL-Wechsel nach erstem Speichern,
Lösch-Bestätigung, Aktionsleiste liegen in `hooks/use-entity-edit-form.ts` und
`components/shared/edit/`. Seiten sind **Deklaration** (`PageDef`, s.
`docs/page-declarations.md`), nicht Kopie. Bausteine für weitere Seiten:

- `SectionDef.collapsed` (startet zugeklappt, öffnet beim Anker-Klick), `SectionDef.footer`
  (abschnittseigene UI mit eigenen Hooks unter dem Feldergrid, in derselben Karte).
- `EditDef.returnTargets` + `?returnTo=` (Rückkehr zum Aufrufer, Whitelist; Seiten ohne
  `returnTargets` verhalten sich unverändert; Parameter wandert über `entityTabs` mit).
- `readOnly`/`disabled` an `InputField`/`SelectField`/`TextAreaField`; `SelectField.valueType`
  (`string`/`number`/`boolean`).
- `PageDef.edit` ist optional (`defineListPage` statt `definePage`) – Zeilenklick/„Neu"
  führen dann auf die Legacy-Seite (`listMeta.legacyEditPage`).
- `EditDef.actions` (eigene POST-Aktionen, ein Submit-Weg über `onSubmitMeta`;
  `SubmitMeta.onWritten` für Buttons, die nach dem Schreiben weiterarbeiten).
- `EntityEditDialog` (`components/shared/edit/`) – Formular ohne Routing/Reiter, für
  Assistenten (`useEntityEditForm({ onSaved })`).
- **Historie** ist ein Tab (`?tab=history`), automatisch aus `EntityMetadata.historizable`
  – keine Seite deklariert ihn. Generisch: `lib/rs/history.ts`, `hooks/use-history.ts`,
  `components/shared/history/*`. Kommentar-Fähigkeit über `HistoryInfo.supportsUserComments`
  (`BaseDao.supportsHistoryUserComments`).
- **Anhänge** (`UIAttachmentList`) generisch: `components/shared/attachments/`,
  `lib/rs/attachments.ts`. Zwei Protokoll-Eigenheiten: jede Schreibantwort enthält die
  **ganze** neue Liste (`setQueryData`, kein Reload); eine Ablehnung ist **HTTP 200 mit
  `TOAST`** (`AttachmentWriteResult` = `ok | rejected`). Kein Lese-Endpunkt (steckt im
  Entitäts-DTO). Upload sequenziell (eine Datei/Call). Verschlüsselung nicht portiert.
- **Mehrfachauswahl/Massenupdate** (`AbstractMultiSelectedPage`, `MultiSelectionSupport`,
  `PageDef.massUpdate`): Zustand liegt in der HTTP-Session (Schlüssel = PagesRest-Klasse,
  TTL 60 min) → **Sticky Sessions**.
- **Excel-Export** generisch: `lib/rs/list-export.ts` (`downloadListExcel`).
- **JIRA-Issues als Links** (`components/shared/jira/`): Config reist einmalig über
  `userStatus` (`JiraClientConfig`, nicht über die öffentliche `SystemData`). `JiraLinkedText`
  (Zellen), `JiraIssuesLinks` (Formularfeld-Zeile), `makeJiraFieldLinks(fieldName)`
  (deklarative Felder). Offen für spätere Seiten: HRPlanning, Contract.

## Phasen

### Phase 0 – Parallelbetrieb ✅

`basePath: "/next"`, Static-Export-Packaging (Gradle-Modul `projectforge-next`,
`npmBuild` → `out/` → `static/next/`), client-seitige i18n, PathResourceResolver-Serving.
Details oben unter „Zwei Betriebsmodi".

### Phase 1 – Menü-gesteuertes Routing pro Seite ✅

`NextMigration.MIGRATED` als Umschaltpunkt (s.o.). `BOOK_LIST` war der erste
Release-Schalter. `lib/menu-url.ts` löst Menü-URLs auf (`next/` intern, `react/`/`wa/`/
absolut als Hard-Navigation); die alte React-App leitet `/next/*` per `RedirectToNext.tsx`
weiter.

### Phase 1.5 – `book` produktionsreif ✅

`book` ist die Referenz-Implementierung. Erledigt: `MagicFilter`-Kontrakt,
Tabellen-Funktionen, Spaltenzustand-Persistenz, Pillen-Filter inkl. Backend-Favoriten und
gemerktem Filter, i18n-Generierung, Validierungs-Metadaten, internationale Datumseingabe,
Anhänge, History-Tab, Zugriffsrechte, Ausleih-/Rückgabe-Aktion, Auth-Flow, CSRF-Schutz. Die
Regeln stehen oben unter „Querschnittliche Fundamente". Browserseitig verifiziert
(`e2e/*.spec.ts`).

**Auth-Flow** vollständig in next, Legacy gelöscht: Login, 2FA (inkl. WebAuthn),
Passwort-vergessen/-Reset, In-Session-2FA-Dialog. `/next/login` ist der **einzige** Login
aller drei Frontends (Wicket, React, `LogoutRest` leiten dorthin, Ziel als `?returnUrl=`).
Schlanke JSON-Endpunkte (`rest/pub/next/*`, `rest/my2fa/My2FANextRest`) delegieren an die
unveränderten Services – keine Auth-Logik dupliziert. Die Ziel-URL hält der Client (Session
rotiert beim Login). `sanitizeRedirectUrl` verwirft Schema/Host auf **beiden** Seiten (sonst
Open Redirect). Sicherheits-Review gegen die React-Version abgeschlossen (Brute-Force,
Session-Fixation, WebAuthn-Challenge wiederverwendet). **Offen:** `toPublicKeyOptions` reicht
`extensions` nicht durch – mit echtem Token prüfen.

**Access-Audit der neuen Endpunkte:** Wicket/UILayout blenden verbotene Menüs/Buttons aus –
diese Schranke fällt für next weg (Autorisierung bleibt bei den DAOs). Eine echte Lücke
gefunden und behoben (`EInvoiceCheckerPageRest` ohne Prüfung, jetzt `FIBU_ORGA_GROUPS`).
`AbstractDynamicPageRest`-Seiten haben keinen DAO-Backstop → jede Unterklasse prüft selbst
(KDoc, `BirthdayButlerPageRest` als Vorbild). `userAccess` ist ein UI-Hinweis, keine
Autorisierung.

### Phase 2 – Dynamic-Renderer in Next vervollständigen (Bulk)

Port von `projectforge-webapp/src/components/base/dynamicLayout/` nach
`components/dynamic/`. Fundament steht (nachgewiesen an `address/edit`); kein Menü zeigt
bislang auf eine dynamische next-Seite.

- **`ResponseAction`-Interpreter** vollständig (`use-dynamic-actions.ts`): REDIRECT,
  UPDATE (inkl. `merge`), GET/POST/PUT/DELETE mit rekursivem Feedback, RELOAD
  (`invalidateQueries`), CHECK_AUTHENTICATION, DOWNLOAD, NOTHING, Toasts, 406.
  MODAL/CLOSE_MODAL sind Notlösungen.
- **`watchFields`**, **CSRF** (`serverData` pro Seite im Layout-State),
  **Form-Handling** kontextgetrieben ohne Form-Library (`data`/`setData`/
  `validationErrors`) – s. CLAUDE.md.
- **Listen** rendern mit der echten `DataTable` (Adapter `lib/dynamic/grid/`,
  Zell-Formatter-Registry `components/data-table/cells/`). **Kein `Function()`/`eval`:**
  `valueGetter`/`valueFormatter` strikt als Punktpfad; `getRowClass` deklarativ aus den
  Prädikat-Formen der Sender. Verifiziert (`e2e/dynamic-grid.spec.ts`: `vacation`,
  `skillentry`, `address`).

**Verbliebene Lücken:**

1. Fehlende UIElement-Typen (`UIElementType.kt`): Entity-Picker (USER/GROUP/EMPLOYEE/
   COST1/COST2/KONTO/LOCALE/TIMEZONE/PICTURE – TASK ist über `TaskSelectField` da),
   RATING (da), EDITOR, ATTACHMENT_LIST, DROP_AREA, PROGRESS, `pageMenu`.
2. `MODAL`/`CLOSE_MODAL` richtig (Modal-Stack in `store/ui-store.ts`; kein teilbarer
   Modal-Deep-Link – Konsequenz des Static-Exports).
3. `UICustomized`-Escape-Hatch als Registry (~30 String-IDs → bespoke Komponenten,
   manuelle Ports).
4. Row-Renderer `customized`/`diffCell`/`importStatusCell` (Kontrakt mit `onClick`-JS),
   TREE_NAVIGATION-Klappen, Mehrfachauswahl, `?modal=true`, `highlightRowId`,
   serverseitige Sortierung.

**Nicht mitportieren – Altlasten der Vorlage:** `getRowClass`/`rowClickFunction` als
JS-Strings via `Function(...)` (Codeausführung aus Server-Response, CSP-problematisch) →
deklarativ ersetzen; AG-Grid-Params-Hüllen → `{ row, value, columnId }`; `filterModel`
(nie gelesen); `FilterPortal.tsx` (Radix `Popover` deckt es ab); `modifyRedirectUrl` auf
explizites `{id}`/`:id`-Matching reduzieren. **Die richtige Lösung für `getRowClass` ist
serverseitig:** ein strukturiertes `rowHighlights` in `UIAgGrid.kt` – die Mustertabelle im
Client ist die Brücke bis dahin.

### Phase 3 – Komplexe Wicket-Seiten handbauen

Fertig: **Auftragsbuch** (`order`), **Debitorenrechnungen** (`outgoingInvoice`),
**Kreditorenrechnungen** (`incomingInvoice`), **Strukturelemente/Aufgabenbaum** (`task`),
**Gruppen** (`group`). **Offen:** **Kalenderseite** (Detailplan
[MIGRATION-calendar.md](MIGRATION-calendar.md), Phasen A/B/D umgesetzt, C offen).

Alle handgebauten Seiten laufen über `PageDef` und die geteilten Bausteine oben. Was pro
Seite bemerkenswert und für weitere Migrationen lehrreich ist:

- **Auftragsbuch** – der Referenz-Härtefall (`AuftragEditForm.kt`, ~826 Z.): unbegrenzt
  wachsende, geschachtelte Sub-Formulare (Positionen), zweite Collection (Zahlungspläne),
  Live-Currency-Berechnung, Cross-Field-Autofill (Projekt→Kunde/PM/…), Forecast,
  Period-of-Performance. Dem `UILayout`-DSL fehlt das Primitive „wiederholbare,
  geschachtelte editierbare Sub-Entität mit Live-Server-Neuberechnung". Gelöst mit RHF+Zod
  gegen ein echtes geschachteltes DTO (`rest/dto/Auftrag.kt`) + REST-Endpunkte für
  Positions-CRUD und Live-Kalkulation (`AuftragDao`/`AuftragsCache` wiederverwendet).
  Feldweise Rechte am „vollständig fakturiert"-Flag (s. Fundamente).
- **Debitorenrechnungen** – erste **Liste-zuerst**-Seite (drei Schachtelungsebenen). Der
  Schalter wurde möglich durch drei Dokumentfunktionen, die vorher nur Wicket hatte, jetzt
  Endpunkte von `OutgoingInvoiceEntityRest`: Word-Export, E-Rechnung, Rechnungs-PDF – alle
  auf dem **persistierten** Stand. **E-Rechnung ist ein Abschnitt** (`SectionDef.footer`),
  kein Dialog: die Fehler sind Felder _dieses_ Formulars. Zwei Buttons „Speichern und
  XRechnung/ZUGFeRD" (deklarierte Actions, bleiben auf der Seite via `onWritten`); der
  Abschnitt blockiert nie. **Prozenteingabe** im Netto-Feld einer Kostzuweisung (`shareOf`
  in `NumberField`, `parsePercentInput`; Basis aus debounced `useInvoiceSums` mit
  `keepPreviousData`).
- **Kreditorenrechnungen** – die einfachere Schwester (keine E-Rechnung/PDF/Word/Kunde/
  Projekt/Leistungszeitraum). Die entity-agnostischen Rechnungs-Bausteine
  (Kostzuweisungen, Summen, `use-invoice-sums`, Statistikzeile) liegen in
  `components/shared/invoice/`, parametrisiert statt dupliziert; projekt-/kundengekoppelte
  Teile bleiben im Ausgangsrechnungs-Feature. `IncomingInvoiceEntityRest` (layoutfrei) +
  Mehrfachauswahl (SEPA-Transfer-Export). **TODO offen:** CSV/SEPA-Import-Assistent und der
  SEPA-Überweisungs-Export als eigene Seite bleiben auf Wicket/React.
- **Strukturelemente/Aufgabenbaum** – s. [MIGRATION-TaskTree.md](MIGRATION-TaskTree.md).
  Baum, Aktionsleiste, Edit-Seite, Listenperspektive, Assistent; `task` umgeschaltet,
  `TASK_TREE` → `next/taskTree`. Sprung zum Strukturelement (`task-edit-link.tsx`) und die
  Consumption-Bar (`consumption-cell.tsx`) zeigen auf next (die letzten hart gebildeten
  Legacy-URLs, umgestellt seit der Timesheet-Migration). Bewusst ausgelassen: die
  Aufgaben-Favoriten (`UserPrefArea.TASK_FAVORITE`) – die Auswahlfelder bieten die
  Schnellauswahl selbst.
- **Gruppen** – vierter Fall, obwohl `GroupPagesRest` ein `UILayout` liefert (die
  generische Route rendert nur den Grid-Knoten, ohne Filterzeile/Favoriten/Zahnrad/Excel
  wäre der Schalter ein Rückschritt). LDAP-Feld über das Anzeige-Flag
  `Group.ldapPosixConfigured`, generische Mehrfach-Entity-Auswahl
  (`entity-multi-autocomplete-field.tsx`), der Assistent legt seine Gruppe über den neuen
  `EntityEditDialog` an. `dynamic-form-dialog.tsx` damit gelöscht.

**Verifikation** durchgängig gegen die laufende Instanz (`e2e/*.spec.ts`,
`org.projectforge.rest.*`). Jede Spezifikation legt Wegwerf-Entitäten an und markiert sie
gelöscht.

### Phase 4 – Ablösung & Aufräumen

- Pro migrierter Seite: Menü auf `next/`, alte Route deaktivieren.
- Wenn alle Seiten migriert: `projectforge-webapp` und `projectforge-wicket` aus
  `settings.gradle.kts` + Build entfernen, `/react`- und `/wa`-Serving/Filter entfernen,
  ggf. `NEXT_APP_PATH` → `/` als Default.
- Aufräumen: `lib/api-client.ts` (unbenutzter Zweit-Client),
  `components/features/book/mock-data.ts` (oder für `msw`-Tests nutzen – bislang **keine**
  Tests; `filter-fns.ts`/`lib/menu-url.ts` wären ein guter Anfang), `_parked/[category]/`
  reaktivieren, `app/(authenticated)/page.tsx`/`demo/` sind Vorlagen, kein Produktivziel.

## Kritische Dateien (Referenz)

- **Serving/Routing:** `WebApplicationConfig.java`, `WebXMLInitializer.java`, `Constants.kt`
- **Umschalten:** `NextMigration.kt` (`MIGRATED`, `nextRouteUrl`),
  `lib/hand-built-categories.ts`, `NextMigrationTest`/`NextMigration2FATest`,
  `ProjectForge2FAInitialization`
- **Auth/Session:** `SpringSecurityConfig.kt`, `LoginService.kt`, `WicketUserFilter.kt`,
  `RestUserFilter.kt`; next `rest/pub/next/*`, `rest/my2fa/My2FANextRest.kt`, `lib/webauthn.ts`
- **CSRF:** `rest/core/RestCsrfProtection.kt` (in `RestAuthenticationUtils.kt`),
  `UserStatusRest.kt`, `lib/rs/client.ts`, `application.properties`,
  `SessionCsrfService.kt`, `CookieService.kt`
- **Validierungs-Metadaten:** `PropertyInfo.java`, `ElementsRegistry.kt`/`ElementInfo.kt`,
  `ValidationUtils.kt`, `AbstractPagesRestUtils.kt` (406), `EntityMetaDataRegistry.kt`;
  Generator `GenerateNextFieldMetadataMain.kt` (via `DevelopmentMainForRelease`); Frontend
  `lib/metadata/*`, `lib/validation/{from-metadata,markers,server-errors}.ts`
- **Entitäts-Schreibaufrufe:** `lib/rs/entity.ts`, `AbstractPagesRest.kt` +
  `AbstractPagesRestUtils.kt`, `RestPaths.java`
- **Zugriffsrechte:** `lib/rs/entity-access.ts`, `rest/dto/EntityAccessSupport.kt`,
  `AbstractEntityRest.getById`, `rest/dto/Auftrag.kt`, `AuftragRight.kt`,
  `components/shared/edit/{entity-edit-page,entity-edit-actions}.tsx`
- **Historie:** `AbstractPagesRest.kt` (`HistoryInfo`), `HistoryService.kt`,
  `BaseDao.kt` (`supportsHistoryUserComments`); `lib/rs/history.ts`, `hooks/use-history.ts`,
  `components/shared/history/*`, `components/shared/edit/entity-tabs.ts`
- **Menü:** `MenuItemDefId.kt`, `MenuCreator.kt`, `MenuRest.kt`
- **Dynamic-Renderer:** Backend `projectforge-rest/.../ui/` (`UILayout.kt`,
  `UIElementType.kt`, `ResponseAction.kt`, `UICustomized.kt`), `AbstractPagesRest.kt`,
  `PagesResolver.kt`; Vorlage `projectforge-webapp/src/components/base/dynamicLayout/`; neu
  `components/dynamic/*`, `lib/rs/{client,types}.ts`, `lib/dynamic/grid/*`
- **Tabelle:** `components/data-table/*`; Backend `core/aggrid/AGGridSupport.kt`,
  `GridState.kt`, `rest/dto/datatable/DataTableStateRequest.kt`
- **i18n:** `I18nResources[_de].properties`, `GenerateNextI18nMessagesMain.kt`,
  `projectforge-next/i18n/`, `messages/` (`generated.*` nicht von Hand ändern)
- **Härtefälle:** Auftragsbuch `AuftragEditForm.kt`, `OrderEntityRest.kt`,
  `rest/dto/Auftrag.kt`; Aufgabenbaum `web/task/*`, `TaskServicesRest.kt`,
  `TaskPagesRest.kt`, `rest/dto/Task.kt`, `components/shared/tasks/*`,
  `components/features/task/*`

## Stand & nächste Schritte

**Erledigt:** Phase 0, 1, 1.5 (inkl. Auth, CSRF, Metadaten, Datumseingabe, History,
Anhänge, Zugriffsrechte – s. Fundamente); Phase 2-Fundament + Listen auf der echten
`DataTable`; Phase 3 bis auf den Kalender (Auftragsbuch, Deb./Kred.-Rechnungen,
Strukturelemente/Aufgabenbaum, Gruppen).

**Als nächstes:**

1. **Kalenderseite** – zweiter handgebauter Härtefall und Standard-Startseite; Detailplan
   [MIGRATION-calendar.md](MIGRATION-calendar.md) (Phasen A/B/D umgesetzt, C offen). Zieht
   zwei Phase-2-Stücke mit: die 2-Segment-Route `[category]/[type]` (Neuanlage ohne id,
   `dynamic-form-page.tsx`) und den Query-String in `fetchDynamic` (bis in den Query-Key),
   dazu `COLOR_CHOOSER` in der `UICustomized`-Registry.
2. **Phase 2 in der Breite** – Dynamic-Renderer ausbauen (bringt die ~36 UILayout-Seiten in
   der Masse). Offen: restliche Entity-Picker-Elementtypen, `UICustomized`-Registry,
   tragfähiger MODAL-Stack. Hinter dem Kalender, der die Hälfte davon mitzieht.
3. **Serverseitiges Paging** – [MIGRATION-list-paging.md](MIGRATION-list-paging.md); reine
   Performance-Arbeit (Auftragsliste 7132 Zeilen, 2,68 s, der Rest sitzt im Server). Erst
   den schnellen Pfad aus Stufe 5 messen, bevor der Id-Listen-Cache aus Stufe 2 gebaut wird.
4. **Mehrere Testkonten** – erledigt (`E2ETestAccountsService` legt vier Rollen an, s.
   CLAUDE.md „Testing against the running system"). Verbleibende Restarbeit: Specs für die
   Ablehnungsfälle, die vorher nur als Erfolg prüfbar waren – 403 des E-Rechnungs-Prüfers,
   `useReadAccessGuard`, der `rejected`-Schreibpfad, der englische Locale-Pfad, die
   Gegenprobe zu `book-list-gear.spec.ts` (voller Reindex nur als Admin).
5. **Ein Spec für Favoriten umbenennen/löschen** (nur Anwenden/Speichern/Änderungsmarker
   sind abgedeckt).
6. **Auth-Restprüfungen mit echtem zweiten Faktor** – der Legacy-Login ist gelöscht, keine
   Rückfallebene. Offen: WebAuthn-Token (`toPublicKeyOptions`/`extensions`), OTP/SMS/Mail-2FA
   nach dem Login, der Reset-Mail-Token-Link, „angemeldet bleiben", der In-Session-2FA-Dialog.

**Reihenfolge-Grundsatz:** `book` war die Vorlage – was dort fehlte, fehlte jeder Seite.
Die Rolle ist auf die geteilten Bausteine übergegangen (`PageDef`,
`components/shared/edit/`, `components/data-table/`, `lib/rs/`): eine Fähigkeit, die eine
neue Seite braucht, gehört dorthin, nicht in ihr Feature-Verzeichnis – so bekommen die
schon migrierten Seiten sie mit.
