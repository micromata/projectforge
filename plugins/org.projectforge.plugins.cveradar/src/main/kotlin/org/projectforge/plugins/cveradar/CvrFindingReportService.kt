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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.model.CvrFindingDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.projectforge.plugins.cveradar.rest.FindingReport
import org.projectforge.plugins.cveradar.rest.FindingReportScope
import org.projectforge.plugins.cveradar.rest.InventoryLevel
import org.projectforge.plugins.cveradar.rest.NodeFinding
import org.projectforge.plugins.cveradar.rest.NodeFindingList
import org.projectforge.plugins.cveradar.rest.ReferenceLinkView
import org.projectforge.plugins.cveradar.rest.ReportCase
import org.projectforge.plugins.cveradar.rest.ReportFinding
import org.projectforge.plugins.cveradar.rest.ReportTechnology
import org.projectforge.plugins.cveradar.rest.ReportVulnerability
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/**
 * The report of the open findings below a node of the inventory (see [FindingReport]): per vulnerability the data of
 * the NVD, the case of the security team and the technologies that matched. What the user may not see isn't in it, not
 * even as a count; everything is taken from the database.
 */
@Service
class CvrFindingReportService {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var caseFileService: CvrCaseFileService

    @Autowired
    private lateinit var findingDao: CvrFindingDao

    @Autowired
    private lateinit var inventoryService: CvrInventoryService

    @Autowired
    private lateinit var osvService: CvrOsvAdvisoryService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /** The open findings below a node the user may read, of vulnerabilities not deleted. */
    private class Scoped(
        val scope: CvrInventoryService.ReportScope,
        val findings: List<CvrFindingDO>,
        val vulnerabilities: Map<Long, CvrVulnerabilityDO>,
    )

    private fun scoped(user: PFUserDO, level: InventoryLevel, id: Long): Scoped {
        access.checkReader(user)
        val scope = inventoryService.reportScope(user, level, id)
        val open = findingDao.selectOpenByInstances(scope.instances.keys)
            .filter { scope.componentId == null || it.componentId == scope.componentId }
        val vulnerabilities = byId(CvrVulnerabilityDO::class.java, open.mapNotNull { it.vulnerabilityId })
            .filterValues { !it.deleted }
        return Scoped(scope, open.filter { it.vulnerabilityId in vulnerabilities }, vulnerabilities)
    }

    /**
     * The open findings below a node one by one, for the tab "Findings" of its dialog: with the case of the security
     * team, the names as they are (the user sees these instances anyway). Same access as [report].
     */
    fun nodeFindings(user: PFUserDO, level: InventoryLevel, id: Long): NodeFindingList {
        val scoped = scoped(user, level, id)
        val cases = caseDao.selectByVulnerabilityIds(scoped.vulnerabilities.keys)
        val components = byId(CvrComponentDO::class.java, scoped.findings.mapNotNull { it.componentId })
        val technologies = byId(CvrTechnologyDO::class.java, scoped.findings.mapNotNull { it.technologyId })
        val findings = scoped.findings.map { finding ->
            val vulnerability = scoped.vulnerabilities.getValue(finding.vulnerabilityId!!)
            val case = cases[vulnerability.id]
            val instance = scoped.scope.instances.getValue(finding.instanceId!!)
            NodeFinding(
                id = finding.id!!, vulnerabilityId = vulnerability.id!!, cveId = vulnerability.cveId,
                severity = vulnerability.severity, cvssScore = vulnerability.cvssScore, kev = vulnerability.kevAdded != null,
                caseCode = case?.code, caseStatus = case?.status, casePriority = case?.priority,
                certainty = finding.certainty, matched = finding.matched, area = instance.area, system = instance.system,
                instance = instance.instance, component = components[finding.componentId]?.let { "${it.code} ${it.name}" },
                technology = technologies[finding.technologyId]?.let { listOfNotNull(it.product, it.version).joinToString(" ") },
            )
        }.sortedWith(
            compareBy<NodeFinding> { !it.kev }.thenBy { it.casePriority?.ordinal ?: Int.MAX_VALUE }
                .thenByDescending { it.cvssScore }.thenBy { it.cveId }.thenBy { it.system }.thenBy { it.instance ?: "" },
        )
        return NodeFindingList(findings)
    }

    fun report(user: PFUserDO, level: InventoryLevel, id: Long, anonymize: Boolean): FindingReport {
        val scoped = scoped(user, level, id)
        val scope = scoped.scope
        val findings = scoped.findings
        val vulnerabilities = scoped.vulnerabilities
        val cases = caseDao.selectByVulnerabilityIds(vulnerabilities.keys)
        val components = byId(CvrComponentDO::class.java, findings.mapNotNull { it.componentId })
        val technologies = byId(CvrTechnologyDO::class.java, findings.mapNotNull { it.technologyId })
        val names = Names(anonymize)
        val reported = sorted(
            findings.filter { it.vulnerabilityId in vulnerabilities }.groupBy { it.vulnerabilityId!! }
                .map { (vulnerabilityId, list) ->
                    val case = cases[vulnerabilityId]
                    reportVulnerability(
                        vulnerabilities.getValue(vulnerabilityId), case,
                        findingsOf(list, scope.instances, components, technologies, names),
                    ) to case
                },
        )
        return FindingReport(
            schemaVersion = SCHEMA_VERSION,
            generated = Date(),
            scope = FindingReportScope(
                scope.level,
                if (anonymize) ANONYMIZED_SCOPE else scope.code,
                if (anonymize) null else scope.name,
            ),
            anonymized = anonymize,
            vulnerabilityCount = reported.size,
            findingCount = findings.size,
            vulnerabilities = reported,
        )
    }

    /**
     * The report of an ad-hoc check (see [CvrAdHocCheckService]): per vulnerability the findings in the uploaded
     * files, which the caller built; the vulnerabilities are read here. Anything for a deleted one is left out.
     * [advisoryFindings]: the findings of OSV advisories not linked to a vulnerability yet, by vulnerability key; the
     * vulnerability is made of the advisories for the report only, it isn't stored.
     */
    fun adHocReport(
        findings: Map<Long, List<ReportFinding>>,
        advisoryFindings: Map<String, List<ReportFinding>>,
        anonymize: Boolean,
    ): FindingReport {
        val vulnerabilities = byId(CvrVulnerabilityDO::class.java, findings.keys).filterValues { !it.deleted }
        val cases = caseDao.selectByVulnerabilityIds(vulnerabilities.keys)
        val advisoriesOfKey = osvService.advisoriesOf(advisoryFindings.keys)
        val fromAdvisories = advisoryFindings.mapNotNull { (key, list) ->
            val advisories = advisoriesOfKey[key] ?: return@mapNotNull null
            reportVulnerability(osvService.toVulnerability(advisories), null, list) to null
        }
        val reported = sorted(
            vulnerabilities.map { (id, vulnerability) ->
                reportVulnerability(vulnerability, cases[id], findings.getValue(id)) to cases[id]
            } + fromAdvisories,
        )
        return FindingReport(
            schemaVersion = SCHEMA_VERSION,
            generated = Date(),
            scope = FindingReportScope(null, null, null, adHoc = true),
            anonymized = anonymize,
            vulnerabilityCount = reported.size,
            findingCount = reported.sumOf { it.findings.size },
            vulnerabilities = reported,
        )
    }

    /** Works for a vulnerability that isn't stored as well (made of OSV advisories for an ad-hoc check). */
    private fun reportVulnerability(vulnerability: CvrVulnerabilityDO, case: CvrCaseDO?, findings: List<ReportFinding>): ReportVulnerability {
        val (remediation, other) = caseFileService.partitionedReferences(vulnerability)
        return ReportVulnerability(
            cveId = vulnerability.cveId, nvdStatus = vulnerability.nvdStatus, published = vulnerability.published,
            description = vulnerability.description, cvssScore = vulnerability.cvssScore,
            severity = vulnerability.severity, cvss31Vector = vulnerability.cvss31Vector,
            cvss40Vector = vulnerability.cvss40Vector, cwes = CvrCaseFileService.cwesOf(vulnerability), kevAdded = vulnerability.kevAdded,
            references = (remediation + other).take(MAX_REFERENCES),
            case = case?.let { caseOf(it) },
            findings = findings,
        )
    }

    /** The most urgent first: in the KEV catalogue, then by the priority of the case, then by the CVSS score. */
    private fun sorted(list: List<Pair<ReportVulnerability, CvrCaseDO?>>): List<ReportVulnerability> = list.sortedWith(
        compareBy<Pair<ReportVulnerability, CvrCaseDO?>> { it.first.kevAdded == null }
            .thenBy { it.second?.priority?.ordinal ?: Int.MAX_VALUE }
            .thenByDescending { it.first.cvssScore }
            .thenBy { it.first.cveId },
    ).map { it.first }

    /** One entry per system, component, technology and kind of match, with the instances it matched in. */
    private fun findingsOf(
        findings: List<CvrFindingDO>,
        instances: Map<Long, CvrInventoryService.InstanceLabel>,
        components: Map<Long, CvrComponentDO>,
        technologies: Map<Long, CvrTechnologyDO>,
        names: Names,
    ): List<ReportFinding> {
        return findings.groupBy { listOf(instances[it.instanceId]?.systemId, it.componentId, it.technologyId, it.certainty, it.matched) }
            .values.map { group ->
                val first = group.first()
                val label = instances.getValue(first.instanceId!!)
                val component = components[first.componentId]
                val technology = technologies[first.technologyId]
                ReportFinding(
                    system = names.system(label),
                    instances = group.mapNotNull { instances[it.instanceId]?.let { instance -> names.instance(instance) } }
                        .distinct().sorted(),
                    component = component?.let { names.component(it) },
                    certainty = first.certainty,
                    matched = first.matched,
                    technology = technology?.let {
                        ReportTechnology(it.kind, it.vendor, it.product, it.version, it.ecosystem, it.purl, it.cpe, it.origin)
                    },
                )
            }.sortedWith(compareBy({ it.certainty }, { it.system }, { it.component }))
    }

    private fun caseOf(case: CvrCaseDO) = ReportCase(
        code = case.code, title = case.title, status = case.status, priority = case.priority,
        priorityReason = case.priorityReason, fixVersion = case.fixVersion, workaround = plainText(case.workaround),
        techMeasures = plainText(case.techMeasures), nonTechMeasures = plainText(case.nonTechMeasures),
        referenceLinks = CvrReferenceLinks.parse(case.referenceLinks).map { ReferenceLinkView(it.url, it.label) },
    )

    /**
     * The codes of the inventory, or neutral ones numbered in the order of their first use (AI-03): `S1`, `S1-I1`,
     * `S1-C1`. The default instance of a system has no code either way.
     */
    private class Names(val anonymize: Boolean) {
        private val systems = mutableMapOf<Long, String>()
        private val instances = mutableMapOf<Long, String>()
        private val components = mutableMapOf<Long, String>()

        fun system(label: CvrInventoryService.InstanceLabel): String =
            if (anonymize) systems.getOrPut(label.systemId) { "S${systems.size + 1}" } else label.system

        fun instance(label: CvrInventoryService.InstanceLabel): String? {
            val code = label.instance ?: return null
            if (!anonymize) return code
            val system = system(label)
            return instances.getOrPut(label.instanceId) { "$system-I${instances.count { it.value.startsWith("$system-") } + 1}" }
        }

        fun component(component: CvrComponentDO): String =
            if (anonymize) components.getOrPut(component.id!!) { "C${components.size + 1}" } else "${component.code} ${component.name}"
    }

    private fun <T : BaseDO<Long>> byId(clazz: Class<T>, ids: Collection<Long>): Map<Long, T> {
        if (ids.isEmpty()) return emptyMap()
        return persistenceService.executeQueryBatched(
            "from ${clazz.simpleName} e where e.id in :ids", clazz, "ids", ids.toSet(),
        ).associateBy { it.id!! }
    }

    companion object {
        const val SCHEMA_VERSION = "1"
        private const val ANONYMIZED_SCOPE = "PROJECT"
        private const val MAX_REFERENCES = 10

        /** The measures and the workaround are stored as html: as text, with lists and paragraphs on lines of their own. */
        internal fun plainText(html: String?): String? {
            if (html.isNullOrBlank()) return null
            val document: Document = Jsoup.parse(html)
            document.select("li").forEach { it.prependText("- ") }
            document.select("br, p, div, li, h1, h2, h3, h4, h5, h6, tr").forEach { it.before("\n") }
            return document.body().wholeText().lines().joinToString("\n") { it.trim() }
                .replace(Regex("\n{3,}"), "\n\n").trim().takeIf { it.isNotEmpty() }
        }
    }
}
