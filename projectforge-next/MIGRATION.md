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
3. Nicht browserseitig verifiziert: englischer Locale-Pfad, vollständiger
   Login-Flow mit echten Daten, das visuelle Ergebnis der Tabelle
   (Spaltenbreiten, Resize, Popovers), der Favoriten-Durchlauf
   (anwenden/anlegen/umbenennen/überschreiben/löschen) sowie Speichern, Anlegen
   und Löschen eines Buchs gegen das echte Backend.

Erledigt seit der letzten Fassung: **das geteilte Edit-Gerüst.** Was `book`-Edit
zuerst allein hielt – Submit-Ablauf, 406-Mapping, „gespeichert“-Toast, URL-Wechsel
nach dem ersten Speichern, Lösch-Bestätigung und Aktionsleiste – liegt inzwischen in
`hooks/use-entity-edit-form.ts` und `components/shared/edit/`, und alle drei
handgebauten Seiten (`book`, `cost1`, `order`) laufen darüber: nicht als Kopie,
sondern als Deklaration (`PageDef`, s. `docs/page-declarations.md`). Abgeleitet, wie
vorgesehen, an der zweiten und dritten Seite – nicht an der ersten geraten.

Außerdem erledigt: die Form-Library-Drift (`CLAUDE.md` schreibt
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

#### Erledigt: Access-Checking der neuen Endpunkte (Audit)

**Warum überhaupt.** ProjectForge ist an zwei Stellen solide abgesichert:
`RestUserFilter` lässt nichts Unangemeldetes an `/rs/**`, und die eigentliche
Autorisierung liegt im Keller bei den DAOs (`BaseDao.find` →
`checkLoggedInUserSelectAccess`, `BaseDao.select`, Schreibrechte in
`BaseDOPersistenceService`). Eine dritte Schranke lag aber im **Layout**: Wicket und
die `UILayout`-Seiten haben Menüeinträge und Buttons ausgeblendet, die der Benutzer
nicht benutzen darf. next baut seine Seiten und sein Menü selbst – diese Schranke
fällt weg. `AbstractEntityRest.reindexFull` ist genau deshalb schon explizit auf die
Admin-Gruppe geprüft. Das Audit hat die übrigen neuen Endpunkte am selben Maßstab
durchgesehen.

**Ergebnis: eine echte Lücke.** `EInvoiceCheckerPageRest` (`/rs/eInvoiceChecker`,
neu) hatte keinerlei Prüfung – und als `AbstractDynamicPageRest` auch keine DAO, die
im Keller einspringen könnte. Der Schutz war ausschließlich der Menüeintrag
(`MenuCreator`, `checkAccess = { isInGroup(*FIBU_ORGA_GROUPS) }`), also genau das
Muster, das für next nicht mehr trägt. Alle vier Endpunkte prüfen jetzt
`accessChecker.checkIsLoggedInUserMemberOfGroup(*UserRightService.FIBU_ORGA_GROUPS)`
– dieselbe Konstante, aus der auch der Menüeintrag seine Gruppen zieht. Die Wirkung
war begrenzt (die Daten liegen in der eigenen Session, `ExpiringSessionAttributes`),
die offene Tür trotzdem real: jeder Angemeldete durfte den E-Rechnungs-Parser
benutzen.

**Alles andere ist gedeckt** – über die DAO, nicht über einen Check im
Rest-Controller: `GET {id}`, `list`, `startMultiSelection`, `saveorupdate`,
`markAsDeleted`/`undelete`/`delete`, `history/{id}`, die Order-Exporte und
`recalculate`/`forecastExportSettings` (die beiden prüfen zusätzlich selbst
`hasLoggedInUserSelectAccess`, weil sie nichts schreiben und ohne DAO-Aufruf
antworten). `columnStates` und `filter/*` fassen nur die Prefs des Aufrufers an.
`JobsMonitorPageRest` hat keinen `accessChecker`, aber der `JobHandler` prüft pro Job
(`readAccess`/`writeAccess`, siehe `ReindexJob`).

**`userAccess` ist ein UI-Hinweis, keine Autorisierung.** `listMeta` liefert
`update = true` ungeprüft (aus `AbstractPagesRest` übernommen), die übrigen Flags mit
`throwException = false`. next liest das Feld derzeit gar nicht – KDoc an
`ListMetaData.userAccess` hält jetzt fest, dass die DAO die Autorität bleibt, damit
eine künftige Seite sich nicht darauf verlässt.

**Kein Filter, der prüft, „ob ein AccessChecker gewerkelt hat".** Naheliegend, aber
nicht tragfähig: `BookDao` überschreibt `hasUserSelectAccess` und `hasAccess` mit
konstantem `true` (Bücher darf jeder sehen – so ist es auch in Wicket) und berührt
den `AccessChecker` nie. Ein solcher Filter würde jeden `/rs/book/…`-Request als
Verstoß melden; 20 DAOs überschreiben `hasAccess`, 11 `hasUserSelectAccess`.
Umgekehrt hätte ein Filter, der auf den DAO-Eintritt prüft, kein Signal für die
`AbstractDynamicPageRest`-Seiten – also gerade für die Klasse, in der die eine Lücke
lag. Dazu prüft ein Filter erst _nach_ der Ausführung. Statt Mechanik daher Doku:
`AbstractDynamicPageRest` sagt im KDoc, dass es hier keinen Backstop gibt, jede
Unterklasse selbst prüfen muss und ein ausgeblendeter Menüeintrag für next nichts
mehr zählt (`BirthdayButlerPageRest` als Vorbild).

**Bewusst nicht angefasst** (alle drei existieren so in `master`):

- `GET /rs/task/info/{id}` (`TaskServicesRest`) liest direkt aus dem `TaskTree` ohne
  `hasUserSelectAccess` – anders als `getTree`, das pro Knoten prüft. Neu ist nur,
  dass next den Endpunkt aufruft (`lib/rs/task.ts`).
- Der fehlende `writeAccess`-Check im History-Append-Pfad (siehe oben).
- `AbstractEntityRest.forceDelete` ist der einzige Schreib-Endpunkt ohne
  `validateCsrfToken`; von next nicht verdrahtet (`lib/rs/entity.ts` nennt
  `DELETE`/`FORCE_DELETE` ausdrücklich als nicht angebunden), betrifft also nur die
  Alt-Clients.

**Hier entdeckt, inzwischen erledigt:** eine `AccessException` konnte als **HTTP 200
mit Toast** zurückkommen, weil `UserException.displayUserMessage` default `true` ist
(`GlobalDefaultExceptionHandler`). `lib/rs/entity.ts` las das als `kind: "ok"`, und
`use-entity-edit-form.ts` meldete dann Erfolg und leitete zur Liste weiter –
geschrieben wurde nichts, die Rückmeldung war trotzdem falsch. Zwei Präzisierungen
gegenüber der ersten Diagnose:

- **Die vier CRUD-Endpunkte waren nie betroffen.** `AbstractPagesRestUtils` fängt in
  `saveOrUpdate`/`delete`/`markAsDeleted`/`undelete` jede Exception selbst ab;
  `handleException` macht aus einer `UserException` – und `AccessException` _ist_ eine
  – ein **HTTP 406** mit `ValidationError(msg, messageId = ex.i18nKey)` und, falls
  gesetzt, `fieldId = ex.causedByField`. Der `GlobalDefaultExceptionHandler` sieht die
  Exception dort gar nicht.
- **Betroffen waren die eigenen Endpunkte hinter `postEntityAction`** (Ausleihen,
  Order-Aktionen …), die keinen solchen `try/catch` haben. Die lesen jetzt
  `kind: "rejected"` – siehe „Erledigt: Zugriffsrechte im Formular".

**Nicht verifiziert:** die Ablehnung des E-Rechnungs-Prüfers. Das lokale Testkonto
ist Admin und in den FiBu-Gruppen, damit ist nur der Erfolgsfall prüfbar; für den
403-Fall braucht es einen Benutzer ohne FIBU/ORGA-Mitgliedschaft.

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

#### Erledigt: Zugriffsrechte im Formular (Speichern/Löschen und einzelne Felder)

**Warum das eine eigene Schranke ist.** Wicket blendet Buttons aus, die der Benutzer
nicht drücken darf (`AbstractEditForm.updateButtonVisibility`), die UILayout-Seiten
bekommen `userAccess` mitgeliefert. next baut sein Formular selbst – ohne Gegenstück
zeigt es jedem den Speichern-Button und lernt erst nach dem Abschicken, dass die DAO
ablehnt. Autorisierung bleibt Sache der DAO; hier geht es darum, dem Benutzer nichts
anzubieten, was er nicht darf.

**Die generische Schranke: zwei Flags am DTO.** `writeAccess`/`deleteAccess`, deklariert
im Interface `rest/dto/EntityAccessSupport.kt` und **an einer Stelle für alle Entitäten**
gefüllt: `AbstractEntityRest.getById` setzt sie, wenn das DTO das Interface implementiert
und `editMode` gilt – aus genau denselben DAO-Aufrufen, die `checkUserAccess` für die
`UILayout.UserAccess` der layoutgetriebenen Seiten macht (`hasLoggedInUserUpdateAccess`,
`hasLoggedInUserDeleteAccess`, beide mit `throwException = false`). Ein eigener Block im
`transformFromDB` einer Rest-Klasse würde die DAO nur ein zweites Mal fragen; `Auftrag`
und `Task` implementieren deshalb nur noch das Interface. `null` heißt „nicht gefragt" –
was eine Listenzeile bekommt, denn kein Listeneintrag hat einen Speichern-Button.

Gelesen wird das an genau einer Stelle, `lib/rs/entity-access.ts`:

- **Fehlendes Flag heißt erlaubt** (`flags.writeAccess !== false`). Anders herum
  müsste jede nicht umgestellte Entität ihr Formular sofort verlieren – ein
  Doku-Versäumnis würde als Rechteproblem erscheinen.
- **`data: unknown` als Parametertyp** ist Absicht: mit einem Interface aus zwei
  optionalen Feldern schlägt TypeScripts Weak-Type-Erkennung bei jedem konkreten DTO
  zu, das die Flags (noch) nicht hat.
- **`isNew` wird separat übergeben**, weil Einfüge-Recht keine Eigenschaft des
  Datensatzes ist, sondern der _Liste_ (`userAccess.insert`). Ein neues Objekt hat
  daher `write = true`, `delete = false`.

Verbraucher: `entity-edit-page.tsx` (Löschen-Button nur bei `access.delete`, `canSave`
an die Aktionsleiste) und `entity-edit-actions.tsx`. Zwei Details:

- Der Speichern-Button und die `saveOption`-Auswahl werden **ganz weggelassen**, nicht
  ausgegraut. Ein grauer Button liest sich als „noch nicht", und es gibt nichts, was
  dieser Benutzer tun könnte, um ihn zu aktivieren.
- Die Return-/CTRL-Return-Abkürzung ist mitgeprüft. Sonst speichert die Tastatur, was
  die Maus nicht anbietet.

**Feldweise Rechte: `vollstaendigFakturiertWriteAccess`.** Das Flag „vollständig
fakturiert" (Auftragsposition _und_ Zahlungsplan) darf nur ändern, wer
`RechnungDao.USER_RIGHT_ID` mit `READWRITE` hat **und** in der FiBu-Gruppe ist –
dieselbe Bedingung, die `AuftragRight` beim Speichern prüft. Das Muster für solche
Felder, zweifach bewusst anders als Wicket:

- Das Feld wird **immer gerendert**, ohne das Recht read-only. Wicket blendet es
  komplett aus; dann sieht der Benutzer den Zustand des Datensatzes nicht.
- Der Hinweis am read-only-Feld ist **die Meldung, mit der das Backend die Änderung
  ablehnen würde** (`fibu.auftrag.error.vollstaendigFakturiertProtection`) – eine
  Quelle für Regel und Erklärung. Wicket bietet umgekehrt ein editierbares
  Kästchen an, dessen Speichern die DAO anschließend verweigert.

Arbeitsteilung: **die Flags stoppen den ehrlichen Client, die DAO den unehrlichen.**
Kein Vorab-Check im `validate()` – nur `checkUpdateAccess` weiß, ob sich der Wert
gegenüber `dbObj` überhaupt _geändert_ hat.

**`kind: "rejected"` in `lib/rs/entity.ts`.** Eine Ablehnung aus einem eigenen
Endpunkt (hinter `postEntityAction`) kommt als HTTP 200 mit nichts als einem
Danger-Toast. Unterschieden von einem geglückten Speichern _mit_ Warnung wird sie am
`targetType`: eine Ablehnung ist `TOAST` und nur das, ein Speichern antwortet mit
`REDIRECT`. `use-entity-edit-form.ts` zeigt den Toast und bleibt auf der Seite.

**Backend-Fund nebenbei:** `AuftragRight.hasAccess` schützte das Flag der
_Positionen_, nicht das der _Zahlungspläne_ – dasselbe Feld, dieselbe Regel, eine
Lücke. Jetzt beide, mit Test in `AuftragDaoTest`.

**Die Grenzen, damit keine spätere Seite mehr annimmt als da ist:** `page-def` hat
**kein** `readOnlyWhen`; `readOnly` in `DeclaredField` ist ein statisches Flag und
wird nur von `NumberField` beachtet (`declared-form-field.tsx:153`) –
Select/Input/TextArea/Checkbox ignorieren es. Genau deshalb sind die Checkboxen des
Auftragsformulars in `position-row.tsx`/`payment-schedule-row.tsx` handgerendert. Wer
feldweise Rechte deklarativ braucht, muss das erst im `page-def` nachziehen.

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

**Was ersetzt wurde.** `BookEntityRest.createEditLayout` hängt ein
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
Reiter „Anhänge“ nutzt `attachment.list`, also den Titel, den `BookEntityRest`
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

**Nachtrag (Aufgabenbaum, Schritt 2): Zahlenbereiche gehören derselben Quelle.**
`@Column` kann sie nicht ausdrücken – `length` ist bei Zahlen eine Ziffernanzahl,
`precision`/`scale` eine Speichergröße, keines davon „0 bis 100 Prozent". Wicket sagte
es deshalb pro Formularfeld (`MinMaxNumberField`), und `TaskPagesRest.validate` hatte
die drei Bereiche des `TaskDO` als handgeschriebene Prüfung. Jetzt:

- **`@PropertyInfo(min = "…", max = "…")`** (Strings, weil eine Annotation nur
  Compile-Zeit-Konstanten hält) an der Property – `TaskDO.progress` 0–100,
  `maxHours` 0–9999, `duration` 0–10000, `AuftragDO.probabilityOfOccurrence` 0–100.
- `ElementsRegistry.getElementInfo` parst sie nach `ElementInfo.min/max`
  (`BigDecimal`); ein unlesbares Literal wirft, statt eine Regel still fallen zu lassen.
- **`ValidationUtils.validateFields` erzwingt sie für jede Entität** – mit der
  Meldung, die Wickets `MinMaxNumberField` benutzt
  (`validation.error.range.integerOutOfRange`), damit beide Frontends zum selben Wert
  dasselbe sagen. Die Kopie in `TaskPagesRest.validate` ist entfallen; dort steht nur
  noch die Kreuzregel `duration` ⇔ `endDate`.
- `GenerateNextFieldMetadataMain` emittiert `min`/`max` (als `toPlainString`, sonst
  käme 10000 als `1E+4` an), `lib/metadata/types.ts` hält sie im Kontrakt und
  `from-metadata.ts` baut daraus die Zod-Refinements. `order-schema.ts` und
  `task-schema.ts` deklarieren also **keinen** Bereich mehr selbst; der
  `overrides`-Parameter bleibt nur für die Kostennummern-Segmente, die der
  `SegmentedNumberField` ohnehin pro Segment braucht, und die Metadaten gewinnen.

#### Erledigt: das Edit-Gerüst für Seiten mit mehreren Aufrufern (Aufgabenbaum, Schritt 2)

Drei Ergänzungen am geteilten Gerüst, alle drei aus dem Aufgabenformular entstanden und
für jede weitere Seite gültig:

- **`SectionDef.collapsed`.** Eine Sektion startet zugeklappt (dieselbe `Collapsible`
  wie `RepeatableRow`, Kopfzeile = Trigger). `EditPageShell.sections` nimmt jetzt
  `ReactNode | ((active: boolean) => ReactNode)`, damit eine geklappte Sektion erfährt,
  dass ihr Anker-Tab angeklickt wurde – dann öffnet sie sich, schließt aber beim
  Wegscrollen nie wieder. Das Öffnen passiert beim Rendern (abgeleiteter State), nicht
  im Effekt: React verlangt es so, und die Karte flackert dadurch nicht.
- **`readOnly` jenseits von `NumberField`.** `InputField`, `SelectField` und
  `TextAreaField` haben jetzt `disabled` und geben `readOnly` an `FieldShell` weiter
  (kein Pflicht-Stern an einem Feld, das dieser Nutzer nicht füllen darf).
  `SelectField.numeric` wurde zu **`valueType: "string" | "number" | "boolean"`** –
  Radix kennt nur String-Optionen, und `TaskDO.kost2IsBlackList` ist ein `Boolean`.
- **`EditDef.returnTargets` + `?returnTo=`.** Eine Edit-Seite kehrt zum _Aufrufer_
  zurück, nicht zu einer festen Liste: eine Aufgabe wird aus dem Baum geöffnet und
  (seit Schritt 4a) aus ihrer eigenen Liste. `hooks/use-edit-return.ts` löst den
  Parameter gegen die deklarierten Ziele auf – eine **Whitelist**, kein Saubermachen
  von URLs, ein unbekannter Wert fällt auf das erste Ziel zurück. Der Parameter wandert
  über `entityTabs` mit, damit der Umweg über die History den Aufrufer nicht vergisst.
  Seiten ohne `returnTargets` (`book`, `cost1`, `order`) verhalten sich unverändert.
  Wer `useSearchParams` nutzt, braucht im Static Export eine `<Suspense>`-Grenze – das
  ist der Grund, warum `app/(authenticated)/task/[id]/page-client.tsx` eine hat.
- Außerdem: `dataType: "TASK"` wird in `DeclaredFormField` auf `TaskSelectField`
  (Modal-Baum) verteilt statt auf die Entitäts-Autocomplete, und
  `EntityAutocomplete`/`EntityAutocompleteField` nehmen `params` (im Query-Key
  enthalten, sonst bediente ein Projektwechsel den Cache des alten Projekts).

#### Erledigt: zwei Vokabeln für Spalten (Aufgabenbaum, Schritt 4a)

Beide am `ColumnBase` und für jede Liste gültig, entstanden an der Aufgabenliste:

- **`visible?: (ctx) => boolean`.** Die Seite _hat_ die Spalte nicht – für eine, deren
  Gegenstand in dieser Installation nicht existiert oder die dieser Nutzer nicht sehen
  darf (Kostenträger konfiguriert, Auftragspositionen gebucht, FiBu-Gruppe). Das ist
  **nicht** dieselbe Sache wie eine ausgeblendete Spalte: die ist die Wahl des Nutzers und
  im Spaltenpanel umkehrbar. Gefiltert wird in `entity-list-page.tsx`, bevor die
  Audit-Spalten angehängt werden, also erreicht so eine Spalte TanStack nie und kann auch
  im Panel nicht auftauchen. Der Kontext ist `listMeta.variables`, das die Seite ohnehin
  lädt – die Antwort auf so eine Frage ist die des Backends
  (`AbstractEntityRest.addVariablesForListPage`), nicht eine im Client abgeleitete.
- **`sortable: false`.** Ein berechneter Wert, nach dem das Backend nicht ordnen kann;
  wird zu `enableSorting: false` an der Spaltendefinition, den Rest kann
  `DataTableColumnHeader` schon.

Dazu, backendseitig, `BaseDTO.copyFrom4ListRow` als das Mittel, eine Liste mit einer
eigenen, schlanken Zeile zu bedienen: `Task` ist das erste Beispiel dafür, dass eine solche
Zeile auch _berechnete_ Werte tragen darf, die am DO gar nicht stehen.

Zwei weitere Korrekturen an der gemeinsamen Schicht, beide an der Aufgabenliste
aufgefallen und beide für jede Liste gültig:

- **Die Liste nennt sich selbst als Aufrufer.** `useEditTargets` hängt an die URL, die ein
  Zeilenklick und der „Neuer Eintrag“-Knopf öffnen, ein `?returnTo=<eigene Route>` – aber
  nur, wenn die Edit-Seite diese Route unter ihren `returnTargets` führt. Ohne das kehrte
  eine aus der _Liste_ geöffnete Aufgabe beim Abbrechen in den **Baum** zurück, weil der
  dort das erste und damit das Standardziel ist. Seiten ohne `returnTargets` (`book`,
  `cost1`, `order`) bekommen weiterhin keinen Parameter: ihr Standard ist die eigene Liste
  und sagt schon dasselbe.
- **`leafKeyOf` an drei geteilten Stellen.** Ein Backend-Schlüssel, der Text _und_
  Namensraum ist, liegt im Katalog als `<key>._`; die bloße Form lässt next-intl mit
  `INSUFFICIENT_PATH` scheitern. `task.title.list` ist genau so einer geworden
  (`…list.select` kam dazu), und das traf `entity-list-page.tsx` (Titel der Liste),
  `use-edit-return.ts` (Beschriftung des Rückwegs) und `task-perspective-link.tsx`.
  Gelöst wurde es in der geteilten Schicht mit `lib/leaf-key.ts`, nicht dadurch, dass die
  Aufgabenseite den Schlüssel meidet – jede Seite, deren Titelschlüssel Kinder bekommt,
  wäre sonst die nächste.
- **`NextMigration.nextRouteUrl(category, route, legacyUrl)`.** Eine **zweite Perspektive**
  einer schon migrierten Entität, unter einer Route, die die Kategorie nicht hergibt: die
  Aufgabe hat ihre Liste (`next/task`, die `NextPage.route`) und ihren Strukturbaum
  (`next/taskTree`), und der Menüeintrag öffnet den Baum, während jeder Redirect der
  Kategorie in die Liste geht. `NextPage` kann nur _eine_ Route ausdrücken, also nennt der
  Aufrufer die zweite – und bleibt trotzdem an `MIGRATED` gekoppelt: solange die Entität
  nicht migriert ist, ist die Antwort die übergebene Legacy-URL. Kein Fall für eine Seite
  mit nur einer Perspektive; die nimmt weiter `listUrl`.
- **`e2e/fixtures/list-table.ts` – `listRows` / `waitForRows` / `waitForRow`.** Was eine
  Liste zeigt, während ihre erste Seite lädt, sind acht `<TableRow>` voller `Skeleton` – im
  selben `tbody`. Ein Spec, der auf `tbody tr` wartet und dann eine Zelle liest, liest ein
  leeres Kästchen **und besteht dabei**; dasselbe gilt für den Leer-Zustand. Die Zeilen des
  Ergebnisses sind die mit `data-row-id` (setzt nur `DataTableRow`), und das ist das eine
  Signal, das „diese Zellen tragen Werte aus der Datenbank" heißt. Alle Listen-Specs sind
  darauf umgestellt; `waitForRow(text)` deckt zusätzlich den Fall ab, dass nach einer Suche
  noch das vorige Ergebnis steht (`keepPreviousData`).

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

- Die **Liste** ist bereits REST-migriert (`OrderEntityRest.createListLayout`,
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
- **Vorgehen:** handgebaute Next-Seite (`components/features/order/`) mit RHF+Zod
  gegen ein **echtes, geschachteltes Order-DTO** (heute `Auftrag.kt` `positionen`
  als rohe `MutableList<AuftragsPositionDO>`). Neue/erweiterte REST-Endpunkte für
  Positions-CRUD + Live-Kalkulation (Backend-Logik `AuftragDao`/`AuftragsCache`
  wiederverwenden). Menü-URL auf `next/` schalten.

**Verifikation.** Auftrag mit mehreren Positionen + Zahlungsplan
anlegen/ändern, Summen/Forecast gegen Wicket vergleichen, History prüfen.

#### Debitorenrechnungen (`outgoingInvoice`) – vollständig migriert

Die vierte Seite nach `book`, `cost1` und `order`. Sie war die erste, die **nur als
Liste** migriert wurde: `RechnungDO` hat mit `RechnungsPosition` →
`KostZuweisung` drei Schachtelungsebenen, mehr als die Auftragsposition. Die Liste
lief deshalb voraus, die Edit-Seite blieb zunächst Wicket – seit dem Release-Schalter
in `NextMigration.MIGRATED["outgoingInvoice"]` (kein `listOnly` mehr, `editRoute =
invoice/:id`, `newEntryRoute = invoice/new`) führen Zeilenklick, „Neu" und jeder
serverseitige Redirect nach dem Speichern nach next. Wickets Formular bleibt allein
über die Escape-Hatch-Verlinkung der Seite erreichbar.

Was den Schalter möglich gemacht hat, sind die drei Dokumentfunktionen, die es vorher
nur in Wicket gab und für die kein REST-Endpunkt existierte – alle drei sind jetzt
Endpunkte von `OutgoingInvoiceEntityRest` und Teil des next-Formulars:

- **Word-Export** (`GET exportInvoiceWord/{id}`, eine Menüvariante je
  `InvoiceService.getTemplateVariants()`),
- **E-Rechnung** (`GET eInvoice/{id}/validate|xrechnung|zugferd` plus
  `POST saveAndCheckEInvoice`, ein **Abschnitt des Formulars**, unabhängig davon, ob
  der Verkäufer konfiguriert ist – anders als Wickets `addEInvoiceMenu()`),
- **Rechnungs-PDF** (`GET/POST/DELETE invoicePdf/{id}`, eigenes Feld im
  E-Rechnungs-Abschnitt; die Anhangsliste daneben filtert den Marker
  `__INVOICE_PDF__` heraus).

Alle drei arbeiten auf dem **persistierten** Stand und sind ohne id deaktiviert – ein
Dokument, das nach außen geht, soll dem entsprechen, was in der Datenbank steht. Wicket
exportiert dagegen den ungespeicherten Formularstand
(`setDefaultFormProcessing(false)`). Beim Word-Export bleibt es dabei: er lädt den
gespeicherten Stand herunter. Die E-Rechnung dagegen speichert selbst, und zwar
ausdrücklich – ihre Buttons heißen „Speichern und XRechnung" bzw. „Speichern und
ZUGFeRD" (siehe unten). Ein Hinweis auf nicht gespeicherte Änderungen ist damit
gegenstandslos und entfallen.

**Die E-Rechnung ist kein Dialog, sondern ein Abschnitt.** Wickets
`EInvoiceModalDialog` war zuerst 1:1 übernommen (`e-invoice-dialog.tsx`, hinter einem
zweiten Eintrag des Export-Buttons) und ist wieder abgeschafft: Was eine E-Rechnung
verhindert, sind Felder _dieses_ Formulars – Kundenadresse, Verkäufer-Bankkonto,
Rechnungsnummer –, und die Prüfliste dazu in ein zweites, schmaleres
Bearbeitungsfenster zu legen heißt, den Fehler dort anzuzeigen, wo er nicht behoben
werden kann. Jetzt steht sie im Abschnitt „E-Rechnung" (`fibu.konto.eInvoice`) unter
genau diesen Feldern (`e-invoice-section.tsx` = `SectionDef.footer`, dazu
`e-invoice-checklist.tsx` und `e-invoice-actions.tsx`) – am Ende des Formulars, direkt
über den Anhängen, die der ZUGFeRD-Export einbettet. Vier Folgen:

- **Der Abschnitt blockiert nie.** Fehlende Adresse oder Bankverbindung verstecken ihn
  nicht (Wicket verbirgt den Menüeintrag bei unkonfiguriertem Verkäufer); die
  Prüfliste ist Hinweis, keine Sperre. Auch die Buttons sind nie deaktiviert: sie
  speichern zuerst, und das Speichern ist der Weg aus genau dem heraus, was die
  Prüfliste nennt. Verweigert wird erst der Export dahinter – wie ihn das Backend
  verweigern würde. „Verkäufer nicht konfiguriert" ist eine Zeile der Liste wie jede
  andere.
- **`SectionDef.footer` ist die neue Primitive** (`lib/page-def/types.ts`,
  gerendert von `declared-sections.tsx`): abschnittseigene UI _unter_ dem
  deklarierten Feldergrid, in derselben Karte. Eine `ComponentType` und keine
  `render`-Funktion, weil so ein Rumpf eigene Hooks hält. Ein reines `render` hätte
  Grid und `DeclaredFormField`-Verdrahtung duplizieren müssen.
- **Zwei Buttons statt drei, jeder mit dem Speichern davor**: „Speichern und
  XRechnung" und „Speichern und ZUGFeRD"
  (`fibu.rechnung.eInvoice.saveAndXRechnung|saveAndZugferd`, zwei neue Keys). Der
  Zwischenschritt ist eine **deklarierte Action** (`edit.actions` →
  `POST saveAndCheckEInvoice`) und **bleibt auf der Seite** – der Frontend-Pfad für
  Actions ignoriert den `REDIRECT`-`ResponseAction` (`hooks/use-entity-edit-form.ts`),
  während das reguläre Speichern zur Liste zurückführt. Genau das braucht ein
  Abarbeiten der Prüfliste. Der Endpunkt validiert mit `validate(dbObj, postData)`,
  damit die Leistungszeitraum-Regel des DTOs mitläuft, prüft den CSRF-Token wie das
  geerbte `saveOrUpdate` – und exportiert selbst nichts: der Button lädt danach die
  Prüfliste neu (`eInvoiceQueryKey`) und lädt nur herunter, wenn sie leer ist.
  Wickets eigener „Speichern und E-Rechnung"-Button (`saveAndOpen`) hat damit keine
  Entsprechung mehr; sein Zweck – speichern und dann exportieren – steckt in beiden
  Buttons.
- **`SubmitMeta.onWritten` ist dafür neu** (`lib/rs/submit-meta.ts`). Ein Button, der
  nach dem Schreiben weiterarbeitet, muss eine Ablehnung von einem Erfolg
  unterscheiden können – und `form.handleSubmit()` löst in beiden Fällen ohne
  Rückgabewert auf, weil eine Ablehnung hier eine normale Antwort ist (HTTP 406, bei
  einer `AccessException` ein Toast) und nichts wirft. Der Callback bekommt das
  `EntityWriteResult`; ohne ihn verhält sich jeder Submit wie bisher.

Dazu kamen für das Formular: serverseitige Leistungszeitraum-Validierung
(`PeriodOfPerformanceValidator`, wie beim Auftrag), `formDefaults` (Default-MwSt,
Bankkonten, `eInvoiceConfigured`, Template-Varianten) in einem GET je Mount,
Kost2-Vorbelegung und -Warnung über `activeKost2`/`kost2Check` (Wickets Regel aus
`onRenderCostRow`, aber mit Text statt nur gelbem Rahmen),
`sellerBankAccount` als Select und der Auftragspositions-Picker über
`order/positionAutosearch` samt Sprung zum Auftrag.

Zwei Fehler wurden dabei behoben: `transformForDB` nullte Wickets
`uiStatusAsXml`-Spalte bei jedem next-Save (derselbe Defekt bestand für den Auftrag),
und `RechnungDao.find` warf für eine unbekannte id eine NPE statt 404 zu liefern.

Zwei Dinge folgen aus der Liste-zuerst-Phase, beide bewusst generisch gebaut statt
für die Rechnung:

- **`PageDef.edit` ist optional.** Eine Deklaration ohne Form-Hälfte entsteht über
  `defineListPage` statt `definePage`; wohin Zeilenklick und „Neu" führen,
  entscheidet `useEditTargets` – bei fehlender Form auf die Legacy-Seite, die das
  Backend selbst benennt (`listMeta.legacyEditPage`). `EntityEditPage` verlangt
  weiterhin eine Form (`EditablePageDef`), kann eine solche Seite also nicht
  bekommen.
- **Mehrfachauswahl** (`AbstractMultiSelectedPage`, `MultiSelectionSupport`) ist die
  erste Fähigkeit, die keine migrierte Seite hatte. Sie gehört als Primitive in
  `components/data-table` + `PageDef.massUpdate`, nicht in die Rechnung: das
  Backend-Protokoll ist für jede Entität dasselbe. Achtung, der Zustand liegt in
  der HTTP-Session (Schlüssel ist die _PagesRest_-Klasse, TTL 60 min) – das
  braucht Sticky Sessions.

**Prozenteingabe im Netto-Feld einer Kostzuweisung** kam nachgeliefert: „50 %" teilt die
Position, wie Wickets `CurrencyConverter` mit der Positionssumme als Total es tut. Sie
sitzt in `NumberField` (`shareOf`), nicht in der Rechnung – der Konverter ist auch in
Wicket allgemein, heute aber nur an dieser einen Stelle mit einem Total versehen
(`RechnungCostEditTablePanel`). Zwei Dinge waren dabei zu klären: `parseNumberInput`
verschluckte ein „%" bisher stillschweigend und las „50 %" als 50, deshalb ist der Fall
mit `parsePercentInput` jetzt ausdrücklich; und die Basis kommt aus der debounced
Serverantwort (`useInvoiceSums`), die zwischen zwei Tastendrücken kurz leer wäre – daher
`keepPreviousData` dort und ein `shareOf`, das eine fehlende Basis von „nimmt keinen
Anteil" unterscheidet: ohne Basis bleibt die Eingabe stehen, statt zu 50 € zu werden.

**Verifikation.** Die e2e-Suite läuft gegen die lokale Instanz (`E2E_BASE_URL`), das
Testkonto hat `FIBU_AUSGANGSRECHNUNGEN`. Abgedeckt sind Liste, Formular, Positionen,
Kostzuweisungen inklusive Fehlbetrag und Kost2-Warnung, Anhänge, Rechnungs-PDF und
beide Exportarten (`e2e/invoice-*.spec.ts`); backend-seitig die Suiten unter
`org.projectforge.rest.fibu.*`. Jede Spezifikation legt ihre eigene `GEPLANT`-Rechnung
an und markiert sie danach gelöscht – keine nennt eine Zeile der Datenbank, und keine
verbraucht eine Rechnungsnummer.

**Was bewusst offen bleibt:**

- **`AccessException` beim Speichern liefert HTTP 200 plus Toast** und wird von
  `hooks/use-entity-edit-form.ts` als Erfolg gelesen – der Benutzer sieht die Meldung,
  das Formular verhält sich aber als sei gespeichert worden. Das ist keine Eigenheit
  der Rechnung, sondern der gemeinsame Save-Pfad, und gehört in eine eigene Änderung.
- **Eine in next soft-gelöschte Position wird physisch verworfen**, wenn die Rechnung
  danach in Wicket geöffnet und gespeichert wird
  (`AbstractRechnungEditForm.removeIf(AbstractBaseDO::getDeleted)`). Risiko des
  Parallelbetriebs; der reguläre Weg führt nicht mehr nach Wicket.
- **Die dritte Anhangsart fehlt – in beiden Frontends.** Der E-Rechnungs-Abschnitt
  unterscheidet heute zwei Arten: das Hauptdokument (der Anhang mit der Beschreibung
  `__INVOICE_PDF__`) und alles andere, das die ZUGFeRD-Datei einbettet
  (`EInvoiceExportService.embedAttachments`/`embedAttachmentsInPdf` nehmen jeden
  Anhang außer dem Marker). Nicht ausdrückbar ist ein Anhang, der an der Rechnung
  hängt und **nicht** eingebettet werden soll. Das ist keine Lücke der Migration:
  Wicket kennt sie genauso wenig – sein Dialog hat ein eigenes Upload-Feld für das
  Hauptdokument (`RechnungEditForm.processInvoicePdfUpload`), schreibt es aber als
  gewöhnlichen JCR-Anhang mit dem Marker, und die „E-Rechnungs-Anhänge" darunter sind
  nur eine Nur-Lese-Liste der bestehenden Anhänge. Es braucht ein persistiertes Flag
  je Anhang (neue `FileInfo`-Property, OakStorage, `AttachmentsService.changeFileInfo`)
  und betrifft Wicket mit; deshalb eine eigene Änderung und nicht Teil dieser.
- **Die 2FA-Kurzbefehle** (`ProjectForge2FAInitialization`, `My2FAShortCut.FINANCE*`)
  nennen Wicket-URLs und `PagesRest`-Klassen; für einen Seitenaufruf unter
  `/next/invoice/...` greift keins der Muster. Der schreibende REST-Zugriff ist über
  `WRITE:outgoingInvoice` weiterhin geschützt. Das betrifft alle migrierten
  FIBU-Seiten gleichermaßen (der Auftrag steht genauso da) und ist deshalb eine
  übergreifende Aufgabe, nicht eine der Rechnung.

**Nachgetragen: die E-Rechnungs-Prüfung ist übersetzt.**
`EInvoiceExportService.validate` lieferte englische Klartexte („Invoice number is
missing") statt i18n-Keys, in jeder Locale – vorbestehend und nicht next-spezifisch,
Wickets Fehlerzeile zeigte dieselben Sätze. Der Dienst übersetzt sie jetzt selbst
(zehn Keys `fibu.rechnung.eInvoice.error.*`), wovon beide Frontends profitieren. Die
Antwort bleibt eine Liste von Sätzen und wird keine Liste von Keys: jeder Aufrufer
legt sie einem Benutzer unverändert vor, ein Key müsste also zweimal mit demselben
Bundle aufgelöst werden. Das Flag `configured` bleibt daneben stehen, wird aber nicht
mehr eigens angezeigt – „Verkäufer nicht konfiguriert" ist der erste Eintrag der Liste
(`…error.sellerNotConfigured`). Die Tests vergleichen entsprechend gegen
`translate(key)` und nicht gegen englische Teilzeichenketten, die nur für ein
englisches Konto gehalten hätten.

#### Offen: Sprung zum Strukturelement zeigt noch auf Wicket

Die Auftragsposition verlinkt ihr Strukturelement wieder auf dessen eigene Seite
(`components/shared/tasks/task-edit-link.tsx`, neuer Tab, damit das Formular mit allen
ungespeicherten Eingaben stehen bleibt). Das war eine Rückmeldung aus dem Betrieb: in der alten
Version kam man von der Position zum Strukturelement und dort an die darauf gebuchten
**Zeitberichte**.

Das Ziel ist **fest verdrahtetes Wicket** (`wa/taskEdit?id=…`) und damit die einzige Stelle in
next, die eine Legacy-URL selbst bildet. Grund: `listMeta.legacyEditPage` liefert für die
Kategorie `task` `react/task/edit/:id` – `task` steht nicht in `NextMigration.MIGRATED`, also
fällt `legacyApp` auf die React-App zurück –, und das React-Formular ist ein reines
UILayout-Formular ohne die Aktion, um die es hier geht: „Zeitberichte anzeigen" ist ein
Content-Menü-Eintrag von Wickets `TaskEditPage` (`task.menu.showTimesheets`).

**Nach der Migration der Strukturelement-Seite ist das umzustellen:** Link von Wicket auf die
next-Route, das heißt `wa/taskEdit?id=…` in `task-edit-link.tsx` ersetzen durch die URL, die das
Backend dann nennt (`useLegacyEditUrl`/`nextEditPage` mit einem `task`-Eintrag in `MIGRATED`),
und die Sonderbehandlung hier wieder auflösen. Dasselbe gilt für die schon vorhandene
Strukturbaum-Seite (`app/(authenticated)/taskTree/page.tsx`), die ihren Zeilenklick heute über
`listMeta.legacyEditPage` in die React-App schickt.

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

#### Aufgabenbaum (dritter handgebauter Fall) – Bestandsaufnahme

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

##### Was in next steht (`/next/taskTree`, ~1030 Z. in 12 Dateien)

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

##### Lücke 1 – Routing/Menü (in Schritt 5 erledigt)

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

##### Lücke 2 – die Aktionen der Baumseite (`TaskTreePage.init()`, `TaskTreeForm`)

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
Schritt 3 der Reihenfolge unten – bis auf **Favoriten** und **Aufgaben-Assistent** (bewusst
ausgelassen). „Filter zurücksetzen" hängt _nicht_ an
der `filter/reset`-Lücke, wie hier vermutet: der Baum hat seinen eigenen Filter, s. Schritt 3.

Die **Listenansicht** ist in Schritt 4a erledigt – und die Vermutung „erst das Listen-Layout
füllen" war falsch. Das `UITable`-Layout bleibt einspaltig; die zehn Spalten sind im `PageDef`
deklariert, wo die Spalten einer handgebauten Liste hingehören. Der Umschaltknopf steht in
beiden Richtungen.

##### Lücke 3 – die Edit-Seite, handgebaut als `PageDef`

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
   (`wa/timesheetEdit`, `wa/timesheetList`, `wa/ganttChartEdit`, `wa/accessList`)
   – bleiben also zunächst Links ins Alt-Frontend.

##### Reihenfolge

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

   Die drei Gerüst-Ergänzungen, die dabei entstanden sind, stehen in Phase 1.5 unter
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

   **Zwei Einträge fehlen bewusst** (entschieden: „beide erst mal auslassen"), weil next sie
   nicht selbst bedienen kann: die **Favoriten** (`UserPrefListPage` für
   `UserPrefArea.TASK_FAVORITE`, `TaskFavoritesRest` ist vollständig, aber es fehlt die
   Verwaltungsseite) und der **Aufgaben-Assistent** (`TaskWizardPageRest` ist ein Torso,
   s. Lücke 2). Beide bleiben über den Legacy-Link im Seitenkopf erreichbar. Wickets
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
   Perspektiven nie verschiedene Zahlen zeigen; und es kostet keine Abfrage, weil der
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

   Zwei neue Vokabeln im `ColumnBase` (in Phase 1.5 registriert): `visible` (die Seite
   _hat_ die Spalte nicht – anders als eine im Spaltenpanel abgewählte) und
   `sortable: false`.

   Wickets **Listenansicht**-Knopf ist jetzt beidseitig da:
   `components/shared/tasks/task-perspective-link.tsx` in beide Richtungen, in der Leiste
   des Baums und über `PageDef.listActions` in der Toolbar der Liste. Die Rückrichtung ist
   mit `task.title.list` beschriftet – Wickets eigener Knopf liest das unübersetzte Modell
   `"listView"`, ein Fehler dort und kein Text zum Abschreiben. Dazu die Liste als
   **zweites** `returnTarget`; der Baum bleibt das erste und damit das Ziel einer
   Add-URL ohne `returnTo`. Genau deshalb musste `useEditTargets` lernen, den Parameter
   selbst zu setzen (siehe Phase 1.5): sonst schickte ein Abbruch in der aus der Liste
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

   **b) Offen: Aufgaben-Assistent.** `TaskWizardPageRest` ist schlimmer als ein Torso – es
   hat nur `getForm`, und den `execute`-Endpunkt, auf den der eigene Fertig-Knopf postet,
   **gibt es nicht**. Die eigentliche Arbeit ist `TaskWizardPage.create`: den Baum
   hochlaufen und `GroupTaskAccessDO`-Zeilen schreiben (Leiter/Mitarbeiter/Extern
   rekursiv, Gast an jedem Vorfahren), für drei Gruppen-Picker, für die next keine
   Komponente hat. Also ein Schreib-Endpunkt plus ein Rechte-Feature, keine
   Formular-Migration. Bis dahin über den Legacy-Link erreichbar.

5. **Erledigt: Menü + Umschaltung.** `NextMigration.MIGRATED["task"]` und
   `lib/hand-built-categories.ts` sind zusammen umgeschaltet (`NextMigrationTest` erzwingt
   es), `MenuItemDefId.TASK_TREE` geht über das neue `NextMigration.nextRouteUrl` auf
   `next/taskTree` – die Einzelheiten in Lücke 1 oben. Die Wicket-Seite bleibt über die
   Fluchtluke am Seitentitel erreichbar (`NextMigration.legacyListUrl`), was sie muss:
   Favoriten und Aufgaben-Assistent sind die zwei Einträge, die next nicht hat.

   Gegen das laufende System geprüft (nach `npm run build` +
   `:projectforge-next:copyNextBuild`): der Menüeintrag antwortet mit `next/taskTree`,
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
- **Zugriffsrechte im Formular:** `projectforge-next/lib/rs/entity-access.ts`,
  `components/shared/edit/entity-edit-page.tsx` + `entity-edit-actions.tsx`,
  feldweise `components/features/order/edit/position-row.tsx` +
  `payment-schedule-row.tsx`; Backend `projectforge-rest/.../rest/dto/Auftrag.kt`
  (`vollstaendigFakturiertWriteAccess`; die generischen `writeAccess`/`deleteAccess`
  in `rest/dto/EntityAccessSupport.kt`, gefüllt in `rest/core/AbstractEntityRest.getById`) +
  `rest/fibu/OrderEntityRest.kt` (`transformFromDB`),
  `projectforge-business/.../fibu/AuftragRight.kt`; Wicket-Vorbild
  `AbstractEditForm.updateButtonVisibility`
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
  `projectforge-rest/.../fibu/OrderEntityRest.kt`, `rest/dto/Auftrag.kt`
- **Aufgabenbaum:** Wicket `projectforge-wicket/.../web/task/`
  (`TaskTreePage.java`, `TaskTreeForm.java`, `TaskTreeBuilder.java`,
  `TaskEditPage.java`, `TaskEditForm.java`, `TaskListForm.java`,
  `web/admin/TaskWizardForm.java`); Backend
  `projectforge-rest/.../rest/task/TaskServicesRest.kt` (Baum + Spalten),
  `TaskPagesRest.kt`, `TaskFavoritesRest.kt`, `TaskWizardPageRest.kt` (Torso),
  `rest/dto/Task.kt`, Rechte `projectforge-business/.../task/TaskDao.kt`
  (`hasAccessForKost2AndTimesheetBookingStatus`), Klappzustand
  `.../task/TaskTree.kt` (`USER_PREFS_KEY_OPEN_TASKS`), Kost2-Anhang
  `.../task/TaskHelper.kt`; next `app/(authenticated)/taskTree/page.tsx`,
  `components/shared/tasks/*` (Baum, Aktionsleiste, Routen),
  `components/features/task/*` (Edit-Seite), `lib/rs/task.ts`, `lib/docs-links.ts`,
  `e2e/task-tree.spec.ts`, `e2e/task-tree-actions.spec.ts`, `e2e/task-edit.spec.ts`,
  `e2e/task-kost2-preview.spec.ts`

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
- **Zugriffsrechte im Formular** – `writeAccess`/`deleteAccess` am DTO
  (`EntityAccessSupport`, für alle Entitäten in `AbstractEntityRest.getById` gefüllt),
  gelesen in `lib/rs/entity-access.ts` (fehlendes Flag = erlaubt), Speichern-Button und
  Löschen-Button werden ohne Recht weggelassen statt ausgegraut, Tastatur-Abkürzung
  mitgeprüft; feldweise Rechte am Beispiel „vollständig fakturiert" (immer sichtbar,
  read-only, Hinweis = die Meldung des Backends); Ablehnungen aus eigenen Endpunkten
  als `kind: "rejected"` statt als Erfolg. Grenze: `page-def` kennt kein
  `readOnlyWhen`.
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
   möglich). Beim **Aufgabenbaum** stehen Baumseite, Aktionsleiste, die handgebaute
   Edit-Seite und die Listenperspektive (Schritte 1–4a des eigenen Abschnitts); offen
   sind nur noch der Aufgaben-Assistent (4b) und das Umschalten von Menü und
   `NextMigration.MIGRATED["task"]` (5). Routing steht wie entschieden: Baum
   `/next/taskTree`, Liste `/next/task`, Edit `/next/task/:id`.
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
