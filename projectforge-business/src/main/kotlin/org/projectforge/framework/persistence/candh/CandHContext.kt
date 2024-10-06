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

package org.projectforge.framework.persistence.candh

import kotlinx.collections.immutable.toImmutableList
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.PfHistoryMasterDO
import org.projectforge.framework.persistence.history.PropertyOpType

class CandHContext constructor(
    /**
     * Needed for initializing the history context with the master entry.
     */
    val entity: BaseDO<*>,
    var currentCopyStatus: EntityCopyStatus = EntityCopyStatus.NONE,
    debug: Boolean = false,
    /**
     * If given, history entries are created.
     */
    entityOpType: EntityOpType? = null,
) {
    val historyEntries: List<PfHistoryMasterDO>?
        get() = historyContext?.getPreparedMasterEntries()

    internal val debugContext = if (debug) DebugContext() else null

    // Only if entityOpType is given, history entries are created.
    internal val historyContext = if (entityOpType != null) HistoryContext(entity, entityOpType) else null

    internal fun addHistoryEntry(
        propertyTypeClass: Class<*>,
        optype: PropertyOpType = PropertyOpType.Update,
        oldValue: Any?,
        value: Any?,
        propertyName: String?,
    ) {
        historyContext?.add(
            propertyTypeClass = propertyTypeClass,
            optype = optype,
            propertyName = propertyName,
            newValue = value,
            oldValue = oldValue,
        )
    }

    internal fun addHistoryMasterWrapper(
        entity: BaseDO<*>,
        entityOpType: EntityOpType = EntityOpType.Update,
    ) {
        historyContext?.addHistoryMasterWrapper(
            entity = entity,
            entityOpType = entityOpType,
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
