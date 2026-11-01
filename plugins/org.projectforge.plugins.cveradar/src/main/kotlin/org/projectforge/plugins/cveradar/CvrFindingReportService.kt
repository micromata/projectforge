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
    private lateinit var persistenceService: PfPersistenceService

    fun report(user: PFUserDO, level: InventoryLevel, id: Long, anonymize: Boolean): FindingReport {
        access.checkReader(user)
        val scope = inventoryService.reportScope(user, level, id)
        val findings = findingDao.selectOpenByInstances(scope.instances.keys)
            .filter { scope.componentId == null || it.componentId == scope.componentId }
        val vulnerabilities = byId(CvrVulnerabilityDO::class.java, findings.mapNotNull { it.vulnerabilityId })
            .filterValues { !it.deleted }
        val cases = caseDao.selectByVulnerabilityIds(vulnerabilities.keys)
        val components = byId(CvrComponentDO::class.java, findings.mapNotNull { it.componentId })
        val technologies = byId(CvrTechnologyDO::class.java, findings.mapNotNull { it.technologyId })
        val names = Names(anonymize)
        val reported = findings.filter { it.vulnerabilityId in vulnerabilities }.groupBy { it.vulnerabilityId!! }
            .map { (vulnerabilityId, list) ->
                val vulnerability = vulnerabilities.getValue(vulnerabilityId)
                val case = cases[vulnerabilityId]
                val view = caseFileService.vulnerabilityView(vulnerability)
                ReportVulnerability(
                    cveId = vulnerability.cveId, nvdStatus = vulnerability.nvdStatus, published = vulnerability.published,
                    description = vulnerability.description, cvssScore = vulnerability.cvssScore,
                    severity = vulnerability.severity, cvss31Vector = vulnerability.cvss31Vector,
                    cvss40Vector = vulnerability.cvss40Vector, cwes = view.cwes, kevAdded = vulnerability.kevAdded,
                    references = (view.remediationReferences + view.otherReferences).take(MAX_REFERENCES),
                    case = case?.let { caseOf(it) },
                    findings = findingsOf(list, scope.instances, components, technologies, names),
                ) to case
            }
            .sortedWith(
                compareBy<Pair<ReportVulnerability, CvrCaseDO?>> { it.first.kevAdded == null }
                    .thenBy { it.second?.priority?.ordinal ?: Int.MAX_VALUE }
                    .thenByDescending { it.first.cvssScore }
                    .thenBy { it.first.cveId },
            ).map { it.first }
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
            findingCount = findings.count { it.vulnerabilityId in vulnerabilities },
            vulnerabilities = reported,
        )
    }

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
