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

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import java.util.*

class DisplayHistoryEntry {
    class DiffCount(val type: EntityOpType, val count: Int, val operation: String)

    /**
     * The id of the history entry (pk of database).
     */
    var id: Long? = null
    var modifiedAt: Date? = null

    /**
     * Human-readable time ago (localized).
     */
    var timeAgo: String? = null
    var modifiedByUserId: Long? = null
    var modifiedByUser: String? = null
    var operationType: EntityOpType? = null
    val operation: String
        get() = HistoryFormatService.translate(operationType)
    var userComment: String? = null
    @Suppress("unused") // Used by React frontend.
    @get:JsonProperty
    val diffSummary: List<DiffCount>
        get() {
            var inserted = 0
            var updated = 0
            var deleted = 0
            attributes.forEach { attr ->
                when (attr.operationType) {
                    PropertyOpType.Insert -> inserted++
                    PropertyOpType.Update -> updated++
                    PropertyOpType.Delete -> deleted++
                    else -> {}
                }
            }
            if (inserted == 0 && updated == 0 && deleted == 0) {
                when (operationType) {
                    EntityOpType.Insert -> inserted++
                    EntityOpType.Update -> updated++
                    EntityOpType.Delete -> deleted++
                    else -> {}
                }
            }
            val actions = mutableListOf<DiffCount>()
            if (inserted > 0) {
                actions.add(DiffCount(EntityOpType.Insert, inserted, translate("operation.inserted")))
            }
            if (updated > 0) {
                actions.add(DiffCount(EntityOpType.Update, updated, translate("operation.updated")))
            }
            if (deleted > 0) {
                actions.add(DiffCount(EntityOpType.Delete, deleted, translate("operation.deleted")))
            }
            return actions
        }

    var attributes = mutableListOf<DisplayHistoryEntryAttr>()

    companion object {
        internal fun create(historyEntry: HistoryEntry, context: HistoryLoadContext): DisplayHistoryEntry {
            val modifiedByUser = context.getUser(historyEntry.modifiedBy)
            return DisplayHistoryEntry().also { entry ->
                entry.id = historyEntry.id
                entry.modifiedAt = historyEntry.modifiedAt
                entry.timeAgo = TimeAgo.getMessage(historyEntry.modifiedAt)
                entry.modifiedByUserId = modifiedByUser?.id
                entry.modifiedByUser = modifiedByUser?.getFullname() ?: historyEntry.modifiedBy
                entry.operationType = historyEntry.entityOpType
                entry.userComment = historyEntry.userComment
            }
        }
    }
}
