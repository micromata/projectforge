#!/usr/bin/env python3
# Throw-away tool of the spike PF-CVR-DT-MIG phase 0 (see ../README.md); removed after the spike.
# Python 3 standard library only.
#
#   spike.py bom      <inventory.json> <out-dir>      one CycloneDX 1.5 BOM per instance + manifest.json
#   spike.py upload   <out-dir>                       uploads the BOMs, waits until DT has processed them
#   spike.py compare  <out-dir> <findings.json>       DT findings vs. own findings -> compare.csv, dt-findings.json
#
# Environment: DT_URL (default http://localhost:8090), DT_API_KEY (upload, compare).

import base64
import csv
import json
import os
import sys
import time
import urllib.parse
import urllib.request
import uuid

DT_URL = os.environ.get("DT_URL", "http://localhost:8090").rstrip("/")


def project_name(row):
    """D4: clear names, strictly confidential ones neutral."""
    if row["confidentiality"] == "STRICTLY_CONFIDENTIAL":
        return f"pf-{row['instance_id']}"
    instance = row["instance_code"] or "default"
    return f"{row['system_code']} / {instance}"


def project_tags(row):
    if row["confidentiality"] == "STRICTLY_CONFIDENTIAL":
        return []
    tags = [f"criticality-{row['criticality'].lower()}"]
    if row["stage"]:
        tags.append(f"stage-{row['stage'].lower()}")
    if row["exposed"]:
        tags.append("exposed")
    return tags


def cpe_of(row):
    if row["cpe"]:
        return row["cpe"]
    if row["cpe_vendor"] and row["cpe_product"]:
        return f"cpe:2.3:a:{row['cpe_vendor']}:{row['cpe_product']}:{row['version'] or '*'}:*:*:*:*:*:*:*"
    return None


def purl_of(row):
    if row["purl"]:
        return row["purl"]
    if row["purl_type"] and row["purl_name"]:
        namespace = f"{row['purl_namespace']}/" if row["purl_namespace"] else ""
        version = f"@{urllib.parse.quote(row['version'])}" if row["version"] else ""
        return f"pkg:{row['purl_type']}/{namespace}{row['purl_name']}{version}"
    return None


def component_of(row):
    component = {
        "type": "library" if purl_of(row) else "application",
        "bom-ref": f"pf-technology-{row['technology_id']}",
        "name": row["product"],
        "properties": [{"name": "projectforge:technologyId", "value": str(row["technology_id"])}],
    }
    if row["vendor"]:
        component["publisher"] = row["vendor"]
    if row["version"]:
        component["version"] = row["version"]
    purl, cpe = purl_of(row), cpe_of(row)
    if purl:
        component["purl"] = purl
    if cpe:
        component["cpe"] = cpe
    return component


def cmd_bom(inventory_file, out_dir):
    with open(inventory_file, encoding="utf-8") as f:
        rows = json.load(f)
    instances = {}
    for row in rows:
        # Ecosystems (e.g. `maven`) are no components; they matter for the check requests (MAT-11) only.
        if row["kind"] == "ECOSYSTEM":
            continue
        instances.setdefault(row["instance_id"], []).append(row)
    os.makedirs(out_dir, exist_ok=True)
    manifest = []
    for instance_id, list_ in instances.items():
        first = list_[0]
        bom = {
            "bomFormat": "CycloneDX",
            "specVersion": "1.5",
            "serialNumber": f"urn:uuid:{uuid.uuid4()}",
            "version": 1,
            "metadata": {
                "component": {"type": "application", "name": project_name(first)},
                "properties": [{"name": "projectforge:instanceId", "value": str(instance_id)}],
            },
            "components": [component_of(row) for row in list_],
        }
        file = f"instance-{instance_id}.cdx.json"
        with open(os.path.join(out_dir, file), "w", encoding="utf-8") as f:
            json.dump(bom, f, indent=2)
        manifest.append({
            "instanceId": instance_id,
            "systemId": first["system_id"],
            "projectName": project_name(first),
            "tags": project_tags(first),
            "file": file,
            "components": len(list_),
            "withoutPurlOrCpe": sum(1 for row in list_ if not purl_of(row) and not cpe_of(row)),
            "withoutVersion": sum(1 for row in list_ if not row["version"]),
        })
    with open(os.path.join(out_dir, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)
    print(f"{len(manifest)} BOMs, {sum(m['components'] for m in manifest)} components written to {out_dir}")


def request(method, path, body=None):
    api_key = os.environ.get("DT_API_KEY")
    if not api_key:
        sys.exit("DT_API_KEY is not set")
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{DT_URL}{path}", data=data, method=method)
    req.add_header("X-Api-Key", api_key)
    req.add_header("Accept", "application/json")
    if data is not None:
        req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=120) as response:
        text = response.read().decode()
        return json.loads(text) if text else None


def load_manifest(out_dir):
    with open(os.path.join(out_dir, "manifest.json"), encoding="utf-8") as f:
        return json.load(f)


def cmd_upload(out_dir):
    manifest = load_manifest(out_dir)
    tokens = []
    for entry in manifest:
        with open(os.path.join(out_dir, entry["file"]), "rb") as f:
            bom = base64.b64encode(f.read()).decode()
        answer = request("PUT", "/api/v1/bom", {
            "projectName": entry["projectName"],
            "projectVersion": "current",
            "autoCreate": True,
            "projectTags": [{"name": tag} for tag in entry["tags"]],
            "bom": bom,
        })
        tokens.append((entry["projectName"], answer["token"]))
        print(f"uploaded {entry['projectName']}")
    started = time.time()
    for name, token in tokens:
        while request("GET", f"/api/v1/bom/token/{token}")["processing"]:
            time.sleep(2)
        print(f"processed {name} after {time.time() - started:.0f} s")


def find_project(name):
    query = urllib.parse.urlencode({"name": name, "version": "current"})
    return request("GET", f"/api/v1/project/lookup?{query}")


def cve_ids(vulnerability):
    """The CVE ids of a DT vulnerability: its own id if it is one, plus the CVE aliases (question 13)."""
    ids = set()
    if vulnerability.get("vulnId", "").startswith("CVE-"):
        ids.add(vulnerability["vulnId"])
    for alias in vulnerability.get("aliases") or []:
        if alias.get("cveId"):
            ids.add(alias["cveId"])
    return ids


def cmd_compare(out_dir, findings_file):
    manifest = load_manifest(out_dir)
    with open(findings_file, encoding="utf-8") as f:
        own = json.load(f)
    own_keys = {(f["instance_id"], f["cve_id"]): f for f in own}
    dt_keys = {}
    raw = {}
    for entry in manifest:
        project = find_project(entry["projectName"])
        findings = request("GET", f"/api/v1/finding/project/{project['uuid']}?suppressed=true")
        raw[entry["projectName"]] = findings
        for finding in findings:
            vulnerability = finding["vulnerability"]
            ids = cve_ids(vulnerability) or {vulnerability["vulnId"]}
            for cve_id in ids:
                dt_keys[(entry["instanceId"], cve_id)] = finding
    with open(os.path.join(out_dir, "dt-findings.json"), "w", encoding="utf-8") as f:
        json.dump(raw, f, indent=2)
    with open(os.path.join(out_dir, "compare.csv"), "w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["instance", "vulnerability", "result", "pf technology", "pf certainty", "pf status",
                         "dt component", "dt version", "dt purl", "dt source", "dt epss", "dt cvss3",
                         "dt analysis"])
        for key in sorted(set(own_keys) | set(dt_keys), key=lambda k: (k[0], k[1])):
            pf, dt = own_keys.get(key), dt_keys.get(key)
            result = "both" if pf and dt else "pf only" if pf else "dt only"
            component = (dt or {}).get("component", {})
            vulnerability = (dt or {}).get("vulnerability", {})
            writer.writerow([
                key[0], key[1], result,
                (pf or {}).get("technology_id"), (pf or {}).get("certainty"), (pf or {}).get("status"),
                component.get("name"), component.get("version"), component.get("purl"),
                vulnerability.get("source"), vulnerability.get("epssScore"), vulnerability.get("cvssV3BaseScore"),
                ((dt or {}).get("analysis") or {}).get("state"),
            ])
    both = len(set(own_keys) & set(dt_keys))
    print(f"both: {both}, pf only: {len(own_keys) - both}, dt only: {len(dt_keys) - both}")
    print(f"details in {os.path.join(out_dir, 'compare.csv')}")


COMMANDS = {"bom": (cmd_bom, 2), "upload": (cmd_upload, 1), "compare": (cmd_compare, 2)}

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS or len(sys.argv) - 2 != COMMANDS[sys.argv[1]][1]:
        sys.exit("usage: spike.py bom|upload|compare ... (see the head of this file)")
    COMMANDS[sys.argv[1]][0](*sys.argv[2:])
