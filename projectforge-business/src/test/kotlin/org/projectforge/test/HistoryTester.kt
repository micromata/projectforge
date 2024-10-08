/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.test

import org.junit.jupiter.api.Assertions
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.jpa.PfPersistenceService

class HistoryTester(
    private val persistenceService: PfPersistenceService,
) {
    private var totalNumberHistoryEntries: Int = 0
    private var totalNumberOfHistoryAttrs: Int = 0

    /**
     * The recent history entries since last check. This is set by [assertNumberOfNewHistoryEntries].
     */
    var entries: List<HistoryEntryWithEntity>? = null

    init {
        reset()
    }

    /**
     * Resets the statistics (by reading current number of history entries).
     */
    fun reset() {
        val count = count()
        totalNumberHistoryEntries = count.first
        totalNumberOfHistoryAttrs = count.second
    }

    /**
     * Get the recent history entries for debugging. The recent entries are the last entries in the database, independent of the
     * entity. The entities are loaded as well if available.
     * You should use [assertNumberOfNewHistoryEntries] instead to check the number of new entries as well as getting the list of the newest entries
     * since last check.
     */
    fun getRecentHistoryEntries(maxResults: Int): List<HistoryEntryWithEntity> {
        val result = mutableListOf<HistoryEntryWithEntity>()
        persistenceService.runInTransaction { context ->
            context.executeQuery(
                "from HistoryEntryDO order by id desc",
                HistoryEntryDO::class.java,
                maxResults = maxResults,
            ).forEach { entry ->
                val entity = entry.entityId?.let {
                    try {
                        val entityClass = Class.forName(entry.entityName)
                        context.selectById(entityClass, it)
                    } catch (ex: Exception) {
                        // Not found, OK.
                        null
                    }
                }
                result.add(HistoryEntryWithEntity(entry, entity))
            }
        }
        return result
    }

    /**
     * @return This for chaining.
     */
    fun assertNumberOfNewHistoryEntries(
        expectedNumberOfNewHistoryEntries: Int,
        expectedNumberOfNewHistoryAttrEntries: Int = 0,
    ): HistoryTester {
        val count = count()
        val numberOfNewHistoryEntries = count.first - totalNumberHistoryEntries
        val numberOfNewHistoryAttrs = count.second - totalNumberOfHistoryAttrs
        val recentHistoryEntries = getRecentHistoryEntries(numberOfNewHistoryEntries)
        // For debugging, it's recommended to set the breakpoint at the following line:
        Assertions.assertEquals(
            expectedNumberOfNewHistoryEntries,
            numberOfNewHistoryEntries,
            "Number of new history entries = $numberOfNewHistoryEntries, attrs = $numberOfNewHistoryAttrs. If it fails, check the last entries by calling entries or set the breakpoint a few lines above."
        )
        Assertions.assertEquals(
            expectedNumberOfNewHistoryAttrEntries,
            numberOfNewHistoryAttrs,
            "Number of history attr entries. If it fails, check the last entries by calling entries or set the breakpoint a few lines above."
        )
        totalNumberHistoryEntries = count.first
        totalNumberOfHistoryAttrs = count.second
        entries = recentHistoryEntries
        return this
    }

    private fun count(): Pair<Int, Int> {
        val countHistoryEntries = persistenceService.selectSingleResult(
            "select count(*) from HistoryEntryDO",
            Long::class.java,
        )
        val countAttrEntries = persistenceService.selectSingleResult(
            "select count(*) from HistoryEntryAttrDO",
            Long::class.java,
        )
        return Pair(countHistoryEntries!!.toInt(), countAttrEntries!!.toInt())
    }
}
