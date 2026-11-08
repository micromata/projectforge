# PF-CVR Migration plan - Dependency-Track as detection engine

| Field | Value |
|---|---|
| COMP-ID | PF-CVR |
| DOC-ID | PF-CVR-DT-MIG |
| Status | Draft 0.3 (plan; phase 0 done: go, see section 8; parallel operation decided, D2) |
| Owner | Kai Reinhard |
| Date | 2026-11-06 |
| Related | [PF-CVR_Dependency-Track_Evaluation.md](PF-CVR_Dependency-Track_Evaluation.md) (target architecture, sections referenced as *Eval x.y*), [PF-CVR_CVE-Radar_Vulnerability_Management.md](PF-CVR_CVE-Radar_Vulnerability_Management.md) (requirement IDs) |

---

## 1. Decisions

| # | Topic | Decision |
|---|---|---|
| D1 | Go/no-go | The spike (phase 0) is a real gate: the build starts only if the criteria of section 3 are met. |
| D2 | Parallel operation | DT and the own matching run in parallel. DT is optional: active only if `deptrack.url` and `deptrack.apiKey` are configured, otherwise everything stays as it is. Both write into the same findings, and each finding knows its sources (own matching, DT or both). A finding stays as long as one source reports it; a discarded one stays anyway (MAT-06). Cases, notifications and the triage inbox are unchanged. The findings by source show the comparison in operation; whether and when the own matching is retired is decided from that experience (4.5). |
| D5 | Database | The CVE-Radar ships since 9.2.1, so its Flyway scripts stay unchanged. New scripts `V<release>.<n>` of the releases shipping the phases only add: the DT columns of system and instance, the sources of a finding. Nothing is renamed or dropped during the parallel operation; that belongs to the retirement (4.5). |
| D3 | Vulnerabilities without finding | Check requests (MAT-11) and vulnerabilities without SBOM components come from DT's vulnerability database. Its API can't deliver them per ecosystem and threshold (spike, section 8), so a slim OSV import stays for MAT-11 only. |
| D4 | Names in DT | Projects are named in clear text (`<system> / <instance>`) with tags for area, stage, criticality and exposure; strictly confidential systems and instances are named `pf-<id>` and get no tags. |
| D6 | DT frontend | Not reachable from outside and not proxied by PF; only the DT admins use it, via SSH tunnel. The ST and the SOs see what they need from DT in PF, read-only from the DT API (4.4). Fallback if that is not enough: forward authentication at the reverse proxy (Eval 7.5). |

Consequences for the evaluation paper: drop and rename only with the retirement (D5; Eval 8, 11); the technology catalogue
stays as the source of the CPE for technologies without purl or CPE (see 4.2); the status mapping of Eval 7.3 is
derived from case and team check, since findings only know New and Discarded (see 4.4).

## 2. Overview

| Phase | Content | Depends on | Result |
|---|---|---|---|
| 0 | Spike | – | go/no-go, answers to Eval 10 |
| 1 | Foundation: DT for development, client, configuration, health | 0 | PF talks to DT |
| 2 | PF → DT: projects and BOMs | 1 | DT knows the inventory |
| 3 | DT → PF: vulnerabilities and findings | 2 | findings from both sources |
| 4 | Analysis write-back, check requests, EPSS/KEV, ad-hoc check | 3 | functional parity |
| 5 | Retirement (deferred) | 3, 4, experience of the parallel operation | own importers and matching removed |
| 6 | Upload tokens and SBOM endpoint (INV-15) | 2 | CI uploads |
| – | Independent of DT, any time: EPSS in `CvrPriorityRule`, escalation (ESC), system messages in the chat | – | |

Since DT is inactive unless configured (D2), every phase can be merged on its own; develop never has half a matching.
Phase 5 waits for the decision after the parallel operation.

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

Result: section 8; the "(verify)" marks of the evaluation paper are resolved.

## 4. Phases 1–5

### 4.1 Phase 1 – Foundation

Done (configuration, client, probe and tile of the system dashboard, admin guide `cve_radar_deptrack`).

- `docker/compose/dependency-track/docker-compose.yml` for development: API server, frontend, PostgreSQL, pinned
  versions, port bound to localhost; README with first start (admin password, service account `svc:projectforge`
  with the permissions of Eval 5.2, API key).
- `CveRadarConfig.deptrack`: `url`, `apiKey` (only in the configuration, never logged, IMP-05), `webhookSecret`
  (phase 3); unconfigured means inactive (as `graylog`). The crons of sync and reconciliation come with their phases.
- `sync/DependencyTrackClient.kt` on `CveRadarHttp` / `PooledHttpClients`, client id `cveradar-deptrack` in
  `IntegrationConfig` (timeouts, size limit), header `X-Api-Key`. An outgoing proxy isn't supported by
  `PooledHttpClients` yet (as for NVD, OSV, EUVD); open. Only the calls PF needs: projects, BOM
  upload and processing status, components, findings, vulnerability, analysis. Lists are paged (at most 100
  entries per page, total in the header `X-Total-Count`); the client always reads all pages.
- Health: DT version and reachability as a tile of the system dashboard (`SubsystemStatusProvider`, probe job
  `cveradar.deptrack.probe`, NFR-05); a failing DT doesn't affect the other pages.
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
- **Trigger:** where `CvrInventoryService` and `CvrCatalogService` call `matchingService.rematch*`, the affected
  instances are marked `dt_sync_pending` in addition, if DT is configured. The job `cveradar.deptrack.sync` (every few minutes) uploads the
  pending instances (`PUT /api/v1/bom`), so a series of changes leads to one upload. A nightly run uploads all, which
  heals lost uploads.
- **Deletion** of an instance or system deletes its DT project.
- Tests: BOM writer (baseline, deviations, CPE from the catalogue, strictly confidential), sync queue.

### 4.3 Phase 3 – Vulnerabilities and findings (DT → PF)

- **Reconciliation** `CvrDependencyTrackSyncService.reconcile(instance)`: reads the findings of the project
  (`/api/v1/finding/project/{uuid}`) and maps each one back:
  - component → technologies via purl + version (or CPE) within the instance: DT merges components with the same
    identity, so one DT component can stand for technologies of several PF components (spike: 47 of 753 in one
    instance); the property `projectforge:technologyId` keeps only the first one and serves as a shortcut only;
    technology → component and instance as today;
  - vulnerability → `CvrVulnerabilityDO` by CVE id (an alias of a GHSA/OSV id with CVE id counts as the CVE; an id
    without CVE alias is compared case-insensitively, DT keeps GHSA ids in lower case), created
    or updated from DT's data: description, CVSS 3.1/4.0, severity, CWE, EPSS (precedence over the EUVD), KEV flag,
    published, source, withdrawn (`rejected`);
  - finding upsert with the key (vulnerability, component, instance) and the technology; certainty *sure*, *possible*
    for a technology without version or with CPE version `*` (spike 14).
  The finding gets the source DT. A finding DT no longer reports loses the source DT; `CvrMatchingService.remove`
  correspondingly clears the source of the own matching. A finding without source is removed, except a discarded one
  (MAT-06). Changed findings go through `CvrFindingNotifier`, so cases, notifications and the triage inbox are
  unchanged; a finding found second by the other source is no new finding.
- **Job** `cveradar.deptrack.reconcile` (hourly, all projects) is the source of truth (Eval 7.2).
- **Webhook** `/rsPublic/cveradar/deptrack/notification` (Eval 7.2): the payload is used only to find the project;
  PF then reconciles this project. So nothing of the payload except the project UUID is trusted.
- **Vulnerability fields:** a vulnerability is found via the CVE id. The NVD stays the source of description, CVSS
  and CWE of the vulnerabilities it has; DT sets only what is still empty and its own data (EPSS, KEV, `rejected`), so
  the two sources don't overwrite each other. A vulnerability only DT knows is created with the new `CvrSource`
  `DEPENDENCY_TRACK`. Manual vulnerabilities and incidents (IMP-14) stay as they are. The renaming of the NVD fields
  (`nvd_status`, `nvd_last_modified` → `source_*`) waits for the retirement (D5).
- The jobs NVD and OSV keep running.
- Tests: mapping of findings (alias, without version, discarded stays, sources set and cleared, removal only
  without source), webhook authentication and
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

  Written only for findings with the source DT. PF stores the state last written (`dt_analysis_state` on the finding); a different state found during
  reconciliation is a change made in DT: taken over for a finding not yet worked on in PF, otherwise reported to the
  security team (Eval 7.3).
- **Check requests (MAT-11, D3):** vulnerabilities of the ecosystems in use above the threshold (CVSS ≥ 9, KEV or
  EPSS ≥ 30 %) from the OSV import, reduced to MAT-11 (DT's API filters findings, not vulnerabilities, see section
  8); EPSS and KEV of these vulnerabilities from DT where it knows them. The exclusion via frameworks (INV-30/31) uses the
  catalogue as today.
- **EPSS and KEV:** `CvrPriorityRule.suggest` takes EPSS (PRIO-01), the threshold of MAT-11 too. DT's KEV flag
  (`isKev`, from the CISA and ENISA lists) sets `kev_added`; no own KEV job.
- **Ad-hoc check:** stays with the own matching. If DT is configured, `CvrAdHocCheckService` also uploads the list
  into a temporary DT project `pf-adhoc-<uuid>`, waits for the processing token, reads the findings, deletes the
  project and shows the sources per hit; the job keeps its UI. Open whether that is needed.
- **DT views in PF (D6):** read-only, for the ST and the SOs, same need-to-know as the instance: components of an
  instance with outdated versions, policy violations, risk trend (`/api/v1/component/project/{uuid}`,
  `/api/v1/violation/project/{uuid}`, `/api/v1/metrics/project/{uuid}/days/{n}`). Which of them are needed is
  asked before the build; nothing that changes DT.
- **EUVD:** stays as enrichment (EUVD id, exploitation day, EPSS for vulnerabilities without DT value); its
  approximate CPE index and the vulnerabilities only the EUVD knows go away.

### 4.5 Phase 5 – Retirement (deferred)

Decided after the parallel operation (D2), from the findings by source. The table is the list of candidates.

| Remove | Keep |
|---|---|
| `sync/Nvd*` (import, client, parsers), the parts of `sync/Osv*` and `CvrOsvAdvisoryService` MAT-11 doesn't need, `CvrCpeMatchDO`, `CvrPurlMatchDO`, `CvrImportStateDO` entries of NVD | `VersionComparator` (used by `CvrCaseStatusService` for fix versions); OSV import and `CvrOsvAdvisoryDO` reduced to MAT-11 (D3) |
| `CvrMatchingService` (DT matches CPE-only technologies too, spike 3) | Technology catalogue (CPE source of the BOM writer, framework exclusion of MAT-11) |
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
- **Effort:** phases 1–4 about 2–3k LOC new; the 5–7k LOC removed (Eval 8) only with the retirement; phase 6 about
  0.5–1k LOC.
- **Parallel operation:** both sources are maintained until the retirement. Where they disagree, the finding shows
  its sources and the triage decides; a finding of one source only is no error in itself.

## 8. Spike result (phase 0)

DT 5.2.1 locally (compose file of phase 1), sources NVD, GitHub Advisories and OSV, EPSS and KEV as by default.
Inventories: a test installation and a copy of a production installation (6 instances, 1,890 technologies, 144
findings of the own matching); the own findings came from a complete OSV import. Tools: `docker/compose/dependency-track/spike/`.

**Decision: go, in parallel operation (D2).** Findings are as complete as the own matching, KEV and EPSS come with DT, the mapping back works via
purl + version, and DT needs only PostgreSQL.

Weighing of the decision:

- **Detection:** parity, not a gain. On the production copy DT found no additional finding; its 3 additional findings
  in the test installation came only from the incomplete own OSV import there.
- **Benefit beyond detection:** importers and matching (NVD/OSV parsing, version ranges, CPE matching) are maintained
  by the community instead of PF; KEV (CISA, ENISA) and EPSS for every CVE without own code; outdated components from
  the repository metadata (in one instance of the production copy 511 of 706 components not on the latest
  version); VEX export; policies (licences, versions).
- **Cost:** another service to operate (DT and PostgreSQL), about 2–3k LOC integration, the mapping back via purl +
  version; the saving is smaller than estimated, since the OSV import stays reduced to MAT-11 (D3).
- **Basis of the go:** the maintenance saving and the additional functions. If detection alone counted, keeping the
  own matching would be equivalent (the no-go branch of section 3: EPSS in the priority only).

| # | Question | Result |
|---|---|---|
| 10 | Findings compared with the own matching | Production copy: all 141 pairs of instance and vulnerability of the own 144 findings found by DT, none missing, none additional (the 3 extra own findings are the same pairs in a second component, see 11). Test installation with an incomplete OSV import: 3 additional findings in DT, all from GitHub Advisories. A technology with neither purl nor CPE gets no finding (forced test case). |
| 1 | KEV, EPSS, EUVD | KEV from the CISA and ENISA lists (`isKev`, findings filterable by it), EPSS for about 400,000 CVEs, both mirrored daily. The EUVD is no source; PF keeps it as enrichment (4.4). |
| 2 | SPDX | Refused (`Unable to determine schema version`); PF converts to CycloneDX anyway. |
| 3 | CPE only | Matched (a CPE with version gave the CVEs of that version). |
| 4 | Analysis via API | `GET`/`PUT /api/v1/analysis` with state, justification, response, details, comment, suppression; the audit trail comes with the analysis. Only API v1; API v2 covers the administration (service accounts, API keys, data sources, secrets). |
| 5 | Across projects | `/api/v1/finding/grouped` (occurrences per vulnerability) and `/api/v1/vulnerability/source/{source}/vuln/{id}/projects`. |
| 6 | Webhook | Not checked: the publishers are visible to DT admins only. Phase 3, with the fallback of a secret path segment (Eval 7.2). |
| 7 | Internal components | Not checked; the external analysers stay off. To be checked in phase 2 for the lookup of repository metadata, which queries public repositories with the coordinates of the components. |
| 8 | Resources, mirror, services | API server about 0.5 GB in operation (limit 4 GB), database about 2 GB. First mirror on a developer machine: NVD and OSV about 3 minutes each, GitHub Advisories about 25 minutes. Only PostgreSQL, no Kafka. A mirror runs on its schedule, not when a source is enabled (README). Telemetry is scheduled by default; to be switched off. |
| 9 | API keys | Keys of service accounts expire (30 days by default, at most 366), are shown once, listed with public id and last use. The key of the team `projectforge` can't read the administration (403). Portfolio access control not checked (not needed: PF is the only writer, Eval 6.3). |
| 11 | `bom-ref`, properties | `bom-ref` is not kept; properties are kept per component (`/api/v1/component/{uuid}/property`), not in the findings. DT merges components with the same identity within a project (753 → 706 in one instance) and keeps the property of the first: the mapping back goes via purl + version to all technologies of the instance (4.3). |
| 12 | Vulnerabilities by ecosystem and threshold | No: `/api/v1/vulnerability` only pages; the filters (severity, EPSS, KEV) exist for findings only. Hence the OSV import reduced to MAT-11 (D3). |
| 13 | Rejected vulnerability, alias | Withdrawn GitHub and OSV advisories carry `rejected`; for CVEs rejected by the NVD to be checked in phase 3 (IMP-15). GHSA findings come with their CVE alias (`aliases[].cveId`); a GHSA without CVE keeps its id, in lower case. |
| 14 | Without version, version ranges | A purl without version gets no finding; a CPE with version `*` gets every vulnerability of the product. PF marks both as possible (4.3); the production inventory has no technology without version. |

For the client (4.1): every list is paged (at most 100 entries, `X-Total-Count`), the findings of a project too.
