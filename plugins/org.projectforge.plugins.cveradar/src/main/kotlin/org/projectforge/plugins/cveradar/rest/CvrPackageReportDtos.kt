/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.cveradar.rest

import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.model.CvrPriority
import java.math.BigDecimal
import java.util.Date

/**
 * The vulnerabilities of a [FindingReport] per package (library and version): what an update of the package fixes at
 * once. The format the PDF and the Excel file are made of (see `CvrPackageReportBuilder`), and a JSON document of its
 * own; for an ad-hoc check as for a node of the inventory (area, system, instance, component). Built of the finding
 * report only, so it holds what the user may see and nothing else, anonymized as that.
 */
class PackageReport(
    val schemaVersion: String,
    val generated: Date,
    val scope: FindingReportScope,
    val anonymized: Boolean,
    val packageCount: Int,
    /** The vulnerabilities per package: one vulnerability in two packages counts twice. */
    val vulnerabilityCount: Int,
    /** The [vulnerabilityCount] per severity (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`; `NONE` for those without one). */
    val severityCounts: Map<String, Int>,
    /** The most urgent first: in the KEV catalogue, then by the highest severity, then by the number of vulnerabilities. */
    val packages: List<ReportPackage>,
    /** The vulnerabilities left out on request (handled separately), as in the finding report. */
    val excludedCveIds: List<String> = emptyList(),
)

class ReportPackage(
    /** The name as the ecosystem spells it: Maven `groupId:artifactId`, npm with its scope, otherwise the product. */
    val name: String,
    val version: String?,
    /** The type of the package URL (`maven`, `npm`, …), null for a technology without one. */
    val ecosystem: String?,
    /** The ecosystem as people write it: Maven, npm, PyPI, … */
    val ecosystemLabel: String?,
    val purl: String?,
    /** Where it was found: the files of an ad-hoc check, the components of the inventory. */
    val components: List<String>,
    val vulnerabilityCount: Int,
    val maxScore: BigDecimal?,
    val maxSeverity: String?,
    /** One of its vulnerabilities is in the catalogue of known exploited vulnerabilities (CISA KEV). */
    val kev: Boolean,
    /** The lowest version none of the matched ranges contains (the highest of their fixes); null if none names one. */
    val fixVersion: String?,
    /** The vulnerabilities whose matched ranges name no fix (an update to [fixVersion] doesn't cover them). */
    val withoutFixCount: Int,
    /** The most urgent first: in the KEV catalogue, then by the severity, then by the CVSS score. */
    val vulnerabilities: List<PackageVulnerability>,
)

class PackageVulnerability(
    /** The CVE id, or the id of the advisory without one (GHSA, PYSEC, …). */
    val id: String,
    /** Where the id is described: `NVD` for a CVE, `GitHub Advisory` for a GHSA, otherwise `OSV`. */
    val source: String,
    val url: String,
    val cvssScore: BigDecimal?,
    val severity: String?,
    val description: String?,
    /** The matched version range, e.g. `>= 2.0, < 2.14.0`; null if the match names none. */
    val affectedRange: String?,
    /** The fix of the matched range (its `< x`), null if it names none. */
    val fixVersion: String?,
    /** Matched by the name only or without a version: to be checked (certainty `POSSIBLE` in every finding). */
    val possible: Boolean,
    /** When the CVE was added to the catalogue of known exploited vulnerabilities (CISA KEV). */
    val kevAdded: Date?,
    /** The case of the security team, if there is one. */
    val case: PackageVulnerabilityCase?,
)

class PackageVulnerabilityCase(
    val code: String?,
    val status: CvrCaseStatus?,
    val priority: CvrPriority?,
    val fixVersion: String?,
)

/** The formats a [PackageReport] is downloaded in. */
enum class PackageReportFormat(val extension: String, val contentType: String) {
    JSON("json", "application/json"),
    PDF("pdf", "application/pdf"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
}
