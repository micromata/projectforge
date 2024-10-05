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
import org.projectforge.framework.persistence.history.*
import kotlin.reflect.KClass

/**
 * Context for handling history entries.
 */
internal class HistoryContext(
    /**
     * Needed for initializing the history context with the first master entry, if required.
     */
    val entity: BaseDO<*>,
    val entityOpType: EntityOpType = EntityOpType.Update
) {
    // All created masterEntries for later processing.
    internal val masterEntries = mutableListOf<PfHistoryMasterDO>()

    // Stack for the current masterEntry, proceeded by CandHMaster.
    private val masterStack = mutableListOf<PfHistoryMasterDO>()

    private val currentMaster: PfHistoryMasterDO?
        get() = masterStack.lastOrNull()

    /**
     * Push a new master of type [PfHistoryMasterDO] to the stack. Don't forget to call [popHistoryMaster] when you're done.
     */
    fun pushHistoryMaster(
        entity: BaseDO<*>,
        entityOpType: EntityOpType = EntityOpType.Update,
    ): PfHistoryMasterDO {
        PfHistoryMasterDO.create(entity = entity, entityOpType = entityOpType).let {
            masterStack.add(it)
            masterEntries.add(it)
            return it
        }
    }

    /**
     * Pop the last master from the stack. Throws an exception if the stack is empty.
     * If the master has no attributes, it will be removed from the masterEntries list.
     */
    fun popHistoryMaster(): PfHistoryMasterDO {
        if (currentMaster?.attributes.isNullOrEmpty()) {
            // If the master has no attributes, we don't need to keep it.
            masterEntries.remove(currentMaster)
        }
        return masterStack.removeAt(masterStack.size - 1)
    }

    private val currentMasterAttributes: MutableSet<PfHistoryAttrDO>
        get() {
            if (currentMaster == null) {
                // Now we need to create the first master entry.
                pushHistoryMaster(entity, entityOpType)
            }
            currentMaster!!.let { master ->
                master.attributes = master.attributes ?: mutableSetOf()
                return master.attributes!!
            }
        }

    /**
     * Add a new history entry for the given property context. The current master will be used.
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
     * Add a new history entry for the given property context. The current master will be used.
     */
    fun add(
        propertyTypeClass: Class<*>,
        optype: PropertyOpType,
        oldValue: Any?,
        newValue: Any?,
        propertyName: String?,
    ) {
        currentMasterAttributes.add(
            PfHistoryAttrDO.create(
                propertyTypeClass = propertyTypeClass,
                optype = optype,
                oldValue = oldValue,
                newValue = newValue,
                propertyName = propertyName,
                master = currentMaster
            )
        )
    }
}
