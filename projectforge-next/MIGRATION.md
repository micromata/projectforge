# Frontend-Migration nach projectforge-next

Dieses Dokument beschreibt das Zielbild und den schrittweisen Weg, das gesamte
ProjectForge-Frontend nach **projectforge-next** (Next.js, App Router) zu
migrieren – im Parallelbetrieb, so dass Releases pro Seite möglich sind.

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

`output: 'export'` ist deshalb **nur in Prod** gesetzt (`isProd` in
`next.config.ts`). Grund: der Dev-Server lehnt mit aktivem Export jeden
dynamischen Param ab, den `generateStaticParams()` nicht auflistet – jeder
Deep-Link (`/next/books/5`, `/next/address/edit/42`) antwortet dann mit 500, also
genau die URLs, die man testen will. In Prod funktionieren sie, weil Spring auf
die SPA-Shell (`404.html`) zurückfällt und der Client die Params zur Laufzeit
liest; der Dev-Server hat diesen Fallback nicht und will stattdessen
vorrendern. Das CI-Gate bleibt scharf, weil der Gradle-Build (`npmBuild`) immer
mit Export baut.

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

**Erledigt: Tabellen-Funktionen portiert.** Vorlage war der TanStack-Umbau der
alten React-App (`projectforge-webapp/src/.../components/table/` mit
`DynamicTanStackGrid.tsx` und `tanstack/*`) – konzeptuell übernommen, nicht
kopiert, da dort reactstrap/Bootstrap statt shadcn/Tailwind zum Einsatz kommt.

`components/data-table/` enthält jetzt: `data-table.tsx` (Rendering inkl. Pinning
und Resize-Handle), `data-table-column-panel.tsx` (Ein-/Ausblenden, Pinning,
Drag-Reorder), `column-filter.tsx` (Text/Zahl/Datum/Auswahl), `filter-fns.ts`,
`use-table-state.ts` (die sechs State-Slices),
`use-column-state-persistence.ts`, `types.ts` (`ColumnMeta`-Augmentation).

Wichtige Details für spätere Seiten:

- **`table-fixed` + `colgroup` sind zwingend.** Setzt man Breiten nur auf die
  Header-Zellen, layoutet der Browser nach Inhalt und Header/Body driften
  auseinander. Ebenso braucht die Tabelle eine **explizite** Breite
  (`getTotalSize()`, `minWidth: 100%`): mit `w-full` streckt der Browser die
  Spalten proportional, die gerenderte Breite weicht von `getSize()` ab und
  Resizing greift nicht.
- **Kein zusätzliches `<td>` pro Zeile.** Ein Extra-`<td>` (z.B. für einen
  Hover-Balken) belegt einen Spalten-Slot und verschiebt alle Zellen um eine
  Spalte. Dekoration gehört in ein Pseudoelement.
- **`meta.label` statt `columnDef.header`,** wo Klartext gebraucht wird
  (Spalten-Panel, aria-Labels): `header` rendert eine Komponente.
- **Spalten-Filter laufen clientseitig** – konsequent dazu, dass `getList` die
  ganze Liste liefert. Eine einzige `filterFn` dispatcht über die Wertform, damit
  der Filter-State JSON-serialisierbar bleibt (Voraussetzung für die Persistenz).
- **Zwei Bugs der Vorlage nicht mitportiert:** gepinnte Spalten nutzen
  `getAfter('right')` statt hartkodiert `0` (mehrere rechts gepinnte überlagerten
  sich), und der State-POST feuert nicht beim Mount (schrieb sonst bei jedem
  Seitenaufruf zurück, was er gerade geladen hatte).

**Erledigt: i18n wird generiert, nicht dupliziert.** Das Backend hat `columns.*`
und `filter.*` bereits vollständig in beiden Sprachen; die Frontend-Kataloge
hatten angefangen, sie zu kopieren.
`GenerateNextI18nMessagesMain` (in `DevelopmentMainForRelease`, direkt neben dem
Sortier-Schritt, der die Quelle pflegt) erzeugt `messages/generated.<locale>.json`
aus `I18nResources`. Keys werden per **Prefix** gewählt (`book.`, `columns.`,
`filter.`, …) statt enumeriert – ein paar unbenutzte Keys sind besser als eine
Liste, die veraltet. Die generierten Dateien liegen getrennt und werden **tief**
gemerged, damit frontend-eigene Texte (z.B. `login.username`, im Backend nicht
vorhanden) erhalten bleiben und sich einen Namespace teilen können.
Kollisionsfall `book.title` (Leaf) + `book.title.add` (Objekt) → `_`-Schlüssel.
**Regel:** neue UI-Texte zuerst in `I18nResources` anlegen, dann generieren.

**Erledigt: Auth-Flow vollständig in next.** Login, 2FA (inkl. WebAuthn),
Passwort-vergessen, Passwort-Reset per Token-Link und der In-Session-2FA-Dialog
laufen jetzt in `next`; die React-Anmeldung ist nur noch Rückfallebene (Löschung
als eigener Commit).

- **Neue schlanke JSON-Endpunkte** statt UILayout-Parsing:
  `org.projectforge.rest.pub.next.LoginNextRest` (`/rsPublic/nextLogin`),
  `TwoFactorLoginNextRest` (`/rsPublic/next2FALogin`), `PasswordResetNextRest`
  (`/rsPublic/nextPasswordReset`) und `org.projectforge.rest.my2fa.My2FANextRest`
  (`/rs/next2FA`). Alle delegieren an die unveränderten Services
  (`LoginService`, `My2FAServicesRest`, `WebAuthnServicesRest`,
  `PasswordResetService`, `UserService`) – **keine** Auth-Logik dupliziert.
  Grund: Im alten Protokoll steckt der Login-Fehler als `UIAlert` im Layout und
  die verfügbaren 2FA-Methoden ergeben sich aus den vom Server eingebauten
  Buttons. Genau das soll next nicht mehr lesen.
- **`TwoFactorMethods`-DTO** (`otp`/`sms`/`mail`/`webAuthn`) mit derselben
  Bedingungslogik wie `My2FAServicesRest.fillCodeCol`: SMS nur bei
  `smsConfigured` + gültiger Mobilnummer, WebAuthn nur bei registriertem Token,
  Mail beim Passwort-Reset gesperrt. So zeigt next keinen Button an, den der
  Server ablehnen würde.
- **Explizites `TWO_FACTOR_REQUIRED`** statt Rückschluss aus einem
  fehlgeschlagenen `userStatus`-Call. `GET nextLogin/status` liefert zusätzlich
  `motd` und den 2FA-Zwischenzustand – damit bleibt ein Browser-Reload während
  des 2FA-Schritts im 2FA-Formular (war vorher ein Bug).
- **In-Session-2FA ist transparent.** `RestAuthenticationUtils` antwortet
  next-Clients (Header `X-PF-Frontend: next`, sonst Referer) mit
  `403 {twoFactorRequired, expiryMillis}` statt einer `ResponseAction` mit
  `/react`-URL. `lib/rs/client.ts` fängt das zentral ab, der
  `TwoFactorProvider` öffnet den Dialog, danach wird der Request **einmal**
  wiederholt. `/rs/next2FA` steht dafür in `My2FARequestHandler.NO_2FA_URLS`.
- **`SessionCsrfService.checkToken` ist public**, weil next kein
  `PostData`/`ServerData` benutzt.
- **WebAuthn**: `lib/webauthn.ts` portiert die Konvertierung 1:1 aus
  `projectforge-webapp/src/utilities/webauthn.js` – das Backend erwartet
  base64url plus zurückgespiegelte `requestId`/`challenge`/`sessionToken`, nicht
  das `webauthn-json`-Format.
- **i18n konsequent generiert:** die Auth-Prefixe (`login`, `password`,
  `user.My2FACode.`, `user.changePassword.`, `webauthn.error.`, `username`,
  `cancel`, …) sind in `GenerateNextI18nMessagesMain` aufgenommen; die
  Frontend-Kataloge halten nur noch die Texte ohne Backend-Pendant
  (Zwischenzustände wie „Prüfe…", „Warte auf Token…").
- Der Passwort-Reset führt eigene Session-Bookkeeping (eigener Session-Key)
  neben `PasswordResetPageRest`: jedes Frontend erzeugt und verbraucht seinen
  eigenen Link, validiert wird der Token vom gemeinsamen `PasswordResetService`.
  Die Server-Garantien bleiben: 10-Minuten-2FA-Fenster, CSRF-Token, Mail-OTP
  gesperrt, Token nach Erfolg invalidiert.

**Sicherheits-Review gegen die React-Version (abgeschlossen).** Der Auth-Teil
wurde Code-für-Code gegen `LoginPageRest`, `My2FAServicesRest`,
`PasswordResetPageRest` und die React-Komponenten geprüft. Keine Rechte- oder
Authentifizierungslücke: Brute-Force (`LoginProtection`,
`My2FABruteForceProtection` über `internalCheckOTP`), Session-Fixation
(`LoginService.internalLogin`) und die serverseitige WebAuthn-Challenge aus der
Session werden vollständig wiederverwendet; `RegisterUser4Thread` ist überall in
`finally` geklammert. Behoben wurden:

- `LoginNextRest.login()` prüfte `ThreadLocalUserContext.userContext == null`.
  Der Thread-lokale Wert ist auf `/rsPublic/*` immer `null` (der
  `RestUserFilter` läuft nur auf `/rs/*`), also landete **jeder** Login im
  2FA-Dialog. Jetzt wie in `getStatus`: `getUserContext(request)?.new2FARequired`.
- `two-factor-provider.tsx` sammelt mehrere gleichzeitig unterbrochene Requests
  in einer Resolver-Liste; vorher scheiterten alle bis auf einen.
- `sanitizeRedirectUrl` (`lib/menu-url.ts`) verwirft `returnUrl`/`redirectUrl`
  mit Schema oder Host – `LoginServiceRest.getRedirectUrl` gibt den Wert
  ungeprüft zurück, `/next/login?returnUrl=…` war damit ein Open Redirect.
- `NextTwoFactorSupport.sendMailCode` prüft `isMail2FADisabledForUser` vorab
  (sonst `require` in `My2FAHttpService` → HTTP 500 statt Meldung).

Bewusst als Legacy-Parität belassen: `cancel` ist überall ein zustandsändernder
`@GetMapping` (Umstellung betrifft beide Frontends), das `last2FA`-Cookie wird
auch im Reset-Flow geschrieben, `CookieService.checkStayLoggedIn` stellt
`lastSuccessful2FA` nicht wieder her (Zeile auskommentiert), und `NO_2FA_URLS`
matcht per Prefix. Strenger als Legacy: `PasswordResetNextRest.setPassword`
erzwingt das 2FA serverseitig, `PasswordResetPageRest.post()` nur per UI.
Offen: `toPublicKeyOptions` reicht `extensions` nicht mehr durch – mit einem
echten Token im Browser prüfen.

**Erledigt: Spaltenzustand-Persistenz.** Breiten, Sichtbarkeit, Pinning,
Reihenfolge und Sortierung liegen serverseitig in den User-Prefs pro Entität und
gelten damit geräteübergreifend. Gespeichert wurde schon vorher über
`setColumnStates`, aber **nur beim UILayout-Aufbau zurückgelesen** (in die
ColumnDefs, `AGGridSupport.restoreColumnsFromUserPref`) – handgebaute Seiten
haben kein Layout dafür. Deshalb gibt es jetzt einen GET-Endpunkt
(`AbstractPagesRest.getColumnStates`, `RestPaths.COLUMN_STATES`), der den
`GridState` als natives TanStack-Format liefert. Die Tabelle montiert erst, wenn
er da ist – er initialisiert TanStacks State, ein Nachschieben würde mit den
Änderungen des Nutzers kollidieren. `columnFilters` wird bewusst **nicht**
wiederhergestellt (unsichtbar greifende Filter irritieren; das Backend liefert
sie ohnehin nie zurück).

**Erledigt: Listen-Filter als Pillen-Zeile.** Erst ein ein-/ausschiebbares
Seitenpanel, dann verworfen: 288px Spalte für ~40 Suchfelder, nur über eine
Schiene erreichbar, die den Tabellenkopf überlappte. Primäre Oberfläche ist nun –
wie bei den alten MagicFilters – eine Pillen-Zeile unter der Suchleiste: ein
„+“-Chip wählt ein Feld (cmdk, sucht Label _und_ rohe Id, damit technische Felder
findbar bleiben), Klick auf eine Pille bearbeitet sie im Popover, „Alle Filter“
öffnet einen Dialog mit allen Feldern im Raster. Der Dialog bearbeitet einen
Draft und übernimmt erst auf Knopfdruck, weil jede angewandte Änderung ein neuer
Query-Key ist. „Spalten“ sitzt in derselben Zeile – beide wirken auf die Liste,
nicht auf die Seite. Bausteine: `components/data-table/filter-pills.tsx`,
`filter-pill.tsx`, `filter-field-picker.tsx`, `filter-field.tsx`,
`filter-field-inputs.tsx`, `filter-field-grid.tsx`, `filter-all-dialog.tsx`,
`filter-value.ts`, `use-list-filters.ts`, `lib/rs/filter-elements.ts`,
`hooks/use-initial-list.ts`.

- **Die Felder kommen vom Backend**, nicht aus dem Frontend: `UINamedContainer("searchFilter")`
  mit `FILTER_ELEMENT`-Kindern, abgeleitet aus `baseDao.searchFields` plus vier
  Standardfeldern (Änderer, Änderungszeitraum, History-Wert, gelöscht). Für Bücher
  sind das 21 Felder über alle sechs Typen (STRING, DATE, TIMESTAMP, BOOLEAN,
  OBJECT, LIST). Jede weitere Entität funktioniert ohne Zutun.
- **Serverseitig** über `MagicFilter.entries` – anders als die Spalten-Filter im
  Header, die auf dem geladenen Ergebnis arbeiten.
- **Zwei Kontrakt-Fallen:** STRING wird zu `LIKE` über das _ganze_ Feld, die Zeile
  setzt daher Wildcards (`Larkin` fände sonst `Peter J. Larkin` nicht) und
  respektiert selbst gesetzte. Bereichsgrenzen heißen auf dem Draht `from`/`to`,
  nicht `fromValue`/`toValue` (`@JsonProperty`).
- **Pillen lesen die Werte so, wie sie eingegeben wurden:** LIST-Ids werden zu
  Anzeigenamen, Bereiche als „von – bis“, die LIKE-Wildcards wieder abgeschnitten;
  eine BOOLEAN-Pille zeigt nur das Label, „true“ trägt nichts bei.

**Erledigt: Spalten-Filter im Header.** Der Trichter sammelte Werte, wandte sie
aber nie an – `manualFiltering` ließ TanStack das ungefilterte Core-Model
zurückgeben, und `useMagicFilterQuery` schnitt die aktuelle Seite selbst heraus,
so dass ein Client-Filter nur diese Seite gesehen hätte. Da `getList` das ganze
Ergebnis liefert, gibt der Hook jetzt alle Zeilen zurück; Filtern und Paging macht
die Tabelle (sie filtert vor dem Paginieren), Sortierung und Suchstring gehen
weiter an Spring. Oberhalb von 20 verschiedenen Werten öffnet das Popover den
Vergleichs- statt den Auswahl-Filter: die Werteliste war unbegrenzt und
unvirtualisiert, eine Spalte mit fast eindeutigen Werten montierte Tausende
Komponenten pro Klick. Die Auswahl bleibt erreichbar, beschriftet mit ihrer Größe
(„Auswahl (4.312)“), damit die Kosten vor dem Umschalten sichtbar sind.

**Erledigt: Gespeicherte Filter.** Das sind die **Filter-Favoriten des Backends**
(`AbstractPagesRest`, `filter/select|create|update|rename|delete`, abgelegt in den
User-Prefs) – ein hier gespeicherter Filter erscheint also auch in der Liste der
alten React-Seite. Bausteine: `components/data-table/use-filter-favorites.ts`,
`filter-favorites-menu.tsx`, `filter-favorite-entry.tsx`, `lib/rs/client.ts`.

- **Kontrakt-Fallen:** `filter/delete` und `filter/rename` sind
  zustandsändernde `@GetMapping` (Legacy-Parität, s.o. bei `cancel`).
  `filter/create` erwartet den Filter **ohne** `id` – mit Id würde das Backend
  daraus ein Update machen. `filter/update` ersetzt den ganzen Favoriten, der
  Name muss also mitreisen, sonst verliert er ihn; die Antwort ist eine **leere
  Map**. Nur `filter/select` antwortet mit einer vollen `InitialListData`.
- **Eine Quelle der Wahrheit für die Liste:** sie kommt mit `initialList` und wird
  im `["initialList", entity]`-Cache gepatcht (nicht in eigenem State gehalten).
  Gelesen wird über `useInitialList`, nicht per `getQueryData` – letzteres
  abonniert nicht, ein umbenannter Favorit würde nicht neu rendern.
- **Welcher Favorit angewandt ist, ist Client-State.** Das Backend speichert den
  aktuellen Filter bei _jedem_ Listenaufruf neu (`saveCurrentFilter`); seine `id`
  zurückzulesen würde einen Favoriten behaupten, dessen Werte der Nutzer längst
  geändert hat.
- Ein leerer Name ist erlaubt: `Favorites.fixNamesAndIds` vergibt „unbenannt“
  (`favorite.untitled`).

**Offen:**

1. **OBJECT- und TIMESTAMP-Felder vervollständigen.** OBJECT (z.B. „geändert
   durch“) nutzt derzeit ein einfaches Textfeld – für die Entitätssuche fehlt eine
   Autocomplete-Komponente gegen `autoCompletion.url`. Bei TIMESTAMP fehlt die
   Schnellauswahl (`selectors`: Jahr/Monat/Woche/Tag/bis-jetzt).
2. **Gesetzte Filter beim Seitenaufruf wiederherstellen.** Das Backend liefert den
   gespeicherten aktuellen `MagicFilter` in `initialList` mit; die Seite startet
   bislang bewusst leer (daran hängt auch der `currentId`-Punkt oben).
   `filter/reset` ist noch nicht angebunden.
3. **Zell-Rendering/Formatter fehlen noch.** Die alte App hat einen
   Formatter-Zoo (`Formatter.jsx`, `FormatterFormat.js`: Währung, Prozent,
   Datum/Timestamp, Task-Pfade, `displayName`-Auflösung), den der Dynamic-Renderer
   in Phase 2 braucht. Muster: Registry `name → Komponente`
   (`CellRendererDispatch.tsx`) – ohne die AG-Grid-Params-Hülle.
4. **CSRF-Schutz für die next-Aufrufe** – siehe eigener Abschnitt unten. Muss in
   `books` gelöst werden, bevor es in die Breite geht: jede migrierte Seite erbt
   den Mechanismus.
5. **Konventions-Drift:** `books`-Edit nutzt `@tanstack/react-form`, `CLAUDE.md`
   schreibt `react-hook-form` vor. Vor der Bulk-Migration entscheiden.
6. Nicht browserseitig verifiziert: englischer Locale-Pfad, vollständiger
   Login-Flow mit echten Daten, das visuelle Ergebnis der Tabelle
   (Spaltenbreiten, Resize, Popovers) und der Favoriten-Durchlauf
   (anwenden/anlegen/umbenennen/überschreiben/löschen).

#### Offen: CSRF-Schutz (querschnittlich, blockiert die Breite)

**Der Stand.** Die Authentifizierung hängt am `JSESSIONID`-Cookie, das der Browser
bei _jedem_ Request mitschickt – auch bei einem, den eine fremde Seite auslöst.
Der Schutz dagegen ist im Backend vorhanden (`SessionCsrfService`: Token pro
Session, 30 Zeichen, `NumberHelper.getSecureRandomAlphanumeric`), aber er greift
nur dort, wo er auch aufgerufen wird – und das ist an den `PostData`/`ServerData`-
Kontrakt der alten React-App gebunden: `createServerData(request)` legt das Token
in die `FormLayoutData` einer Edit-Seite, `validateCsrfToken(request, postData)`
liest es aus `postData.serverData` zurück. **next benutzt weder `PostData` noch
`ServerData`** und bekommt deshalb an keiner Stelle ein Token in die Hand.

Genau eine Ausnahme existiert schon und ist die Vorlage: der Passwort-Reset. Dafür
wurde `SessionCsrfService.checkToken(request, token)` public gemacht,
`PasswordResetNextRest` gibt das Token im `GET`-Aufruf mit
(`csrfToken = createServerData(request).csrfToken`) und prüft es beim `setPassword`
gegen die Session. Für alles andere in next fehlt es.

**Ungeschützt sind damit heute** alle zustandsändernden Aufrufe aus
`lib/rs/client.ts`:

- `setColumnStates` (`@PostMapping`, `updateColumnStates` prüft nichts – auch für
  die React-App nicht),
- `filter/create` und `filter/update` (`@PostMapping`, ohne Prüfung),
- `filter/rename`, `filter/delete`, `filter/select` – als `@GetMapping`
  zustandsändernd und damit sogar per `<img src>` auslösbar,
- `saveOrUpdate`/`markAsDeleted`/`delete`/`undelete`/`cancel`: die prüfen
  serverseitig **und würden einen next-Aufruf ablehnen**, weil ohne
  `serverData.csrfToken` `checkToken` fehlschlägt. Sobald die Edit-Seiten in next
  wirklich speichern, läuft das also auf. Zusätzlich antwortet der Fehlerfall mit
  einer `ResponseAction` (`TargetType.UPDATE`) – ein Format, das next nicht liest.

Der Schaden ist real, nicht theoretisch: Es gibt kein `SameSite`-Attribut in der
Konfiguration (nirgends gesetzt, weder Code noch `application.properties`), also
gilt der Browser-Default `Lax`. Der schützt Cross-Site-`POST`s, aber **nicht**
die zustandsändernden `GET`s oben – ein Link genügt, um einem eingeloggten Nutzer
Filter-Favoriten zu löschen.

**Zu entscheiden (Vorschlag):**

1. **Ein Token für die ganze Session an next ausliefern**, nicht pro Seite: am
   naheliegendsten in `userStatus` (holt next beim App-Start ohnehin) bzw. beim
   Login. Es liegt im Client in einem Modul-State neben dem 2FA-Handler, nicht in
   `localStorage` – ein XSS soll es nicht abgreifen können, und ein Reload holt
   `userStatus` neu.
2. **`request()` in `lib/rs/client.ts` schickt es bei jeder nicht-`GET`-Methode
   als Header** (`X-PF-CSRF-Token`) mit. Zentral, damit keine Aufrufstelle es
   vergessen kann – dieselbe Stelle, an der schon `X-PF-Frontend: next` und die
   2FA-Wiederholung sitzen. Ein Header ist dem Body-Feld vorzuziehen: er
   funktioniert auch für Aufrufe ohne Body und ist Cross-Site nicht setzbar.
3. **Serverseitig ein Filter/Interceptor für `/rs/*`**, der für next-Clients
   (`X-PF-Frontend: next`) bei jeder zustandsändernden Methode gegen
   `SessionCsrfService.checkToken` prüft und mit `403` antwortet – nicht mit einer
   `ResponseAction`. Der Rest-Client-Fall (`loggedInByAuthenticationToken`) bleibt
   ausgenommen, wie in `validateCsrfToken` schon vorgesehen.
4. **Die zustandsändernden `@GetMapping`s auf `POST` umstellen** – betrifft
   `filter/rename|delete|select`, `filterReset` und `cancel`. Das berührt beide
   Frontends, also entweder beide Aufrufstellen mitziehen oder die Methode
   zusätzlich anbieten, solange `/react` noch lebt.
5. **`SameSite=Lax` explizit setzen** (`server.servlet.session.cookie.same-site`)
   statt sich auf den Browser-Default zu verlassen – Defense in Depth, ersetzt
   Punkt 2/3 nicht. `Strict` würde die Rückkehr aus dem Passwort-Reset-Mail-Link
   brechen.

Punkt 4 und 5 sind Backend-Aufräumarbeiten und können später kommen; **1–3 sind
die Voraussetzung dafür, dass eine next-Seite überhaupt schreiben darf** – ohne
sie scheitert das erste echte Speichern aus `books`-Edit an `validateCsrfToken`.

### Phase 2 – Dynamic-Renderer in Next vervollständigen (Bulk-Migration)

Port der Referenz `projectforge-webapp/src/components/base/dynamicLayout/` nach
`projectforge-next/components/dynamic/`.

#### Erledigt: Protokoll-Fundament (Stand 08/2026)

Das Fundament steht, nachgewiesen an genau einer Seite (`address/edit`). Kein
Menü-Eintrag zeigt bislang auf eine dynamische next-Seite; die Route wird direkt
aufgerufen (`/next/address/edit/42`).

- **Frontend-Wahl serverseitig zentralisiert:**
  `projectforge-business/.../NextMigration.kt` ist die einzige Stelle, die über
  `/react` vs. `/next` entscheidet. `PagesResolver` (kein `REACT_PATH` mehr),
  `AbstractPagesRest.addNewEntryUrl`/`getStandardEditPage` und `MenuItemDefId`
  fragen dort. Eine Seite umschalten = ein Eintrag in `NextMigration.MIGRATED`.
  Kategorie und next-Route dürfen abweichen (`book` → `books`), ebenso die
  Edit-Route handgebauter Seiten (`books/:id` statt `books/edit/:id`).
  Review-Gate bei jedem Flip: `grep -rn "REACT_APP_PATH"` – ca. 10 verstreute
  Literale (Timesheet, MyAccount, TeamEvent, Kalender, Login) und 7
  Plugin-Menü-Literale zeigen noch direkt auf `/react`.
- **`ResponseAction`-Interpreter** vollständig: `components/dynamic/use-dynamic-actions.ts`
  deckt REDIRECT, UPDATE (inkl. `merge`), GET/POST/PUT/DELETE mit rekursivem
  Feedback (Tiefe max. 5), RELOAD (`invalidateQueries`, kein `location.reload`),
  CHECK_AUTHENTICATION, DOWNLOAD, NOTHING und die Message-Toasts ab.
  406 → `validationErrors` in den State, pro Feld über `fieldId` gerendert.
  MODAL/CLOSE_MODAL sind Notlösungen (Seite öffnen bzw. `router.back()`).
- **`watchFields`** verdrahtet: `setData` diffed pfadbewusst gegen
  `ui.watchFields` (`lib/dynamic/watch-fields.ts`), sammelt 150 ms und postet
  `{category}/watchFields`.
- **CSRF** gelöst: `serverData` liegt im Layout-State und wird in jedem `PostData`
  zurückgespiegelt; ein neues `serverData` in einer Antwort ersetzt es. Pro Seite,
  nie global.
- **Form-Handling entschieden:** dynamische Seiten bleiben kontextgetrieben
  (`data`/`setData`/`validationErrors`), **ohne Form-Library** – das Feldset ist
  serverdefiniert und die Validierung kommt als 406 vom Server, ein zur Laufzeit
  generiertes Zod-Schema wäre wertlos. Handgebaute Features nutzen weiter
  `@tanstack/react-form`. `CLAUDE.md` ist entsprechend präzisiert.
- **Element-Typen** für `address/edit`: `INPUT` löst über `dataType` auf
  (`components/input/dynamic-input-resolver.tsx`), `SELECT`/`CREATABLE_SELECT`
  (single/multi × feste Werte/Server-Lookup) in `components/select/`,
  `RADIOBUTTON`, `LIST`, Autocomplete, Date. Alles Übrige rendert als
  `DynamicFallback` – in dev sichtbar, damit Lücken beim Diff auffallen.
- **Routen:** `app/(authenticated)/[category]/[type]/[...params]/` mit
  Platzhalter-`generateStaticParams()` und `useParams()` zur Laufzeit; Deep-Links
  liefert `NextSpaResourceResolver` über `404.html`. Achtung Route-Shadowing:
  konkrete Routen (`books`) gehen dem Catch-all vor – `HAND_BUILT_CATEGORIES` in
  `page-client.tsx` hält das synchron mit `NextMigration.MIGRATED`.

Verbliebene Lücken:

1. **`DataTable`-Integration** für Listen: `components/dynamic/components/dynamic-table.tsx`
   ist eine handgeschriebene `<table>` (liest nur `hide`) und sollte durch die nun
   vollständige `DataTable` ersetzt werden. Dafür braucht es einen **Adapter**
   `UIAgGridColumnDef → ColumnDef` (Vorlage: `tanstack/tableUtils.ts`
   `buildColumnDefs`), der die AG-Grid-Wire-Namen normalisiert:
   `filter: 'agTextColumnFilter'|'agNumberColumnFilter'|'agDateColumnFilter'` →
   `FilterKind`, `type: 'numericColumn'|'rightAligned'` → `meta.align`,
   `headerName` → `meta.label`, `hide`/`pinned`/`width` → Initial-State.
   `sortModel: [{colId, sort}]` → `SortingState`. So bleibt „AgGrid" in der
   Adapter-Schicht und sickert nicht in die Komponenten.
2. **Fehlende UIElement-Typen** aus `UIElementType.kt` ergänzen: Entity-Picker
   (USER, GROUP, EMPLOYEE, COST1, COST2, KONTO, TASK, LOCALE, TIMEZONE, PICTURE),
   RATING, EDITOR, ATTACHMENT_LIST, DROP_AREA, PROGRESS, `pageMenu`.
3. **`MODAL`/`CLOSE_MODAL`** richtig: der `location.state.background`-Trick des
   alten Routers existiert im App Router nicht. Später über einen Modal-Stack in
   `store/ui-store.ts` + `ui/dialog.tsx`; Trade-off: keine teilbaren
   Modal-Deep-Links (Konsequenz des Static-Exports).
4. **`UICustomized`-Escape-Hatch** (alt: ~30 String-IDs → bespoke Komponenten)
   als Registry nachbauen; die Komponenten selbst sind manuelle Ports
   (Adress-Bild/Telefon/VCard-Import, `book.lendOutComponent`,
   Kalender-Recurrency, Cost-Number, Invoice-Positionen, WebAuthn, `access.table`,
   …).

**Nicht mitportieren – Altlasten der Vorlage:**

- **`getRowClass` und `rowClickFunction` als JavaScript-Strings**, die per
  `Function(...)` bzw. `window.<name>` ausgeführt werden (`UIAgGrid.kt` liefert
  z.B. `"if (params.node.data?.deleted) …"`). Das ist Codeausführung aus einer
  Server-Response und in Next.js zusätzlich CSP-problematisch. Deklarativ
  ersetzen (Row-Class aus `row.original.deleted` ableiten).
- **AG-Grid-Params-Hüllen** (`{ data, value, colDef: { field, cellRendererParams } }`)
  durch die Renderer-Kette. Im Port `{ row, value, columnId }`.
- **`filterModel`** – Prop existiert, wird nie gelesen, Backend liefert immer `{}`.
- **`FilterPortal.tsx`** (händisches Popover mit Collision-Detection) – Radix/
  shadcn `Popover` deckt das ab; im Port bereits so gelöst.
- **`modifyRedirectUrl`**: ersetzt Platzhalter für _jedes_ Row-Feld, auch als
  Pfadsegment (`/feld` → `/wert`). Auf explizites `{id}`/`:id`-Matching reduzieren.

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
  unbenutzt) entfernen – oder für Tests mit `msw` nutzen, das als Dependency
  vorhanden, aber nirgends eingebunden ist. Es gibt bislang **keine Tests**;
  `filter-fns.ts` und `lib/menu-url.ts` wären reine Funktionslogik und ein guter
  Anfang.
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
- **CSRF:** `projectforge-rest/.../rest/core/SessionCsrfService.kt`,
  `rest/dto/ServerData.kt`, `AbstractDynamicPageRest.kt` (`createServerData`/
  `validateCsrfToken`); next-Vorlage `rest/pub/next/PasswordResetNextRest.kt`;
  Cookie-Flags `CookieService.kt`
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
- **i18n:** Quelle `projectforge-business/src/main/resources/I18nResources[_de].properties`;
  Generator `projectforge-application/src/test/kotlin/org/projectforge/development/GenerateNextI18nMessagesMain.kt`
  (läuft via `DevelopmentMainForRelease`); Frontend `projectforge-next/i18n/`,
  `messages/` (`generated.*.json` nicht von Hand ändern)
- **Tabelle:** `projectforge-next/components/data-table/*`; Port-Vorlage
  `projectforge-webapp/src/components/base/dynamicLayout/components/table/`
  (inkl. `tanstack/`); Backend-Spaltenzustand
  `projectforge-rest/.../core/aggrid/AGGridSupport.kt`, `GridState.kt`,
  `rest/dto/datatable/DataTableStateRequest.kt`
- **Auftragsbuch:** `projectforge-wicket/.../web/fibu/AuftragEditForm.kt`,
  `projectforge-rest/.../fibu/AuftragPagesRest.kt`, `rest/dto/Auftrag.kt`

## Stand & nächste Schritte

**Erledigt:**

- **Phase 0** – Parallelbetrieb, Static-Export-Packaging, client-seitige i18n.
- **Phase 1** – Menü-Schalter pro Seite; `BOOK_LIST` zeigt auf `next/books`.
- **Phase 1.5, größter Teil** – `MagicFilter`-Kontrakt (Listen laden wieder),
  Tabellen-Funktionen portiert (Resizing, Spalten ein-/ausblenden, Pinning,
  Reorder, Spalten-Filter), Spaltenzustand-Persistenz, Listen-Filter als
  Pillen-Zeile inkl. gespeicherter Filter (Backend-Favoriten),
  i18n-Generierung aus `I18nResources`.
- **Auth-Flow** – Login, 2FA inkl. WebAuthn, Passwort-vergessen/-Reset und
  In-Session-2FA-Dialog laufen in next (React-Login nur noch Rückfallebene).

**Als nächstes:**

1. **CSRF-Schutz verdrahten** (s. eigener Abschnitt in Phase 1.5). Zieht sich durch
   alle Seiten und blockiert das erste echte Speichern aus next – deshalb vor der
   Bulk-Migration.
2. **Phase 1.5 abschließen:** OBJECT-Autocomplete und TIMESTAMP-Schnellauswahl,
   gesetzte Filter beim Seitenaufruf wiederherstellen. Vorher das visuelle
   Ergebnis der Tabelle und den Favoriten-Durchlauf im Browser prüfen – das steht
   noch aus.
3. **Phase 2** – Dynamic-Renderer ausbauen (bringt die ~36 UILayout-Seiten in der
   Masse). Profitiert direkt von der fertigen `DataTable`; braucht als ersten
   Schritt den `UIAgGridColumnDef → ColumnDef`-Adapter und die Formatter.
4. **Phase 3** – Auftragsbuch als handgebauter Härtefall (parallel zu Phase 2
   möglich).
5. **Auth im Browser durchspielen** (steht noch aus, s. Liste unten) und danach
   den React-Auth-Code löschen: `WebAuthnAuthenticate.jsx`,
   `actions/authentication.js`, `/react/public/login`-Routing.

**Reihenfolge-Grundsatz:** `books` bleibt die Vorlage – was dort fehlt, fehlt
jeder migrierten Seite. Deshalb erst `books` fertig, dann in die Breite.
