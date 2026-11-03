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
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrFindingDO
import org.projectforge.plugins.cveradar.model.CvrFindingStatus
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrInstanceDO
import org.projectforge.plugins.cveradar.model.CvrPurlMatchDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.projectforge.plugins.cveradar.sync.NvdCpeEntry
import org.projectforge.plugins.cveradar.sync.NvdCpeParser
import org.projectforge.plugins.cveradar.sync.OsvPackages
import org.projectforge.plugins.cveradar.sync.OsvPackages.PurlPackage
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
 * How a library meets the affected versions of an OSV advisory ([CvrPurlMatchDO], IMP-13): by the package of its purl
 * or of its catalogue node, the version compared with the range ([purlVersionMatch]). A Maven library without groupId
 * (jar listing) meets by its artifact only: *possible*. An advisory not linked to a vulnerability yet becomes one on its
 * first match ([CvrOsvAdvisoryService.materialize]). Where the advisories of a vulnerability know the package of a
 * library, their verdict counts: a *possible* match of the same vulnerability by CPE is dropped.
 *
 * Matching is idempotent: a run creates what is missing, corrects what changed and removes what no longer matches; a
 * discarded finding is left alone while its component and instance exist, even if it doesn't match for a while (MAT-06). The scope of a run is a set of vulnerabilities (after
 * an import), a set of components (after a change of the inventory) or everything.
 */
@Service
class CvrMatchingService {
    @Autowired
    private lateinit var findingDao: CvrFindingDao

    @Autowired
    private lateinit var findingNotifier: CvrFindingNotifier

    @Autowired
    private lateinit var osvService: CvrOsvAdvisoryService

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

    private class Hit(val technologyId: Long?, val certainty: CvrFindingCertainty, val matched: String)

    /** An entry of the purl index with the ids it belongs to; the advisory isn't loaded (its document is large). */
    private class PurlCandidate(
        val match: CvrPurlMatchDO,
        val osvId: String,
        val key: String,
        val vulnerabilityId: Long?,
    )

    /** The candidates of the purl index by package name and (Maven) by artifact. */
    private class PurlCandidates(val byName: Map<String, List<PurlCandidate>>, val byShortName: Map<String, List<PurlCandidate>>) {
        val isEmpty get() = byName.isEmpty() && byShortName.isEmpty()
    }

    /**
     * The hits of a technology by the purl index per vulnerability key, with the vulnerability if the advisories are
     * linked to one; [covered]: the keys whose advisories know a package of the technology (whatever the version).
     */
    private class PurlHits(val hits: Map<String, Hit>, val vulnerabilityIds: Map<String, Long>, val covered: Set<String>)

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
        val hits = if (model.keys.isEmpty() && model.packages.isEmpty()) emptyMap() else findHits(model, vulnerabilityIds)
        val existing = findingDao.selectInScope(vulnerabilityIds, componentIds)
            .groupBy { Triple(it.vulnerabilityId!!, it.componentId!!, it.instanceId!!) }
        existing.forEach { (key, findings) ->
            val hit = hits[key]
            // A duplicate (should not exist) is removed.
            findings.drop(1).forEach { remove(it, result) }
            val finding = findings.first()
            when {
                // A discarded finding stays with its reason while its component and instance exist (MAT-06), so a
                // version changed and reverted doesn't bring it back as new.
                hit == null && finding.status == CvrFindingStatus.DISCARDED &&
                        finding.componentId in model.components && finding.instanceId in model.instances -> result.unchanged++

                hit == null -> remove(finding, result)
                finding.certainty != hit.certainty || finding.technologyId != hit.technologyId || finding.matched != hit.matched -> {
                    finding.certainty = hit.certainty
                    finding.technology = hit.technologyId?.let { persistenceService.getReference(CvrTechnologyDO::class.java, it) }
                    finding.matched = hit.matched
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
                },
                checkAccess = false,
            )
            result.created++
        }
        if (result.created + result.updated + result.removed > 0) {
            log.info { "Matching finished: $result" }
        }
        if (result.created > 0) {
            findingNotifier.findingsCreated()
        }
        return result
    }

    /**
     * After the OSV import: the advisories that match the inventory become vulnerabilities (or are linked to the stored
     * one of their key). The caller matches the returned vulnerabilities then ([rematchVulnerabilities]).
     * @return The ids of the vulnerabilities of the advisories.
     */
    @Synchronized
    fun materializeAdvisories(advisoryIds: Collection<Long>): Set<Long> {
        if (advisoryIds.isEmpty()) return emptySet()
        val model = loadModel(null)
        val candidates = purlCandidates(model.packages.values.flatten(), "a.id", advisoryIds)
        if (!candidates.isEmpty) {
            val hits = model.technologies.flatMap { technology ->
                val component = model.components[technology.componentId] ?: return@flatMap emptyList()
                if (model.instancesOf(component).isEmpty()) return@flatMap emptyList()
                val purl = purlHitsOf(technology.id, technology.version, model.packages[technology.id].orEmpty(), candidates)
                purl.hits.filterKeys { it !in purl.vulnerabilityIds }.entries
            }
            osvService.materialize(hits.map { it.key }, possibleOnly(hits))
        }
        return persistenceService.executeQueryBatched(
            "select a.vulnerability.id from CvrOsvAdvisoryDO a where a.id in :ids and a.vulnerability is not null",
            Long::class.javaObjectType, "ids", advisoryIds,
        ).toSet()
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
        /** The packages of the technologies with a purl (directly or by their catalogue node). */
        val packages: Map<Long, List<PurlPackage>>,
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
        val packages = active.associate { it.id!! to packagesOf(it.purl, nodes[it.catalogNodeId]) }
        return Model(instances, activeComponents, active, keys.filterValues { it.isNotEmpty() }, packages.filterValues { it.isNotEmpty() })
    }

    private fun packagesOf(purl: String?, node: CvrCatalogNodeDO?): List<PurlPackage> =
        listOfNotNull(OsvPackages.ofPurl(purl), node?.let { OsvPackages.of(it.purlType, it.purlNamespace, it.purlName) }).distinct()

    private fun keysOf(technology: CvrTechnologyDO, node: CvrCatalogNodeDO?): List<Key> =
        keysOf(technology.cpe, technology.vendor, technology.product, node)

    private fun keysOf(cpe: String?, vendor: String?, product: String?, node: CvrCatalogNodeDO?): List<Key> {
        val keys = mutableListOf<Key>()
        cpe?.let {
            val fields = NvdCpeParser.splitCpe(it.trim())
            if (fields.size >= 5 && fields[0] == "cpe" && fields[3] != "*" && fields[4] != "*") {
                keys.add(Key(fields[3].lowercase(), fields[4].lowercase(), true))
            }
        }
        node?.cpeProduct?.takeIf { it.isNotBlank() }?.let {
            keys.add(Key(node.cpeVendor?.takeIf { v -> v.isNotBlank() }?.lowercase(), it.trim().lowercase(), true))
        }
        product?.takeIf { it.isNotBlank() }?.let {
            keys.add(Key(vendor?.takeIf { v -> v.isNotBlank() }?.let(::normalize), normalize(it), false))
        }
        return keys.distinctBy { Triple(it.vendor, it.product, it.sure) }
    }

    // --- The matching ---

    /**
     * The best hit per vulnerability, component and instance. Advisories that match but aren't linked to a vulnerability
     * yet become one (only without [vulnerabilityIds]: in that scope every advisory is linked).
     */
    private fun findHits(model: Model, vulnerabilityIds: Collection<Long>?): Map<Triple<Long, Long, Long>, Hit> {
        val byProduct = model.keys.values.flatten().groupBy { it.product }
        val candidates: Map<String?, List<CvrCpeMatchDO>> = if (byProduct.isEmpty()) {
            emptyMap()
        } else if (vulnerabilityIds != null) {
            // Restricted to the products of the inventory in the query: a large import changes tens of thousands of CVEs.
            persistenceService.executeQueryBatched(
                "select m from CvrCpeMatchDO m where m.vulnerability.deleted = false and m.vulnerability.id in :ids and m.product in :products",
                CvrCpeMatchDO::class.java, "ids", vulnerabilityIds, Pair("products", byProduct.keys), batchSize = ID_BATCH,
            ).groupBy { it.product }
        } else {
            persistenceService.executeQueryBatched(
                "select m from CvrCpeMatchDO m where m.vulnerability.deleted = false and m.product in :ids",
                CvrCpeMatchDO::class.java, "ids", byProduct.keys,
            ).groupBy { it.product }
        }
        val purlCandidates = purlCandidates(model.packages.values.flatten(), "v.id", vulnerabilityIds)

        class TechnologyHits(val component: CvrComponentDO, val instanceIds: List<Long>, val cpe: Map<Long, Hit>, val purl: PurlHits)

        val perTechnology = model.technologies.mapNotNull { technology ->
            val component = model.components[technology.componentId] ?: return@mapNotNull null
            val instanceIds = model.instancesOf(component)
            if (instanceIds.isEmpty()) {
                return@mapNotNull null
            }
            TechnologyHits(
                component, instanceIds,
                hitsOf(technology.id, technology.version, model.keys[technology.id].orEmpty(), candidates),
                purlHitsOf(technology.id, technology.version, model.packages[technology.id].orEmpty(), purlCandidates),
            )
        }
        val vulnerabilityOfKey = perTechnology.flatMap { it.purl.vulnerabilityIds.entries }.associate { it.key to it.value }.toMutableMap()
        val unlinked = perTechnology.flatMap { it.purl.hits.entries }.filter { it.key !in vulnerabilityOfKey }
        if (unlinked.isNotEmpty() && vulnerabilityIds == null) {
            vulnerabilityOfKey.putAll(osvService.materialize(unlinked.map { it.key }, possibleOnly(unlinked)))
        }
        val hits = mutableMapOf<Triple<Long, Long, Long>, Hit>()
        perTechnology.forEach { technology ->
            // The advisories that know the package decide: a possible match by CPE of the same vulnerability is dropped.
            val covered = technology.purl.covered.mapNotNull { vulnerabilityOfKey[it] }.toSet()
            val merged = technology.cpe.filter { (id, hit) -> hit.certainty == CvrFindingCertainty.AFFECTED || id !in covered }.toMutableMap()
            technology.purl.hits.forEach { (key, hit) ->
                val vulnerabilityId = vulnerabilityOfKey[key] ?: return@forEach
                val old = merged[vulnerabilityId]
                if (old == null || (old.certainty == CvrFindingCertainty.POSSIBLE && hit.certainty == CvrFindingCertainty.AFFECTED)) {
                    merged[vulnerabilityId] = hit
                }
            }
            merged.forEach { (vulnerabilityId, hit) ->
                technology.instanceIds.forEach { instanceId ->
                    val triple = Triple(vulnerabilityId, technology.component.id!!, instanceId)
                    val old = hits[triple]
                    if (old == null || (old.certainty == CvrFindingCertainty.POSSIBLE && hit.certainty == CvrFindingCertainty.AFFECTED)) {
                        hits[triple] = hit
                    }
                }
            }
        }
        return hits
    }

    /** The keys of these hits that no technology matched as affected. */
    private fun possibleOnly(hits: Collection<Map.Entry<String, Hit>>): Set<String> {
        val affected = hits.filter { it.value.certainty == CvrFindingCertainty.AFFECTED }.map { it.key }.toSet()
        return hits.map { it.key }.filter { it !in affected }.toSet()
    }

    /**
     * The entries of the purl index for these packages, of advisories not withdrawn; restricted to the [scope] ids of
     * [scopeField] (`v.id`: vulnerabilities, `a.id`: advisories) if given.
     */
    private fun purlCandidates(packages: Collection<PurlPackage>, scopeField: String, scope: Collection<Long>?): PurlCandidates {
        fun load(field: String, names: Set<String>): Map<String, List<PurlCandidate>> {
            if (names.isEmpty()) return emptyMap()
            val select = "select m, a.osvId, a.vulnerabilityKey, v.id from CvrPurlMatchDO m join m.advisory a left join a.vulnerability v " +
                    "where a.withdrawn is null and (v is null or v.deleted = false) and m.$field in "
            val rows = if (scope == null) {
                persistenceService.executeQueryBatched("$select:ids", Array<Any>::class.java, "ids", names)
            } else {
                // Both lists are bound: the names in chunks as well, so that no statement exceeds the limit of parameters.
                names.chunked(ID_BATCH).flatMap { chunk ->
                    persistenceService.executeQueryBatched(
                        "$select:names and $scopeField in :ids", Array<Any>::class.java, "ids", scope, Pair("names", chunk), batchSize = ID_BATCH,
                    )
                }
            }
            return rows.map { PurlCandidate(it[0] as CvrPurlMatchDO, it[1] as String, it[2] as String, it[3] as Long?) }
                .groupBy { if (field == "packageName") it.match.packageName!! else it.match.shortName!! }
        }
        if (scope != null && scope.isEmpty()) return PurlCandidates(emptyMap(), emptyMap())
        val (byShortName, byName) = packages.partition { it.byShortName }
        return PurlCandidates(load("packageName", byName.map { it.name }.toSet()), load("shortName", byShortName.map { it.name }.toSet()))
    }

    /** The hits of a technology with this [version] and these [packages] by the purl index. */
    private fun purlHitsOf(technologyId: Long?, version: String?, packages: List<PurlPackage>, candidates: PurlCandidates): PurlHits {
        val hits = mutableMapOf<String, Hit>()
        val vulnerabilityIds = mutableMapOf<String, Long>()
        val covered = mutableSetOf<String>()
        packages.forEach { pkg ->
            val list = (if (pkg.byShortName) candidates.byShortName[pkg.name] else candidates.byName[pkg.name]).orEmpty()
            list.filter { it.match.ecosystem == pkg.ecosystem }.forEach candidate@{ candidate ->
                candidate.vulnerabilityId?.let { vulnerabilityIds[candidate.key] = it }
                if (!pkg.byShortName) {
                    covered.add(candidate.key)
                }
                val versionCertainty = purlVersionMatch(version, candidate.match) ?: return@candidate
                // Without groupId the artifact may be another one of the same name.
                val certainty = if (pkg.byShortName) CvrFindingCertainty.POSSIBLE else versionCertainty
                val old = hits[candidate.key]
                if (old == null || (old.certainty == CvrFindingCertainty.POSSIBLE && certainty == CvrFindingCertainty.AFFECTED)) {
                    hits[candidate.key] = Hit(technologyId, certainty, describe(candidate.osvId, candidate.match))
                }
            }
        }
        return PurlHits(hits, vulnerabilityIds, covered)
    }

    /** The best hit per vulnerability for a technology with this [version] and these [keys]; [candidates] by product. */
    private fun hitsOf(
        technologyId: Long?,
        version: String?,
        keys: List<Key>,
        candidates: Map<String?, List<CvrCpeMatchDO>>,
    ): Map<Long, Hit> {
        val hits = mutableMapOf<Long, Hit>()
        keys.forEach { key ->
            candidates[key.product]?.forEach candidate@{ match ->
                // By name the vendor may be spelled otherwise ("Apache Software Foundation" vs "apache"): a
                // match by name is only possible anyway.
                if (key.sure && key.vendor != null && key.vendor != match.vendor) {
                    return@candidate
                }
                val versionCertainty = versionMatch(version, match) ?: return@candidate
                val certainty = if (key.sure && versionCertainty == CvrFindingCertainty.AFFECTED) {
                    CvrFindingCertainty.AFFECTED
                } else {
                    CvrFindingCertainty.POSSIBLE
                }
                val vulnerabilityId = match.vulnerability!!.id!!
                val old = hits[vulnerabilityId]
                if (old == null || (old.certainty == CvrFindingCertainty.POSSIBLE && certainty == CvrFindingCertainty.AFFECTED)) {
                    hits[vulnerabilityId] = Hit(technologyId, certainty, describe(match))
                }
            }
        }
        return hits
    }

    // --- Libraries outside the inventory (ad-hoc check) ---

    /** A library that is not stored: what [keysOf] and [packagesOf] need of a technology, and its version. */
    internal class Query(
        val cpe: String?,
        val vendor: String?,
        val product: String?,
        val version: String?,
        val node: CvrCatalogNodeDO?,
        val purl: String? = null,
    )

    /**
     * What a library matched: the vulnerability, how sure and the entry that did. [vulnerabilityId] is null for
     * advisories not linked to a vulnerability yet (an ad-hoc check stores nothing); [vulnerabilityKey] names them.
     */
    internal class QueryHit(val vulnerabilityId: Long?, val vulnerabilityKey: String?, val certainty: CvrFindingCertainty, val matched: String)

    /**
     * The libraries against all vulnerabilities of the index, nothing stored: per library (same order) the best hit
     * per vulnerability, with the rules of the inventory. The candidates are loaded for the products of these
     * libraries only, so a caller passes them in blocks.
     */
    internal fun match(queries: List<Query>): List<List<QueryHit>> {
        val keys = queries.map { keysOf(it.cpe, it.vendor, it.product, it.node) }
        val packages = queries.map { packagesOf(it.purl, it.node) }
        val products = keys.flatten().map { it.product }.toSet()
        if (products.isEmpty() && packages.all { it.isEmpty() }) {
            return queries.map { emptyList() }
        }
        val candidates = persistenceService.executeQueryBatched(
            "select m from CvrCpeMatchDO m where m.vulnerability.deleted = false and m.product in :ids",
            CvrCpeMatchDO::class.java, "ids", products,
        ).groupBy { it.product }
        val purlCandidates = purlCandidates(packages.flatten(), "v.id", null)
        return queries.mapIndexed { index, query ->
            val purl = purlHitsOf(null, query.version, packages[index], purlCandidates)
            val covered = purl.covered.mapNotNull { purl.vulnerabilityIds[it] }.toSet()
            val byCpe = hitsOf(null, query.version, keys[index], candidates)
                .filter { (id, hit) -> hit.certainty == CvrFindingCertainty.AFFECTED || id !in covered }
            val linked = purl.hits.filterKeys { it in purl.vulnerabilityIds }.mapKeys { purl.vulnerabilityIds.getValue(it.key) }
            // One hit per vulnerability, the surer one.
            (byCpe.keys + linked.keys).map { id ->
                val hit = listOfNotNull(byCpe[id], linked[id]).minBy { it.certainty.ordinal }
                QueryHit(id, null, hit.certainty, hit.matched)
            } + purl.hits.filterKeys { it !in purl.vulnerabilityIds }.map { (key, hit) -> QueryHit(null, key, hit.certainty, hit.matched) }
        }
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

        /**
         * Whether [version] is covered by the entry of the purl index: null no, AFFECTED yes, POSSIBLE if the version is
         * unknown (MAT-03). The versions are compared by [VersionComparator] after [normalizeVersion].
         */
        internal fun purlVersionMatch(version: String?, match: CvrPurlMatchDO): CvrFindingCertainty? {
            if (version.isNullOrBlank()) {
                return CvrFindingCertainty.POSSIBLE
            }
            val ecosystem = match.ecosystem
            val v = normalizeVersion(version, ecosystem)
            fun cmp(other: String) = VersionComparator.compare(v, normalizeVersion(other, ecosystem))
            val covered = match.exactVersion?.let { cmp(it) == 0 }
                ?: ((match.introduced?.let { cmp(it) >= 0 } ?: true) &&
                        (match.fixed?.let { cmp(it) < 0 } ?: true) &&
                        (match.lastAffected?.let { cmp(it) <= 0 } ?: true))
            return if (covered) CvrFindingCertainty.AFFECTED else null
        }

        /**
         * A version as [VersionComparator] orders it right: without build metadata (`+…`); Maven without the qualifier
         * of a release (`5.3.0.RELEASE`, `2.6.0.Final` as `5.3.0`, `2.6.0`); PyPI with a post release as a further
         * number (`1.0.post1` as `1.0.0.1`: after `1.0`, before `1.0.1`).
         */
        internal fun normalizeVersion(version: String, ecosystem: String?): String {
            val v = version.trim().substringBefore('+')
            return when (ecosystem) {
                "Maven" -> v.replace(MAVEN_RELEASE, "")
                "PyPI" -> v.replace(PYPI_POST, ".0.$1")
                else -> v
            }
        }

        private val MAVEN_RELEASE = Regex("""[.\-](final|release|ga)$""", RegexOption.IGNORE_CASE)
        private val PYPI_POST = Regex("""[.\-_]?post(\d+)$""", RegexOption.IGNORE_CASE)

        /** The advisory, the package and its range, e.g. `GHSA-… Maven com.fasterxml.jackson.core:jackson-databind (>= 2.0.0, < 2.12.7.1)`. */
        private fun describe(osvId: String, match: CvrPurlMatchDO): String {
            val range = match.exactVersion?.let { listOf("= $it") } ?: listOfNotNull(
                match.introduced?.let { ">= $it" }, match.fixed?.let { "< $it" }, match.lastAffected?.let { "<= $it" },
            )
            val versions = if (range.isEmpty()) "all versions" else range.joinToString(", ")
            return "$osvId ${match.ecosystem} ${match.packageName} ($versions)".take(1000)
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
