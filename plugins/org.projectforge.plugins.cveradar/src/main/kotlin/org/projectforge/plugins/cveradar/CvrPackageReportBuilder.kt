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

package org.projectforge.plugins.cveradar

import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.rest.FindingReport
import org.projectforge.plugins.cveradar.rest.PackageReport
import org.projectforge.plugins.cveradar.rest.PackageVulnerability
import org.projectforge.plugins.cveradar.rest.PackageVulnerabilityCase
import org.projectforge.plugins.cveradar.rest.ReportFinding
import org.projectforge.plugins.cveradar.rest.ReportPackage
import org.projectforge.plugins.cveradar.rest.ReportTechnology
import org.projectforge.plugins.cveradar.rest.ReportVulnerability
import java.math.BigDecimal
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Turns a [FindingReport] into the [PackageReport]: the findings grouped by the package (name, version and package URL
 * of their technology), per package its vulnerabilities. Findings without a technology (entered by hand) name no
 * package and are left out. Reads nothing else, so the package report holds exactly what the finding report holds.
 */
object CvrPackageReportBuilder {
    const val SCHEMA_VERSION = "1"

    private val SEVERITIES = CvrFindingReportService.SEVERITIES

    fun build(report: FindingReport): PackageReport {
        val pairs = report.vulnerabilities.flatMap { vulnerability ->
            vulnerability.findings.filter { it.technology != null }.map { vulnerability to it }
        }
        val packages = pairs.groupBy { (_, finding) -> keyOf(finding.technology!!) }.map { (_, list) ->
            packageOf(list.first().second.technology!!, list)
        }.sortedWith(
            compareBy<ReportPackage> { !it.kev }.thenByDescending { SEVERITIES.indexOf(it.maxSeverity) }
                .thenByDescending { it.vulnerabilityCount }.thenBy { it.name }.thenBy { it.version },
        )
        val all = packages.flatMap { it.vulnerabilities }
        return PackageReport(
            schemaVersion = SCHEMA_VERSION,
            generated = report.generated,
            scope = report.scope,
            anonymized = report.anonymized,
            packageCount = packages.size,
            vulnerabilityCount = all.size,
            severityCounts = SEVERITIES.reversed().associateWith { severity ->
                all.count { (it.severity?.takeIf { s -> s in SEVERITIES } ?: "NONE") == severity }
            },
            packages = packages,
            excludedCveIds = report.excludedCveIds,
        )
    }

    private fun keyOf(technology: ReportTechnology) = listOf(packageName(technology), technology.version, technology.purl)

    private fun packageOf(technology: ReportTechnology, list: List<Pair<ReportVulnerability, ReportFinding>>): ReportPackage {
        val vulnerabilities = list.groupBy { it.first }.mapNotNull { (vulnerability, pairs) ->
            vulnerabilityOf(vulnerability, pairs.map { it.second })
        }.sortedWith(
            compareBy<PackageVulnerability> { it.kevAdded == null }.thenByDescending { SEVERITIES.indexOf(it.severity) }
                .thenByDescending { it.cvssScore ?: BigDecimal.valueOf(-1) }.thenBy { it.id },
        )
        val ecosystem = technology.ecosystem ?: purlType(technology.purl)
        return ReportPackage(
            name = packageName(technology),
            version = technology.version,
            ecosystem = ecosystem,
            ecosystemLabel = ecosystem?.let { ECOSYSTEM_LABELS[it.lowercase()] ?: it },
            purl = technology.purl,
            components = list.mapNotNull { componentOf(it.second) }.distinct().sorted(),
            vulnerabilityCount = vulnerabilities.size,
            maxScore = vulnerabilities.mapNotNull { it.cvssScore }.maxOrNull(),
            maxSeverity = vulnerabilities.mapNotNull { it.severity }.maxByOrNull { SEVERITIES.indexOf(it) },
            kev = vulnerabilities.any { it.kevAdded != null },
            fixVersion = vulnerabilities.mapNotNull { it.fixVersion }.maxWithOrNull(VersionComparator),
            withoutFixCount = vulnerabilities.count { it.fixVersion == null },
            vulnerabilities = vulnerabilities,
        )
    }

    /** One per package and vulnerability: the sure match wins over a possible one. */
    private fun vulnerabilityOf(vulnerability: ReportVulnerability, findings: List<ReportFinding>): PackageVulnerability? {
        val id = vulnerability.cveId ?: return null
        val affected = findings.filter { it.certainty == CvrFindingCertainty.AFFECTED }
        val best = affected.ifEmpty { findings }
        val matched = best.firstNotNullOfOrNull { it.matched }
        return PackageVulnerability(
            id = id,
            source = sourceOf(id),
            url = urlOf(id),
            cvssScore = vulnerability.cvssScore,
            severity = vulnerability.severity,
            description = vulnerability.description,
            affectedRange = rangeOf(matched),
            fixVersion = best.mapNotNull { CvrMatchingService.fixedVersionOf(it.matched) }.maxWithOrNull(VersionComparator),
            possible = affected.isEmpty(),
            kevAdded = vulnerability.kevAdded,
            case = vulnerability.case?.let { PackageVulnerabilityCase(it.code, it.status, it.priority, it.fixVersion) },
        )
    }

    /** The file of an ad-hoc check, or the system and component of the inventory. */
    private fun componentOf(finding: ReportFinding): String? =
        listOfNotNull(finding.system, finding.component).joinToString(": ").takeIf { it.isNotEmpty() }

    /**
     * The name of the package as its ecosystem spells it: Maven `groupId:artifactId` (of the package URL, the product
     * is the artifactId only), the others as the product (npm with its scope).
     */
    internal fun packageName(technology: ReportTechnology): String {
        val purl = technology.purl
        if (purlType(purl) == "maven") {
            val path = purl!!.substringAfter('/').substringBefore('@').substringBefore('?')
            val namespace = path.substringBeforeLast('/', "")
            if (namespace.isNotEmpty()) {
                return decode(namespace).replace('/', '.') + ":" + decode(path.substringAfterLast('/'))
            }
        }
        return technology.product ?: purl?.substringAfter('/')?.substringBefore('@')?.let(::decode) ?: "?"
    }

    private fun purlType(purl: String?): String? =
        purl?.takeIf { it.startsWith("pkg:") }?.removePrefix("pkg:")?.substringBefore('/')?.lowercase()

    private fun decode(text: String) = URLDecoder.decode(text, StandardCharsets.UTF_8)

    /** The range of a matched entry (`… (>= 2.0, < 2.14.0)`), see `CvrMatchingService.describe`. */
    internal fun rangeOf(matched: String?): String? =
        matched?.substringAfterLast(" (", "")?.removeSuffix(")")?.trim()?.takeIf { it.isNotEmpty() }

    internal fun sourceOf(id: String): String = when {
        id.startsWith("CVE-") -> "NVD"
        id.startsWith("GHSA-") -> "GitHub Advisory"
        else -> "OSV"
    }

    /**
     * The public page of a vulnerability: a CVE at the NVD, a GHSA in the GitHub Advisory Database (in lower case, as
     * GitHub spells it), other advisories at OSV.dev (as `vulnerability-url.ts` of the client).
     */
    internal fun urlOf(id: String): String = when {
        id.startsWith("CVE-") -> "https://nvd.nist.gov/vuln/detail/${encode(id)}"
        id.startsWith("GHSA-") -> "https://github.com/advisories/GHSA-${encode(id.substring(5).lowercase())}"
        else -> "https://osv.dev/vulnerability/${encode(id)}"
    }

    private fun encode(text: String) = URLEncoder.encode(text, StandardCharsets.UTF_8)

    private val ECOSYSTEM_LABELS = mapOf(
        "maven" to "Maven", "npm" to "npm", "pypi" to "PyPI", "nuget" to "NuGet", "golang" to "Go", "cargo" to "Cargo",
        "composer" to "Composer", "gem" to "RubyGems", "deb" to "Debian", "rpm" to "RPM", "apk" to "Alpine",
        "swift" to "Swift", "hex" to "Hex", "pub" to "Pub", "cocoapods" to "CocoaPods", "conan" to "Conan",
        "github" to "GitHub", "docker" to "Docker", "oci" to "OCI",
    )
}
