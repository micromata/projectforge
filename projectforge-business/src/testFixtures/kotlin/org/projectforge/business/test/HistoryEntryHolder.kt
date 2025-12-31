/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.test

import org.projectforge.framework.persistence.history.HistoryEntryAttrDO
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.PropertyOpType
import kotlin.reflect.KClass

/**
 * For debugging purposes only. Holds a HistoryEntryDO and the corresponding entity, if available.
 * @return this for chaining.
 */
class HistoryEntryHolder(val entry: HistoryEntryDO, val entity: Any?) {
    fun assertAttr(
        propertyName: String,
        value: String?,
        oldValue: String?,
        opType: PropertyOpType = PropertyOpType.Update,
        propertyTypeClass: KClass<*>? = java.lang.String::class,
        msg: String = "",
    ): HistoryEntryHolder {
        HistoryTester.assertHistoryAttr(entry, propertyName = propertyName, value = value, oldValue = oldValue, opType = opType, propertyTypeClass = propertyTypeClass, msg = msg)
        return this
    }

    /**
     * The attributes of the history entry.
     */
    val attributes: Set<HistoryEntryAttrDO>?
        get() = entry.attributes
}
