# PF-CVR Evaluation - Dependency-Track as detection engine for the CVE-Radar

| Field | Value |
|---|---|
| COMP-ID | PF-CVR |
| DOC-ID | PF-CVR-DT |
| Status | Draft 0.2 (decision paper; spike done, result: go, see the migration plan, section 8) |
| Owner | Kai Reinhard |
| Date | 2026-11-06 |
| Related | [PF-CVR_CVE-Radar_Vulnerability_Management.md](PF-CVR_CVE-Radar_Vulnerability_Management.md) (spec, requirement IDs below refer to it), [PF-CHAT_Chat_Discussion.md](PF-CHAT_Chat_Discussion.md), [PF-CVR_Dependency-Track_Migration.md](PF-CVR_Dependency-Track_Migration.md) (migration plan) |

---

## 1. Question and objective

The CVE-Radar (`plugins/org.projectforge.plugins.cveradar`) imports vulnerabilities itself (NVD, OSV, EUVD),
matches them against its own inventory and organises triage, feedback and closure in ProjectForge (PF). The
question is whether parts of it should be retired in favour of [OWASP Dependency-Track](https://dependencytrack.org/)
(DT), which is integrated instead.

The target picture is an **asset management for software and infrastructure** (areas, systems, instances,
components, devices) with uploads, ratings, notifications, escalations and discussions. Where DT does not cover a
part, PF takes it over – in particular chat and notifications.

This document answers: *what does DT already do well, what is better kept in PF, and what would the
combination look like?* The statements about DT taken from the documentation were checked in the spike (section 10);
its results are in the migration plan, section 8.

## 2. Starting point

### 2.1 CVE-Radar today

| Area | Spec | State |
|---|---|---|
| Import NVD (API 2.0, CVSS ≥ 7, KEV), OSV (per ecosystem), EUVD (EPSS, exploitation) | IMP | implemented (`sync/*`, `CveRadarJobs`); EPSS only for vulnerabilities the EUVD knows, not yet used for the priority suggestion (`CvrPriorityRule` uses CVSS and KEV) |
| Inventory: area tree → system → instance → component → technology, baseline + deviations, confidentiality, criticality | INV-01..12 | implemented (`CvrInventoryService`) |
| Technology catalogue (ecosystem → product → library) | INV-28..32 | implemented (`CvrCatalogService`) |
| Uploads: CycloneDX/SPDX JSON, lockfiles, `mvn`/`gradle` lists, jar listings, pip, Go, cargo, composer, deb/apk/rpm, zip | INV-14, 26, 27, 33 | implemented (`LibraryListParser`, `LibraryArchive`) |
| Matching CPE + purl, version ranges, "possibly affected" | MAT | implemented (`CvrMatchingService`, `VersionComparator`) |
| Case per CVE/incident, P1–P4, multiple ratings, triage, measures, AI prompt export/import | PRIO, TRI, ACT, AI | implemented (`CvrCaseFileService`, `CvrRatingDO`) |
| Team checks, employee confirmations, closure overview | FB, CLS | implemented |
| Devices and agent checks from Graylog | EXT-04..07 | implemented (`GraylogSyncService`) |
| Notifications (3 rule types) | NOT | implemented (`CvrFindingsRuleHandler` etc.) |
| Discussion per case and team check | DISC | implemented (`CvrCaseChatHost`, `CvrTeamCheckChatHost`) |
| Need-to-know per confidentiality level | VIS, SEC | implemented (`CveRadarAccess`) |
| Escalation | ESC | specified, only in the UI mock (`lib/cve-radar-mock/escalation-rule.ts`) |

Size: about 14.7k LOC backend, 5k LOC tests, 20k LOC Next.js (thereof about 4k mock), 0.6k LOC Flyway.

### 2.2 Dependency-Track

Open source (Apache 2.0), current major version 5 with PostgreSQL as the only external service, container images,
REST API v1 (projects, BOMs, findings, analysis) and API v2 (so far administration: service accounts, API keys, data
sources, secrets), SSO via OIDC, LDAP or local users.

- Consumes and produces CycloneDX SBOMs and CycloneDX VEX.
- Vulnerability sources: NVD, GitHub Advisories, OSV, Sonatype OSS Index, Snyk, Trivy, VulnDB, private sources;
  EPSS.
- Portfolio of projects (name, version, classifier, tags, parent project); components per project.
- Policy engine (licence, severity, coordinates, purl, CPE, hashes, age, version distance), global or per project.
- Audit per finding (project × component × vulnerability): analysis state, justification, vendor response,
  comments, suppression.
- Notifications: webhook, mail, Slack, Teams, Mattermost, Webex, Jira, Kafka; groups such as
  `NEW_VULNERABILITY`, `NEW_VULNERABLE_DEPENDENCY`, `BOM_PROCESSED`, `PROJECT_AUDIT_CHANGE`, `POLICY_VIOLATION`,
  also as scheduled summaries.
- Access: users (local, LDAP, OIDC) and service accounts (`svc:` prefix) in teams; fine-grained permissions
  (create/read/update/delete per area); portfolio access control per team, inherited down the project hierarchy.
- Machine access: team API keys, service account API keys, workload identity federation (GitHub Actions, GitLab CI,
  Kubernetes, SPIRE) without stored secrets.

### 2.3 Commercial platforms (Checkmarx One)

Checkmarx One stands here for the commercial AppSec platforms; it answers a different question than the CVE-Radar.

- **Scope:** the own source code (SAST, SCA, IaC, secrets, containers, API, DAST, malicious packages). The CVE-Radar
  asks which *operated* instance is affected, who has to act and who has rated it, also for technologies without
  code or build (products, appliances, operating systems via CPE).
- **Overlap with DT:** only the SCA. Checkmarx adds reachability, its own research and malicious-package data and
  the integration into IDE and pull requests; DT adds CPE matching and runs internally.
- **Not covered:** inventory with baseline and deviations, cases, team checks, risk acceptance by the SO,
  need-to-know, PF login; they would stay in PF anyway.
- **Operation and costs:** mainly SaaS, so dependency lists or source code leave the house (EU region: to be asked);
  licence per contributing developer (both to be asked of the vendor). DT is open source and internal (7.5).
- **Combination:** a scanner of the own code can upload its CycloneDX SBOM to PF (6.3), which forwards it to DT like
  any other BOM; SAST findings stay in the scanner.

## 3. Coverage per requirement group

Legend: **DT** – DT covers it as well or better; **PF** – PF covers it, DT doesn't or only partly;
**both** – split between the two.

| Group | DT | PF | Verdict |
|---|---|---|---|
| IMP-01..13 vulnerability import (NVD, OSV, KEV, EPSS) | More sources, maintained by the community, mirroring with rate limits built in. KEV flag from the CISA and ENISA lists, EPSS from FIRST for every CVE, both mirrored daily. | Own importers with resume, history and severity filter; EPSS only via the EUVD, without effect on PRIO-01 or MAT-11 yet. | **DT** |
| IMP-14 manual vulnerability / incident without CVE | Internal vulnerabilities can be created; incidents without software reference don't fit. | Cases and `INC-yyyy-n` incidents. | **PF** (incidents), DT for internal advisories |
| IMP-16 EUVD | No vulnerability source; only ENISA's KEV list. | Implemented. | **PF** as optional enrichment |
| INV-01..12 hierarchy, baseline/deviations, criticality, confidentiality, stage, site, exposure | Projects with parent and tags, no inheritance, no confidentiality. | Implemented. | **PF** |
| INV-14/15 SBOM upload, CI upload with token | Core function; API keys per team or service account, workload identity federation. Keys not scopable to one project. | Upload into PF; CI token per system specified, not implemented. | **both**: see section 6 |
| INV-26/27/33 lockfiles, build lists, jar listings | CycloneDX only; an SPDX upload is refused. | `LibraryListParser`. | **PF** as converter to CycloneDX |
| INV-17, EXT-04..07 clients, devices, agent checks | No device or agent concept. | Graylog connector. | **PF** |
| INV-28..32 technology catalogue | Not needed: precise purl matching, repository metadata ("outdated"). | Curated catalogue for estimates. | **both**: DT matches; the catalogue stays as CPE source of the BOM export and for MAT-11 |
| INV-34 own artifacts never looked up externally | "Internal components" are excluded from external analysers; the external analysers stay off anyway, the lookup of repository metadata (outdated versions) is to be checked in phase 2. | Name patterns. | **DT**, configured |
| MAT-01..03, 06, 12 matching, possibly affected | Mature CPE/purl matching with version ranges. | Own matching. | **DT** |
| MAT-04/05, 13 finding status workflow, reasons, reopen | Analysis states, justification, vendor response, audit trail. No approval step for "risk accepted". | Workflow with SO approval. | **both**: PF leads, DT mirrors (7.3) |
| MAT-08 VEX export | Built in. | – | **DT** |
| MAT-10/11 grouping per system, check requests | No portfolio-wide case; per project only. | Case and team checks. | **PF** |
| PRIO, TRI case per CVE, P1–P4 with deadlines, assignment, multiple ratings, mass actions | Severity per finding, no internal priority, no SLA, no assignment (the analysis has state, justification, response, details and suppression only). | Implemented (deadlines partly). | **PF** |
| ESC escalation with acknowledgement | Only notifications, no acknowledgement or escalation chain. | `NotificationService` with acknowledgement; ESC still to be built. | **PF** |
| AI prompt export/import | – | Implemented. | **PF** |
| ACT, FB, CLS, QA measures, client advisories, feedback, closure | – | Implemented. | **PF** |
| NOT notifications to persons and groups | To channels (webhook, mail, chat tools), per project/tag, without dedup or acknowledgement per person. | Rules, dedup, banner, badge, mail, SMS. | **PF**; DT notifies PF only (webhook) |
| DISC discussion | Comments per finding only. | Chat with channels, moderation, files. | **PF** |
| RPT dashboards, key figures | Portfolio and project metrics, trends, risk score. | Dashboard, my view, MTTR per priority. | **both**: DT for component metrics, PF for SLA and organisation |
| VIS, SEC need-to-know, PF groups | Teams, fine-grained permissions, portfolio access control per team; no confidentiality levels. | Confidentiality levels per system, responsible groups. | **PF**; see section 5 |
| Policies (licence, outdated versions) | Built in. | – | **DT** (new capability) |
| NFR-05 proxy, offline operation | Proxy configurable; mirrors need internet. | Proxy per integration client. | both |

**Short answer:** DT is better at *detection* – vulnerability data, matching, SBOM handling, policies, VEX. PF is
better at *assets in the organisation and the work on them* – infrastructure inventory with need-to-know, the case
per CVE across the portfolio, priorities, deadlines, escalation, feedback, closure, chat and personal
notifications.

## 4. Feature comparison: what DT can do and where PF is the better place

Only features relevant for the target picture. "Where" names the system that should own the feature.

| DT feature | What DT offers | Where | Why |
|---|---|---|---|
| Vulnerability mirror and analysers | NVD, GHSA, OSV, OSS Index, Snyk, Trivy, VulnDB, EPSS, maintained upstream | **DT** | Maintaining own importers (rate limits, formats, outages) is effort without benefit for PF. PF keeps EUVD only if wanted. |
| Matching CPE/purl | Version ranges per ecosystem, re-analysis daily and on every upload | **DT** | More mature than `CvrMatchingService`; PF only for systems without SBOM if DT's CPE matching is not enough (spike). |
| Internal vulnerabilities | Own advisories for components | **DT** for software, **PF** for incidents | A leaked certificate or a misconfiguration is no component finding; PF incidents cover them with the same workflow. |
| Projects, versions, hierarchy, tags | Flat inventory of SBOMs, parent projects | **PF** (DT as mirror) | PF has areas, stage, site, criticality, confidentiality, responsible groups and baseline/deviations; DT has no inheritance, every version is a separate SBOM. |
| SBOM ingestion, dependency graph | CycloneDX incl. transitive dependencies, gzip/zstd, processing token | **DT** | Core competence. PF converts what is no CycloneDX (lockfiles, jar listings) and forwards it. |
| Component search across the portfolio | Search by name, purl, CPE, hash | **PF** | "Where does Log4j < 2.17 run?" must respect need-to-know (VIS) and show areas and stages; DT shows every project the team may see. |
| Outdated components (repository metadata) | Latest version per component, version distance | **DT** | Free benefit; PF shows it at most as a hint. |
| Policy engine | Licences, severity, age, version distance, coordinates | **DT** | New capability (licence compliance). Violations reach PF as notification, not as a case. |
| Audit per finding | Analysis state, justification, vendor response, comments, suppression | **PF** leads, DT mirrors | DT works per project × component. PF needs one case per CVE across all instances, several ratings, binding priority with reason, SO approval for risk acceptance, deadlines – and one place where the team works. |
| VEX | CycloneDX VEX import and export | **DT** | Fed by PF's decisions (7.3); customers or auditors get VEX from DT. |
| Notifications | Rules per group, scope, project or tag to webhook, mail, Teams, Slack, Jira, Kafka | **PF** | DT sends to fixed channels and addresses; PF notifies persons via their responsible groups, with dedup, acknowledgement, banner, badge, digest and texts that name systems only to those allowed to see them (DISC-05). DT only calls PF's webhook. |
| Jira integration | Ticket per notification | **PF** (none) | Cases, team checks and chat are in PF; a second ticket system splits the work. |
| Comments | Comment trail per finding | **PF** | Chat with channels (internal / public), moderation, files, mentions. |
| Metrics, risk score, trends | Per project and portfolio | **both** | DT for component and risk trends, PF for organisational key figures (MTTR per priority, deadline compliance, per group, RPT-04). |
| UI | Full web UI for the portfolio | **PF** for everybody, DT for the admins only | Employees, responsible groups, the ST and the SOs get one tool with PF login, German texts and their own view. What they need from DT (components of an instance, policy violations, risk trend) PF shows read-only from the DT API, so the DT frontend needs no access from outside (7.5). |
| Escalation, feedback, closure, AI export, Q&A | – | **PF** | Not in DT. |

## 5. Roles and permissions

### 5.1 Comparison

| Aspect | CVE-Radar (PF) | Dependency-Track |
|---|---|---|
| Identities | PF users and groups, also for every employee | Local, LDAP, OIDC users and service accounts; teams via OIDC/LDAP group synchronisation |
| Roles | READER < SECURITY_TEAM < SECURITY_OFFICER from configured PF groups; PF admin grants nothing | No roles; permissions per team or user (`VIEW_PORTFOLIO`, `BOM_UPLOAD`, `VULNERABILITY_ANALYSIS_UPDATE`, `POLICY_MANAGEMENT`, `ACCESS_MANAGEMENT`, …, CRUD variants) |
| Object level | Responsible group per area/system, inherited downwards; only it and the SO write a system (SEC-02) | Portfolio access control: team ↔ project, inherited to child projects; `PORTFOLIO_ACCESS_CONTROL_BYPASS` sees everything |
| Confidentiality | Internal / confidential / strictly confidential with per-person grants, optionally time-limited and historised (VIS-01..03) | None; visibility is only team ↔ project |
| Employees | See released advisories, confirm, answer team checks | No concept |
| Audit of access | Grants historised in PF | Changes of teams and permissions in DT |

PF's model is about *who in the organisation may know which system is vulnerable*; DT's is about *which team may
work on which project*. Mapping PF's levels and grants onto DT teams and ACLs would create a second rights model
that drifts from the first.

### 5.2 Recommendation

- **Only PF has the full model.** Everybody works in PF, including the security team and the SOs; only the DT
  admins have DT accounts (local or OIDC):

  | DT team / account | Members | Permissions |
  |---|---|---|
  | `svc:projectforge` (service account) | PF integration | `VIEW_PORTFOLIO`, `PORTFOLIO_MANAGEMENT_CREATE/UPDATE/DELETE`, `BOM_UPLOAD`, `VIEW_VULNERABILITY`, `VULNERABILITY_ANALYSIS_UPDATE`, `VIEW_POLICY_VIOLATION`, `PORTFOLIO_ACCESS_CONTROL_BYPASS` |
  | DT admins | Operations | `ACCESS_MANAGEMENT`, `SYSTEM_CONFIGURATION`, `SECRET_MANAGEMENT`, `POLICY_MANAGEMENT`, `VULNERABILITY_MANAGEMENT` |

- **Policies and internal advisories** are maintained by the DT admins on behalf of the ST, until PF offers them
  itself (only if needed).

- **Strictly confidential systems:** DT sees them with neutral project names (e.g. `pf-<instance id>`) and without
  revealing tags, so the DT team learns the components but not which system runs them. Alternatively portfolio
  access control with one team for strictly confidential projects, with PF granting membership – more effort,
  decide after the spike.
- **No `ACCESS_MANAGEMENT` for PF's service account**: PF does not manage DT users or teams.
- Every PF endpoint of the integration keeps `@AccessChecked` and the CVE-Radar roles; the webhook endpoint
  authenticates DT, not a user, and changes nothing a user could not see afterwards.

## 6. Tokens for automatic SBOM uploads (CI/CD, build)

### 6.1 What DT offers

- `POST /api/v1/bom` (multipart) or `PUT` (base64 JSON) with project UUID or `projectName` + `projectVersion`,
  `autoCreate`, `parentName`/`parentUUID`, `projectTags`, `isLatest`; answers with a token, status via
  `/api/v1/event/token/{token}` (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`). Ready-made: GitHub Action
  `DependencyTrack/gh-upload-sbom`, Jenkins plugin.
- Permissions: `BOM_UPLOAD`, for the first upload of a new project also `PROJECT_CREATION_UPLOAD`.
- Credentials: team API key, service account API key, or workload identity federation – the CI job exchanges its
  short-lived platform token (GitHub Actions, GitLab CI, Kubernetes, SPIRE) for a session of up to 24 h; bindings
  restrict it by subject and claims (repository, branch) with CEL conditions. Keys of service accounts expire (30 days
  by default, at most `dt.api-key.max-lifetime-days`, 366 by default), are shown only once at creation and are listed
  with public id and last use.
- **Limit:** API keys can't be restricted to one project; restriction is only via the team's portfolio access
  control. A key of a team without ACL, or with `PROJECT_CREATION_UPLOAD`, may overwrite every BOM the team can
  reach – a compromised pipeline could make a vulnerable system look clean by uploading an empty SBOM.

To use direct uploads safely, every system (or repository) needs its own DT team with an ACL on its project, a
service account and a binding or key – a second inventory of rights in DT.

### 6.2 What PF has

- INV-15 (SBOM via authenticated REST endpoint, token per system naming the target) is specified, not implemented.
- The existing `REST_CLIENT` user token (`UserTokenType`, `RestUserFilter`) is personal and carries all rights of
  its user: unsuitable for pipelines.

### 6.3 Recommendation: pipelines upload to PF

- **Upload token per target**, issued in PF by the responsible group or the ST for a system baseline or one
  instance (INV-15): random secret shown once, stored as a hash, optional expiry, revocable, last use shown,
  issue and revocation historised. The token *names* the target – the request can't choose another one
  (ownership from the database, not from the request).
- **Endpoint** `/rsPublic/cveradar/sbom` (outside `RestUserFilter`), token in a header, constant-time comparison,
  `LoginProtection` throttling, size limits and `LibraryArchive` rules (SEC-05). Accepts CycloneDX, SPDX and the
  formats of `LibraryListParser`, so lockfile-only projects work too.
- PF records origin `CI` and the date (INV-16, stale list reminders keep working), computes the effective list and
  forwards it to DT with `svc:projectforge`. Pipelines never get DT credentials; DT needs no team per system.
- **Workload identity later:** PF could accept GitHub/GitLab OIDC tokens with a binding per target (issuer,
  repository, branch) instead of a static token – same idea as DT's federation, only if teams ask for it.
- Ready-made actions: a few lines of `curl` in the docs (user guide), no own plugin.

Trade-off: PF is in the upload path (a failed upload is retried by the next build; DT keeps the last BOM), and
token management has to be built (about 0.5–1k LOC incl. UI). In return there is one rights model, one inventory
and one place where teams see their uploads.

## 7. Recommended target architecture

**DT detects, PF owns the assets and the work.** DT is operated as a backend service; all users, the security team
and the SOs included, stay in PF.

```
 CI pipeline ──CycloneDX──┐
                          ▼
 PF upload (lockfile, ──► PF converter ──CycloneDX──► Dependency-Track ──► NVD, GHSA, OSV, OSS Index, EPSS …
 jar list, SBOM)                                         │      ▲
                                                         │      │ analysis (status, justification)
                          webhook + reconciliation pull  ▼      │
 PF: inventory, cases, ratings, P1–P4, escalation, team checks, chat, notifications, devices (Graylog)
```

### 7.1 Mapping of the inventory

- One DT project per PF instance that has software components; the PF system is the parent project.
- Tags carry area code, stage, criticality and internet exposure, so DT policies and notifications can use them.
- PF stores the DT project UUID on the instance (and the component UUID on the finding).
- PF computes the effective technology list of an instance (baseline + deviations, INV-10) and uploads it as one
  CycloneDX BOM (`PUT /api/v1/bom`). Inheritance stays in PF; DT sees only the result.
- Pipelines upload to PF, not to DT (section 6): PF is the only writer of the DT projects, so the baseline and the
  deviations stay consistent and DT needs a single service account.
- Systems without SBOM (appliances, network devices, SaaS) remain in PF; their technologies go into the BOM with the
  CPE of the technology or the catalogue, which DT matches (spike). No own CPE matching is kept.

### 7.2 From DT to PF

- **Webhook** at `/rsPublic/cveradar/deptrack/notification`: shared secret in a header, compared in constant time,
  failed attempts throttled with `LoginProtection`, security logging – as `rest/pub/MessagingServiceRest.kt` and
  `gateway/sync/GatewaySyncController.kt` do. The payload is untrusted (SEC-05): size limit, schema validation.
  Groups: `NEW_VULNERABILITY`, `NEW_VULNERABLE_DEPENDENCY`, `BOM_PROCESSED`, `PROJECT_AUDIT_CHANGE`.
  Whether DT can send a custom header for outbound webhooks is checked in phase 3 (the configuration of the
  publishers is visible to DT admins only); otherwise a secret path segment.
- **Reconciliation job** (`SchedulerJobRunner`, as `CveRadarJobs`): pulls the findings per project
  (`/api/v1/finding/project/{uuid}`), since webhooks are not retried. The webhook only speeds things up; the pull is
  the source of truth for completeness.
- **HTTP client** as `sync/CveRadarHttp.kt` on `PooledHttpClients`, new client id `cveradar-deptrack` in
  `IntegrationConfig` (timeouts, size limit, proxy), header `X-Api-Key`, key only in the configuration (IMP-05).
- PF creates or updates `CvrFindingDO` (with DT reference) and groups the findings per vulnerability into the
  existing case; the existing rule `CVERADAR_FINDINGS` (`CvrFindingNotifier`) notifies as today.
- The EPSS of a DT finding goes into `CvrVulnerabilityDO.epss` too; it takes precedence over the EUVD value, so
  every CVE has one.

### 7.3 From PF to DT (triage)

PF is the leading system for the status; every change is written to DT as an analysis (`PUT /api/v1/analysis`), so
DT's VEX export, policy evaluation and metrics stay consistent.

| PF finding status (MAT-04) | DT analysis state | DT justification / response |
|---|---|---|
| New | NOT_SET | – |
| In review | IN_TRIAGE | – |
| Affected | EXPLOITABLE | – |
| Not affected | NOT_AFFECTED | justification from the PF reason |
| Discarded (false positive) | FALSE_POSITIVE | – |
| Fix planned | EXPLOITABLE | response UPDATE |
| Fixed | RESOLVED | – |
| Mitigated | NOT_AFFECTED | PROTECTED_BY_MITIGATING_CONTROL, response WORKAROUND_AVAILABLE |
| Risk accepted (SO approved) | EXPLOITABLE | response WILL_NOT_FIX, suppressed |

The table follows the statuses of MAT-04. Findings implement only New and Discarded so far; the rest of the workflow
lives in the case and the team checks, from which the migration plan derives the analysis (its section 4.4).

Analysis changes made directly in DT arrive via `PROJECT_AUDIT_CHANGE` and the pull; PF takes them over only for
findings not yet worked on in PF and otherwise reports them to the security team as a conflict.

### 7.4 Chat and notifications

Stay completely in PF (`ChatHost`, `NotificationService`). New gap: `ChatService.send` requires a logged-in user
with write access; DT events in the case chat ("new affected instance", "status set in DT") need a system message
API (sender "system", no mentions, same visibility rules as DISC-05).

### 7.5 Deployment

**DT runs as an internal service in Docker, without public access; PF is the only entry point from outside.**

- **Containers:** docker compose with the DT v5 API server, the frontend and PostgreSQL; image versions pinned, so
  updates are deliberate and come after a backup.
- **Database:** an own database and user in an existing PostgreSQL cluster (covered by its backup), or an own
  PostgreSQL container with its own backup.
- **Host:** DT is a JVM of its own (spike: about 0.5 GB in operation, limit 4 GB for the mirrors; database about
  2 GB); the first mirror took minutes on a developer machine (NVD and OSV about 3 minutes each, GitHub Advisories
  about 25 minutes). The PF host works only with enough headroom; a small VM of its own in the same internal network
  is cleaner, so load and updates of one don't affect the other.
- **Additional services:** none; version 5.2.1 needs only PostgreSQL (no Kafka/Redpanda, no separate analysers).
- **Telemetry:** DT has a task *Telemetry Submission* that is scheduled by default; to be switched off in the
  administration.

Who has to reach whom:

| From | To | Network |
|---|---|---|
| CI/CD pipelines | PF (`/rsPublic/cveradar/sbom`, token per target, section 6) | public, as PF today |
| PF | DT API (BOM upload, analysis write-back, reconciliation pull) | internal |
| DT | PF webhook (7.2) | internal |
| DT | NVD, GitHub, OSV, OSS Index, EPSS (outbound) | internet, via the outbound proxy if required |
| DT admins | DT frontend and API (the SPA calls the API from the browser) | SSH tunnel only |

**Certificates:** no public certificate for DT. If PF and DT share a Docker network or host (port bound to
localhost only), internal HTTP is sufficient. As soon as the traffic crosses a network (VM of its own), TLS with an
internal certificate is required, since API key and webhook secret travel with it: terminated by the existing
reverse proxy or via an internal CA whose certificate PF gets in its truststore.

**PF as proxy:**

- **For the API, yes:** this is the architecture of section 6. PF accepts the uploads, checks token and rights and
  forwards to DT as `svc:projectforge`; no DT key leaves PF.
- **For the DT frontend, no:** the SPA has its own login (OIDC/JWT), paths and Content Security Policy. A generic
  reverse proxy in PF would be extra code and attack surface and would mix the two rights models; under PF's
  origin, DT's scripts could also act in PF in the name of the logged-in user. Instead PF shows what the ST and
  the SOs need from DT read-only from the API (section 8), and only the admins reach the frontend, via SSH tunnel.
  If these views turn out not to be enough, the fallback is forward authentication: the existing reverse proxy
  routes `/dt/` and `/dt/api/` to DT and asks a PF endpoint (session and CVE-Radar role) before every request
  (`auth_request`); the frontend runs under a sub-path (`BASE_PATH`, `API_BASE_URL`), DT login stays its own.
- **PF without DT:** if DT is not reachable, an upload fails with a clear error and the next build retries (section 6),
  the cases stay usable, and the system status shows DT as unavailable (NFR-05).

## 8. Retire, keep, build

| Retire | Keep | Build |
|---|---|---|
| NVD and OSV import (`sync/Nvd*`, `sync/Osv*`), CPE/purl index (`CvrCpeMatchDO`, `CvrPurlMatchDO`), `CvrOsvAdvisoryDO` | Inventory (area … component), confidentiality, inheritance | DT client, configuration, health in the system status |
| `CvrMatchingService`, `VersionComparator` (unless CPE-only systems need it, 7.1) | Cases, ratings, priorities, measures, AI export/import | Webhook endpoint, reconciliation job, upload tokens and SBOM endpoint (section 6) |
| – (the technology catalogue stays: CPE source of the BOM export, framework exclusion of MAT-11) | Team checks, confirmations, closure overview, technology catalogue | BOM export per instance (CycloneDX writer) |
| Ad-hoc check, or routed to a temporary DT project | `LibraryListParser` as converter to CycloneDX | Analysis write-back, conflict handling; read-only DT views (components of an instance, policy violations, risk trend) as far as the ST and the SOs need them |
| Corresponding Next.js parts of the vulnerability view; their tables, dropped by a new Flyway migration (the CVE-Radar ships since 9.2.1) | Graylog devices, notification rules, chat hosts, EUVD (optional) | System messages in the chat; escalation (ESC, open anyway); EPSS in `CvrPriorityRule` (PRIO-01) and in the threshold of check requests (MAT-11), needed with or without DT |

Estimate for the retirement: about 5–7k LOC backend plus tests and parts of the vulnerability pages in Next.js. The new integration is roughly 2–3k LOC backend plus 0.5–1k LOC for upload tokens (section 6); the workflow parts are unchanged.

## 9. Risks and costs

- **Operation:** another service (DT container plus PostgreSQL, several GB RAM for the mirrors, see 7.5), updates,
  backup, monitoring. PF without DT must stay usable (plugin inactive or degraded, NFR-05).
- **Two databases:** findings exist in DT and PF; drift is avoided only with the UUID mapping, the reconciliation
  pull and a single writer per field (7.3).
- **API stability:** v5 offers API v1 and v2 side by side; findings and analysis exist only in v1, v2 covers the
  administration so far. PF uses v1 and watches the header `X-API-Deprecated`.
- **Data protection:** DT sends purls to external analysers (OSS Index, Snyk). Own artifacts must be configured as
  internal components (INV-34); analysers not wanted are disabled.
- **Identity:** only the DT admins get DT accounts; everybody else, the ST and the SOs included, works via PF.
- **Lost flexibility:** no own "possibly affected" logic for products without exact version (MAT-03): DT finds
  nothing for a purl without version and every vulnerability of the product for a CPE with version `*` (spike);
  PF marks such findings as possible.
- **Benefit beyond today:** policies (licences, outdated versions), VEX, broader sources, community maintenance of
  the matching – work that otherwise stays with PF.

## 10. Spike (before the decision)

DT v5 locally via docker compose, PF's own SBOM (CycloneDX Gradle and npm plugins) and one converted lockfile
uploaded. To check:

1. KEV flag, EPSS (for all CVEs? update cycle?) and EUVD in DT.
2. SPDX support of the BOM upload.
3. Matching of components with CPE only (appliance, network device) and without version.
4. Analysis per finding via API: states, justifications, suppression, audit trail; API v1 vs. v2.
5. Is there an analysis across projects (same component and vulnerability in many projects)?
6. Outbound webhook: custom headers or other authentication; payload of `NEW_VULNERABLE_DEPENDENCY`.
7. Internal components: are they really never sent to external analysers?
8. Resource use and duration of the initial mirror; proxy configuration; additional services needed by v5
   (Kafka/Redpanda, analysers) besides PostgreSQL.
9. API keys: expiry, hashing, rotation; behaviour of portfolio access control for service accounts.
10. Comparison of the findings with the CVE-Radar matching for the same inventory (false positives / negatives).

Results: migration plan, section 8.

## 11. Recommendation and next steps

Recommended: the hybrid of section 7, introduced in stages, so the CVE-Radar keeps working throughout.

1. **Spike** (section 10), decision based on its results.
2. **Switch without parallel operation:** the comparison with the own matching is done once in the spike; DT becomes
   the only matching source and the parts of section 8 are retired by new Flyway migrations (the CVE-Radar ships since
   9.2.1). Plan: [PF-CVR_Dependency-Track_Migration.md](PF-CVR_Dependency-Track_Migration.md).
3. **Build** escalation (ESC) and system chat messages on the PF side, independent of DT.
