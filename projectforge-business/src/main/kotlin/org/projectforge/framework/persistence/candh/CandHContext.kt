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

import mu.KotlinLogging
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.PropertyOpType

private val log = KotlinLogging.logger {}

class CandHContext constructor(
    /**
     * Needed for initializing the history context with the history entry.
     */
    val entity: BaseDO<*>,
    var currentCopyStatus: EntityCopyStatus = EntityCopyStatus.NONE,
    /**
     * If given, history entries are created.
     */
    entityOpType: EntityOpType? = null,
) {
    /**
     * The history entries created by this context.
     * Only given after calling [preparedHistoryEntries].
     */
    var historyEntries: List<HistoryEntryDO>? = null
        private set

    fun preparedHistoryEntries(mergedObj: BaseDO<*>, destObj: BaseDO<*>): List<HistoryEntryDO>? {
        return historyContext?.getPreparedHistoryEntries(mergedObj = mergedObj, destObj = destObj).also { historyEntries = it }
    }

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


    internal fun addHistoryEntryWrapper(
        entity: BaseDO<*>,
        entityOpType: EntityOpType = EntityOpType.Update,
    ): CandHHistoryEntryWrapper? {
        return historyContext?.addHistoryEntryWrapper(
            entity = entity,
            entityOpType = entityOpType,
        )
    }

    fun combine(status: EntityCopyStatus): EntityCopyStatus {
        val newStatus = currentCopyStatus.combine(status)
        if (currentCopyStatus != newStatus) {
            log.debug { "Status changed from $currentCopyStatus to $newStatus" }
        }
        currentCopyStatus = newStatus
        return currentCopyStatus
    }

    fun clone(): CandHContext {
        return CandHContext(entity, currentCopyStatus, entityOpType = historyContext?.entityOpType)
    }
}
