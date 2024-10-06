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

/**
 * Wrapper for PfHistoryAttrDO with additional functionalities.
 */
internal class CandHHistoryAttrWrapper(var attr: PfHistoryAttrDO) {
    val propertyName: String?
        get() = attr.propertyName

    companion object {
        @JvmOverloads
        fun create(
            propertyTypeClass: Class<*>,
            propertyName: String?,
            optype: PropertyOpType,
            oldValue: Any? = null,
            newValue: Any? = null,
            masterWrapper: CandHHistoryMasterWrapper? = null,
        ): CandHHistoryAttrWrapper {
            PfHistoryAttrDO.create(
                propertyTypeClass,
                propertyName = propertyName,
                optype,
                oldValue = oldValue,
                newValue = newValue,
                master = masterWrapper?.master,
            ).let { attr ->
                return CandHHistoryAttrWrapper(attr)
            }
        }
    }
}
