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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrCatalogEdgeDO
import org.projectforge.plugins.cveradar.model.CvrCatalogKind
import org.projectforge.plugins.cveradar.model.CvrCatalogNodeDO
import org.projectforge.plugins.cveradar.model.CvrCatalogOrigin
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.projectforge.plugins.cveradar.rest.CatalogNodeRequest
import org.projectforge.plugins.cveradar.rest.CatalogRef
import org.projectforge.plugins.cveradar.rest.CatalogRow
import org.projectforge.plugins.cveradar.rest.CatalogTree
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The technology catalogue (INV-28): ecosystems, products/frameworks and libraries as a graph shown as a tree. Everybody
 * logged in reads it and picks from it; the security team maintains it. The ecosystems of the OSV data are created
 * when the plugin starts ([ensureEcosystems]). Ids and parents always come from the database, never from the request.
 */
@Service
class CvrCatalogService {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var edgeDao: CvrCatalogEdgeDao

    @Autowired
    private lateinit var nodeDao: CvrCatalogNodeDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var technologyDao: CvrTechnologyDao

    @Autowired
    private lateinit var matchingService: CvrMatchingService

    /** The catalogue as flat rows in tree order: roots first, ecosystems before the rest, then by name. */
    fun tree(user: PFUserDO): CatalogTree {
        val model = load()
        val usage = model.technologies.groupingBy { it.catalogNodeId }.eachCount()
        val rows = mutableListOf<CatalogRow>()
        fun add(node: CvrCatalogNodeDO, parentKey: String?, path: List<Long>) {
            val key = (path + node.id!!).joinToString("/") { "N$it" }
            rows.add(row(node, key, parentKey, model, usage[node.id] ?: 0))
            model.children(node.id!!).filter { it.id !in path && it.id != node.id }.forEach { add(it, key, path + node.id!!) }
        }
        model.nodes.values.filter { model.parentIds(it.id!!).isEmpty() }.sortedWith(ORDER).forEach { add(it, null, emptyList()) }
        return CatalogTree(rows, access.isSecurityTeam(user))
    }

    /** Nodes whose name, alias, CPE or purl contains the term, for the picker of a technology. */
    fun autosearch(term: String?, maxResults: Int): List<CatalogRef> {
        val model = load()
        val needle = term?.trim()?.lowercase().orEmpty()
        return model.nodes.values
            .filter { needle.isEmpty() || haystack(it).contains(needle) }
            .sortedWith(ORDER)
            .take(maxResults.coerceIn(1, MAX_RESULTS))
            .map { ref(it, model) }
    }

    // --- Maintenance (security team) ---

    fun create(user: PFUserDO, request: CatalogNodeRequest): Long = persistenceService.runInTransaction {
        access.checkSecurityTeam(user)
        val model = load()
        val node = CvrCatalogNodeDO().also { it.origin = CvrCatalogOrigin.MANUAL }
        val parents = checkedParents(request, model, null)
        apply(node, request, model)
        val id = nodeDao.insert(node, checkAccess = false)
        syncEdges(node, parents, emptyList())
        id
    }

    fun update(user: PFUserDO, id: Long, request: CatalogNodeRequest) {
        val componentIds = persistenceService.runInTransaction {
            access.checkSecurityTeam(user)
            val model = load()
            val stored = nodeDao.find(id, checkAccess = false)?.takeIf { !it.deleted } ?: throw UserException(ERROR_NOT_FOUND)
            val parents = checkedParents(request, model, id)
            val cpe = Pair(stored.cpeVendor, stored.cpeProduct)
            apply(stored, request, model)
            nodeDao.update(stored, checkAccess = false)
            syncEdges(stored, parents, model.edgesOfChild(id))
            // The CPE of the node is a key of the matching of its technologies.
            if (cpe == Pair(stored.cpeVendor, stored.cpeProduct)) emptySet() else componentsOf(model, id)
        }
        rematch(componentIds)
    }

    /**
     * Folds the node into [targetId]: the technologies of the inventory and the edges move to the target, name and
     * aliases of the node become aliases of the target, the node is deleted. An ecosystem isn't merged.
     */
    fun merge(user: PFUserDO, id: Long, targetId: Long?) {
        val componentIds = persistenceService.runInTransaction {
            mergeNodes(user, id, targetId)
        }
        rematch(componentIds)
    }

    /** The components whose technologies moved to the target (they now match by its CPE). */
    private fun mergeNodes(user: PFUserDO, id: Long, targetId: Long?): Set<Long> {
        access.checkSecurityTeam(user)
        val model = load()
        val source = model.nodes[id] ?: throw UserException(ERROR_NOT_FOUND)
        val target = model.nodes[targetId] ?: throw UserException(ERROR_NOT_FOUND)
        if (source.id == target.id || source.ecosystemKey != null || model.descendants(source.id!!).contains(target.id)) {
            throw UserException(ERROR_MERGE)
        }
        val storedTarget = nodeDao.find(target.id, checkAccess = false)!!
        storedTarget.aliases = joinAliases(
            listOf(storedTarget.aliases, source.name, source.aliases).flatMap { splitAliases(it) }
                .filter { !it.equals(storedTarget.name, ignoreCase = true) },
        )
        nodeDao.update(storedTarget, checkAccess = false)
        model.technologies.filter { it.catalogNodeId == source.id }.forEach {
            val technology = technologyDao.find(it.id, checkAccess = false)!!
            technology.catalogNode = storedTarget
            technologyDao.update(technology, checkAccess = false)
        }
        val targetDescendants = model.descendants(target.id!!)
        // The children of the source become children of the target, its parents parents of the target (where that
        // makes no cycle).
        model.edgesOfParent(source.id!!).forEach { edge ->
            if (edge.childId != target.id) addEdge(storedTarget, edge.child!!)
            removeEdge(edge)
        }
        model.edgesOfChild(source.id!!).forEach { edge ->
            val parentId = edge.parentId
            if (parentId != target.id && parentId !in targetDescendants) addEdge(edge.parent!!, storedTarget)
            removeEdge(edge)
        }
        nodeDao.markAsDeleted(nodeDao.find(source.id, checkAccess = false)!!, checkAccess = false)
        return componentsOf(model, source.id!!)
    }

    private fun componentsOf(model: Model, nodeId: Long): Set<Long> =
        model.technologies.filter { it.catalogNodeId == nodeId }.mapNotNull { it.componentId }.toSet()

    /** After the transaction; a failure doesn't undo the change of the catalogue. */
    private fun rematch(componentIds: Set<Long>) {
        try {
            matchingService.rematchComponents(componentIds)
        } catch (ex: Exception) {
            log.error(ex) { "Matching after a change of the catalogue failed: ${ex.message}" }
        }
    }

    /** Only a node without children and without technologies referring to it. */
    fun delete(user: PFUserDO, id: Long) = persistenceService.runInTransaction {
        access.checkSecurityTeam(user)
        val model = load()
        val node = model.nodes[id] ?: throw UserException(ERROR_NOT_FOUND)
        if (model.children(id).isNotEmpty() || model.technologies.any { it.catalogNodeId == id }) {
            throw UserException(ERROR_NOT_EMPTY)
        }
        model.edgesOfChild(id).forEach { removeEdge(it) }
        nodeDao.markAsDeleted(nodeDao.find(node.id, checkAccess = false)!!, checkAccess = false)
    }

    /** The node of a technology of the inventory: it has to exist and mustn't be deleted. */
    fun checkedNode(id: Long): CvrCatalogNodeDO =
        nodeDao.find(id, checkAccess = false)?.takeIf { !it.deleted } ?: throw UserException(ERROR_NOT_FOUND)

    /**
     * Creates the ecosystems of INV-23 once. A key that exists (deleted ones too) isn't created again, so a
     * deleted ecosystem stays deleted.
     */
    @Synchronized
    fun ensureEcosystems() {
        val existing = persistenceService.executeQuery("from CvrCatalogNodeDO e", CvrCatalogNodeDO::class.java)
            .mapNotNull { it.ecosystemKey }.toSet()
        ECOSYSTEMS.filter { it.first !in existing }.forEach { (key, name, purlType) ->
            nodeDao.insert(
                CvrCatalogNodeDO().also {
                    it.kind = CvrCatalogKind.ECOSYSTEM
                    it.name = name
                    it.ecosystemKey = key
                    it.purlType = purlType
                    it.origin = CvrCatalogOrigin.OSV
                },
                checkAccess = false,
            )
        }
    }

    // --- Internals ---

    private fun apply(node: CvrCatalogNodeDO, request: CatalogNodeRequest, model: Model) {
        val kind = request.kind ?: throw UserException(ERROR_KIND)
        val ecosystemKey = request.ecosystemKey.clean(50)
        if (kind == CvrCatalogKind.ECOSYSTEM) {
            if (ecosystemKey == null) throw UserException(ERROR_ECOSYSTEM_KEY)
        } else if (ecosystemKey != null) {
            throw UserException(ERROR_ECOSYSTEM_KEY)
        }
        if (node.ecosystemKey != null && node.ecosystemKey != ecosystemKey) {
            // The key belongs to an ecosystem for good (it names the OSV data, and the seed would bring it back).
            throw UserException(ERROR_ECOSYSTEM_KEY)
        }
        if (ecosystemKey != null && ecosystemKey in model.allKeys(except = node.id)) {
            throw UserException(ERROR_ECOSYSTEM_KEY)
        }
        node.kind = kind
        node.name = request.name.clean(255) ?: throw UserException(ERROR_NAME)
        node.aliases = joinAliases(splitAliases(request.aliases).filter { !it.equals(node.name, ignoreCase = true) })
        node.description = request.description.clean(TEXT_LENGTH)
        node.ecosystemKey = ecosystemKey
        node.cpeVendor = request.cpeVendor.clean(100)
        node.cpeProduct = request.cpeProduct.clean(100)
        node.purlType = request.purlType.clean(50)?.lowercase()
        node.purlNamespace = request.purlNamespace.clean(255)
        node.purlName = request.purlName.clean(255)
        if (node.purlName != null && node.purlType == null) throw UserException(ERROR_PURL)
        if (node.purlName != null && model.nodes.values.any {
                it.id != node.id && it.purlType == node.purlType && it.purlNamespace == node.purlNamespace &&
                    it.purlName == node.purlName
            }
        ) {
            throw UserException(ERROR_PURL_EXISTS)
        }
    }

    /** The parents of the request as stored nodes; never the node itself or one of its descendants. */
    private fun checkedParents(request: CatalogNodeRequest, model: Model, id: Long?): List<CvrCatalogNodeDO> {
        val ids = request.parentIds.distinct()
        if (request.kind == CvrCatalogKind.ECOSYSTEM && ids.isNotEmpty()) throw UserException(ERROR_PARENT)
        val forbidden = if (id == null) emptySet() else model.descendants(id) + id
        return ids.map { parentId ->
            if (parentId in forbidden) throw UserException(ERROR_CYCLE)
            nodeDao.find(parentId, checkAccess = false)?.takeIf { !it.deleted } ?: throw UserException(ERROR_NOT_FOUND)
        }
    }

    /** [existing] are the edges to the parents so far (deleted ones included, they are revived when needed). */
    private fun syncEdges(node: CvrCatalogNodeDO, parents: List<CvrCatalogNodeDO>, existing: List<CvrCatalogEdgeDO>) {
        val wanted = parents.map { it.id!! }.toSet()
        existing.filter { it.parentId !in wanted }.forEach { removeEdge(it) }
        parents.forEach { addEdge(it, node) }
    }

    private fun addEdge(parent: CvrCatalogNodeDO, child: CvrCatalogNodeDO) {
        val known = persistenceService.selectSingleResult(
            "from CvrCatalogEdgeDO e where e.parent.id = :parent and e.child.id = :child",
            CvrCatalogEdgeDO::class.java,
            Pair("parent", parent.id),
            Pair("child", child.id),
        )
        if (known == null) {
            edgeDao.insert(CvrCatalogEdgeDO().also { it.parent = parent; it.child = child }, checkAccess = false)
        } else if (known.deleted) {
            edgeDao.undelete(known, checkAccess = false)
        }
    }

    private fun removeEdge(edge: CvrCatalogEdgeDO) {
        val stored = edgeDao.find(edge.id, checkAccess = false) ?: return
        if (!stored.deleted) edgeDao.markAsDeleted(stored, checkAccess = false)
    }

    private fun row(node: CvrCatalogNodeDO, key: String, parentKey: String?, model: Model, usage: Int) = CatalogRow(
        key = key,
        id = node.id!!,
        parentKey = parentKey,
        kind = node.kind!!,
        name = node.name!!,
        aliases = node.aliases,
        description = node.description,
        ecosystemKey = node.ecosystemKey,
        cpeVendor = node.cpeVendor,
        cpeProduct = node.cpeProduct,
        purlType = node.purlType,
        purlNamespace = node.purlNamespace,
        purlName = node.purlName,
        origin = node.origin,
        parents = model.parentIds(node.id!!).mapNotNull { model.nodes[it] }.sortedWith(ORDER)
            .map { CatalogRef(it.id!!, it.name!!) },
        usage = usage,
        lastUpdate = node.lastUpdate,
    )

    private fun ref(node: CvrCatalogNodeDO, model: Model): CatalogRef {
        val ecosystem = model.ecosystemKeyOf(node.id!!)
        val purl = node.purlName?.let { name ->
            "pkg:${node.purlType}/${node.purlNamespace?.let { "$it/" } ?: ""}$name"
        }
        return CatalogRef(
            id = node.id!!,
            displayName = listOfNotNull(node.name, ecosystem?.let { "($it)" }.takeIf { node.kind != CvrCatalogKind.ECOSYSTEM })
                .joinToString(" "),
            kind = node.kind,
            name = node.name,
            vendor = node.cpeVendor,
            ecosystem = if (node.kind == CvrCatalogKind.ECOSYSTEM) node.ecosystemKey else ecosystem,
            cpe = if (node.cpeProduct != null) "cpe:2.3:a:${node.cpeVendor ?: "*"}:${node.cpeProduct}" else null,
            purl = purl,
        )
    }

    private fun haystack(node: CvrCatalogNodeDO) =
        listOfNotNull(node.name, node.aliases, node.cpeVendor, node.cpeProduct, node.purlNamespace, node.purlName)
            .joinToString("\n").lowercase()

    private fun String?.clean(maxLength: Int): String? = this?.trim()?.take(maxLength)?.takeIf { it.isNotEmpty() }

    private fun splitAliases(value: String?): List<String> =
        value?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    private fun joinAliases(values: List<String>): String? =
        values.distinctBy { it.lowercase() }.joinToString("\n").take(TEXT_LENGTH).takeIf { it.isNotEmpty() }

    private fun load(): Model {
        val nodes = persistenceService.executeQuery(
            "from CvrCatalogNodeDO e where e.deleted = false", CvrCatalogNodeDO::class.java,
        ).associateBy { it.id!! }
        val edges = persistenceService.executeQuery(
            "from CvrCatalogEdgeDO e where e.deleted = false", CvrCatalogEdgeDO::class.java,
        ).filter { it.parentId in nodes && it.childId in nodes }
        val technologies = persistenceService.executeQuery(
            "from CvrTechnologyDO t where t.deleted = false and t.catalogNode is not null", CvrTechnologyDO::class.java,
        )
        val allKeys = persistenceService.executeQuery(
            "select e.ecosystemKey from CvrCatalogNodeDO e where e.ecosystemKey is not null", String::class.java,
        )
        return Model(nodes, edges, technologies, allKeys)
    }

    private class Model(
        val nodes: Map<Long, CvrCatalogNodeDO>,
        val edges: List<CvrCatalogEdgeDO>,
        val technologies: List<CvrTechnologyDO>,
        private val keys: List<String>,
    ) {
        private val byParent = edges.groupBy { it.parentId }
        private val byChild = edges.groupBy { it.childId }

        /** The ecosystem keys in use, deleted nodes included (the key stays unique for good), without the node's own. */
        fun allKeys(except: Long?): Set<String> {
            val own = except?.let { nodes[it]?.ecosystemKey }
            return keys.filter { it != own }.toSet()
        }

        fun children(id: Long): List<CvrCatalogNodeDO> =
            byParent[id].orEmpty().mapNotNull { nodes[it.childId] }.sortedWith(ORDER)

        fun parentIds(id: Long): List<Long> = byChild[id].orEmpty().mapNotNull { it.parentId }

        fun edgesOfParent(id: Long): List<CvrCatalogEdgeDO> = byParent[id].orEmpty()

        fun edgesOfChild(id: Long): List<CvrCatalogEdgeDO> = byChild[id].orEmpty()

        /** Everything below the node, however deep (guarded against a cycle in the data). */
        fun descendants(id: Long): Set<Long> {
            val found = mutableSetOf<Long>()
            val todo = ArrayDeque(listOf(id))
            while (todo.isNotEmpty()) {
                byParent[todo.removeFirst()].orEmpty().mapNotNull { it.childId }.filter { found.add(it) }.forEach { todo.add(it) }
            }
            return found
        }

        /** The key of the first ecosystem above the node. */
        fun ecosystemKeyOf(id: Long): String? {
            val seen = mutableSetOf(id)
            val todo = ArrayDeque(parentIds(id))
            while (todo.isNotEmpty()) {
                val next = todo.removeFirst()
                if (!seen.add(next)) continue
                nodes[next]?.ecosystemKey?.let { return it }
                todo.addAll(parentIds(next))
            }
            return null
        }
    }

    companion object {
        private const val TEXT_LENGTH = 4000
        private const val MAX_RESULTS = 100

        private val ORDER = compareBy<CvrCatalogNodeDO>({ it.kind != CvrCatalogKind.ECOSYSTEM }, { it.name?.lowercase() })

        /** OSV ecosystem key, name, purl type (INV-23). */
        private val ECOSYSTEMS = listOf(
            Triple("Maven", "Java/Maven", "maven"),
            Triple("npm", "Node/npm", "npm"),
            Triple("PyPI", "Python/PyPI", "pypi"),
            Triple("NuGet", ".NET/NuGet", "nuget"),
            Triple("Packagist", "PHP/Composer", "composer"),
            Triple("Go", "Go", "golang"),
            Triple("crates.io", "Rust/Cargo", "cargo"),
        )

        const val ERROR_NOT_FOUND = "plugins.cveradar.catalog.error.notFound"
        const val ERROR_NAME = "plugins.cveradar.catalog.error.name"
        const val ERROR_KIND = "plugins.cveradar.catalog.error.kind"
        const val ERROR_ECOSYSTEM_KEY = "plugins.cveradar.catalog.error.ecosystemKey"
        const val ERROR_PARENT = "plugins.cveradar.catalog.error.parent"
        const val ERROR_CYCLE = "plugins.cveradar.catalog.error.cycle"
        const val ERROR_PURL = "plugins.cveradar.catalog.error.purl"
        const val ERROR_PURL_EXISTS = "plugins.cveradar.catalog.error.purlExists"
        const val ERROR_NOT_EMPTY = "plugins.cveradar.catalog.error.notEmpty"
        const val ERROR_MERGE = "plugins.cveradar.catalog.error.merge"
    }
}
