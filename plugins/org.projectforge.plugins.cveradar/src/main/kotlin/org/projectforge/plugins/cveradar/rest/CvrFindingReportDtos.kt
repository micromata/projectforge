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
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrOrigin
import org.projectforge.plugins.cveradar.model.CvrPriority
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import java.math.BigDecimal
import java.util.Date

/**
 * The open findings below a node of the inventory as one JSON document, to keep or to hand to an AI system for advice
 * on the fix (see `finding-report-prompt.ts`). Only what the user may see (VIS-02); [anonymized]: the codes and names
 * of areas, systems, instances and components replaced by neutral ones (AI-03).
 */
class FindingReport(
    val schemaVersion: String,
    val generated: Date,
    val scope: FindingReportScope,
    val anonymized: Boolean,
    val vulnerabilityCount: Int,
    val findingCount: Int,
    /** The most urgent first: in the KEV catalogue, then by the priority of the case, then by the CVSS score. */
    val vulnerabilities: List<ReportVulnerability>,
)

/** A node of the inventory ([level], [code], [name]), or the files of an ad-hoc check ([adHoc], no level). */
class FindingReportScope(val level: InventoryLevel?, val code: String?, val name: String?, val adHoc: Boolean = false)

class ReportVulnerability(
    val cveId: String?,
    val nvdStatus: String?,
    val published: Date?,
    val description: String?,
    val cvssScore: BigDecimal?,
    val severity: String?,
    val cvss31Vector: String?,
    val cvss40Vector: String?,
    val cwes: List<String>,
    /** When the CVE was added to the catalogue of known exploited vulnerabilities (CISA KEV). */
    val kevAdded: Date?,
    /** The references of the NVD, those for the remediation (patch, vendor advisory, mitigation) first. */
    val references: List<NvdReference>,
    /** The assessment of the security team, if there is a case. */
    val case: ReportCase?,
    val findings: List<ReportFinding>,
)

/** The case of the security team; the measures and the workaround as plain text. */
class ReportCase(
    val code: String?,
    val title: String?,
    val status: CvrCaseStatus?,
    val priority: CvrPriority?,
    val priorityReason: String?,
    val fixVersion: String?,
    val workaround: String?,
    val techMeasures: String?,
    val nonTechMeasures: String?,
    val referenceLinks: List<ReferenceLinkView>,
)

/** One technology of a component that matched, with the instances it matched in (the default instance has no code). */
class ReportFinding(
    val system: String?,
    val instances: List<String>,
    val component: String?,
    val certainty: CvrFindingCertainty?,
    /** What matched, e.g. the CPE with its version range. */
    val matched: String?,
    val technology: ReportTechnology?,
)

class ReportTechnology(
    val kind: CvrTechnologyKind?,
    val vendor: String?,
    val product: String?,
    val version: String?,
    val ecosystem: String?,
    val purl: String?,
    val cpe: String?,
    /** Where it came from: entered by hand, an SBOM, a lockfile, … */
    val origin: CvrOrigin?,
)
