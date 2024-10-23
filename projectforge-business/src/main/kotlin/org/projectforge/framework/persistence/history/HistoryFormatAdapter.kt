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

import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * You may register history adapters for customizing convertion of history entries.
 */
open class HistoryFormatAdapter {
    protected val userGroupCache = UserGroupCache.getInstance()

    /**
     * A customized adapter may manipulate all found history entries by modifing, deleting or adding entries.
     * Does nothing at default.
     * @param item Item the history entries are related to.
     * @param entries All found history entries for customization.
     */
    open fun convertEntries(item: Any, entries: MutableList<HistoryFormatService.DisplayHistoryEntryDTO>) {
    }

    open fun convert(
        item: Any, historyEntry: HistoryEntry
    ): HistoryFormatService.DisplayHistoryEntryDTO {
        var user: PFUserDO? = null
        try {
            user = userGroupCache.getUser(historyEntry.modifiedBy?.toLong())
        } catch (e: NumberFormatException) {
            // Ignore error.
        }
        val entryDTO = HistoryFormatService.DisplayHistoryEntryDTO(
            modifiedAt = historyEntry.modifiedAt,
            timeAgo = TimeAgo.getMessage(historyEntry.modifiedAt),
            modifiedByUserId = historyEntry.modifiedBy,
            modifiedByUser = user?.getFullname(),
            operationType = historyEntry.entityOpType,
            operation = HistoryFormatService.translate(historyEntry.entityOpType)
        )
        historyEntry.attributes?.forEach { attr ->
            val dhe = FlatDisplayHistoryEntry(historyEntry, attr)
            val diffEntryDTO = HistoryFormatService.DisplayHistoryDiffEntryDTO(
                operationType = attr.opType,
                operation = HistoryFormatService.translate(attr.opType),
                property = dhe.propertyName,
                oldValue = dhe.oldValue,
                newValue = dhe.newValue
            )
            entryDTO.diffEntries.add(diffEntryDTO)
        }
        return entryDTO
    }
}
