# Website concept projectforge.org (step 1: content, structure, messaging)

Status: draft for review. Comments in German, website texts in English (ready to paste).
Design is done elsewhere; this document only defines content, order and structure.

**Festlegungen (aus der Abstimmung):**
- Zielgruppe: Projektdienstleister (IT-Agenturen, Beratungen, Ingenieurbüros), 10–500 Personen.
  Entscheider: Geschäftsführung/Controlling, Projektleitung, IT-Betrieb.
- Markt DACH/EU, Seite auf Englisch, Absender „we“ = Micromata, lockerer Ton.
- Open Source (Community Edition, GPLv3, dual-licensed), kostenlos ohne Einschränkungen.
  Hosting/Support/Einführung durch Micromata: on request.
- Kontakt: GitHub Issues. Keine öffentliche Demo. Kein Team-Abschnitt.
- Nicht erwähnen: IHK, Marketing, Memo, ToDo, Wicket; auch keine Features, die es nicht gibt
  (kein CalDAV-Server – Kalender nur per ICS-Abo; keine OpenAPI/Swagger).

Platzhalter `[TODO: …]` = Angabe, die ich nicht belegen kann und die du ergänzen/streichen musst.

---

## 1. Positioning

### Core message

> **From order to invoice – one place for projects, time, people and money.**

Alternatives for the hero headline:
- "Run your service business in one place."
- "Projects, timesheets, invoices, liquidity. Finally in one tool."

### Elevator pitch (≈ 50 words)

> ProjectForge is the open-source back office for project-driven companies. Plan projects and
> orders, let your team book time in seconds, handle leave, write e-invoices and keep an eye on
> forecast and liquidity – all in one web app that runs on your own server. Built and used daily
> by Micromata since 2001. Free, without limits.

### The four reasons (Vorteilsargumentation)

| # | Claim | Proof points | Answers the pain |
|---|-------|--------------|------------------|
| 1 | **One tool instead of a tool zoo** | Order book → task tree → timesheets → invoices → forecast & liquidity share the same data | "Our numbers live in five tools and three spreadsheets." |
| 2 | **Your data, your server** | Self-hosted (Docker or JAR), PostgreSQL, LDAP / Keycloak / Authentik, 2FA with WebAuthn security keys, audit history, GDPR privacy jobs | "We can't put salaries and customer data into a US cloud." |
| 3 | **Made for DACH/EU** | DATEV import/export, XRechnung & ZUGFeRD, PEPPOL validation, public holidays, leave accounts, German + English UI | "International tools don't know DATEV or e-invoicing duties." |
| 4 | **Open source, no limits, proven** | GPL community edition with all features, no user limits, no per-seat fees; 25 years in daily production use | "Per-user SaaS prices explode as we grow." |

### Differentiation (argumentativ, ohne Wettbewerbernamen)

- *Issue tracker + time plugin:* great for tickets, but orders, invoices, leave and liquidity are missing.
  → ProjectForge connects time with money.
- *Time-tracking/invoicing SaaS:* easy start, but data in the cloud, per-seat pricing, limited controlling.
  → Same convenience, self-hosted, no seat price.
- *Full ERP suite:* powerful but heavy to introduce and to run.
  → Focused on service companies, ready to run in minutes.
- *HR tools + spreadsheets:* leave in one tool, bookings in another, forecast in Excel.
  → Approved leave becomes a timesheet automatically; forecast is live.

### Objections & answers (→ FAQ / sections)

| Objection | Answer (text) |
|-----------|---------------|
| "Free – is that serious?" | "ProjectForge runs our own company. We develop it because we need it – and we share it. There's no 'enterprise edition' holding back features." |
| "Who keeps it running?" | "Docker image, automatic database migrations, a system dashboard that tells you about problems before your users do. Or let us host it for you." |
| "How long does the introduction take?" | "Up and running in minutes; your first projects and bookings in an afternoon. Need help with structures, imports or accounting? Ask us." |
| "Where is my data?" | "Wherever you want: on your server, in your data center, behind your firewall." |
| "Is it modern?" | "Brand-new interface since version 9 (2026): fast, responsive, dark mode, global search." |

---

## 2. Sitemap

```
Home
Features            (detail page, clusters 1–9)
Download            (Docker, JAR, GitHub Releases)
Docs ▾              Quick start · Installation · Admin guide · User guide · Development · Technologies · Migration
Changelog
FAQ
Services            (new, short page – or section on Home; on request)
About               (legal, trademark, no team table)
GitHub (icon)
```

**Änderungen ggü. heute:** Team-Sektion entfällt. „Services“ kommt neu hinzu, entweder als Abschnitt
auf der Startseite oder als eigene kurze Seite. Mein Vorschlag ist eine eigene Seite, die die
Startseite verlinkt. Sourceforge verschwindet, Kontakt läuft über GitHub Issues. Die Videos von
2020/21 zeigen die alte Oberfläche: raus bzw. erst wieder mit neuen Videos.

---

## 3. Home page – structure and texts

### 3.1 Hero

- **Headline:** From order to invoice – one place for projects, time, people and money.
- **Subline:** The open-source back office for project-driven companies. Self-hosted, made for DACH/EU, free without limits.
- **Primary CTA:** `Download` → /download
- **Secondary CTA:** `See all features` → /features
- **Trust line under CTAs:** Open source (GPL) · In daily use since 2001 · English & German
- **Visual:** Screenshot S1 (dashboard/menu, light) – optional S14 dark as variant.

### 3.2 Three value boxes (replaces Scalable/Easy/Best of two worlds)

1. **Everything connected**
   Orders, tasks, timesheets, invoices and liquidity share the same data. No more copying numbers between tools.
2. **Your server, your rules**
   Run it on your own infrastructure with LDAP or single sign-on, two-factor login and a full audit trail.
3. **Free. Really.**
   All features, any number of users, no subscription. Professional services if you want them.

### 3.3 "How it fits together" (Ablauf, gut als Diagramm/Stepper)

> **Win the order** → **Plan the project** → **Book the time** → **Send the invoice** → **Know your numbers**

| Step | Text |
|------|------|
| Win the order | Track offers and orders with budgets, contact persons and payment schedules. |
| Plan the project | Break it down in a task tree, assign people, set who may book where. |
| Book the time | Your team books in seconds – from templates, the calendar or several days at once. |
| Send the invoice | Turn bookable positions into invoices, export XRechnung or ZUGFeRD. |
| Know your numbers | Forecast, contribution margins, invoicing quota and liquidity – always up to date. |

### 3.4 Feature highlights (6 tiles, each linking to the cluster on /features)

Projects & Orders · Time Tracking · People & Leave · Finance & E-Invoicing · Controlling & Liquidity · Collaboration
(Texts = cluster "benefit line" from section 4, one screenshot per tile.)

### 3.5 "Made for DACH and the EU"

> **Speaks DATEV. Knows XRechnung.**
> Import your DATEV accounting records and incoming invoices, export salaries, create e-invoices
> in XRechnung and ZUGFeRD and validate them for PEPPOL. Public holidays, leave accounts and a
> German/English interface are built in. And because it runs on your own server, GDPR is a
> matter of your own policies – not of someone else's cloud.

### 3.6 Security & operations (für IT-Betrieb)

> **Sleep well, admins.**
> - Single sign-on via Keycloak or Authentik, or LDAP – as client or as master.
> - Two-factor login with authenticator apps, WebAuthn security keys, mail or SMS.
> - Fine-grained rights down to single tasks; every change is kept in the history.
> - Docker image, PostgreSQL, automatic migrations.
> - A system dashboard collects problems and mails you a digest – before your users notice.

### 3.7 Proof / numbers

> **Battle-tested since 2001.**
> ProjectForge started as Micromata's own tool and still runs our company every day –
> from timesheets to the yearly forecast. [TODO: Kennzahlen, z. B. „x users“, „y million timesheets“,
> „used by n companies“ – nur was belegbar und freigegeben ist.]

Optional stat row: `25 years in production` · `9 major versions` · `2 languages` · `0 € license fees`

### 3.8 Professional services (on request)

> **Want a hand? We're here.**
> We built ProjectForge, so we know it best. On request we host your instance, help you set up
> structures, rights and accounting, import your existing data, connect LDAP or single sign-on,
> or build extensions you need.
> **CTA:** `Get in touch` → GitHub issue (Template „Services request“, siehe 6.)

### 3.9 FAQ (Auswahl für die Startseite)
How much does it cost? · Can you host it for us? · Where is my data stored? · Do you provide support? · How do I report bugs or wishes?

### 3.10 Closing CTA

> **Ready in five minutes.**
> Grab the Docker image, start it, follow the setup wizard. That's it.
> `Download` · `Quick start guide`

---

## 4. Feature clusters (→ /features, sorted by benefit for the target group)

Format per cluster: heading · benefit line · feature list (title – one sentence) · screenshot ref.

### 4.1 Projects & Orders
*Benefit:* Know every order, every budget and every project's status.
- **Order book** – Offers and orders with positions, budgets, payment schedules and any number of contact persons; flags when something is ready to be invoiced.
- **Task tree** – Structure customers, projects and phases in an unlimited hierarchy and control who sees and books what.
- **Project wizard** – Create a new project with tasks, groups and rights in one go.
- **Consumption bars** – See at a glance which tasks are on budget and which are running hot.
- **Resource planning** – Plan your people across projects week by week.
- **Gantt charts** – Show your plan to your team and your customers.
*Screens:* S5 (task tree), S6 (order book).

### 4.2 Time Tracking
*Benefit:* Booking time takes seconds, not a Friday afternoon.
- **Timesheets with templates** – Book recurring work from favorites in a click.
- **Book from the calendar** – Drag your day together, or book several days at once (holidays and weekends skipped).
- **No double bookings** – Overlaps on the same cost unit are prevented automatically.
- **Monthly report & personal statistics** – For employees, project leads and payroll.
- **Bulk edit** – Fix many bookings at once.
*Screens:* S3 (timesheet list), S4 (multi-day booking).

### 4.3 People & Leave
*Benefit:* Leave, employees and skills – without an extra HR tool.
- **Leave requests** – Request, approve, get notified; conflicts in the team are detected.
- **Leave becomes a timesheet** – Approved vacation is booked automatically every night.
- **Leave accounts** – Entitlements, carry-overs and remaining days always correct.
- **Employees & salaries** – Employee data and salaries, importable from Excel, exportable to DATEV.
- **Skill matrix** – Who knows what? Your team rates its skills and interests.
*Screens:* S10 (leave request), S2 (calendar with leave).

### 4.4 Finance & E-Invoicing
*Benefit:* From booked hours to a compliant invoice.
- **Outgoing invoices** – Positions linked to orders and projects, Word templates, PDF output, cancellation invoices.
- **E-invoicing** – XRechnung and ZUGFeRD export, PEPPOL validation, and an e-invoice checker for invoices you receive.
- **Incoming invoices** – Capture or import them, assign them to cost units.
- **Cost accounting** – Cost units (cost 1/cost 2) and accounts for clean project and customer results.
- **Currencies** – Foreign-currency amounts with online exchange rates.
*Screens:* S8 (invoice incl. e-invoice export).

### 4.5 Controlling & Liquidity
*Benefit:* Know today what next quarter looks like.
- **Forecast** – Expected revenue by month, compared with previous years, as Excel export.
- **Key figures & charts** – Contribution margins, order statistics, invoicing quota on rearrangeable chart tiles.
- **Liquidity planning** – Expected payments, recurring series and probabilities in one forecast.
- **Bank accounts** – Import account statements and see balances over time.
- **DATEV** – Import accounting records and incoming invoices; business assessment (BWA) reports.
*Screens:* S7 (forecast charts), S9 (liquidity).

### 4.6 Collaboration
*Benefit:* The everyday stuff your team needs anyway.
- **Team calendars** – Shared calendars with holidays and birthdays; subscribe on any device via ICS, import external calendars.
- **Address book** – Shared addresses, synced to phones via CardDAV, vCard export.
- **Data transfer** – Share files with colleagues and customers, protected by password and expiry, with audit log.
- **Word templates** – Generate contracts and serial letters from Word templates (Merlin), as Word or PDF.
- **Polls** – Quick team polls.
- **Office organization** – Contracts, inbox/outbox, visitor book, books, software licenses and hardware.
*Screens:* S11 (data transfer).

### 4.7 Easy to use
*Benefit:* A tool people actually like to open.
- **New interface** – Fast, responsive, works on tablet and phone; light and dark theme.
- **Global search** – Find any customer, project, invoice or address in seconds.
- **Your menu** – Favorites bar, recently used pages, customizable menu.
- **Excel everywhere** – Export every list.
- **History** – Every change with who and when, plus comments.
- **Notifications** – In-app and mail notifications on rules you define. *(experimental – ggf. weglassen)*
*Screens:* S1 (menu/dashboard), S14 (dark mode).

### 4.8 Security & Integration
*Benefit:* Fits into your IT, not the other way round.
- **Single sign-on** – Keycloak or Authentik; or LDAP as client or master (incl. POSIX/Samba).
- **Two-factor login** – Authenticator apps, WebAuthn security keys, mail or SMS codes.
- **Fine-grained rights** – Groups, roles and access per task.
- **External gateway** – Offer calendars, contacts and file sharing to the outside without exposing the main system.
- **Integrations** – REST tokens, phone (Sipgate direct call, SMS), JIRA links.
*Screens:* S12 (2FA).

### 4.9 Operations & Extensibility
*Benefit:* Easy to run, easy to extend.
- **Docker or JAR** – Runs anywhere Java runs; PostgreSQL in production.
- **System dashboard** – Problems, statistics, scheduler and caches in one place; error digest by mail.
- **Log viewer** – Live log and full log-file search with context, right in the browser.
- **Scripting** – Kotlin and Groovy scripts for your own reports and exports, even as buttons on pages.
- **Plugins** – Extend ProjectForge with your own modules.
- **Open source** – Kotlin, Spring Boot, Next.js – read it, change it, contribute.
*Screens:* S13 (system dashboard).

### features.adoc – neue Gliederung
Intro (2 Sätze + Link Download) → 4.1 … 4.9 als Abschnitte (statt Tabellen: Liste „Titel – Satz“, je
1 Screenshot) → „Technical requirements“ (Browser, Docker/JAR, Java, PostgreSQL) → CTA.
Weglassen: Library als Hauptfeature (nur in 4.6 erwähnt), „Samba next version“, „demo system on this web site“.

---

## 5. Screenshot list

**Regeln:** 1600×1000 px, Browser auf 1440 px Breite, Sprache Englisch, Light Theme (außer S14).
Es sind keine echten Kunden-, Personen- oder Finanzdaten zu sehen. Benutzer im Testsystem:
„Example Consulting GmbH“ und fiktive Namen.

**Testsystem:** `bin/pfDev.sh setup <n>`, danach `bin/pfDev.sh run <n>`. Das ergibt einen
HSQLDB-Slot mit TEST_DATA. Login: admin, die Accounts stehen in `~/ProjectForge-<n>/testAccounts.txt`.
Fehlende Daten (Aufträge, Rechnungen, Liquidität, Forecast über mehrere Jahre) muss ich vorher
ergänzen. Mein Vorschlag: die Seed-Daten einmalig per Skript über die REST-API anlegen, damit
sich die Screenshots für spätere Releases wiederholen lassen.

| # | Motif | Purpose | Page | Source | Data needed |
|---|-------|---------|------|--------|-------------|
| S1 | Main menu dropdown + favorites bar | "modern, easy" (hero) | any page, menu open | Live (own user, menu only) | – |
| S2 | Team calendar, week view with leave & holidays | Collaboration / leave | Calendar | Test | 3–4 users, leave, events |
| S3 | Timesheet list with period filter | Time tracking | Time sheets | Test | ~30 bookings |
| S4 | Multi-day booking dialog | Time tracking | Calendar → book days | Test | – |
| S5 | Task tree with consumption bars | Projects | Structure tree | Test | tree with budgets + bookings |
| S6 | Order book list / order edit | Orders | Order book | Test | 8–10 orders, statuses |
| S7 | Statistics: forecast & contribution margin charts | Controlling (key visual!) | Statistics | Test | orders/invoices over 2–3 years |
| S8 | Invoice edit with e-invoice export | Finance | Invoices | Test | 5–10 invoices |
| S9 | Liquidity forecast | Controlling | Liquidity | Test | series + invoices |
| S10 | Leave request with approval / leave account | People | Application for leave | Test | entitlements |
| S11 | Data transfer area | Collaboration | Data transfer | Test | a few files |
| S12 | Two-factor setup | Security | Two-factor authentication | Test | – |
| S13 | System dashboard | Operations | System dashboard | Live (prüfen: keine Hostnamen/Namen) | – |
| S14 | Same as S7 or S3 in dark mode | "modern" | – | Test | – |
| S15 | Skill matrix statistics *(optional)* | People | Skill matrix | Test | ratings |

Die vorhandenen Screenshots von 2022 (Skillmatrix, StructureTree, Calendar, Datatransfer) und
`admin-plugins.png` werden auf der Startseite ersetzt.

---

## 6. FAQ – new set

**Startseite (`faq-main`):**
1. *How much does it cost?* – Nothing. The community edition is open source (GPL) with all features and no user limits. You only pay if you want our services (hosting, support, introduction) – on request.
2. *Can you host ProjectForge for us?* – Yes, on request. Open a GitHub issue or contact us … [TODO: Kontaktweg für vertrauliche Anfragen? Ein öffentliches Issue ist für Firmenanfragen evtl. unpassend → E-Mail zusätzlich?]
3. *Where is my data stored?* – On your server, in your database. [TODO: bestätigen, dass ProjectForge keine Daten nach außen sendet (z. B. Update-Check, Wechselkurse holen ist nur Abruf) – dann „ProjectForge does not phone home.“]
4. *Do you provide support?* – Community support via GitHub issues; professional support on request.
5. *How do I report bugs or wishes?* – GitHub issues (bestehende FAQ 30, aktualisiert).

**Neu:** languages (EN/DE); moving from spreadsheets/other tools (Excel imports: salaries, incoming invoices, DATEV, bank statements); system requirements; mobile use.

**Löschen bzw. in den Admin Guide verschieben:**
- administration-40 (Hypersonic) und administration-50 (SHA-Reset): löschen
- technical-10 (Internet Explorer): ersetzen durch „modern browsers“
- general-30, -50, -60, -90 und technical-30: alte Bugs bzw. Workarounds, löschen
- general-20 (config.xml): prüfen

Die übrigen fachlichen FAQs bleiben stehen und werden in Schritt 2 mit dem User Guide geprüft.

---

## 7. Open questions for you

1. **Kontakt für Services-Anfragen.** Reicht ein öffentliches GitHub Issue, oder kommt zusätzlich eine E-Mail-Adresse auf die Seite?
2. **Kennzahlen für 3.7.** Welche Zahlen dürfen genannt werden (Nutzer, Firmen, Buchungen)?
3. **Services.** Eigene Seite oder nur ein Abschnitt auf der Startseite?
4. **Benachrichtigungen.** Das Feature ist noch experimentell. Schon bewerben?
5. **Videos.** Die alten Videos entfernen, oder bleiben sie, bis es neue gibt?
