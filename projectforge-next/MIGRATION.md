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
   zusätzlich handgebaut (Muster: `book`).

## Zwei Betriebsmodi (wichtig)

- **Dev:** Der Next-**Node-Server** läuft auf `:3000` (`next dev`) – volle
  HMR/Hot-Code-Replacement für schnelle Entwicklung. API-Calls gehen per
  `next.config.ts`-`rewrites()` (bzw. Spring-CORS) an das Backend auf `:8080`.
- **Prod:** **Kein Node-Server.** Reiner Static Export, von Spring unter `/next`
  ausgeliefert.

`output: 'export'` ist deshalb **nur in Prod** gesetzt (`isProd` in
`next.config.ts`). Grund: der Dev-Server lehnt mit aktivem Export jeden
dynamischen Param ab, den `generateStaticParams()` nicht auflistet – jeder
Deep-Link (`/next/book/5`, `/next/address/edit/42`) antwortet dann mit 500, also
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
  SPA-Shell. Damit sind Deep-Links/Bookmarks (`/next/book/5`) möglich.
  Der Wurzelpfad `/next/` braucht zusätzlich einen expliziten View-Controller
  (leerer Resource-Pfad wird vom Resolver nicht aufgelöst).
- **API-Calls sind root-relativ**, nicht mit basePath geprefixt: Spring serviert
  `/rs` + `/rsPublic` an der Origin-Root, nicht unter `/next`. Die Dev-`rewrites()`
  brauchen daher `basePath: false`.
- **Mock-Route-Handler entfernt** (`app/rs/book/*`): `route.ts`-Handler sind mit
  `output: 'export'` grundsätzlich inkompatibel. `mock-data.ts` bleibt liegen,
  wird aber nirgends mehr importiert (Kandidat für MSW oder Löschung).
- **Static-Export-Anpassungen:** `/login` braucht eine Suspense-Boundary
  (`useSearchParams`); `book/[id]` ist in Server-Wrapper (`generateStaticParams`
  mit Platzhalter) + Client-Component (`page-client.tsx`, ID via `useParams`)
  geteilt.
- **i18n** ist client-seitig (`i18n/config.ts`, `i18n/locale-provider.tsx`):
  Cookie → Browser-Sprache → `de`, übernimmt nach Login `userData.locale`. Beide
  Kataloge sind gebündelt; `NextIntlClientProvider` braucht eine explizite
  `timeZone`, sonst schlägt das Prerendering fehl. Das next-intl-**Plugin** und
  `i18n/request.ts` sind entfernt.

**Verifiziert** gegen die laufende App: `/next/book/`, Deep-Links
`/next/book/5` und `/next/order/edit/5` liefern die Shell (200), Assets laden
korrekt, fehlende Assets ergeben 404, `/react` unbeschädigt. Der Gradle-Build ist
inkrementell (kein Node-Build ohne Änderung).

### Phase 1 – Menü-gesteuertes Routing pro Seite ✅ erledigt

`getNextListUrl()` in `MenuItemDefId.kt`; **`BOOK_LIST` ist auf `next/book`
umgestellt** – der erste Release-Schalter. Alle anderen Einträge zeigen weiter auf
`react/...` bzw. `wa/...`.

Beide Frontends müssen das Präfix beachten:

- **projectforge-next:** `lib/menu-url.ts` löst Menü-URLs auf → `next/` als
  internes Client-Routing, `react/`/`wa/`/absolut als Hard-Navigation.
  `MenuLink` in `top-navigation.tsx` nutzt das.
- **Alte React-App:** neue Route `/next/*` → `RedirectToNext.tsx` (analog zum
  bestehenden `/wa/*` → `RedirectToWicket`), in **beiden** Routen-Listen
  (`AuthorizedRoutes.jsx`, `ProjectForge.jsx`). Ohne das würde die alte App
  `next/book` als eigene Kategorie interpretieren und ins Leere laufen.

**Wichtig für weitere Umstellungen:** Die Menü-URL muss die **Next-Route** nennen,
nicht die **Next-Route**, die von der REST-Kategorie abweichen darf. Die
handgebauten Seiten benennen ihre Route deshalb absichtlich wie die Kategorie
(`book`, `cost1`) – so muss sich niemand einen Plural merken. Zeigt die URL auf
eine nicht existierende Route, liefert Spring stillschweigend die SPA-Shell und
die Seite bleibt leer.

### Phase 1.5 – `book` produktionsreif machen 🔶 in Arbeit

`book` ist die Referenz-Implementierung: Was hier nicht funktioniert, fehlt
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

**Erledigt: Auth-Flow vollständig in next, Legacy gelöscht.** Login, 2FA (inkl.
WebAuthn), Passwort-vergessen, Passwort-Reset per Token-Link und der
In-Session-2FA-Dialog laufen in `next`. `/next/login` ist der **einzige** Login
der Anwendung: Wicket (`WicketUserFilter`, `WicketUtils.redirectToLogin`), die
alte React-App (`actions/authentication.js`) und `LogoutRest` leiten dorthin um,
Ziel-URL als `?returnUrl=<urlencoded>` (`Constants.NEXT_LOGIN_URL` /
`NEXT_LOGIN_RETURN_URL_PARAM` in `projectforge-business`, weil der
`WicketUserFilter` dort liegt). Gelöscht: `LoginPageRest`,
`PasswordForgottenPageRest`, `PasswordResetPageRest`,
`My2FAPublicServicesRest` und die unbenutzte `login()`-Action der React-App.
`My2FAServicesRest` bedient damit nur noch angemeldete Nutzer:
`fillLayout4PublicPage` und der `afterLogin`-Zweig (`CHECK_AUTHENTICATION`) sind
weg, `WebAuthnAuthenticate.jsx` startet nicht mehr automatisch.

- **Die Ziel-URL hält der Client.** `LoginServiceRest.getRedirectUrl` liefert nur
  noch den Default (`/react/calendar`); die Referer-Auswertung und der
  Session-Key `originUrl` sind weg. Grund: Ein erfolgreicher Login rotiert die
  HttpSession (Session-Fixation, `LoginService.internalLogin`), also überlebt
  nichts, was der Server vor dem Login dort ablegt – das alte Login-Formular trug
  die URL über die Rotation in `serverData.returnToCaller`, ein JSON-Client hat
  diesen Rückweg nicht. In next steht der `returnUrl` ohnehin durchgehend in der
  Adressleiste, auch über den 2FA-Schritt. Der Server-Wert gilt daher nur für
  einen Login ohne angeforderte Ziel-URL (`app/login/page.tsx`, `goTo`).
  Symptom vorher: der explizit angeforderte `returnUrl` wurde nach dem Login
  durch `/react/calendar` ersetzt – gefunden von `e2e/login.spec.ts`.

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
- Der Passwort-Reset führt eigenes Session-Bookkeeping (eigener Session-Key).
  Die Server-Garantien des gelöschten `PasswordResetPageRest` bleiben:
  10-Minuten-2FA-Fenster, CSRF-Token, Mail-OTP gesperrt, Token nach Erfolg
  invalidiert; validiert wird der Token vom unveränderten
  `PasswordResetService`.
- **Dev-Umgebung:** unter `:8080` gibt es keinen Login mehr, solange kein
  Static-Export von next vorliegt. Entweder einmal
  `./gradlew :projectforge-next:npmBuild` laufen lassen, oder sich über den
  Dev-Server `:3000/next/login` anmelden – dank gemeinsamem `JSESSIONID` gilt
  die Session auch für `/wa/*` und `/react/*` unter `:8080`.

**Sicherheits-Review gegen die React-Version (abgeschlossen).** Der Auth-Teil
wurde vor der Löschung Code-für-Code gegen `LoginPageRest`, `My2FAServicesRest`,
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
- `sanitizeRedirectUrl` verwirft `returnUrl`/`redirectUrl` mit Schema oder Host,
  auf beiden Seiten (`lib/menu-url.ts` und `LoginServiceRest.sanitizeRedirectUrl`
  – die Client-Kopie allein schützt niemanden). `/next/login?returnUrl=…` war
  sonst ein Open Redirect und damit ein überzeugender Phishing-Zwischenschritt:
  das Opfer hat sich wirklich bei ProjectForge angemeldet.
- `NextTwoFactorSupport.sendMailCode` prüft `isMail2FADisabledForUser` vorab
  (sonst `require` in `My2FAHttpService` → HTTP 500 statt Meldung).

Bewusst als Legacy-Parität belassen: `cancel` ist überall ein zustandsändernder
`@GetMapping` (Umstellung betrifft beide Frontends), das `last2FA`-Cookie wird
auch im Reset-Flow geschrieben, `CookieService.checkStayLoggedIn` stellt
`lastSuccessful2FA` nicht wieder her (Zeile auskommentiert), und `NO_2FA_URLS`
matcht per Prefix. Strenger als das gelöschte Legacy:
`PasswordResetNextRest.setPassword` erzwingt das 2FA serverseitig,
`PasswordResetPageRest.post()` hatte es nur per UI verlangt.
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
- **`id` und `name` müssen bei _jedem_ Listenaufruf mitgehen.** `getList` speichert
  den Filter, den es bekommt, als „aktuellen“ Filter des Nutzers
  (`saveCurrentFilter`) – ohne Id überschreibt also der erste Listenaufruf nach dem
  Auswählen den Favoriten-Bezug, und die geänderten Werte lassen sich nicht mehr in
  den Favoriten speichern. Die Referenz gehört deshalb zu den Filterwerten
  (`useListFilters.favorite`) und nicht in den Favoriten-Hook: sie muss stehen,
  bevor die Query gebaut wird.
- **Die Referenz überlebt das Ändern der Werte** – genau das macht „in diesen
  Favoriten speichern“ möglich. Der Name im Menü heißt also „basiert auf“, nicht
  „identisch mit“.
- **„Gibt es etwas zu speichern?“ rechnet der Client.** Das Icon am aktuellen
  Favoriten ist ein Sternchen, solange die Werte abweichen, und ein Häkchen
  („aktuell“), wenn sie dem gespeicherten Stand entsprechen – dieselbe Sprache wie
  im alten Panel (`FavoriteEntry.jsx`), nur dass die alte Listenseite `isModified`
  hartcodiert (`SearchFilter.jsx`) und damit immer das Sternchen zeigt. Verglichen
  wird über `filterFingerprint` (Werte + Suchstring, normalisiert). Die Basis dafür
  kennt der Client nur für einen in dieser Sitzung angewandten oder gespeicherten
  Favoriten – `initialList` liefert nur Namen und Ids (`Favorites.idTitleList`),
  nicht die Werte. „Unbekannt“ zählt deshalb als geändert, damit Speichern
  erreichbar bleibt.
- Ein leerer Name ist erlaubt: `Favorites.fixNamesAndIds` vergibt „unbenannt“
  (`favorite.untitled`).

**Erledigt: Filtereinstellungen werden gemerkt.** Kommt man auf die Buchseite
zurück, ist die letzte Filtereinstellung wieder vorbelegt – Pillen-Werte,
Suchstring und der zugrunde liegende Favorit (inkl. der Möglichkeit, die
zwischenzeitlichen Änderungen in ihn zu speichern). Das braucht keinen neuen
Endpunkt: `AbstractPagesRest.getList` ruft bei _jedem_ Listenaufruf
`saveCurrentFilter(filter)` (User-Prefs, Key `Favorites.PREF_NAME_CURRENT`), und
`initialList` gibt genau diesen Filter als `filter` zurück. Es fehlte nur das
Auslesen. Bausteine: `components/data-table/use-remembered-filter.ts`
(`useRememberedFilter` liest, `useRememberFilter` schreibt), `useListFilters` nimmt
den Filter als `restoredFilter`.

- **Nur die Filter-Zeile wird persistiert, nicht die Spalten-Filter.** Die
  Spalten-Filter in den Tabellenköpfen laufen clientseitig, sind eingeklappt und
  würden eine wiedergeöffnete Liste unsichtbar filtern. `useTableState` setzt
  `columnFilters` deshalb bewusst immer leer, obwohl der Spaltenzustand ansonsten
  gespeichert wird.
- **Die Werte müssen beim ersten Render dastehen**, genau wie der
  Spaltenzustand: sie initialisieren React-State, ein Nachschieben würde
  überschreiben, was der Nutzer zwischenzeitlich getippt hat. Die Seite hält die
  Liste deshalb hinter einem Spinner zurück, bis `initialList` **und**
  `columnStates` da sind (ein Request mehr entsteht nicht – `initialList` wird
  ohnehin für die Filterfelder geholt).
- **Der `initialList`-Cache muss mitwandern.** Er hat `staleTime: Infinity` (das
  Layout darin ändert sich nur mit einem Release), also würde ein Verlassen und
  Zurückkehren _ohne_ Reload den Filter vom ersten Seitenaufruf wiederherstellen.
  `useRememberFilter` patcht den Cache-Eintrag deshalb parallel zum Backend – die
  Liste bleibt eine Quelle der Wahrheit, dieselbe Stelle, die auch die
  Favoritenliste patcht.
- **Was bewusst nicht wiederhergestellt wird:** Seitengröße und Sortierung. Die
  Sortierung kommt aus dem Spaltenzustand (`columnStates`), damit es dafür nur eine
  Quelle gibt; die Seitengröße reist im Filter mit, ist aber eine Tabellen- und
  keine Filtereinstellung.

**Erledigt: Datumseingabe international (`components/shared/date-input.tsx`).**
Jede Datumseingabe war ein `<input type="date">`, dessen Anzeigeformat **und**
erster Wochentag vom Browser bzw. Betriebssystem kommen – nicht aus `userData`.
Ein englisches System zeigte einem deutschen Konto `08/09/2026` zum Tippen neben
`09.08.2026` in der Tabelle daneben, und der Kalender begann sonntags, obwohl
`firstDayOfWeekSunday0` Montag sagt. Damit verstieß die Eingabeseite gegen die
Regel, dass alles Locale-Abhängige durch **einen** Helfer läuft.

Jetzt gibt es genau eine Komponente (plus `date-input-calendar.tsx` für das
Popover), die alle vier Aufrufstellen bedienen: `DynamicDateInput` (DATE aus
UILayout), `RangeField` (Filter-Bereich), `ComparisonFilter` (Spalten-Filter) und
`InputField type="date"` (handgebaute Formulare). **Es gibt kein `type="date"`
mehr im Projekt** – das ist die Prüfung.

- **Der Wert ist immer der ISO-String `yyyy-MM-dd`, nie ein `Date`.** So reist ein
  `LocalDate` über den Draht (`LocalDateConverter`), und `filter-fns.ts` vergleicht
  Datums-Spaltenfilter **lexikografisch** auf `YYYY-MM-DD`. Nur der Text _im Feld_
  ist lokalisiert. `Date` ↔ ISO passiert ausschließlich in `dateOf`/`isoOf`
  (`lib/date-parse.ts`), aus den drei Zahlen in der lokalen Zone – `new Date(iso)`
  wäre UTC-Mitternacht und kann einen Tag zurückfallen.
- **Feldreihenfolge und Trennzeichen kommen aus
  `Intl.DateTimeFormat.formatToParts`**, also aus derselben Quelle, aus der
  `formatDate` seine Ausgabe zieht: Eingabe- und Anzeigeformat können nicht
  driften, und es gibt keine eigene Mustertabelle (kein moment.js).
  `FormatContext` trägt dafür zusätzlich `weekStartsOn` (genau die Zahl, die
  react-day-picker erwartet) und `datePattern` (die Maske als Platzhalter).
- **Verhalten wie im Legacy-`DateInput.jsx`:** streng geparst beim Tippen (damit
  dem Nutzer nicht in die Eingabe hineinkorrigiert wird), tolerant bei Blur und
  Enter („9.8.26", „090826"), ↑/↓ ±1 Tag, Klick auf den gewählten Tag löscht ihn.
  Zweistellige Jahre mit dem Pivot von moment (69 → 1969), `31.02.` ergibt `null`
  statt überzulaufen.
- **Löschen ohne alle Zeichen zu entfernen:** ein ✕ im Feld (auf `pointerdown`,
  denn der Knopf verschwindet mit dem leeren Feld – bei `pointerup` hielte das
  umgebende Popover den Klick für einen Klick nach außen) sowie „Rücksetzen" im
  Kalender-Popover. **Kein** Escape: das Feld sitzt in Popovers und Dialogen, deren
  Escape gewinnt (Radix hört am Document).
- **Heute ist deutlich markiert** – ein Ring in der Akzentfarbe über die
  `classNames`-Prop des Kalenders, nicht per Änderung an `components/ui/`.
- Geprüft gegen das laufende System: `e2e/date-input.spec.ts` leitet Maske,
  Datumslayout und den ersten Wochentag aus `userStatus` ab, statt sie
  auszuschreiben – sonst würde der Test nur ein deutsches Konto prüfen und genau
  den Fehler verdecken, um den es hier ging.
- **Offen und bewusst nicht dabei:** `TIME` und `TIMESTAMP`
  (`dynamic-input-resolver.tsx`) bleiben native Inputs. Keine heutige Seite gibt
  Uhrzeiten ein; sobald eine es tut, gehört die Zeit in dieselbe Komponente.

**Offen:**

1. **OBJECT- und TIMESTAMP-Felder vervollständigen.** OBJECT (z.B. „geändert
   durch“) nutzt derzeit ein einfaches Textfeld – für die Entitätssuche fehlt eine
   Autocomplete-Komponente gegen `autoCompletion.url`. Bei TIMESTAMP fehlt die
   Schnellauswahl (`selectors`: Jahr/Monat/Woche/Tag/bis-jetzt).
2. **`filter/reset` anbinden.** Die gemerkten Filter lassen sich derzeit nur Pille
   für Pille bzw. über „Zurücksetzen“ im Alle-Filter-Dialog leeren; das leert den
   Client-State, ruft aber nicht den Endpunkt, der auch den gespeicherten Filter
   im Backend zurücksetzt (`RestPaths.FILTER_RESET`, ebenfalls ein
   zustandsänderndes `@GetMapping`). Beim Zurücksetzen müsste auch der
   Favoriten-Bezug fallen.
3. **Das Edit-Gerüst ist noch nicht geteilt.** `book`-Edit hält Submit-Ablauf,
   406-Mapping, „gespeichert“-Toast, URL-Wechsel nach dem ersten Speichern,
   Lösch-Bestätigung und Aktionsleiste selbst. Jede weitere handgebaute Edit-Seite
   würde diese Blöcke kopieren. Sobald die zweite existiert (Phase 3,
   Auftragsbuch), gehören sie generalisiert nach `components/shared/` bzw.
   `hooks/` – ein `useEntityEditForm(entity, schema, …)` plus eine
   `EntityEditActions`-Leiste. Bewusst erst dann: mit nur einem Aufrufer wäre die
   Abstraktion geraten, nicht abgeleitet.
4. Nicht browserseitig verifiziert: englischer Locale-Pfad, vollständiger
   Login-Flow mit echten Daten, das visuelle Ergebnis der Tabelle
   (Spaltenbreiten, Resize, Popovers), der Favoriten-Durchlauf
   (anwenden/anlegen/umbenennen/überschreiben/löschen) sowie Speichern, Anlegen
   und Löschen eines Buchs gegen das echte Backend.

Erledigt seit der letzten Fassung: die Form-Library-Drift (`CLAUDE.md` schreibt
inzwischen `@tanstack/react-form` + Zod für handgebaute Formulare vor, dynamische
Seiten bleiben bewusst ohne Form-Library – s. Phase 2) und die doppelt deklarierten
Validierungsregeln (s. „Erledigt: Validierungsregeln nicht duplizieren“ – die Regeln
werden jetzt aus den Entities generiert).

#### Erledigt: CSRF-Schutz (querschnittlich, war Voraussetzung für die Breite)

**Das Problem.** Die Authentifizierung hängt am `JSESSIONID`-Cookie, das der
Browser bei _jedem_ Request mitschickt – auch bei einem, den eine fremde Seite
auslöst. Der Schutz dagegen war im Backend vorhanden (`SessionCsrfService`), griff
aber nur über den `PostData`/`ServerData`-Kontrakt der alten React-App – und
**next benutzt weder `PostData` noch `ServerData`**. Ungeschützt waren damit
`setColumnStates` und `filter/create|update` (`@PostMapping` ohne Prüfung) sowie
`filter/rename|delete|select` (zustandsändernde `@GetMapping`s, per `<img src>`
auslösbar); `saveOrUpdate` & Co. hätten einen next-Aufruf umgekehrt _abgelehnt_,
weil `serverData.csrfToken` fehlte.

**Der Weg: zwei unabhängige Schranken in `rest/core/RestCsrfProtection.kt`,**
aufgerufen aus `RestAuthenticationUtils.doFilter` – also einmal zentral für alle
`/rs/*`-Aufrufe, nach der Authentifizierung und vor dem 2FA-Handler. Neue
Endpunkte erben den Schutz dadurch ohne Zutun; ein Merksatz in `CLAUDE.md` erübrigt
sich deshalb bewusst.

1. **`Sec-Fetch-Site` (alle Clients, alle Methoden).** Der Header wird vom Browser
   gesetzt und ist für die aufrufende Seite nicht fälschbar – die einzige Schranke,
   die auch die zustandsändernden `@GetMapping`s deckt, die nirgends ein Token
   führen. Erlaubt sind nur `same-origin` und `none` (Bookmark/Direkteingabe).
   **`same-site` wird bewusst abgelehnt:** jeder `/rs`-Aufruf kommt von einer Seite
   dieser App, ist also immer `same-origin`; eine kompromittierte
   Schwester-Subdomain käme sonst durch – und würde von Schranke 2 nicht gestoppt,
   weil ein Angreifer den `X-PF-Frontend`-Header einfach wegließe.
2. **Session-Token im Header `X-PF-CSRF-Token` (next-Clients, nicht-`GET`).**
   Ausgeliefert in `userStatus` (holt next beim App-Start ohnehin), im Client in
   einem Modul-State neben dem 2FA-Handler – nicht in `localStorage`, damit ein XSS
   ihn nicht abgreift. Gesetzt wird er zentral in `rawRequest`, damit keine
   Aufrufstelle ihn vergessen kann. Ein Header, kein Body-Feld: funktioniert auch
   ohne Body und ist cross-site nicht ohne Preflight setzbar.

Warum nur next-Clients bei Schranke 2: die UILayout-Clients führen ihr Token im
Body, und den im Filter zu lesen würde den Stream verbrauchen. Deshalb muss
Schranke 1 client-unabhängig bleiben. Rest-Clients mit Access-Token
(`loggedInByAuthenticationToken`) sind von beiden ausgenommen – sie hängen an
keinem Ambient-Cookie.

**Ein veraltetes Token bleibt unsichtbar.** Kommt der Nutzer per Stay-logged-in
herein, erzeugt `LoginService.checkStayLoggedIn` eine neue Session (und damit ein
neues Token) – und zwar in `authenticate()`, unmittelbar vor der Prüfung. Der
Client hält dann noch das alte. Deshalb antwortet die Ablehnung für next-Clients
mit `403 {csrfTokenRequired:true}`; `lib/rs/client.ts` holt daraufhin `userStatus`
und wiederholt den Request **genau einmal**. Die alte React-App bekommt in
derselben Lage eine `errorpage.csrfError`-Validierungsmeldung und der Nutzer muss
ein zweites Mal speichern.

**Ergänzend:** `validateCsrfToken` liest das Token jetzt auch aus dem Header, damit
`saveOrUpdate`/`markAsDeleted`/… aus next nicht an ihrer eigenen Prüfung scheitern
(deren Fehlerfall ist eine `ResponseAction` mit HTTP 200, die next nicht lesen
kann). Und `server.servlet.session.cookie.same-site=Lax` steht nun explizit in
`application.properties` statt sich auf den Browser-Default zu verlassen –
Defense in Depth. `Strict` würde die Rückkehr aus dem Passwort-Reset-Mail-Link
brechen.

**Restrisiko, bewusst offen:** Ein Browser, der zu alt für `Sec-Fetch-Site` ist und
sich nicht als next ausweist, passiert beide Schranken. Für `POST`s fängt das
`SameSite=Lax` auf; offen bleiben die zustandsändernden `@GetMapping`s
(`filter/rename|delete|select`, `filterReset`, `cancel`), die Lax durchlässt. Sie
auf `POST` umzustellen berührt beide Frontends – entweder beide Aufrufstellen
mitziehen oder die Methode zusätzlich anbieten, solange `/react` lebt.

**Nicht browserseitig verifiziert:** der Stay-logged-in-Retry, das erste echte
Speichern aus `book`-Edit und der Dev-Betrieb auf `:3000` (dort greift die
`corsFilterEnabled`-Ausnahme in `checkSameSite`, weil der Dev-Server eine andere
Origin ist).

#### Erledigt: Speichern und Löschen (`book`-Edit konnte nie speichern)

**Das Problem.** `lib/rs/client.ts` hatte ein `save(entity, id, body)`, das
`PUT /rs/{entity}/{id}` ansprach – einen Endpunkt, den es nie gab (Überrest der
entfernten Next-Mock-Routen). `book`-Edit konnte also von Anfang an nicht
speichern, und Löschen war ein `toast.info("noch nicht implementiert")`.

**Der echte Kontrakt** (aus `AbstractPagesRest`/`AbstractPagesRestUtils`), jetzt in
`lib/rs/entity.ts` – bewusst getrennt von `client.ts`, weil er nicht die
Klartext-JSON-Form der übrigen Aufrufe hat:

- `PUT /rs/{entity}/saveorupdate` für **Anlegen und Ändern** – unterschieden wird
  serverseitig an `data.id`. Body ist immer der `PostData`-Umschlag (`{ data }`),
  nie die Entität allein.
- `DELETE /rs/{entity}/markAsDeleted` löscht historisierte Entitäten, `PUT
…/undelete` macht es rückgängig. `RestPaths.DELETE`/`FORCE_DELETE` zerstören Zeile
  **und** Historie und sind absichtlich nicht angebunden.
- Die Antwort ist eine `ResponseAction`, **nicht die gespeicherte Entität**. Die id
  eines neuen Datensatzes kommt nur als `variables.id` (aus `onAfterEdit`, `-1` =
  keine). Der Client muss die Entität also neu lesen – die Hooks invalidieren
  deshalb (`["book", id]`, `["book"]`, `["history","book",id]`) statt den Cache mit
  dem zu beschreiben, was sie abgeschickt haben.
- **HTTP 406 ist eine reguläre Antwort** und trägt `validationErrors`. Kein Fehler:
  `EntityWriteResult` ist deshalb `{ kind: "ok" } | { kind: "validationErrors" }`,
  und nur echte Transportfehler werfen.
- **Falle:** Ein abgelaufenes CSRF-Token antwortet mit **HTTP 200** und einer
  `ResponseAction` samt `validationErrors` (`errorpage.csrfError`) – die Form, die
  die UILayout-Seiten lesen. `rawRequest` hat dann schon einmal mit frischem Token
  wiederholt; `write()` in `entity.ts` prüft `validationErrors` daher auch im
  Erfolgsfall, sonst sähe ein fehlgeschlagenes Speichern wie ein geglücktes aus.

**406 auf die Felder mappen** (`lib/validation/server-errors.ts`, entitätsneutral):
`validationErrors` gehen per `fieldId` in den `onServer`-Slot der Fehlerkarte von
`@tanstack/react-form`. Der Slot ist genau der richtige, weil form-core ihn beim
nächsten `change`/`blur` des Feldes selbst leert (`ValidationLogic`) – wer den
abgelehnten Wert neu tippt, wird die Server-Meldung ohne eigene Buchführung los.
Zwei Details:

- Ein Fehler ohne `fieldId` oder mit einem Feld, das die Form nicht rendert, wäre
  unsichtbar. Er kommt als `unassigned` zurück (Aufrufer zeigt einen Toast) und
  landet zugleich in `onServer.form`, damit die Form ungültig bleibt und
  unveränderte Werte nicht erneut abgeschickt werden.
- Der Typ-Trick in `ErrorMapTarget` ist Absicht: form-core leitet den Typ des
  `onServer`-Slots aus einem `onServer`-**Validator** ab. Unsere Form hat keinen
  (der Server validiert, er gibt uns keinen Validator), also steht dort
  `undefined`, obwohl `setErrorMap` den Slot zur Laufzeit sehr wohl liest.

**Nebenbei aufgeräumt**, weil es am selben Formular hing:

- **`BookType` war falsch.** Das Frontend kannte `BOOK|MAGAZINE|EBOOK|OTHER`; das
  Backend hat 11 Werte und kein `OTHER` – ein „Sonstiges“ hätte das Backend also
  abgelehnt. Die Werte standen daraufhin einmal in `types.ts`
  (`BOOK_TYPE_VALUES`/`BOOK_STATUS_VALUES`) – inzwischen kommen sie generiert aus dem
  Backend-Enum und die Listen sind ganz weg (s. „Erledigt: Validierungsregeln nicht
  duplizieren“).
- **Optionslabel kommen aus dem Bundle.** `use-book-options.ts` baute die
  i18n-Keys zunächst selbst so nach, wie `BookType.i18nKey` es tut (`AUDIO_BOOK` →
  `book.type.audiobook`) – statt hartcodiertem „Buch“/„Vermisst“ in den Sections;
  seit dem Metadaten-Generator liefert das Backend den Key mit.
- **Pflichtfeld-Meldung** ist keine deutsche Zeichenkette im Zod-Schema mehr,
  sondern der Marker `REQUIRED`; das rendernde Feld übersetzt ihn zu
  `validation.error.fieldRequired` mit dem Feldlabel als Argument.
- Lösch-Bestätigung: `components/shared/confirm-dialog.tsx` (shadcn
  `alert-dialog`), Texte aus dem Backend-Bundle
  (`question.markAsDeletedQuestion`, `markAsDeleted`).

#### Erledigt: Änderungshistorie als eigene Route (echter Tab-Reiter)

**Das Problem.** Die Historie hing als letzte Section im Scroll-Bereich von
`EditPageShell`. Die „Reiter“ dort sind nur Scroll-Anker (`hooks/use-scroll-spy.ts`),
also ist **jede** Section immer gemountet – und damit lud jedes Öffnen eines Buchs
sofort die komplette Historie. Bei Entitäten mit langer Historie ist das teuer und
für die meisten Aufrufe umsonst.

**Die Lösung: eine eigene Route.** `/next/book/{id}/history` ist eine echte Seite
(`app/(authenticated)/book/[id]/history/`), die Historie mountet also erst, wenn
der Reiter geöffnet wird. `EditPageTabs` kennt dafür jetzt zwei Sorten Reiter:

- ohne `href` → Scroll-Anker wie bisher, positionsgekoppelt an `sections`,
- mit `href` → `<Link role="tab" aria-selected>`, wechselt die Route.

Welcher Reiter aktiv ist, bestimmt auf Routen-Seiten das neue Prop `activeId`
(statt des Scroll-Spy-`activeIndex`). Die Reiterleiste selbst steht **einmal** in
`components/shared/edit/entity-tabs.ts` und wird von Formular und Historie-Seite
geteilt – von der Historie aus zeigen die Formular-Reiter als Links zurück auf
`/book/{id}` (ohne Sprung zur jeweiligen Section: der Scroll-Spy kennt keine
Hash-Ziele – bewusst offen).

**Generisch, nicht buchspezifisch.** Historie gibt es für jede
`AbstractPagesRest`-Entität, deshalb liegt nichts davon im `book`-Feature:
Kontrakt in `lib/rs/history.ts`, Query/Mutation in `hooks/use-history.ts`
(Key `["history", entity, id]`), UI in `components/shared/history/`
(`history-section` → `history-timeline` → `history-entry-item` → `history-attr-diff`,
plus `history-comment-dialog`). Komponiert wird sie inzwischen generisch von
`components/shared/edit/entity-history-page.tsx` aus der Seitendeklaration.

**Backend: `history/{id}` erweitert statt zweitem Endpunkt.** Der Lese-Endpunkt
existierte längst; gefehlt hat nur die Angabe, ob der Client Kommentare anhängen
darf. Er antwortet daher jetzt `{ entries, supportsUserComments }`
(`AbstractPagesRest.HistoryInfo`), das Flag aus `BaseDao.supportsHistoryUserComments`
– d. h. nur für `HistoryUserCommentSupport`-Entitäten (heute `PFUserDO`, `GroupDO`;
Bücher **nicht**). Das alte React-Frontend liest die Kommentar-Fähigkeit weiterhin
aus `UILayout.UserAccess.editHistoryComments` und wurde nur an die neue Antwortform
angepasst (`containers/page/form/history/index.jsx`: `json.entries`). Für
handgebaute next-Seiten gibt es kein `UILayout`, daher der Weg über die Antwort.

**Zwei Backend-Bugs im Kommentar-Pfad mitgefixt:**

- `HistoryEntryUserCommentModalRest.append` prüfte den Leer-Guard gegen
  `dto.userComment` (den _bestehenden_ Kommentar) statt gegen `dto.appendComment` –
  der **erste** Kommentar eines Eintrags wurde damit verworfen.
- `HistoryService.appendUserComment` schrieb literal `"null"` als erste Zeile, weil
  es `entry.userComment` bedingungslos voranstellte.

Der fehlende `writeAccess`-Check im Append-Pfad bleibt bewusst unangetastet (er
würde das Verhalten des alten Frontends ändern).

**i18n:** die Texte kommen jetzt aus dem Backend-Bundle
(`label.historyOfChanges`, `history.*`, `operation.*`, `changes`, `nothingFound` –
neu in `PREFIXES`); `books.edit.history.*`, `books.edit.sections.history` und
inzwischen auch `books.edit.tabs.history` sind aus den handgeschriebenen Katalogen
entfernt – den Reiter benennt `entity-tabs.ts` mit `label.historyOfChanges`.

**Nicht browserseitig verifiziert:** dass das Öffnen eines Buchs keinen
History-Request mehr auslöst und der Reiter „Verlauf“ ihn erst beim Wechsel
absetzt; ebenso der Kommentar-Dialog (in next noch von keiner Seite erreichbar, da
User/Gruppe dort keine handgebaute Editierseite haben) und die beiden Backend-Fixes.

#### Erledigt: Anhänge (`UIAttachmentList`) – generisch, nicht buchspezifisch

**Was ersetzt wurde.** `BookPagesRest.createEditLayout` hängt ein
`UIFieldset(title = "attachment.list")` mit `UIAttachmentList(category, dto.id,
maxSizeInKB)` an; gerendert hat das im alten Frontend
`DynamicAttachmentList.jsx` (Drop-Zone + AG-Grid + Mehrfachauswahl). In next ist
daraus `components/shared/attachments/` geworden – Anhänge kann **jede**
`AbstractPagesRest`-Entität haben (Bücher, Verträge, Aufträge, Rechnungen,
Skripte), also liegt im `book`-Feature nur die Komposition
(`edit/sections/attachment-section.tsx`, ein `SectionCard` um `AttachmentList`).
Bausteine: Kontrakt in `lib/rs/attachments.ts`, Query/Mutationen in
`hooks/use-attachments.ts` (Key `["attachments", entity, id]`), UI in
`attachment-list` → `attachment-drop-area` / `attachment-row` /
`attachment-edit-dialog`.

**Zwei Eigenheiten des Protokolls prägen den Client** – beide am laufenden
System verifiziert, nicht aus dem Code geschlossen:

1. **Jeder Schreibvorgang antwortet mit der _vollständigen_ neuen Liste** unter
   `variables.data.attachments` (`AttachmentsActionListener`: `afterUpload` →
   `UPDATE`, `afterModification`/`afterDeletion` → `CLOSE_MODAL`, jeweils mit
   `ResponseData(attachments)`). Die Mutationen schreiben die Antwort deshalb per
   `setQueryData` in den Cache statt zu invalidieren – ein Nachladen wäre ein
   Request für Daten, die man schon hat.
2. **Eine Ablehnung ist ein HTTP 200 mit `targetType: "TOAST"`** (Datei existiert
   bereits, Datei zu groß – beide gehen über `UIToast`, s.
   `GlobalDefaultExceptionHandler`). Der Status-Code allein unterscheidet Erfolg
   und Ablehnung also **nicht**; würde man ihn glauben, verschwände eine Datei
   stillschweigend und die Liste bliebe veraltet. Daher liefert die
   `lib/rs`-Schicht ein `AttachmentWriteResult` (`ok` | `rejected` mit der
   übersetzten Backend-Meldung).

**Es gibt keinen Lese-Endpunkt.** `AbstractPagesRest` bettet die Anhänge über
`AttachmentsSupport` in das Entitäts-DTO ein (`Book.attachments`) – Lesen heißt
`GET /rs/{entity}/{id}` und das Feld herausgreifen.

**Nichts wird im Client formatiert.** `sizeHumanReadable`, `createdFormatted`,
`lastUpdateFormatted`, `lastUpdateTimeAgo` und das berechnete `encrypted` kommen
fertig aus dem Backend, in Locale und Zeitzone des Nutzers (`framework/jcr/Attachment`).

**Bewusste Abweichungen von der alten Vorlage:**

- **Kein `DataTable`.** Eine Handvoll Dateien in einer Formular-Section braucht
  weder Sortierung noch Paging noch Spaltenzustand, und der eigene
  Scroll-Container des Tabellen-Primitivs würde mit dem des Formulars kollidieren
  → schlichtes `<ul>/<li>`. Aus demselben Grund fehlen `multiDownload` und
  `multiDelete`: die Aktionen pro Zeile deckenden Fall ohne Auswahlmodell ab.
- **Keine Größenprüfung im Client.** Das Limit ist das des Backends
  (`FileSizeChecker`, pro Installation via `projectforge.jcr.maxDefaultFileSize`),
  es lehnt mit übersetzter Meldung ab – die Zahl hier zu wiederholen wäre eine
  zweite Stelle, die falsch sein kann.
- **Upload läuft sequenziell.** Der Endpunkt nimmt **eine** Datei pro Aufruf
  (Part-Name `file`), und seine Dublettenprüfung läuft gegen die schon
  gespeicherten Dateien – parallele Aufrufe könnten zwei gleiche Namen
  durchlassen.
- **Löschen fragt mit `question.deleteQuestion`,** nicht mit
  `markAsDeletedQuestion`: das JCR führt keine Historie gelöschter Dateien.
- **Download ist ein normaler Link,** kein `fetch`: die Antwort _ist_ die Datei,
  das muss der Browser erledigen.
- **Verschlüsselung (`attachment.encrypt`, `testDecryption`) ist nicht portiert.**
  Ein vorhandenes `encrypted` wird als Schloss-Icon angezeigt, das Verschlüsseln
  selbst bleibt offen.

**Ein querschnittlicher Fix in `lib/rs/client.ts`:** bei einem `FormData`-Body
darf der `Content-Type` **nicht** gesetzt werden – der vom Browser erzeugte
enthält die Boundary, die der Client nicht kennen kann; ein
`application/json` dort lässt Springs Part-Parser scheitern.

**i18n:** alles aus dem Backend-Bundle (`attachment.`, `file.upload.`, `edit`,
`download`, `description`, `question.deleteQuestion` – neu in `PREFIXES`). Der
Reiter „Anhänge“ nutzt `attachment.list`, also den Titel, den `BookPagesRest`
dem Fieldset gibt; `books.edit.tabs.*` bleibt nur für die Gruppierungen ohne
Backend-Pendant (`tabTitleKey` in der Seitendeklaration, siehe `entity-tabs.ts`).

**Browserseitig verifiziert** gegen das echte Backend
(`e2e/book-attachments.spec.ts`): Hochladen, Umbenennen inkl. Beschreibung,
Löschen, die Dubletten-Meldung sowie der Hinweis „erst nach dem Speichern“ bei
einem neuen Buch. Der Test räumt seine Dateien selbst wieder ab.

#### Erledigt: Ausleih-/Rückgabe-Aktion des Buchs

Ersetzt das `UICustomized(“book.lendOutComponent”)` der alten Seite
(`BookLendOut.jsx`). Die Endpunkte blieben unangetastet: `POST /rs/book/lendOut`
und `POST /rs/book/returnBook` (`BookServicesRest`), beide mit dem
`PostData`/`ResponseAction`/406-Kontrakt von `saveorupdate` – nur mit `POST`.
Deshalb steht der Aufruf in `lib/rs/entity.ts` (`postEntityAction`) und nicht in
`lib/rs/list-actions.ts`, das die Klartext-JSON-Form spricht. Bausteine sonst:
`hooks/use-entity-detail.ts` (`useEntityAction`, geteiltes `invalidate`),
`lib/rs/submit-meta.ts`, `edit/sections/loan-section.tsx`,
`edit/sections/book-loan-actions.tsx`; welche Aktionen eine Entität hat, sagt
`EditDef.actions` in ihrer Deklaration.

- **Beide Aktionen speichern das ganze Buch mit** – sie laufen durch
  `saveOrUpdate`, sind also kein Teil-Update. Gesendet werden daher die
  **aktuellen Formularwerte**; eine ungespeicherte Ausleihnotiz reist mit, statt
  verloren zu gehen. Damit Anzeige und Datenbank nicht driften, werden die Werte
  danach vom Server zurückgelesen (Caches invalidieren → der bestehende
  `useEffect([book, form])` setzt das Formular auf den Serverstand).
- **Ein Submit-Weg, drei Aktionen.** Ausleihen/Zurückgeben müssen dieselbe
  Zod-Validierung und dieselben Werte benutzen wie Speichern (sonst würde ein
  geleerter Titel über die Ausleihe mitgespeichert). Gelöst über `onSubmitMeta`
  von `@tanstack/react-form`: die Knöpfe rufen
  `form.handleSubmit({ action: “lendOut” })`, `onSubmit` wählt daran die
  Mutation. Kein zweiter Schreibpfad, ein gemeinsamer 406-Zweig.
- **Die Antwort ist ein REDIRECT auf die Liste** (Nebenwirkung von
  `onAfterEdit`) und wird – wie beim Speichern – ignoriert: die Seite bleibt
  stehen, denn das Ergebnis der Ausleihe ist genau das, was der Nutzer sehen
  will.
- **`lendOutBy`/`lendOutDate` sind jetzt read-only** („Name, Datum”), nur
  `lendOutComment` bleibt editierbar. `lend-out-by-field.tsx` ist gelöscht: es
  war ein Freitextfeld, das bei jeder Eingabe `{ id: -1, … }` setzte – eine Id,
  die das Backend nicht auflösen kann. Den Ausleihenden setzt ohnehin nur der
  Server aus der Session. Beide Felder bleiben in Schema und Formularwerten, sie
  müssen mitgesendet werden.
- **Datum, nicht Zeitstempel:** `formatDate`, weil `lendOutDate` ein `LocalDate`
  ist. Die alte Komponente nahm `jsTimestampFormatMinutes` und zeigte ein
  sinnloses 00:00.
- **„Zurückgeben” nur für den Ausleihenden** – wie im alten Frontend eine reine
  Client-Regel, das Backend prüft sie nicht. Verglichen wird `user.userId ===
lendOutBy.id` statt Legacy's `username`: `UserRef` führt kein `username`.
- **„Ausleihen” ist immer sichtbar** (Legacy-Parität): ein Klick bucht ein
  bereits ausgeliehenes Buch still auf den aktuellen Nutzer um.
- **Nur bei gespeichertem Buch** (`if (dto.id != null)` im alten Layout).

**Browserseitig verifiziert** gegen das echte Backend (`e2e/book-lend-out.spec.ts`):
Ausleihen (eigener Name + heutiges Datum, Seite bleibt stehen), Persistenz nach
Reload inkl. der ungespeicherten Notiz, Zurückgeben leert alle drei Felder, ein
neues Buch zeigt keine Knöpfe, und ein geleerter Titel verhindert den Request.
Der Test gibt das Buch am Ende wieder zurück.

#### Erledigt: Validierungsregeln nicht duplizieren

**Der Stand.** Feldlängen, Typen und Pflichtfelder sind im Backend genau einmal
deklariert – und zwar _nicht_ per Bean Validation (die Entities haben keine
`@NotNull`/`@Size`-Annotationen), sondern über zwei Quellen:

- JPA `@Column(length = …, nullable = …)` am Getter der `…DO`-Klasse
  (`BookDO.title` → 255, `BookDO.keywords` → 1024),
- ProjectForge-eigenes `@PropertyInfo(i18nKey, required, type)`
  (`projectforge-common/.../common/anots/PropertyInfo.java`), am Feld oder als
  `@get:PropertyInfo` am Getter.

Zusammengeführt werden sie in `projectforge-rest/.../ui/ElementsRegistry.kt`:
`maxLength` kommt aus der Spaltenlänge, `required` wird gesetzt, sobald
`!colinfo.nullable || propertyInfo.required` gilt (Booleans ausgenommen), und ab
`maxLength >= 256` wird ein `UIInput` zur `UITextArea` befördert. Auf dem Draht
landet das in `UIInput.maxLength/required/dataType` bzw. `UITextArea.maxLength`.

**Das Duplikat (behoben).** Der Dynamic-Renderer liest diese Angaben schon aus dem
`UILayout` (`components/dynamic/components/input/*`). Nur der handgebaute Zweig
deklarierte sie erneut: `book-edit-schema.ts` hatte `required` hartcodiert,
`general-section.tsx` setzte es ein zweites Mal als Prop, die Feldlängen fehlten
ganz, und `BOOK_TYPE_VALUES`/`BOOK_STATUS_VALUES` in `types.ts` waren abgeschriebene
Enum-Konstanten (an denen die Liste schon einmal falsch war). Eine Spaltenlänge im
Backend zu ändern verändert das Frontend jetzt.

**Grundsatz (gilt weiter).** Feldlängen, Typen und `required` werden im Frontend
**nie** erneut deklariert. Frontend-Validierung ist reine UX-Vorwegnahme der
Server-Regel; die Autorität bleibt der Server (406).

**Zwei Kanäle, getrennt nach Seitentyp:**

1. **Dynamische Seiten (Phase 2): zur Laufzeit.** Nichts zu bauen – `maxLength`,
   `required` und `dataType` kommen im `UILayout` mit. Regel für neue
   Element-Komponenten in `components/dynamic/`: diese Props durchreichen, niemals
   eigene Grenzen oder Pflichtfeld-Logik erfinden.
2. **Handgebaute Seiten: generiert**, analog zur i18n-Generierung.
   `projectforge-application/src/test/.../development/GenerateNextFieldMetadataMain.kt`
   scannt per `Reflections` alle `@Entity`-Klassen, holt zu jeder Property
   `ElementsRegistry.getElementInfo` und schreibt eine Datei pro Entität nach
   `projectforge-next/lib/metadata/<entity>.generated.ts` (65 Dateien; Aufruf über
   `DevelopmentMainForRelease`, Output committet). Kein Regel-Detail wird dabei neu
   abgeleitet. `GenerateNextFieldMetadataTest` vergleicht byteweise und meldet
   zusätzlich Waisen, also Dateien zu umbenannten Entitäten.

   Emittiert wird prettier-konform (jedes Objekt aufgeklappt, doppelte
   Anführungszeichen, trailing commas), damit `lib/metadata` nicht in
   `.prettierignore` muss. **Kein Barrel:** eine Datei pro Entität, ohne
   Seiteneffekte, also vollständig tree-shakebar – die Bundle-Kosten der breiten
   Entitätsauswahl sind null.

   **Die Enum-Wertelisten kommen mit:** pro Enum-Property Konstantenname und
   `i18nKey` (`I18nEnum`), `dataType: "STRING"`. `BookStatus`/`BookType` in
   `components/features/book/types.ts` leiten sich daraus ab, ebenso die
   Optionslisten in `use-book-options.ts` – deren selbstgebauter `i18nKey()`-Rater
   (lowercase + Unterstriche strippen) ist weg.

**Frontend-Ableitung.** `lib/metadata/types.ts` (handgeschrieben) hält den Kontrakt,
`lib/validation/from-metadata.ts` macht daraus Zod-Bausteine
(`requiredString`, `nullableString`, `enumField`, `enumOptions`); die Meldungen
laufen über Marker (`lib/validation/markers.ts`, `@required`/`@maxLength:n`), weil
nur das rendernde Feld sein Label kennt. `book-edit-schema.ts` bleibt
handgeschrieben – das DTO `rest/dto/Book.kt` hat nicht die Feldmenge des DO –, bezieht
aber jede Regel aus `fromMetadata(BOOK_METADATA)`. Ein Feldname, den die Metadaten
nicht kennen, ist dank Literal-Union bereits ein `tsc`-Fehler.
`book-edit-fields.tsx` liest `required` und `maxLength` über `useFieldMetadata` (kein
Prop mehr), setzt `maxLength` als HTML-Attribut und leitet `SelectField.clearable`
aus `!required` ab; `general-section.tsx` entscheidet nur noch Reihenfolge, Label und
Layout.

**Zwei Backend-Befunde, die dabei mitbehoben wurden** (eigener Commit, weil sie
bisher stillschweigend akzeptierte Daten zu einem 406 machen – für `/react` genauso
wie für next und über `saveOrUpdate` auch für Import-/Massenpfade; Wicket verhält
sich schon so):

- **`ValidationUtils` prüfte nur, was zufällig im Cache lag.** Es iterierte
  `ElementsRegistry.getProperties(clazz)`, und das ist bloß die Memo-Map, gefüllt
  wenn ein UILayout ein Element gebaut hat. Eine handgebaute next-Seite baut kein
  Edit-Layout, für `BookDO` registrierte also nur `createListLayout` –
  `status`/`type`/`isbn`/… fehlten. **Ein Save aus next validierte damit nicht einmal
  `required` vollständig**, und was fehlte, hing davon ab, welche Seiten die JVM seit
  dem Start bedient hatte. Neu: `ElementsRegistry.listProperties(clazz)` zählt aus
  beiden Annotationsquellen auf (sortiert, sonst flackert der Drift-Test), und
  `validateFields` geht darüber.
- **Der REST-Pfad prüfte `maxLength` nirgends** (Wicket tut es per
  `StringValidator.maximumLength`, die alte React-App nur im Client). Jetzt mit
  neuem Key `validation.error.maxLength` in beiden Bundles. `maxLength` gilt nur für
  `String`-Properties: `@Column.length` ist auch für Nicht-Strings mit 255
  vorbelegt, und die 20 an einem Enum ist eine Speicherlänge, keine Nutzergrenze.

Ein dritter Befund kam vom Generator selbst: `UIDataTypeUtils.getDataType` verglich
für INT/LONG nur je eine Hälfte des Paars (Kotlins `Long::class.java` ist `long`,
`Integer::class.java` dagegen boxed), sodass 64 Properties – alle `id`s,
`KundeDO.nummer`, `RechnungDO.year` … – still auf STRING zurückfielen, im UILayout
wie in den Metadaten.

**Die drei zuvor offenen Fragen, beantwortet:**

- **Entitätsauswahl: alle `@Entity`-Klassen.** Phase 3 braucht geschachtelte DOs wie
  `AuftragsPositionDO`, die keine eigene `AbstractPagesRest` haben; eine „nur was
  eine PagesRest hat"-Regel bräuchte eine handgepflegte Ausnahmeliste, also genau die
  Veraltungsfalle. `SKIP` existiert als Notausgang und ist leer.
- **Woher die Längen kommen: reflexiv**, über `ElementsRegistry` →
  `EntityMetaDataRegistry`, ohne DB und ohne `EntityManagerFactory` (nicht
  `HibernateMetaModel`, das eine laufende Persistenzschicht bräuchte).
- **Ableitungshelfer:** `lib/validation/from-metadata.ts`, s.o. – mit Markern statt
  Texten, weil die Meldung Label und Grenze braucht.

**Verbleibende Lücken.** Die geerbten Properties `id`/`created`/`lastUpdate`/
`deleted` haben keine `ColumnMetaData`, weil `EntityMetaData` nur `declaredFields`
liest – sie erscheinen in den Metadaten mit `dataType` und `i18nKey`, aber ohne
`maxLength`/`nullable` aus der Spalte. Für diese vier ist das folgenlos (keine
Nutzereingabe), für eine künftige Basisklasse mit echten Textspalten wäre es keine.
Ausgelassen werden außerdem Collections und Fremd-DO-Referenzen; deren Felder stehen
in der Datei der jeweiligen Entität, eine geschachtelte Form muss sie dort holen.
Nicht verifiziert: ein zu langer Wert über eine UILayout-Seite (`/react/address`) –
das braucht einen Neustart der laufenden Instanz auf dem neuen Build.

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
  Kategorie und next-Route dürfen abweichen (handgebaut: absichtlich gleich), ebenso die
  Edit-Route handgebauter Seiten (`book/:id` statt `book/edit/:id`).
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
  konkrete Routen (`book`) gehen dem Catch-all vor – `HAND_BUILT_CATEGORIES` in
  `page-client.tsx` hält das synchron mit `NextMigration.MIGRATED`.

Verbliebene Lücken:

1. **Fehlende UIElement-Typen** aus `UIElementType.kt` ergänzen: Entity-Picker
   (USER, GROUP, EMPLOYEE, COST1, COST2, KONTO, TASK, LOCALE, TIMEZONE, PICTURE),
   RATING, EDITOR, ATTACHMENT_LIST, DROP_AREA, PROGRESS, `pageMenu`.
2. **`MODAL`/`CLOSE_MODAL`** richtig: der `location.state.background`-Trick des
   alten Routers existiert im App Router nicht. Später über einen Modal-Stack in
   `store/ui-store.ts` + `ui/dialog.tsx`; Trade-off: keine teilbaren
   Modal-Deep-Links (Konsequenz des Static-Exports).
3. **`UICustomized`-Escape-Hatch** (alt: ~30 String-IDs → bespoke Komponenten)
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

#### Erledigt: Listen rendern mit der echten `DataTable` (Adapter + Formatter)

`components/dynamic/components/dynamic-table.tsx` (handgeschriebene `<table>`, las
nur `hide`, `JSON.stringify` für Objekte) ist gelöscht. Jeder `AG_GRID`/`TABLE`-
Knoten läuft jetzt durch `components/dynamic/components/grid/` auf dieselbe
`DataTable` wie die `book`-Liste – mit Resize, Spalten-Panel, Pinning,
Header-Filtern und Zustands-Persistenz.

- **Adapter in `lib/dynamic/grid/`** (reines TS): `column-def-adapter.ts`
  (`field`→`id`/`accessorFn`, `filter: 'agTextColumnFilter'|…`→`meta.filterKind`,
  `type: 'numericColumn'|'rightAligned'`→`meta.align`, `width`→`size`),
  `initial-state.ts` (`hide`/`pinned`/`width`/Reihenfolge/`sortModel` →
  `ColumnState`), `cell-spec.ts`, `value-path.ts`, `row-class.ts`, `row-click.ts`.
  Das AG-Grid-Vokabular endet damit an dieser Schicht.
- **Zell-Renderer in `components/data-table/cells/`** (nicht in `dynamic/`, damit
  auch handgebaute Seiten sie nutzen dürfen): Registry `CellKind → Komponente` über
  `lib/format.ts`/`lib/format-names.ts`. Drei Formatter-Bugs der Vorlage sind dabei
  behoben statt mitportiert (`AUFTRAG_POSITION` und `TIMESTAMP_SECONDS` matchten
  die falschen Namen und rendern in `/react` `'???'`, `BOOLEAN` gab einen rohen
  Boolean zurück); ein unbekannter Formatter fällt auf Klartext zurück, kein
  `'???'`.
- **Kein `Function()`/`eval`.** `valueGetter`/`valueFormatter` werden strikt als
  Punktpfad geparst, alles andere mit einer Dev-Warnung verworfen (`address`
  liefert z.B. ein `map(...)`-Lambda für die Adressbücher). `getRowClass` erkennt
  die Prädikat-Formen der acht `withGetRowClass`-Sender deklarativ und normalisiert
  `ag-row-green` → `row-green`. **Die richtige Lösung ist serverseitig:**
  `getRowClass: String` sollte in `UIAgGrid.kt` ein strukturiertes `rowHighlights`
  werden – die Mustertabelle ist die Brücke bis dahin.
- **Kein zweiter Request für den Spaltenzustand.**
  `AGGridSupport.restoreColumnsFromUserPref` faltet den gespeicherten Zustand
  bereits in die gesendeten `columnDefs` und liefert das passende `sortModel` – die
  Layout-Antwort _ist_ der wiederhergestellte Zustand, `initialStateFrom(grid)`
  berechnet ihn synchron. Persistiert wird URL-basiert
  (`onColumnStatesChangedUrl`/`resetGridStateUrl` aus dem Layout), weil eine
  Ableitung aus der Kategorie für `TaskServicesRest` falsch wäre.
- **Generische Listen-Route reaktiviert:** `app/(authenticated)/[category]/`
  (Server-Wrapper + `page-client.tsx`), `HAND_BUILT_CATEGORIES` liegt jetzt in
  `lib/hand-built-categories.ts` und wird von beiden generischen Routen geteilt.
- **Verifiziert** (`e2e/dynamic-grid.spec.ts`, Playwright gegen das laufende
  System): `vacation` – `valueGetter` (`employee.displayName`), DATE-Formatter,
  `row-red` aus `getRowClass`, Row-Click auf `/react/vacation/edit/<id>`, und als
  stärkstes Signal Sortieren + Spalte verstecken → Reload → identisches Layout →
  „Spalten zurücksetzen“ stellt sie wieder her. Manuell geprüft: `skillentry`
  (RATING als Sterne, `aria-label` „Bewertung: n“), `address` (Formatter, Pinning,
  Icon-Header; die drei `customized`-Spalten degradieren wie geplant auf Text).
- **Zwei Abweichungen von der Planannahme:** `task/initialList` ist ein
  `TABLE_LIST_PAGE` mit `columns` statt `columnDefs` und läuft daher durch
  `dynamic-grid-fallback.tsx` – TREE_NAVIGATION/CONSUMPTION sind von dort nicht
  erreichbar (sie stecken in `TaskServicesRest`, nicht im Listen-Layout). Und die
  Prüfseiten „skillmatrix“/„invoice“ heißen als REST-Kategorie `skillentry` bzw.
  `outgoingInvoice`; letztere braucht das Recht `FIBU_AUSGANGSRECHNUNGEN` und war
  mit dem Testkonto nicht prüfbar, CURRENCY ist damit noch offen.

Noch offen an dieser Stelle: die Renderer für `customized`/`diffCell`/
`importStatusCell` (deren Kontrakt enthält `onClick`-JS-Quelltext), Auf-/Zuklappen
bei TREE_NAVIGATION, Mehrfachauswahl, `?modal=true`, `highlightRowId` und
serverseitige Sortierung.

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

#### Kalenderseite (zweiter handgebauter Fall) – Detailplan liegt vor

Die **Kalenderseite** (`/react/calendar`) ist der zweite Härtefall und
gleichzeitig die Standard-Startseite nach dem Login. Sie ist ebenfalls nicht über
UILayout abbildbar (FullCalendar-Grid, Drag&Drop/Resize, Slot-Auswahl,
Kalender-Multi-Select mit Farbwahl), das Backend liefert aber schon fertige
FullCalendar-DTOs – es ist also fast reine Frontend-Arbeit.

Vollständiger Umsetzungsplan: **[MIGRATION-calendar.md](MIGRATION-calendar.md)**
(Dateiaufteilung, State-Modell über den Query-Cache statt der Legacy-Spiegel-Refs,
FullCalendar-Theming über die shadcn-Tokens, Tooltip ohne
`dangerouslySetInnerHTML`, i18n-Präfixe, Verifikation, Risiken). Noch nicht
umgesetzt.

Zwei **Voraussetzungen aus Phase 2** stecken darin, die auch anderen Seiten
nützen: eine 2-Segment-Route `[category]/[type]` (der `action`-Endpunkt liefert
für jede Neuanlage `/timesheet/edit?startDate=…` ohne id – dafür gibt es heute
keine Route) und die Weitergabe des Query-Strings in `fetchDynamic`. Dazu die
`UICustomized`-Registry mit `COLOR_CHOOSER`, weil das Kalender-Menü auf
`calendarSettings` verlinkt.

### Phase 4 – Ablösung & Aufräumen

- Pro vollständig migrierter Seite: Menü auf `next/`, alte Route deaktivieren.
- Wenn alle Seiten migriert: `projectforge-webapp` und `projectforge-wicket` aus
  `settings.gradle.kts` + Build entfernen, `/react`- und `/wa`-Serving/Filter
  (`WebApplicationConfig`, `WebXMLInitializer`) entfernen, ggf. `NEXT_APP_PATH` →
  `/` als Default.
- `lib/api-client.ts` (veralteter Zweit-Client, unbenutzt) entfernen; `lib/rs/`
  bleibt einzige Backend-Schnittstelle.
- `components/features/book/mock-data.ts` (seit Entfernen der Mock-Routen
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
- **CSRF:** zentrale Schranken `projectforge-rest/.../rest/core/RestCsrfProtection.kt`
  (eingehängt in `web/rest/RestAuthenticationUtils.kt`), Token-Auslieferung
  `rest/UserStatusRest.kt`, Client `projectforge-next/lib/rs/client.ts`
  (`setCsrfToken`/`rawRequest`), Cookie `application.properties`
  (`server.servlet.session.cookie.same-site`);
  Token-Quelle `projectforge-rest/.../rest/core/SessionCsrfService.kt`,
  `rest/dto/ServerData.kt`, `AbstractDynamicPageRest.kt` (`createServerData`/
  `validateCsrfToken`); next-Vorlage `rest/pub/next/PasswordResetNextRest.kt`;
  Cookie-Flags `CookieService.kt`
- **Validierungs-Metadaten:** `projectforge-common/.../common/anots/PropertyInfo.java`,
  `projectforge-rest/.../ui/ElementsRegistry.kt` (+ `ElementInfo.kt`),
  `rest/core/ValidationUtils.kt`, `AbstractPagesRestUtils.kt` (406-Antwort);
  reflexive Metadaten `projectforge-business/.../framework/persistence/jpa/EntityMetaDataRegistry.kt`;
  Frontend-Duplikat `projectforge-next/components/features/book/edit/book-edit-schema.ts`
  (+ die Enum-Wertelisten in `components/features/book/types.ts`);
  406-Mapping `projectforge-next/lib/validation/server-errors.ts`
- **Entitäts-Schreibaufrufe:** `projectforge-next/lib/rs/entity.ts`
  (`saveorupdate`/`markAsDeleted`/`undelete`), Backend
  `projectforge-rest/.../rest/core/AbstractPagesRest.kt` +
  `AbstractPagesRestUtils.kt`, `framework/persistence/api/RestPaths.java`
- **Änderungshistorie:** Backend `projectforge-rest/.../rest/core/AbstractPagesRest.kt`
  (`HistoryInfo`, `history/{id}`), `HistoryEntryUserCommentModalRest.kt`,
  `projectforge-business/.../framework/persistence/history/HistoryService.kt`
  (`appendUserComment`), Fähigkeits-Flag `.../persistence/api/BaseDao.kt`
  (`supportsHistoryUserComments`) + `HistoryUserCommentSupport`;
  Frontend `projectforge-next/lib/rs/history.ts`, `hooks/use-history.ts`,
  `components/shared/history/*`, Route
  `app/(authenticated)/book/[id]/history/`, Reiterleiste
  `components/shared/edit/entity-tabs.ts`; Vorlage
  `projectforge-webapp/src/containers/page/form/history/`
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
  `messages/` (`generated.*.json` nicht von Hand ändern — `GenerateNextI18nMessagesTest`
  erzwingt das im Build)
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
- **Phase 1** – Menü-Schalter pro Seite; `BOOK_LIST` zeigt auf `next/book`.
- **Phase 1.5, größter Teil** – `MagicFilter`-Kontrakt (Listen laden wieder),
  Tabellen-Funktionen portiert (Resizing, Spalten ein-/ausblenden, Pinning,
  Reorder, Spalten-Filter), Spaltenzustand-Persistenz, Listen-Filter als
  Pillen-Zeile inkl. gespeicherter Filter (Backend-Favoriten) und gemerkter
  Filtereinstellung, i18n-Generierung aus `I18nResources`.
- **Auth-Flow** – Login, 2FA inkl. WebAuthn, Passwort-vergessen/-Reset und
  In-Session-2FA-Dialog laufen in next. `/next/login` ist der einzige Login
  aller drei Frontends, die UILayout-Pendants sind gelöscht
  (`e2e/login.spec.ts`).
- **CSRF-Schutz** – zentral für alle `/rs/*`-Aufrufe (`RestCsrfProtection`:
  `Sec-Fetch-Site` + Session-Token im Header), damit erben neue Endpunkte den
  Schutz ohne Zutun. Damit darf eine next-Seite schreiben.
- **Schreiben in `book`-Edit** – `saveorupdate`/`markAsDeleted` über
  `lib/rs/entity.ts` (PostData + ResponseAction, 406 als reguläre Antwort),
  406-`validationErrors` auf die Formularfelder gemappt, Anlegen inkl.
  URL-Wechsel auf die neue id, Löschen mit Bestätigung. Noch nicht im Browser
  gegen das echte Backend verifiziert.
- **Änderungshistorie** – eigene Route `/book/{id}/history` mit echtem
  Link-Reiter statt Section im Scroll-Bereich (lädt damit erst beim Öffnen),
  generische UI in `components/shared/history/`, Kommentarfunktion über das
  Backend-Flag `supportsUserComments` gesteuert. Browser-Prüfung steht aus.
- **Dynamische Listen auf der echten `DataTable`** – `UIAgGridColumnDef → ColumnDef`-
  Adapter (`lib/dynamic/grid/`), Zell-Formatter-Registry
  (`components/data-table/cells/`), deklarativer Ersatz für `getRowClass`/
  `rowClickRedirectUrl` ohne Codeausführung, URL-basierte Spaltenzustands-
  Persistenz, generische Listen-Route wieder aktiv. Gegen das laufende System
  geprüft (`e2e/dynamic-grid.spec.ts`).
- **Anhänge (`UIAttachmentList`)** – generisch in
  `components/shared/attachments/` + `lib/rs/attachments.ts`, mit dem
  entscheidenden Detail, dass eine Ablehnung als HTTP 200 mit `TOAST` kommt und
  jede Schreibantwort schon die ganze neue Liste enthält. Gegen das laufende
  System geprüft (`e2e/book-attachments.spec.ts`).
- **Datumseingabe international** – eine geteilte Komponente
  (`components/shared/date-input.tsx`) statt vier `<input type="date">`: Layout,
  Platzhaltermaske und erster Wochentag aus `userData`, Wert immer ISO
  `yyyy-MM-dd`. Gegen das laufende System geprüft (`e2e/date-input.spec.ts`).

**Als nächstes:**

1. **Phase 1.5 abschließen:** OBJECT-Autocomplete und TIMESTAMP-Schnellauswahl,
   `filter/reset` samt `isFilterModified`, die Ausleih-/Rückgabe-Aktion des Buchs
   (s. eigener Abschnitt – die Felder stehen, die beiden `BookServicesRest`-
   Endpunkte fehlen), und `book`-Edit als saubere
   Vorlage: Validierungsregeln und Enum-Wertelisten aus den Backend-Metadaten
   ableiten statt sie zu wiederholen (s. eigener Abschnitt). Vorher das visuelle
   Ergebnis der Tabelle, den Favoriten-Durchlauf sowie Speichern/Anlegen/Löschen
   im Browser prüfen – das steht noch aus.
2. **Phase 2** – Dynamic-Renderer ausbauen (bringt die ~36 UILayout-Seiten in der
   Masse). Listen rendern inzwischen mit der echten `DataTable`; als nächstes
   fehlen die Entity-Picker-Elementtypen und der `UICustomized`-Escape-Hatch. Erst
   danach lohnt es, Seiten in `NextMigration.MIGRATED` umzuschalten.
3. **Phase 3** – Auftragsbuch als handgebauter Härtefall (parallel zu Phase 2
   möglich).
4. **Auth-Restprüfungen mit echtem zweiten Faktor** – der Legacy-Login ist
   gelöscht, es gibt keine Rückfallebene mehr. Gegen das laufende System geprüft
   ist: `e2e/login.spec.ts` (Fehlanmeldung, `returnUrl`, fremder Host,
   Passwort-vergessen), der Wicket-Umweg (ohne Session `302` auf
   `/next/login?returnUrl=%2Fwa%2FtaskTree`, mit Session `200`), der Logout
   (`{"url":"/next/login"}`, danach `/rs/userStatus` → `401`) und dass die
   gelöschten UILayout-Endpunkte `404` liefern und nirgends mehr verlinkt sind.
   Offen bleibt, was einen echten zweiten Faktor oder ein Mailkonto braucht:
   WebAuthn-Token (`toPublicKeyOptions` reicht `extensions` nicht mehr durch),
   OTP/SMS/Mail-2FA nach dem Login, der Token-Link der Reset-Mail
   (`/next/password-reset?token=…`), „angemeldet bleiben" und der
   In-Session-2FA-Dialog der alten React-App (z. B. `/react/myAccount`), der
   weiterhin über `My2FAPageRest` läuft.

**Reihenfolge-Grundsatz:** `book` bleibt die Vorlage – was dort fehlt, fehlt
jeder migrierten Seite. Deshalb erst `book` fertig, dann in die Breite.
