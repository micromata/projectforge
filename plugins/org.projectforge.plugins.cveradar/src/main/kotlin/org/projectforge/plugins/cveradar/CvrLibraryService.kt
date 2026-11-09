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

import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.plugins.cveradar.model.CvrCatalogNodeDO
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.model.CvrOrigin
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.LibraryChange
import org.projectforge.plugins.cveradar.rest.LibraryDiff
import org.projectforge.plugins.cveradar.rest.LibraryFileView
import org.projectforge.plugins.cveradar.rest.LibraryLine
import org.projectforge.plugins.cveradar.rest.LibraryRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * The libraries of a component read from lists (INV-14, INV-26): SBOMs, the `package-lock.json` of npm, the output of
 * Maven and of Gradle, single or in a zip ([LibraryArchive]). Several lists are merged; by default they only add to what
 * an earlier upload brought, with `replace` the libraries missing in all of them are removed. Technologies entered by
 * hand are never touched. The caller checks the access to the component (see [CvrInventoryService]).
 */
@Service
class CvrLibraryService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var technologyDao: CvrTechnologyDao

    fun preview(component: CvrComponentDO, request: LibraryRequest): LibraryDiff =
        plan(component, request, Catalog(catalogNodes())).diff()

    /** How far [apply] is: the libraries stored and removed so far, of how many. */
    class Progress(val stored: Int, val toStore: Int, val removed: Int, val toRemove: Int)

    /**
     * In one transaction: a failure leaves the component as it was. [progress] is told after every library stored or
     * removed; an exception thrown by it (a cancelled job) rolls everything back.
     */
    fun apply(
        component: CvrComponentDO,
        request: LibraryRequest,
        progress: (Progress) -> Unit = {},
    ): LibraryDiff = persistenceService.runInTransaction { context ->
        val catalog = Catalog(catalogNodes())
        val plan = plan(component, request, catalog)
        var stored = 0
        var removed = 0
        fun report() = progress(Progress(stored, plan.added.size, removed, plan.removed.size))
        report()
        plan.added.forEach { (entry, origin) ->
            val node = catalog.nodeOf(entry)
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
                    it.source = plan.sources[storedPurl(entry)]
                },
                checkAccess = false,
            )
            stored++
            report()
        }
        // Where the known ones were found this time; derived from the upload like the date below, so without history.
        plan.unchanged.forEach { technology ->
            val source = plan.sources[technology.purl]
            if (source != technology.source) {
                context.em.createQuery("update CvrTechnologyDO t set t.source = :source where t.id = :id")
                    .setParameter("source", source).setParameter("id", technology.id).executeUpdate()
            }
        }
        plan.removed.forEach {
            technologyDao.markAsDeleted(technologyDao.find(it.id, checkAccess = false)!!, checkAccess = false)
            removed++
            report()
        }
        // Also if nothing changed: the list is up to date then. The loaded component must not write the old day back.
        component.librariesDate = LocalDate.now()
        context.em.createQuery("update CvrComponentDO c set c.librariesDate = :date where c.id = :id")
            .setParameter("date", LocalDate.now()).setParameter("id", component.id).executeUpdate()
        plan.diff()
    }

    private class Plan(
        val files: List<LibraryFileView>,
        val added: List<Pair<LibraryEntry, CvrOrigin>>,
        val removed: List<CvrTechnologyDO>,
        /** The stored libraries the files list again. */
        val unchanged: List<CvrTechnologyDO>,
        /** Where each library of the files was found ([CvrTechnologyDO.source]), by stored purl. */
        val sources: Map<String, String>,
    ) {
        val withoutGroupId get() = added.count { it.first.group == null }

        fun diff(): LibraryDiff {
            val removedLines = removed.map { tech ->
                LibraryListParser.fromPurl(tech.purl)?.let { line(it) }
                    ?: LibraryLine(tech.ecosystem ?: "", null, tech.product ?: "", tech.version ?: "")
            }
            // The same library in another version is a change; only where both the stored and the new one are unambiguous.
            val storedByName = removedLines.groupBy { Triple(it.ecosystem, it.group, it.name) }.filterValues { it.size == 1 }
            val addedCountByName = added.groupingBy { Triple(it.first.ecosystem, it.first.group, it.first.name) }.eachCount()
            val changes = added.mapNotNull { (entry, _) ->
                val key = Triple(entry.ecosystem, entry.group, entry.name)
                if (addedCountByName[key] != 1) null
                else storedByName[key]?.single()?.let { LibraryChange(line(entry), it.version) }
            }
            val changedNames = changes.map { Triple(it.line.ecosystem, it.line.group, it.line.name) }.toSet()
            val newOnes = added.filter { Triple(it.first.ecosystem, it.first.group, it.first.name) !in changedNames }
            val goneOnes = removedLines.filter { Triple(it.ecosystem, it.group, it.name) !in changedNames }
            return LibraryDiff(
                files = files,
                added = newOnes.take(MAX_LINES).map { line(it.first) },
                addedCount = newOnes.size,
                removed = goneOnes.take(MAX_LINES),
                removedCount = goneOnes.size,
                changed = changes.take(MAX_LINES),
                changedCount = changes.size,
                unchangedCount = unchanged.distinctBy { it.purl }.size,
                withoutGroupIdCount = withoutGroupId,
            )
        }

        private fun line(entry: LibraryEntry) = LibraryLine(entry.ecosystem, entry.group, entry.name, entry.version)
    }

    private fun plan(component: CvrComponentDO, request: LibraryRequest, catalog: Catalog): Plan {
        if (request.files.size > LibraryListParser.MAX_FILES) {
            throw UserException(LibraryListParser.ERROR_TOO_MANY_FILES, LibraryListParser.MAX_FILES.toString())
        }
        val files = LibraryArchive.expand(request.files.map { Triple((it.name ?: "").take(255), it.text, it.data) })
        val parsed = LibraryListParser.parseAll(files.map { (name, text) -> name.take(255) to text })
        val wanted = LinkedHashMap<String, Pair<LibraryEntry, CvrOrigin>>()
        val sources = mutableMapOf<String, MutableList<String>>()
        parsed.forEach { file ->
            val origin = originOf(file.format)
            // Keyed like the stored purl, so a very long one isn't added again on every upload.
            file.entries.map { resolveGroup(it, catalog) }.forEach { entry ->
                val purl = storedPurl(entry)
                wanted.putIfAbsent(purl, entry to origin)
                sources.getOrPut(purl) { mutableListOf() }.add(file.name + (entry.location?.let { ": $it" } ?: ""))
            }
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
            unchanged = existing.filter { it.purl in wanted },
            sources = sources.mapValues { (_, list) -> list.distinct().joinToString("; ").take(1000) },
        )
    }

    /**
     * A jar name has no groupId; it is taken from the catalogue where exactly one library node has this name
     * (nothing is looked up elsewhere).
     */
    internal fun resolveGroup(entry: LibraryEntry, catalog: Catalog): LibraryEntry {
        if (entry.group != null) return entry
        val node = catalog.byName[entry.ecosystem to entry.name]?.filter { it.purlNamespace != null }?.singleOrNull()
            ?: return entry
        return entry.copy(group = node.purlNamespace)
    }

    private fun storedPurl(entry: LibraryEntry) = entry.purl.take(500)

    /** The library nodes of the catalogue by purl type, namespace and name, and by purl type and name. */
    internal class Catalog(nodes: List<CvrCatalogNodeDO>) {
        val byPurl = nodes.associateBy { Triple(it.purlType!!, it.purlNamespace?.replace("%40", "@"), it.purlName!!) }
        val byName = nodes.groupBy { it.purlType!! to it.purlName!! }

        /** The node of a library: by its purl, else (without a groupId, a jar name) by its name where that is unique. */
        fun nodeOf(entry: LibraryEntry): CvrCatalogNodeDO? =
            byPurl[Triple(entry.ecosystem, entry.group?.replace("%40", "@"), entry.name)]
                ?: if (entry.group == null) byName[entry.ecosystem to entry.name]?.singleOrNull() else null
    }

    internal fun catalog() = Catalog(catalogNodes())

    private fun catalogNodes(): List<CvrCatalogNodeDO> =
        persistenceService.executeQuery(
            "from CvrCatalogNodeDO e where e.deleted = false and e.purlName is not null and e.purlType is not null",
            CvrCatalogNodeDO::class.java,
        )

    companion object {
        private const val MAX_LINES = 200

        /** Where the libraries of a list come from, by its format. */
        internal fun originOf(format: LibraryFormat?) = when (format) {
            LibraryFormat.CYCLONEDX, LibraryFormat.SPDX -> CvrOrigin.SBOM
            LibraryFormat.NPM, LibraryFormat.YARN, LibraryFormat.PNPM, LibraryFormat.PIP, LibraryFormat.GO, LibraryFormat.CARGO,
            LibraryFormat.COMPOSER -> CvrOrigin.LOCKFILE
            LibraryFormat.DEB, LibraryFormat.APK, LibraryFormat.RPM -> CvrOrigin.PACKAGE_LIST
            LibraryFormat.JAR -> CvrOrigin.JAR_LIST
            else -> CvrOrigin.BUILD_LIST
        }
    }
}
