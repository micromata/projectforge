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

package org.projectforge.framework.persistence.history

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.springframework.stereotype.Service

/**
 * History entries will be transformed into human-readable formats.
 */
@Service
class FlatHistoryFormatService {
    @JvmOverloads
    fun <O : ExtendedBaseDO<Long>> selectHistoryEntriesAndConvert(
        baseDao: BaseDao<O>,
        item: O,
        checkAccess: Boolean = true,
    ): List<FlatDisplayHistoryEntry> {
        val history = baseDao.selectHistoryEntries(item, checkAccess = checkAccess)
        return convertToFlatDisplayHistoryEntries(baseDao, history)
    }

    /**
     * Only used by Wicket pages:
     * Gets the history entries of the object in flat format.
     * Please note: If user has no access an empty list will be returned.
     */
    fun convertToFlatDisplayHistoryEntries(
        baseDao: BaseDao<*>,
        historyEntries: List<HistoryEntry>
    ): MutableList<FlatDisplayHistoryEntry> {
        val list = mutableListOf<FlatDisplayHistoryEntry>()
        historyEntries.forEach { entry ->
            baseDao.customizeHistoryEntry(entry)
            val displayEntries = convertToFlatDisplayHistoryEntries(entry)
            mergeEntries(list, displayEntries)
        }
        return list
    }

    private fun convertToFlatDisplayHistoryEntries(entry: HistoryEntry): List<FlatDisplayHistoryEntry> {
        entry.attributes.let { attributes ->
            if (attributes.isNullOrEmpty()) {
                return listOf(FlatDisplayHistoryEntry(entry))
            }
            val result = mutableListOf<FlatDisplayHistoryEntry>()
            attributes.forEach { attr ->
                val se = FlatDisplayHistoryEntry(entry, attr)
                se.initialize(FlatDisplayHistoryEntry.Context(entry.entityName, se))
                result.add(se)
            }

            return result
        }
    }

    /**
     * Merges the given entries into the list. Already existing entries with same masterId and attributeId are not added twice.
     */
    private fun mergeEntries(list: MutableList<FlatDisplayHistoryEntry>, entries: List<FlatDisplayHistoryEntry>) {
        for (entry in entries) {
            if (list.none { it.historyEntryId == entry.historyEntryId && it.attributeId == entry.attributeId }) {
                list.add(entry)
            }
        }
    }


}
