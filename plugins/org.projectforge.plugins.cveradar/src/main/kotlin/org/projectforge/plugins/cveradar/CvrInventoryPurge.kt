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
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.plugins.cveradar.model.CvrAreaDO
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.model.CvrFindingDO
import org.projectforge.plugins.cveradar.model.CvrInstanceDO
import org.projectforge.plugins.cveradar.model.CvrSystemDO
import org.projectforge.plugins.cveradar.rest.InventoryLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Removes a node of the inventory and everything below it from the database, for good (INV-20): the areas, systems,
 * instances and components, their technologies, the findings and the history of all of them. Rows marked as deleted
 * earlier go as well, they would hold the foreign keys otherwise. Bulk statements in one transaction: a bulk import
 * leaves tens of thousands of libraries and findings, too many to delete one by one. The access is checked by the
 * caller ([CvrInventoryService.delete]).
 */
@Service
class CvrInventoryPurge {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /** The ids of what is removed, the areas by depth (the node first). */
    class Scope(
        val areaLevels: List<List<Long>>,
        val systemIds: List<Long>,
        val instanceIds: List<Long>,
        val componentIds: List<Long>,
    )

    fun purge(level: InventoryLevel, id: Long) {
        persistenceService.runInTransaction { context ->
            val scope = scope(context, level, id)
            // Findings of an instance include those of the baseline components (they are per instance).
            val findings = "select f.id from CvrFindingDO f where f.component.id in :ids"
            val findingsOfInstances = "select f.id from CvrFindingDO f where f.instance.id in :ids"
            deleteHistory(context, CvrFindingDO::class.java, findings, scope.componentIds)
            deleteHistory(context, CvrFindingDO::class.java, findingsOfInstances, scope.instanceIds)
            var findingCount = update(context, "delete from CvrFindingDO f where f.component.id in :ids", scope.componentIds)
            findingCount += update(context, "delete from CvrFindingDO f where f.instance.id in :ids", scope.instanceIds)
            val technologyCount =
                update(context, "delete from CvrTechnologyDO t where t.component.id in :ids", scope.componentIds)
            deleteEntities(context, CvrComponentDO::class.java, scope.componentIds)
            deleteEntities(context, CvrInstanceDO::class.java, scope.instanceIds)
            deleteEntities(context, CvrSystemDO::class.java, scope.systemIds)
            // The deepest first: an area is the parent of the ones of the next level.
            scope.areaLevels.reversed().forEach { deleteEntities(context, CvrAreaDO::class.java, it) }
            log.info {
                "CVE-Radar: $level $id deleted with ${scope.areaLevels.sumOf { it.size }} areas, ${scope.systemIds.size} systems, " +
                        "${scope.instanceIds.size} instances, ${scope.componentIds.size} components, " +
                        "$technologyCount technologies and $findingCount findings."
            }
        }
    }

    /** Everything below the node, also the rows marked as deleted. */
    private fun scope(context: PfPersistenceContext, level: InventoryLevel, id: Long): Scope {
        val areaLevels = mutableListOf<List<Long>>()
        if (level == InventoryLevel.AREA) {
            var current = listOf(id)
            while (current.isNotEmpty() && areaLevels.size < MAX_DEPTH) {
                areaLevels.add(current)
                current = ids(context, "select a.id from CvrAreaDO a where a.parent.id in :ids", current)
            }
        }
        val systemIds = when (level) {
            InventoryLevel.AREA -> ids(context, "select s.id from CvrSystemDO s where s.area.id in :ids", areaLevels.flatten())
            InventoryLevel.SYSTEM -> listOf(id)
            else -> emptyList()
        }
        val instanceIds = when (level) {
            InventoryLevel.INSTANCE -> listOf(id)
            else -> ids(context, "select i.id from CvrInstanceDO i where i.system.id in :ids", systemIds)
        }
        val componentIds = when (level) {
            InventoryLevel.COMPONENT -> listOf(id)
            else -> (ids(context, "select c.id from CvrComponentDO c where c.system.id in :ids", systemIds) +
                    ids(context, "select c.id from CvrComponentDO c where c.instance.id in :ids", instanceIds)).distinct()
        }
        return Scope(areaLevels, systemIds, instanceIds, componentIds)
    }

    private fun deleteEntities(context: PfPersistenceContext, clazz: Class<*>, ids: List<Long>) {
        if (ids.isEmpty()) return
        deleteHistory(context, clazz, null, ids)
        update(context, "delete from ${clazz.simpleName} e where e.id in :ids", ids)
    }

    /**
     * The history entries (with their attributes) of the entities of [clazz]: those with the [ids], or, given a
     * [select], those it selects by the [ids] (e.g. the findings of components).
     */
    private fun deleteHistory(context: PfPersistenceContext, clazz: Class<*>, select: String?, ids: List<Long>) {
        val entities = select ?: ":ids"
        val entries = "select h.id from HistoryEntryDO h where h.entityName = :name and h.entityId in ($entities)"
        update(context, "delete from HistoryEntryAttrDO a where a.parent.id in ($entries)", ids, "name" to clazz.name)
        update(context, "delete from HistoryEntryDO h where h.entityName = :name and h.entityId in ($entities)", ids, "name" to clazz.name)
    }

    private fun ids(context: PfPersistenceContext, sql: String, ids: List<Long>): List<Long> =
        ids.chunked(PfPersistenceService.IN_CLAUSE_MAX_BATCH_SIZE).flatMap {
            context.executeQuery(sql, Long::class.javaObjectType, "ids" to it)
        }

    /** Runs [sql] with the parameter `ids` in chunks; the number of rows. */
    private fun update(context: PfPersistenceContext, sql: String, ids: List<Long>, vararg params: Pair<String, Any?>): Int =
        ids.chunked(PfPersistenceService.IN_CLAUSE_MAX_BATCH_SIZE).sumOf {
            context.executeUpdate(sql, "ids" to it, *params)
        }

    companion object {
        private const val MAX_DEPTH = 100
    }
}
