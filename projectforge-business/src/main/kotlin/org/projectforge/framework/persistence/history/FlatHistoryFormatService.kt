/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * History entries will be transformed into human-readable formats.
 * This flat format is used by Wicket pages as well as in e-mail notifications (e. g. AuftragDao and TodoDao).
 */
@Service
class FlatHistoryFormatService {
    @Autowired
    private lateinit var historyFormatService: HistoryFormatService
    /**
     * Select the history entries of the given item and convert them into flat format.
     * Please note: If user has no access an empty list will be returned.
     * @param baseDao The DAO of the item.
     * @param item The item.
     * @param checkAccess If true, the access rights of the user will be checked.
     * @return The history entries in flat format.
     * @param O The type of the item.
     * @param Long The type of the primary key.
     * @see BaseDao.loadHistory
     * @see convertToFlatDisplayHistoryEntries
     */
    @JvmOverloads
    fun <O : ExtendedBaseDO<Long>> selectHistoryEntriesAndConvert(
        baseDao: BaseDao<O>,
        item: O,
        checkAccess: Boolean = true,
    ): List<FlatDisplayHistoryEntry> {
        val entries = historyFormatService.selectAsDisplayEntries(baseDao, item, checkAccess = checkAccess)
        return convertToFlatDisplayHistoryEntries(baseDao, entries)
    }

    /**
     * Only used by Wicket pages and e-mail notifications:
     * Gets the history entries of the object in flat format.
     * @param baseDao The DAO of the item.
     * @param historyEntries The history entries.
     * @return The history entries in flat format.
     */
    private fun convertToFlatDisplayHistoryEntries(
        baseDao: BaseDao<*>,
        historyEntries: List<DisplayHistoryEntry>
    ): MutableList<FlatDisplayHistoryEntry> {
        val list = mutableListOf<FlatDisplayHistoryEntry>()
        historyEntries.forEach { entry ->
            val displayEntries = convertToFlatDisplayHistoryEntries(entry)
            mergeEntries(list, displayEntries)
        }
        return list
    }

    private fun convertToFlatDisplayHistoryEntries(entry: DisplayHistoryEntry): List<FlatDisplayHistoryEntry> {
        entry.attributes.let { attributes ->
            if (attributes.isNullOrEmpty()) {
                return listOf(FlatDisplayHistoryEntry.create(entry))
            }
            val result = mutableListOf<FlatDisplayHistoryEntry>()
            attributes.forEach { attr ->
                result.add(FlatDisplayHistoryEntry.create(entry, attr))
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
