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
import org.projectforge.framework.persistence.history.PfHistoryMasterDO
import org.projectforge.framework.persistence.history.PropertyOpType

/**
 * Wrapper for PfHistoryAttrDO with additional functionalities.
 * The main purpose is to serialize the old and new value late and set it to the attribute. On time of creation
 * the entity id of some objects for old and new value is not known. After the entity is persisted, the id is known and
 * [prepareAndGetAttr] can be called to serialize the old and new value.
 */
internal class CandHHistoryAttrWrapper(
    private var attr: PfHistoryAttrDO,
    private var oldValue: Any?,
    private var newValue: Any?,
) {
    val propertyName: String?
        get() = attr.propertyName

    /**
     * Serialize the old and new value and set it to the attribute.
     * The return value is prepared to be persisted: The master is set and the attribute is added to the master.
     */
    internal fun prepareAndGetAttr(master: PfHistoryMasterDO): PfHistoryAttrDO {
        attr.serializeAndSet(oldValue = oldValue, newValue = newValue)
        attr.master = master
        master.add(attr)
        return attr
    }


    companion object {
        @JvmOverloads
        fun create(
            masterWrapper: CandHHistoryMasterWrapper,
            propertyTypeClass: Class<*>,
            propertyName: String?,
            optype: PropertyOpType,
            oldValue: Any? = null,
            newValue: Any? = null,
        ): CandHHistoryAttrWrapper {
            PfHistoryAttrDO.create(
                propertyTypeClass = propertyTypeClass,
                propertyName = propertyName,
                opType = optype,
            ).let { attr ->
                return CandHHistoryAttrWrapper(attr, oldValue = oldValue, newValue = newValue)
            }
        }
    }
}
