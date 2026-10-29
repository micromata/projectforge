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

import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.plugins.cveradar.model.CvrCatalogNodeDO
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.model.CvrOrigin
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.LibraryDiff
import org.projectforge.plugins.cveradar.rest.LibraryFileView
import org.projectforge.plugins.cveradar.rest.LibraryLine
import org.projectforge.plugins.cveradar.rest.LibraryRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * The libraries of a component read from lists (INV-26): the `package-lock.json` of npm, the output of Maven and of
 * Gradle. Several lists are merged; by default they only add to what an earlier upload brought, with `replace` the
 * libraries missing in all of them are removed. Technologies entered by hand are never touched. The caller checks the
 * access to the component (see [CvrInventoryService]).
 */
@Service
class CvrLibraryService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var technologyDao: CvrTechnologyDao

    fun preview(component: CvrComponentDO, request: LibraryRequest): LibraryDiff =
        plan(component, request, Catalog(catalogNodes())).diff()

    /** In one transaction: a failure leaves the component as it was. */
    fun apply(component: CvrComponentDO, request: LibraryRequest): LibraryDiff = persistenceService.runInTransaction {
        val catalog = Catalog(catalogNodes())
        val plan = plan(component, request, catalog)
        plan.added.forEach { (entry, origin) ->
            val node = catalog.byPurl[Triple(entry.ecosystem, entry.group?.replace("%40", "@"), entry.name)]
                ?: if (entry.group == null) catalog.byName[entry.ecosystem to entry.name]?.singleOrNull() else null
            technologyDao.insert(
                CvrTechnologyDO().also {
                    it.component = component
                    it.kind = CvrTechnologyKind.PRODUCT
                    it.product = entry.product.take(255)
                    it.version = entry.version.take(100)
                    it.ecosystem = entry.ecosystem
                    it.purl = storedPurl(entry)
                    it.catalogNode = node
                    it.vendor = node?.cpeVendor?.take(100)
                    it.origin = origin
                    it.originDate = LocalDate.now()
                },
                checkAccess = false,
            )
        }
        plan.removed.forEach {
            technologyDao.markAsDeleted(technologyDao.find(it.id, checkAccess = false)!!, checkAccess = false)
        }
        plan.diff()
    }

    private class Plan(
        val files: List<LibraryFileView>,
        val added: List<Pair<LibraryEntry, CvrOrigin>>,
        val removed: List<CvrTechnologyDO>,
        val unchanged: Int,
    ) {
        val withoutGroupId get() = added.count { it.first.group == null }

        fun diff() = LibraryDiff(
            files = files,
            added = added.take(MAX_LINES).map { line(it.first) },
            addedCount = added.size,
            removed = removed.take(MAX_LINES).map { tech ->
                LibraryListParser.fromPurl(tech.purl)?.let { line(it) }
                    ?: LibraryLine(tech.ecosystem ?: "", null, tech.product ?: "", tech.version ?: "")
            },
            removedCount = removed.size,
            unchangedCount = unchanged,
            withoutGroupIdCount = withoutGroupId,
        )

        private fun line(entry: LibraryEntry) = LibraryLine(entry.ecosystem, entry.group, entry.name, entry.version)
    }

    private fun plan(component: CvrComponentDO, request: LibraryRequest, catalog: Catalog): Plan {
        val parsed = LibraryListParser.parseAll(request.files.map { (it.name ?: "").take(255) to (it.text ?: "") })
        val wanted = LinkedHashMap<String, Pair<LibraryEntry, CvrOrigin>>()
        parsed.forEach { file ->
            val origin = when (file.format) {
                LibraryFormat.NPM -> CvrOrigin.LOCKFILE
                LibraryFormat.JAR -> CvrOrigin.JAR_LIST
                else -> CvrOrigin.BUILD_LIST
            }
            // Keyed like the stored purl, so a very long one isn't added again on every upload.
            file.entries.map { resolveGroup(it, catalog) }.forEach { wanted.putIfAbsent(storedPurl(it), it to origin) }
        }
        val existing = persistenceService.executeQuery(
            "from CvrTechnologyDO t where t.deleted = false and t.component.id = :component and t.origin in :origins",
            CvrTechnologyDO::class.java,
            Pair("component", component.id),
            Pair("origins", CvrOrigin.LIBRARY_LISTS),
        )
        val known = existing.mapNotNull { it.purl }.toSet()
        return Plan(
            files = parsed.map { LibraryFileView(it.name, it.format, it.entries.size) },
            added = wanted.filterKeys { it !in known }.values.toList(),
            removed = if (request.replace) existing.filter { it.purl !in wanted } else emptyList(),
            unchanged = wanted.keys.count { it in known },
        )
    }

    /**
     * A jar name has no groupId; it is taken from the catalogue where exactly one library node has this name
     * (nothing is looked up elsewhere).
     */
    private fun resolveGroup(entry: LibraryEntry, catalog: Catalog): LibraryEntry {
        if (entry.group != null) return entry
        val node = catalog.byName[entry.ecosystem to entry.name]?.filter { it.purlNamespace != null }?.singleOrNull()
            ?: return entry
        return entry.copy(group = node.purlNamespace)
    }

    private fun storedPurl(entry: LibraryEntry) = entry.purl.take(500)

    /** The library nodes of the catalogue by purl type, namespace and name, and by purl type and name. */
    private class Catalog(nodes: List<CvrCatalogNodeDO>) {
        val byPurl = nodes.associateBy { Triple(it.purlType!!, it.purlNamespace?.replace("%40", "@"), it.purlName!!) }
        val byName = nodes.groupBy { it.purlType!! to it.purlName!! }
    }

    private fun catalogNodes(): List<CvrCatalogNodeDO> =
        persistenceService.executeQuery(
            "from CvrCatalogNodeDO e where e.deleted = false and e.purlName is not null and e.purlType is not null",
            CvrCatalogNodeDO::class.java,
        )

    companion object {
        private const val MAX_LINES = 200
    }
}
