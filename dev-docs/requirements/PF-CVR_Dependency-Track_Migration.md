# PF-CVR Migration plan - Dependency-Track as detection engine

| Field | Value |
|---|---|
| COMP-ID | PF-CVR |
| DOC-ID | PF-CVR-DT-MIG |
| Status | Draft 0.1 (plan; phase 0 decides whether phases 1–6 follow) |
| Owner | Kai Reinhard |
| Date | 2026-11-06 |
| Related | [PF-CVR_Dependency-Track_Evaluation.md](PF-CVR_Dependency-Track_Evaluation.md) (target architecture, sections referenced as *Eval x.y*), [PF-CVR_CVE-Radar_Vulnerability_Management.md](PF-CVR_CVE-Radar_Vulnerability_Management.md) (requirement IDs) |

---

## 1. Decisions

| # | Topic | Decision |
|---|---|---|
| D1 | Go/no-go | The spike (phase 0) is a real gate: the build starts only if the criteria of section 3 are met. |
| D2 | Switch | No parallel operation: the comparison with the own matching is done once in the spike, then the own matching is removed. |
| D5 | Database | The CVE-Radar ships since 9.2.1, so its Flyway scripts stay unchanged. New scripts `V<release>.<n>` of the release that ships the switch add the DT columns, rename the vulnerability fields and drop the tables of the own matching (postgresql and hsqldb where the SQL differs). Inventory, catalogue, cases, ratings, team checks and findings are kept; DT's reconciliation updates them. |
| D3 | Vulnerabilities without finding | Check requests (MAT-11) and vulnerabilities without SBOM components come from DT's vulnerability database. If its API can't deliver them per ecosystem and threshold (spike), a slim OSV import stays for MAT-11 only. |
| D4 | Names in DT | Projects are named in clear text (`<system> / <instance>`) with tags for area, stage, criticality and exposure; strictly confidential systems and instances are named `pf-<id>` and get no tags. |
| D6 | DT frontend | Not reachable from outside and not proxied by PF; only the DT admins use it, via SSH tunnel. The ST and the SOs see what they need from DT in PF, read-only from the DT API (4.4). Fallback if that is not enough: forward authentication at the reverse proxy (Eval 7.5). |

Consequences for the evaluation paper: drop and rename by new migrations (D5; Eval 8, 11); the technology catalogue
stays as the source of the CPE for technologies without purl or CPE (see 4.2); the status mapping of Eval 7.3 is
derived from case and team check, since findings only know New and Discarded (see 4.4).

## 2. Overview

| Phase | Content | Depends on | Result |
|---|---|---|---|
| 0 | Spike | – | go/no-go, answers to Eval 10 |
| 1 | Foundation: DT for development, client, configuration, health | 0 | PF talks to DT |
| 2 | PF → DT: projects and BOMs | 1 | DT knows the inventory |
| 3 | DT → PF: vulnerabilities and findings | 2 | findings from DT; own matching switched off |
| 4 | Analysis write-back, check requests, EPSS/KEV, ad-hoc check | 3 | functional parity |
| 5 | Retirement | 3, 4 | own importers and matching removed |
| 6 | Upload tokens and SBOM endpoint (INV-15) | 2 | CI uploads |
| – | Independent of DT, any time: EPSS in `CvrPriorityRule`, escalation (ESC), system messages in the chat | – | |

Phases 1–5 go into one feature branch and are merged together, so develop never has half a matching. Phase 6 can
follow in its own branch.

## 3. Phase 0 – Spike

DT 5.2.1 locally (compose file of phase 1, created here already; it needs only PostgreSQL), the inventory of a test
installation exported by a throw-away script as CycloneDX (one BOM per instance), plus PF's own SBOM. Compose file and
tools: `docker/compose/dependency-track/` (README there).

The questions of Eval 10, and in addition:

11. Does DT keep `bom-ref` and component `properties` of the uploaded BOM, and are they returned with the findings
    or the components of a project? (Mapping back to the PF technology, 4.3.)
12. Can the vulnerabilities be queried by ecosystem (purl type) and severity/EPSS/KEV, e.g. via
    `/api/v1/vulnerability` or API v2? (D3, check requests.)
13. How does DT report a vulnerability that the NVD rejects (IMP-15), and an alias (GHSA ↔ CVE)?
14. Does DT match a component without version, or with a version range, at all (MAT-03 "possibly affected")?

**Go** if: findings for the same inventory are at least as complete as the own matching (missing ones explained and
acceptable), KEV and EPSS are available or cheap to add, the mapping back to the technology works (11, or via purl +
version), and operation fits the target host (memory, additional services). **No-go** otherwise; then the CVE-Radar is
released with its own matching, and only EPSS in the priority (section 2, last row) is built.

Result: a short report appended to this document; the open "(verify)" marks of the evaluation paper are resolved.

## 4. Phases 1–5

### 4.1 Phase 1 – Foundation

- `docker/compose/dependency-track/docker-compose.yml` for development: API server, frontend, PostgreSQL, pinned
  versions, port bound to localhost; README with first start (admin password, service account `svc:projectforge`
  with the permissions of Eval 5.2, API key).
- `CveRadarConfig.deptrack`: `url`, `apiKey` (only in the configuration, never logged, IMP-05), `webhookSecret`,
  `syncCron`, `reconcileCron`; unconfigured means inactive (as `graylog`).
- `sync/DependencyTrackClient.kt` on `CveRadarHttp` / `PooledHttpClients`, client id `cveradar-deptrack` in
  `IntegrationConfig` (timeouts, size limit, proxy), header `X-Api-Key`. Only the calls PF needs: projects, BOM
  upload and processing status, components, findings, vulnerability, analysis.
- Health: DT version and reachability in the system status (NFR-05); a failing DT doesn't affect the other pages.
- Tests: client against a local stub server with recorded DT answers (from the spike).

### 4.2 Phase 2 – Projects and BOMs (PF → DT)

- **Columns** (new migration, D5): `dt_project_uuid` on system and instance, `dt_synced` (timestamp) and
  `dt_sync_pending` on the instance.
- **Project per instance**, parent project per system (Eval 7.1), named and tagged per D4 by the effective
  confidentiality. A change of the confidentiality renames the projects and replaces the tags.
- **BOM writer** `CvrBomWriter`: the effective technology list of an instance (baseline + deviations, INV-10, from
  `CvrInventoryService`) as CycloneDX 1.5 JSON. Per technology one component with `purl`, otherwise `cpe` (from the
  technology, or built from vendor/product/version of the catalogue node), name and version; `bom-ref`
  `pf-technology-<id>` and property `projectforge:technologyId`. Names matching the INV-34 patterns are written as
  they are; DT is configured to treat them as internal components (documented, checked in the spike, item 7).
- **Trigger:** where `CvrInventoryService` and `CvrCatalogService` call `matchingService.rematch*` today, the affected
  instances are marked `dt_sync_pending` instead. The job `cveradar.deptrack.sync` (every few minutes) uploads the
  pending instances (`PUT /api/v1/bom`), so a series of changes leads to one upload. A nightly run uploads all, which
  heals lost uploads.
- **Deletion** of an instance or system deletes its DT project.
- Tests: BOM writer (baseline, deviations, CPE from the catalogue, strictly confidential), sync queue.

### 4.3 Phase 3 – Vulnerabilities and findings (DT → PF)

- **Reconciliation** `CvrDependencyTrackSyncService.reconcile(instance)`: reads the findings of the project
  (`/api/v1/finding/project/{uuid}`) and maps each one back:
  - component → technology via `bom-ref` / property (spike 11), fallback purl + version within the instance;
    technology → component and instance as today;
  - vulnerability → `CvrVulnerabilityDO` by CVE id (an alias of a GHSA/OSV id with CVE id counts as the CVE), created
    or updated from DT's data: description, CVSS 3.1/4.0, severity, CWE, EPSS (precedence over the EUVD), KEV flag,
    published, source;
  - finding upsert with the key (vulnerability, component, instance) and the technology; certainty *sure*, *possible*
    for a technology without version (spike 14).
  A finding DT no longer reports is removed, except a discarded one (MAT-06), as `CvrMatchingService.remove` does
  today. Changed findings go through `CvrFindingNotifier`, so cases, notifications and the triage inbox are unchanged.
- **Job** `cveradar.deptrack.reconcile` (hourly, all projects) is the source of truth (Eval 7.2).
- **Webhook** `/rsPublic/cveradar/deptrack/notification` (Eval 7.2): the payload is used only to find the project;
  PF then reconciles this project. So nothing of the payload except the project UUID is trusted.
- **Vulnerability fields:** `nvd_status`, `nvd_last_modified`, `raw_document` become `source_status`,
  `source_modified`, `raw_document` (DT's vulnerability JSON); `CvrSource` gets `DEPENDENCY_TRACK`; renamed by a new
  migration (D5). The existing vulnerabilities are kept (cases and findings refer to them) and updated by the
  reconciliation via the CVE id. Manual vulnerabilities and incidents (IMP-14) stay as they are.
- The jobs NVD and OSV are switched off here; their removal follows in phase 5.
- Tests: mapping of findings (alias, without version, discarded stays, removal), webhook authentication and
  throttling (refused without secret, accepted with it).

### 4.4 Phase 4 – Functional parity

- **Analysis write-back** (`PUT /api/v1/analysis`) per finding, derived from finding, case and team check, since
  findings know only New and Discarded:

  | PF state | DT analysis state | Justification / response |
  |---|---|---|
  | Finding new, case new | NOT_SET | – |
  | Case in triage or rated | IN_TRIAGE | – |
  | Finding discarded | FALSE_POSITIVE | – |
  | Case not relevant | NOT_AFFECTED | CODE_NOT_REACHABLE or the reason as comment |
  | Case in remediation | EXPLOITABLE | response UPDATE |
  | Team check of the group responsible for the instance: not affected | NOT_AFFECTED | reason as comment |
  | Same team check: fixed | RESOLVED | – |
  | Same team check: risk accepted (SO) | EXPLOITABLE | response WILL_NOT_FIX, suppressed |

  PF stores the state last written (`dt_analysis_state` on the finding); a different state found during
  reconciliation is a change made in DT: taken over for a finding not yet worked on in PF, otherwise reported to the
  security team (Eval 7.3).
- **Check requests (MAT-11, D3):** vulnerabilities of the ecosystems in use above the threshold (CVSS ≥ 9, KEV or
  EPSS ≥ 30 %) from DT; fallback the OSV import reduced to MAT-11. The exclusion via frameworks (INV-30/31) uses the
  catalogue as today.
- **EPSS and KEV:** `CvrPriorityRule.suggest` takes EPSS (PRIO-01), the threshold of MAT-11 too. If DT has no KEV
  flag (spike 1), a small job reads the CISA KEV JSON and sets `kev_added`.
- **Ad-hoc check:** `CvrAdHocCheckService` uploads the list into a temporary DT project `pf-adhoc-<uuid>`, waits for
  the processing token, reads the findings and deletes the project; the job keeps its UI.
- **DT views in PF (D6):** read-only, for the ST and the SOs, same need-to-know as the instance: components of an
  instance with outdated versions, policy violations, risk trend (`/api/v1/component/project/{uuid}`,
  `/api/v1/violation/project/{uuid}`, `/api/v1/metrics/project/{uuid}/days/{n}`). Which of them are needed is
  asked before the build; nothing that changes DT.
- **EUVD:** stays as enrichment (EUVD id, exploitation day, EPSS for vulnerabilities without DT value); its
  approximate CPE index and the vulnerabilities only the EUVD knows go away.

### 4.5 Phase 5 – Retirement

| Remove | Keep |
|---|---|
| `sync/Nvd*` (import, client, parsers), `sync/Osv*` (unless the MAT-11 fallback needs them), `CvrOsvAdvisoryService`, `CvrOsvAdvisoryDO`, `CvrCpeMatchDO`, `CvrPurlMatchDO`, `CvrImportStateDO` entries of NVD/OSV | `VersionComparator` (used by `CvrCaseStatusService` for fix versions) |
| `CvrMatchingService` (unless CPE-only systems need it, spike 3) | Technology catalogue (CPE source of the BOM writer, framework exclusion of MAT-11) |
| Tables of the removed entities, dropped by a new migration (D5); the rows of NVD/OSV in the import state deleted | `LibraryListParser`, `LibraryArchive`, EUVD import, Graylog, inventory, cases, team checks, chat, notifications |
| Tests `CvrMatchingTest`, `CvrOsvMatchingTest`, `NvdC*ParserTest`, `OsvParserTest`; adapting `CveRadarTestBase` and the tests that create vulnerabilities via `NvdImportService` | |
| Configuration `nvd.*`, `osv.*`; jobs; i18n keys of the removed texts | |
| Next.js: NVD/OSV specific parts of the vulnerability page (CPE configurations, advisories), sources and import status | Next.js: everything else; the vulnerability page shows the DT source (no link into DT, D6) |

Afterwards: remove `cve-radar-db-reset.md` (the scripts are released, see D5), update the spec (IMP, MAT notes: "via
Dependency-Track") and the evaluation paper (status decided).

## 5. Phase 6 – Upload tokens and SBOM endpoint

As Eval 6.3: token per target (system baseline or instance) issued by the responsible group or the ST, hashed, with
expiry, revocable, last use shown, historised; endpoint `/rsPublic/cveradar/sbom` with throttling and size limits.
The upload goes into the inventory as today's upload with origin `CI` (INV-16) and marks the instance
`dt_sync_pending`; PF forwards it in phase 2's job. Tests for both halves (refused without or with a foreign token,
accepted with the token of the target).

## 6. Documentation and release

- Admin guide (`site/_docs/adminguide.adoc` and the German version, same anchors): running DT (Eval 7.5), service
  account, configuration of PF, webhook, internal components, analysers to disable (data protection).
- User guide (both languages): DT as source of the findings; with phase 6 the `curl` upload.
- Changelog: `changelog/unreleased/<date>-cveradar-dependency-track.json` (English and German), without names of
  persons or internal hosts.
- `dev-docs/README.md`: this plan as entry; removed when done, the evaluation paper keeps the decision.

## 7. Risks of the plan

- **Spike result unclear** (e.g. matching quality only partly comparable): decide on the documented examples; no
  partial switch.
- **DT outage during operation:** findings stay as they are, the next reconciliation completes them; uploads are
  pending, not lost.
- **Effort:** phases 1–5 about 2–3k LOC new and 5–7k LOC removed (Eval 8), phase 6 about 0.5–1k LOC.
