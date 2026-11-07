# Dependency-Track for development

Dependency-Track (DT) 5 for the CVE-Radar's migration (`dev-docs/requirements/PF-CVR_Dependency-Track_Migration.md`):
the spike (phase 0) and the development of phases 1–6. Not for production: fixed passwords, ports bound to localhost.
Version 5.2.1 needs only PostgreSQL (no Kafka or separate analysers).

| Service | URL |
|---|---|
| API server | http://localhost:8090 |
| Frontend | http://localhost:8091 |

## First start

```bash
cd docker/compose/dependency-track
podman compose up -d        # or docker compose
podman compose logs -f apiserver
```

1. The first start mirrors the vulnerability sources (NVD, GitHub Advisories, OSV) and takes a while; note its duration
   and the memory used (`podman stats`) for the spike (question 8).
2. Log in at http://localhost:8091 as `admin` / `admin` and set a new password.
3. *Administration → Access Management → Teams*: create the team `projectforge` with the permissions `BOM_UPLOAD`,
   `PORTFOLIO_MANAGEMENT`, `PROJECT_CREATION_UPLOAD`, `VIEW_POLICY_VIOLATION`, `VIEW_PORTFOLIO`,
   `VIEW_VULNERABILITY`, `VULNERABILITY_ANALYSIS`, and generate an API key. The key stays out of the repository.
4. *Administration → Analyzers* and *Vulnerability Sources*: enable the internal analyser, OSV (Maven, npm, PyPI,
   NuGet, Packagist, Go, crates.io) and the GitHub Advisories (needs a GitHub token); EPSS is enabled by default.
   Leave the external analysers (OSS Index, Snyk, VulnDB) off: they would send the component list outside.
   Only the NVD is mirrored by default; without GitHub Advisories and OSV a component with a purl but no CPE gets no
   findings.

Stop with `podman compose down`; `down -v` also deletes the data, so the next start mirrors everything again.

## Spike (phase 0)

`spike/` holds throw-away tools; they are removed after the spike. Their output (`spike/out/`) contains the
inventory of the test installation and is not committed.

```bash
cd docker/compose/dependency-track/spike
mkdir -p out
# 1. Inventory and own findings of the test installation (PostgreSQL in the container projectforge-postgres).
podman exec -i projectforge-postgres psql -U projectforge -d projectforge -At < inventory.sql > out/inventory.json
podman exec -i projectforge-postgres psql -U projectforge -d projectforge -At < findings.sql > out/findings.json
# 2. One CycloneDX 1.5 BOM per instance (bom-ref pf-technology-<id>, names as decision D4).
python3 -I spike.py bom out/inventory.json out
# 3. Upload, wait for the processing, compare.
export DT_API_KEY=...
python3 -I spike.py upload out
python3 -I spike.py compare out out/findings.json
```

`compare` writes `out/compare.csv` (per instance and CVE: both, only PF, only DT) and `out/dt-findings.json` (DT's raw
answers, for the questions about `bom-ref`, aliases and analysis). The results go into the report at the end of the
migration plan.
