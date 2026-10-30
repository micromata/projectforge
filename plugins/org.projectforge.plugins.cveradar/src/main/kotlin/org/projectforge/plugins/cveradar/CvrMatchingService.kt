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

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.hibernate.Session
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.plugins.cveradar.model.CvrCatalogNodeDO
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.model.CvrCpeMatchDO
import org.projectforge.plugins.cveradar.model.CvrFindingBucket
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrFindingDO
import org.projectforge.plugins.cveradar.model.CvrFindingStatus
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrInstanceDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.projectforge.plugins.cveradar.sync.NvdCpeEntry
import org.projectforge.plugins.cveradar.sync.NvdCpeParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.Date
import java.util.zip.GZIPInputStream

private val log = KotlinLogging.logger {}

/**
 * Matches the vulnerabilities against the technologies of the inventory and keeps the findings up to date (MAT-01,
 * MAT-02). A finding is a vulnerability x component x instance: a component of the baseline of a system counts for
 * every instance of the system, a component of an instance for that instance.
 *
 * How a technology meets a vulnerable CPE entry of the NVD document ([CvrCpeMatchDO]):
 * - the product (and vendor) come from the CPE of the technology or from its catalogue node (INV-28): a match is
 *   *affected*; or from its name (lower case, blanks as `_`, the vendor not compared): a match is only *possible*
 *   (MAT-12),
 * - the version is compared with the version or range of the entry ([VersionComparator]); without a version of the
 *   technology a match is only *possible* (MAT-03).
 *
 * Matching is idempotent: a run creates what is missing, corrects what changed and removes what no longer matches; a
 * discarded finding is left alone while its component and instance exist, even if it doesn't match for a while (MAT-06).
 * The scope of a run is a set of vulnerabilities (after an import), a set of components (after a change of the
 * inventory) or everything.
 *
 * A finding a person has worked on (any status but new) is kept when it stops matching, and follows the version of its
 * technology ([CvrFindingDO.matchedVersion]):
 * - the version changes and the vulnerability no longer matches: an open finding becomes *fix planned* with the new
 *   version (MAT-07, MAT-09: the rollout can lag behind, so "fixed" is confirmed by a person, in every instance),
 * - the version changes and the vulnerability matches again, e.g. after a rollback: a finding that was planned or done
 *   is reopened as *new* (MAT-13); the earlier status stays in the history.
 */
@Service
class CvrMatchingService {
    @Autowired
    private lateinit var findingDao: CvrFindingDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    class Result {
        var created = 0
        var updated = 0
        var removed = 0
        var unchanged = 0

        override fun toString() = "created=$created, updated=$updated, removed=$removed, unchanged=$unchanged"
    }

    /** What a technology looks for: a product, the vendor if known, and whether the source is sure. */
    private class Key(val vendor: String?, val product: String, val sure: Boolean)

    private class Hit(val technologyId: Long?, val version: String?, val certainty: CvrFindingCertainty, val matched: String)

    /** After the import: the new and modified vulnerabilities against the whole inventory. */
    fun rematchVulnerabilities(vulnerabilityIds: Collection<Long>): Result =
        if (vulnerabilityIds.isEmpty()) Result() else rematch(vulnerabilityIds, null)

    /** After a change of the inventory: the technologies of the components against all vulnerabilities. */
    fun rematchComponents(componentIds: Collection<Long>): Result =
        if (componentIds.isEmpty()) Result() else rematch(null, componentIds)

    /** After an instance or a system was added or deleted: every component of the system, the ones of its instances included. */
    fun rematchSystem(systemId: Long): Result {
        val ids = persistenceService.executeQuery(
            "select c.id from CvrComponentDO c where c.system.id = :id or c.instance.system.id = :id",
            Long::class.javaObjectType,
            Pair("id", systemId),
        )
        return rematchComponents(ids)
    }

    @Synchronized
    fun rematch(vulnerabilityIds: Collection<Long>? = null, componentIds: Collection<Long>? = null): Result {
        val result = Result()
        val model = loadModel(componentIds)
        val hits = if (model.keys.isEmpty()) emptyMap() else findHits(model, vulnerabilityIds)
        val existing = findingDao.selectInScope(vulnerabilityIds, componentIds)
            .groupBy { Triple(it.vulnerabilityId!!, it.componentId!!, it.instanceId!!) }
        val versions = model.technologies.associate { it.id!! to it.version?.trim()?.takeIf { v -> v.isNotEmpty() } }
        existing.forEach { (key, findings) ->
            val hit = hits[key]
            // A duplicate (should not exist) is removed.
            findings.drop(1).forEach { remove(it, result) }
            val finding = findings.first()
            val present = finding.componentId in model.components && finding.instanceId in model.instances
            val status = finding.status
            when {
                // A discarded finding stays with its reason while its component and instance exist (MAT-06), so a
                // version changed and reverted doesn't bring it back as new.
                hit == null && status == CvrFindingStatus.DISCARDED && present -> result.unchanged++

                hit == null && status != CvrFindingStatus.NEW && present -> followVersion(finding, versions, result)

                hit == null -> remove(finding, result)
                reopens(finding, hit) -> {
                    reopen(finding, hit)
                    result.updated++
                }

                finding.certainty != hit.certainty || finding.technologyId != hit.technologyId || finding.matched != hit.matched ||
                        finding.matchedVersion != hit.version -> {
                    finding.certainty = hit.certainty
                    finding.technology = hit.technologyId?.let { persistenceService.getReference(CvrTechnologyDO::class.java, it) }
                    finding.matched = hit.matched
                    finding.matchedVersion = hit.version
                    findingDao.update(finding, checkAccess = false)
                    result.updated++
                }

                else -> result.unchanged++
            }
        }
        hits.filterKeys { it !in existing }.forEach { (key, hit) ->
            findingDao.insert(
                CvrFindingDO().also {
                    it.vulnerability = persistenceService.getReference(CvrVulnerabilityDO::class.java, key.first)
                    it.component = persistenceService.getReference(CvrComponentDO::class.java, key.second)
                    it.instance = persistenceService.getReference(CvrInstanceDO::class.java, key.third)
                    it.technology = hit.technologyId?.let { id -> persistenceService.getReference(CvrTechnologyDO::class.java, id) }
                    it.certainty = hit.certainty
                    it.matched = hit.matched
                    it.matchedVersion = hit.version
                },
                checkAccess = false,
            )
            result.created++
        }
        if (result.created + result.updated + result.removed > 0) {
            log.info { "Matching finished: $result" }
        }
        return result
    }

    /** A planned or done finding whose technology changed its version and matches again, e.g. after a rollback (MAT-13). */
    private fun reopens(finding: CvrFindingDO, hit: Hit): Boolean {
        val status = finding.status ?: return false
        return (status == CvrFindingStatus.FIX_PLANNED || status.bucket == CvrFindingBucket.DONE) &&
                finding.matchedVersion != hit.version
    }

    private fun reopen(finding: CvrFindingDO, hit: Hit) {
        finding.status = CvrFindingStatus.NEW
        finding.reason = null
        finding.fixedVersion = null
        finding.fixedDate = null
        finding.riskExpires = null
        finding.riskRequestedBy = null
        finding.riskApprovedBySo = null
        finding.riskApprovedByMg = null
        finding.certainty = hit.certainty
        finding.technology = hit.technologyId?.let { persistenceService.getReference(CvrTechnologyDO::class.java, it) }
        finding.matched = hit.matched
        finding.matchedVersion = hit.version
        findingDao.update(finding, checkAccess = false)
    }

    /**
     * The worked-on finding no longer matches. If the version of its technology changed, remember the new one (so a
     * rollback is noticed) and let an open finding wait for the confirmation of the fix (MAT-07, MAT-09); otherwise the
     * vulnerability data changed and a person decides.
     */
    private fun followVersion(finding: CvrFindingDO, versions: Map<Long, String?>, result: Result) {
        val technologyId = finding.technologyId
        // A technology that was removed has no version any more: an open finding waits for the confirmation.
        val removed = technologyId == null || technologyId !in versions
        val changed = !removed && versions[technologyId] != finding.matchedVersion
        val open = finding.status?.bucket == CvrFindingBucket.OPEN
        if (!changed && !(removed && open)) {
            result.unchanged++
            return
        }
        if (open) {
            finding.status = CvrFindingStatus.FIX_PLANNED
            finding.fixedVersion = if (removed) null else versions[technologyId]
            finding.riskExpires = null
            finding.riskRequestedBy = null
            finding.riskApprovedBySo = null
            finding.riskApprovedByMg = null
        }
        if (changed) {
            finding.matchedVersion = versions[technologyId]
        }
        findingDao.update(finding, checkAccess = false)
        result.updated++
    }

    private fun remove(finding: CvrFindingDO, result: Result) {
        findingDao.markAsDeleted(finding, checkAccess = false)
        result.removed++
    }

    // --- The inventory ---

    private class Model(
        val instances: Map<Long, CvrInstanceDO>,
        val components: Map<Long, CvrComponentDO>,
        val technologies: List<CvrTechnologyDO>,
        val keys: Map<Long, List<Key>>,
    ) {
        /** The instances a component counts for. */
        fun instancesOf(component: CvrComponentDO): List<Long> =
            if (component.instanceId != null) {
                listOfNotNull(component.instanceId?.takeIf { it in instances })
            } else {
                instances.values.filter { it.systemId == component.systemId }.map { it.id!! }
            }
    }

    private fun loadModel(componentIds: Collection<Long>?): Model {
        val instances = persistenceService.executeQuery(
            "from CvrInstanceDO i where i.deleted = false", CvrInstanceDO::class.java,
        ).associateBy { it.id!! }
        val components: List<CvrComponentDO>
        val technologies: List<CvrTechnologyDO>
        val technologyQuery = "select t from CvrTechnologyDO t where t.deleted = false and t.kind = :kind"
        val kind = Pair("kind", CvrTechnologyKind.PRODUCT)
        if (componentIds == null) {
            components = persistenceService.executeQuery("from CvrComponentDO c where c.deleted = false", CvrComponentDO::class.java)
            technologies = persistenceService.executeQuery(technologyQuery, CvrTechnologyDO::class.java, kind)
        } else {
            components = persistenceService.executeQueryBatched(
                "from CvrComponentDO c where c.deleted = false and c.id in :ids", CvrComponentDO::class.java, "ids", componentIds,
            )
            technologies = persistenceService.executeQueryBatched(
                "$technologyQuery and t.component.id in :ids", CvrTechnologyDO::class.java, "ids", componentIds, kind,
            )
        }
        val activeComponents = components.associateBy { it.id!! }
        val active = technologies.filter { it.componentId in activeComponents }
        val nodes = persistenceService.executeQueryBatched(
            "from CvrCatalogNodeDO n where n.id in :ids", CvrCatalogNodeDO::class.java, "ids",
            active.mapNotNull { it.catalogNodeId }.toSet(),
        ).associateBy { it.id!! }
        val keys = active.associate { it.id!! to keysOf(it, nodes[it.catalogNodeId]) }
        return Model(instances, activeComponents, active, keys.filterValues { it.isNotEmpty() })
    }

    private fun keysOf(technology: CvrTechnologyDO, node: CvrCatalogNodeDO?): List<Key> {
        val keys = mutableListOf<Key>()
        technology.cpe?.let { cpe ->
            val fields = NvdCpeParser.splitCpe(cpe.trim())
            if (fields.size >= 5 && fields[0] == "cpe" && fields[3] != "*" && fields[4] != "*") {
                keys.add(Key(fields[3].lowercase(), fields[4].lowercase(), true))
            }
        }
        node?.cpeProduct?.takeIf { it.isNotBlank() }?.let {
            keys.add(Key(node.cpeVendor?.takeIf { v -> v.isNotBlank() }?.lowercase(), it.trim().lowercase(), true))
        }
        technology.product?.takeIf { it.isNotBlank() }?.let {
            keys.add(Key(technology.vendor?.takeIf { v -> v.isNotBlank() }?.let(::normalize), normalize(it), false))
        }
        return keys.distinctBy { Triple(it.vendor, it.product, it.sure) }
    }

    // --- The matching ---

    /** The best hit per vulnerability, component and instance. */
    private fun findHits(model: Model, vulnerabilityIds: Collection<Long>?): Map<Triple<Long, Long, Long>, Hit> {
        val byProduct = model.keys.values.flatten().groupBy { it.product }
        val candidates = if (vulnerabilityIds != null) {
            // Restricted to the products of the inventory in the query: a large import changes tens of thousands of CVEs.
            persistenceService.executeQueryBatched(
                "select m from CvrCpeMatchDO m where m.vulnerability.deleted = false and m.vulnerability.id in :ids and m.product in :products",
                CvrCpeMatchDO::class.java, "ids", vulnerabilityIds, Pair("products", byProduct.keys), batchSize = ID_BATCH,
            )
        } else {
            persistenceService.executeQueryBatched(
                "select m from CvrCpeMatchDO m where m.vulnerability.deleted = false and m.product in :ids",
                CvrCpeMatchDO::class.java, "ids", byProduct.keys,
            )
        }.groupBy { it.product }
        val hits = mutableMapOf<Triple<Long, Long, Long>, Hit>()
        model.technologies.forEach { technology ->
            val component = model.components[technology.componentId] ?: return@forEach
            val instanceIds = model.instancesOf(component)
            if (instanceIds.isEmpty()) {
                return@forEach
            }
            model.keys[technology.id]?.forEach { key ->
                candidates[key.product]?.forEach candidate@{ match ->
                    // By name the vendor may be spelled otherwise ("Apache Software Foundation" vs "apache"): a
                    // match by name is only possible anyway.
                    if (key.sure && key.vendor != null && key.vendor != match.vendor) {
                        return@candidate
                    }
                    val versionCertainty = versionMatch(technology.version, match) ?: return@candidate
                    val certainty = if (key.sure && versionCertainty == CvrFindingCertainty.AFFECTED) {
                        CvrFindingCertainty.AFFECTED
                    } else {
                        CvrFindingCertainty.POSSIBLE
                    }
                    val hit = Hit(technology.id, technology.version?.trim()?.takeIf { it.isNotEmpty() }, certainty, describe(match))
                    instanceIds.forEach { instanceId ->
                        val triple = Triple(match.vulnerability!!.id!!, component.id!!, instanceId)
                        val old = hits[triple]
                        if (old == null || (old.certainty == CvrFindingCertainty.POSSIBLE && certainty == CvrFindingCertainty.AFFECTED)) {
                            hits[triple] = hit
                        }
                    }
                }
            }
        }
        return hits
    }

    // --- The index of the CPE entries ---

    /** Replaces the index entries of the vulnerability (after the import wrote or changed it). */
    fun replaceIndex(vulnerabilityId: Long, entries: List<NvdCpeEntry>) =
        replaceIndex(mapOf(vulnerabilityId to entries))

    /** Replaces the index entries of several vulnerabilities in one transaction, the inserts sent in JDBC batches. */
    private fun replaceIndex(entries: Map<Long, List<NvdCpeEntry>>) {
        if (entries.isEmpty()) {
            return
        }
        persistenceService.runInTransaction { context ->
            context.em.unwrap(Session::class.java).jdbcBatchSize = INSERT_BATCH
            context.executeUpdate("delete from CvrCpeMatchDO m where m.vulnerability.id in :ids", Pair("ids", entries.keys))
            entries.forEach { (vulnerabilityId, list) ->
                val reference = context.getReference(CvrVulnerabilityDO::class.java, vulnerabilityId)
                list.forEach { entry ->
                    context.insert(
                        CvrCpeMatchDO().also {
                            it.vulnerability = reference
                            it.part = entry.part
                            it.vendor = entry.vendor
                            it.product = entry.product
                            it.cpeVersion = entry.version
                            it.versionStartIncluding = entry.startIncluding
                            it.versionStartExcluding = entry.startExcluding
                            it.versionEndIncluding = entry.endIncluding
                            it.versionEndExcluding = entry.endExcluding
                            it.criteria = entry.criteria
                        },
                    )
                }
            }
        }
    }

    /** Builds the index from the stored documents (for the vulnerabilities imported before the matching existed). */
    fun rebuildIndex() {
        val ids = persistenceService.executeQuery(
            "select v.id from CvrVulnerabilityDO v where v.deleted = false", Long::class.javaObjectType,
        )
        val mapper = ObjectMapper()
        ids.chunked(INDEX_CHUNK).forEach { chunk ->
            val entries = persistenceService.executeQuery(
                "from CvrVulnerabilityDO v where v.id in :ids", CvrVulnerabilityDO::class.java, Pair("ids", chunk),
            ).associate { vulnerability ->
                vulnerability.id!! to if (vulnerability.nvdStatus.equals("Rejected", ignoreCase = true)) {
                    emptyList()
                } else {
                    vulnerability.rawDocument?.let { raw ->
                        runCatching {
                            val json = GZIPInputStream(ByteArrayInputStream(raw)).use { String(it.readAllBytes(), Charsets.UTF_8) }
                            NvdCpeParser.parse(mapper.readTree(json))
                        }.getOrElse {
                            log.warn { "Can't read the document of ${vulnerability.cveId}: ${it.message}" }
                            emptyList()
                        }
                    } ?: emptyList()
                }
            }
            replaceIndex(entries)
        }
    }

    /**
     * Builds the index and matches everything once, if that didn't happen yet (the state `CPE_INDEX`). Called when the
     * plugin starts, in a thread of its own.
     */
    fun ensureIndex() {
        if (persistenceService.find(CvrImportStateDO::class.java, INDEX_STATE) != null) {
            return
        }
        log.info { "Building the CPE index of the vulnerabilities" }
        rebuildIndex()
        rematch()
        persistenceService.runInTransaction { context ->
            context.em.merge(CvrImportStateDO().also { it.source = INDEX_STATE; it.lastRun = Date() })
        }
    }

    companion object {
        const val INDEX_STATE = "CPE_INDEX"
        private const val INDEX_CHUNK = 200
        private const val INSERT_BATCH = 200

        /** Ids per query next to the products of the inventory, both bound parameters. */
        private const val ID_BATCH = 1000

        /** Lower case, blanks as `_` (the spelling of the CPE dictionary). */
        internal fun normalize(name: String): String = name.trim().lowercase().replace(Regex("\\s+"), "_")

        /** Whether [version] is covered by the entry: null no, AFFECTED yes, POSSIBLE if the version is unknown (MAT-03). */
        internal fun versionMatch(version: String?, match: CvrCpeMatchDO): CvrFindingCertainty? {
            val hasRange = listOf(
                match.versionStartIncluding, match.versionStartExcluding, match.versionEndIncluding, match.versionEndExcluding,
            ).any { it != null }
            if (!hasRange && match.cpeVersion == null) {
                return if (version.isNullOrBlank()) CvrFindingCertainty.POSSIBLE else CvrFindingCertainty.AFFECTED
            }
            if (version.isNullOrBlank()) {
                return CvrFindingCertainty.POSSIBLE
            }
            val v = version.trim()
            val covered = if (hasRange) {
                (match.versionStartIncluding?.let { VersionComparator.compare(v, it) >= 0 } ?: true) &&
                        (match.versionStartExcluding?.let { VersionComparator.compare(v, it) > 0 } ?: true) &&
                        (match.versionEndIncluding?.let { VersionComparator.compare(v, it) <= 0 } ?: true) &&
                        (match.versionEndExcluding?.let { VersionComparator.compare(v, it) < 0 } ?: true)
            } else {
                VersionComparator.compare(v, match.cpeVersion!!) == 0
            }
            return if (covered) CvrFindingCertainty.AFFECTED else null
        }

        /** The CPE with its range, e.g. `cpe:2.3:a:apache:tomcat:*:… (>= 9.0.0, < 9.0.90)`. */
        internal fun describe(match: CvrCpeMatchDO): String {
            val range = listOfNotNull(
                match.versionStartIncluding?.let { ">= $it" }, match.versionStartExcluding?.let { "> $it" },
                match.versionEndIncluding?.let { "<= $it" }, match.versionEndExcluding?.let { "< $it" },
            )
            return (match.criteria + if (range.isEmpty()) "" else " (${range.joinToString(", ")})").take(1000)
        }
    }
}
