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

import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.PropertyOpType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext

/**
 * Wrapper for HistoryEntryDO with additional functionalities.
 * @see CandHHistoryAttrWrapper
 */
internal class CandHHistoryEntryWrapper(private val historyEntry: HistoryEntryDO, private val entity: IdObject<Long>) {
    internal var attributeWrappers: MutableSet<CandHHistoryAttrWrapper>? = null

    fun addAttribute(
        propertyTypeClass: Class<*>,
        propertyName: String?,
        optype: PropertyOpType,
        oldValue: Any? = null,
        newValue: Any? = null,
    ): CandHHistoryAttrWrapper {
        val attr = CandHHistoryAttrWrapper.create(
            propertyTypeClass,
            propertyName = propertyName,
            optype = optype,
            oldValue = oldValue,
            newValue = newValue
        )
        attributeWrappers = attributeWrappers ?: mutableSetOf()
        attributeWrappers!!.add(attr)
        return attr
    }

    /**
     * Will prepare the history entry and all attributes.
     * Calls also [CandHHistoryAttrWrapper.prepareAndGetAttr] for the entry, if entity is [CandHHistoryEntryICustomizer].
     */
    fun prepareAndGetHistoryEntry(): HistoryEntryDO {
        historyEntry.attributes = mutableSetOf()
        attributeWrappers?.forEach { attrWrapper ->
            attrWrapper.prepareAndGetAttr(historyEntry)
        }
        if (entity is CandHHistoryEntryICustomizer) {
            entity.customize(historyEntry)
        }
        return historyEntry
    }

    companion object {
        @JvmOverloads
        fun create(
            entity: IdObject<Long>,
            entityOpType: EntityOpType,
            entityName: String? = HistoryEntryDO.asEntityName(entity),
            modifiedBy: String? = ThreadLocalUserContext.loggedInUserId?.toString(),
        ): CandHHistoryEntryWrapper {
            HistoryEntryDO.create(entity, entityOpType, entityName = entityName, modifiedBy = modifiedBy)
                .let { entry ->
                    return CandHHistoryEntryWrapper(entry, entity)
                }
        }
    }
}
