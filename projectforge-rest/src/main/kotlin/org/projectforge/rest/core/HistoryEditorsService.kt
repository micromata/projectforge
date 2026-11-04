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

package org.projectforge.rest.core

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.history.WithEditors
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Who changed an entry, read from the history ([org.projectforge.framework.persistence.history.HistoryEntryDO.modifiedBy]):
 * the editors of a list's rows (column) and the entries a user has changed (filter, see [ModifiedByFilterSupport]).
 * "Changed" means created or changed at some point, not only the last change.
 */
private val log = KotlinLogging.logger {}

@Service
class HistoryEditorsService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /** The ids of the entries of [doClass] the user has created or changed. */
    fun entityIdsModifiedBy(doClass: Class<*>, userId: Long): Set<Long> {
        return persistenceService.runReadOnly { context ->
            context.em.createQuery(
                """
                SELECT DISTINCT h.entityId
                FROM HistoryEntryDO h
                WHERE h.entityName = :entityName
                AND h.modifiedBy = :userId
                AND h.entityId IS NOT NULL
                """.trimIndent(),
                Long::class.java,
            )
                .setParameter("entityName", doClass.name)
                .setParameter("userId", userId.toString())
                .resultList
                .toSet()
        }
    }

    /**
     * The names of the users who created or changed the given entries of [doClass], per entry, in the order of their
     * first change. Read in chunks (a list has thousands of entries); users unknown to the cache are left out.
     */
    fun editorsOf(doClass: Class<*>, ids: Collection<Long>): Map<Long, List<String>> {
        if (ids.isEmpty()) return emptyMap()
        // MEASUREMENT (temporary): how much does the editors column cost? Remove once the numbers are known.
        val start = System.nanoTime()
        var chunks = 0
        var historyRows = 0
        val userIds = mutableMapOf<Long, MutableSet<Long>>()
        persistenceService.runReadOnly { context ->
            ids.distinct().chunked(CHUNK_SIZE).forEach { chunk ->
                chunks++
                context.em.createQuery(
                    """
                    SELECT h.entityId, h.modifiedBy
                    FROM HistoryEntryDO h
                    WHERE h.entityName = :entityName
                    AND h.entityId IN :ids
                    AND h.modifiedBy IS NOT NULL
                    ORDER BY h.modifiedAt
                    """.trimIndent(),
                    Array<Any>::class.java,
                )
                    .setParameter("entityName", doClass.name)
                    .setParameter("ids", chunk)
                    .resultList
                    .onEach { historyRows++ }
                    .forEach { row ->
                        val userId = (row[1] as String).toLongOrNull() ?: return@forEach
                        userIds.getOrPut(row[0] as Long) { linkedSetOf() }.add(userId)
                    }
            }
        }
        val queryMs = (System.nanoTime() - start) / 1_000_000
        val result = userIds.mapValues { (_, ids) -> ids.mapNotNull { userGroupCache.getUser(it)?.displayName } }
        log.info {
            "Editors ${doClass.simpleName}: ${ids.size} ids, $chunks chunks, $historyRows history rows, " +
                "${userIds.size} entries with editors, ${(System.nanoTime() - start) / 1_000_000} ms (query $queryMs ms)"
        }
        return result
    }

    /** Sets the editors of the rows with a stored entry (positive id) with one query for all of them. */
    fun <R : WithEditors> fillEditors(doClass: Class<*>, rows: Collection<R>, idOf: (R) -> Long?) {
        val editors = editorsOf(doClass, rows.mapNotNull(idOf).filter { it > 0 })
        rows.forEach { row -> row.editors = idOf(row)?.let { editors[it] } }
    }

    private companion object {
        const val CHUNK_SIZE = 1000
    }
}
