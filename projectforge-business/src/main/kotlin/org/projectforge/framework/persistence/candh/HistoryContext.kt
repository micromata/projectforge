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

import org.projectforge.framework.persistence.history.PfHistoryAttrDO
import org.projectforge.framework.persistence.history.PropertyOpType
import kotlin.reflect.KClass

internal class HistoryContext {
    /*private val masterStack = mutableListOf<PfHistoryMasterDO>()

    private val currentMaster: PfHistoryMasterDO
        get() = masterStack.last()

    /**
     * Push a new master of type [PfHistoryMasterDO] to the stack. Don't forget to call [popHistoryMaster] when you're done.
     */
    fun pushHistoryMaster(
        entity: IdObject<Long>,
        entityOpType: EntityOpType = EntityOpType.Update,
    ) {
        masterStack.add(PfHistoryMasterDO.create(entity = entity, entityOpType = entityOpType))
    }

    /**
     * Pop the last master from the stack. Throws an exception if the stack is empty.
     */
    fun popHistoryMaster(): PfHistoryMasterDO {
        return masterStack.removeAt(masterStack.size - 1)
    }*/

    internal val entries = mutableListOf<PfHistoryAttrDO>()

    fun add(propertyContext: PropertyContext<*>, optype: PropertyOpType) {
        propertyContext.apply {
            add(
                propertyTypeClass = (property.returnType.classifier as KClass<*>).java.name,
                optype = optype,
                oldValue = destPropertyValue?.toString(),
                value = srcPropertyValue?.toString(),
                propertyName = propertyName,
            )
        }
    }

    fun add(
        propertyTypeClass: String?,
        optype: PropertyOpType,
        oldValue: String?,
        value: String?,
        propertyName: String?,
    ) {
        entries.add(
            PfHistoryAttrDO.create(
                propertyTypeClass = propertyTypeClass,
                optype = optype,
                oldValue = oldValue,
                value = value,
                propertyName = propertyName,
            )
        )
    }
}
