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

package org.projectforge.framework.persistence.jpa.candh

import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.history.PropertyOpType

class CandHContext(
    var currentCopyStatus: EntityCopyStatus = EntityCopyStatus.NONE,
    // On top-level the field name and for nested fields the path, such as "address.street".
    var currentObjectPath: String = "",
    createHistory: Boolean = true,
    debug: Boolean = false,
) {
    internal val debugContext = if (debug) DebugContext() else null
    internal val historyContext = if (createHistory) HistoryContext() else null
    internal fun addHistoryEntry(fieldName: String, type: PropertyOpType, newValue: Any?, oldValue: Any?) {
        historyContext?.add(
            path = "${currentObjectPath}$fieldName",
            type = type,
            newValue = newValue,
            oldValue = oldValue
        )
    }

    fun combine(status: EntityCopyStatus): EntityCopyStatus {
        val newStatus = currentCopyStatus.combine(status)
        debugContext?.let {
            if (newStatus != currentCopyStatus) {
                it.add(msg = "Status changed from $currentCopyStatus to $newStatus")
            }
        }
        currentCopyStatus = newStatus
        return currentCopyStatus
    }
}