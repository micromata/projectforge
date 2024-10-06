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

import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.PropertyOpType
import kotlin.reflect.KClass

/**
 * Context for handling history entries.
 */
internal class HistoryContext(
    /**
     * Needed for initializing the history context with the first masterWrapper entry, if required.
     */
    val entity: BaseDO<*>,
    val entityOpType: EntityOpType = EntityOpType.Update
) {
    // All created masterWrappers for later processing.
    internal val masterWrappers = mutableListOf<CandHHistoryMasterWrapper>()

    // Stack for the current masterWrapper, proceeded by CandHMaster.
    private val masterWrapperStack = mutableListOf<CandHHistoryMasterWrapper>()

    private val currentMasterWrapper: CandHHistoryMasterWrapper?
        get() = masterWrapperStack.lastOrNull()

    /**
     * Adds history masterWrapper. This will not be removed, even if it has no attributes.
     */
    fun addHistoryMasterWrapper(
        entity: BaseDO<*>,
        entityOpType: EntityOpType = EntityOpType.Update,
    ): CandHHistoryMasterWrapper {
        @Suppress("UNCHECKED_CAST")
        entity as IdObject<Long>
        CandHHistoryMasterWrapper.create(entity = entity, entityOpType = entityOpType).let {
            masterWrappers.add(it)
            return it
        }
    }

    /**
     * Push a new masterWrapper of type [CandHHistoryMasterWrapper] to the stack. Don't forget to call [popHistoryMasterWrapper] when you're done.
     * This masterWrapper will be removed if [popHistoryMasterWrapper] is called, and it has no attributes.
     */
    fun pushHistoryMasterWrapper(
        entity: BaseDO<*>,
        entityOpType: EntityOpType = EntityOpType.Update,
    ): CandHHistoryMasterWrapper {
        @Suppress("UNCHECKED_CAST")
        entity as IdObject<Long>
        CandHHistoryMasterWrapper.create(entity = entity, entityOpType = entityOpType).let {
            masterWrapperStack.add(it)
            masterWrappers.add(it)
            return it
        }
    }

    /**
     * Pop the last masterWrapper from the stack. Throws an exception if the stack is empty.
     * If the masterWrapper has no attributes, it will be removed from the masterWrappers list.
     */
    fun popHistoryMasterWrapper(): CandHHistoryMasterWrapper {
        if (currentMasterWrapper?.attributes.isNullOrEmpty()) {
            // If the masterWrapper has no attributes, we don't need to keep it.
            masterWrappers.remove(currentMasterWrapper)
        }
        return masterWrapperStack.removeAt(masterWrapperStack.size - 1)
    }

    private val currentMasterAttributes: MutableSet<CandHHistoryAttrWrapper>
        get() {
            if (currentMasterWrapper == null) {
                // Now we need to create the first masterWrapper entry.
                pushHistoryMasterWrapper(entity, entityOpType)
            }
            currentMasterWrapper!!.let { masterWrapper ->
                masterWrapper.attributes = masterWrapper.attributes ?: mutableSetOf()
                @Suppress("UNCHECKED_CAST")
                return masterWrapper.attributes!!
            }
        }

    /**
     * Add a new history entry for the given property context. The current masterWrapper will be used.
     */
    fun add(propertyContext: PropertyContext, optype: PropertyOpType) {
        propertyContext.apply {
            val propertyTypeClass = (property.returnType.classifier as KClass<*>).java
            add(
                propertyTypeClass = propertyTypeClass,
                optype = optype,
                oldValue = destPropertyValue,
                newValue = srcPropertyValue,
                propertyName = propertyName,
            )
        }
    }

    /**
     * Add a new history entry for the given property context. The current masterWrapper will be used.
     */
    fun add(
        propertyTypeClass: Class<*>,
        optype: PropertyOpType,
        oldValue: Any?,
        newValue: Any?,
        propertyName: String?,
    ) {
        currentMasterAttributes.add(
            CandHHistoryAttrWrapper.create(
                propertyTypeClass = propertyTypeClass,
                optype = optype,
                oldValue = oldValue,
                newValue = newValue,
                propertyName = propertyName,
                masterWrapper = currentMasterWrapper
            )
        )
    }
}
