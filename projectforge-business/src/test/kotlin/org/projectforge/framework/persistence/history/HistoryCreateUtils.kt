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

package org.projectforge.framework.persistence.history

import org.projectforge.framework.persistence.api.BaseDO
import kotlin.reflect.KClass

/**
 * Should only be used in tests and if you really know what you are doing.
 */
object HistoryCreateUtils {
    /**
     * Attributes createdAd and createdBy will be set by save(HistoryEntrDO, Collection) method.
     */
    fun createHistoryEntry(entity: BaseDO<Long>, opType: EntityOpType): HistoryEntryDO {
        val entry = HistoryEntryDO()
        entry.entityId = entity.id
        entry.entityName = entity::class.java.name
        entry.entityOpType = opType
        return entry
    }

    /**
     * Creates a new history attr object. The parent [HistoryEntry] will be set by save(HistoryEntryDO, Collection) method.
     */
    fun createAttr(
        propertyClass: KClass<*>,
        propertyName: String,
        value: String?,
        oldValue: String? = null,
    ): HistoryEntryAttrDO {
        return createAttr(propertyClass.java, propertyName = propertyName, value = value, oldValue = oldValue)
    }

    /**
     * Creates a new history attr object. The parent [HistoryEntry] will be set by save(HistoryEntryDO, Collection) method.
     */
    fun createAttr(
        propertyClass: Class<*>,
        propertyName: String,
        value: String?,
        oldValue: String? = null,
    ): HistoryEntryAttrDO {
        return createAttr(propertyClass.name, propertyName = propertyName, value = value, oldValue = oldValue)
    }

    /**
     * Creates a new history attr object. The parent [HistoryEntry] will be set by save(HistoryEntryDO, Collection) method.
     */
    fun createAttr(
        propertyTypeClass: String,
        propertyName: String,
        value: String?,
        oldValue: String? = null,
    ): HistoryEntryAttrDO {
        val attr = HistoryEntryAttrDO()
        attr.propertyTypeClass = propertyTypeClass
        attr.propertyName = propertyName
        attr.value = value
        attr.oldValue = oldValue
        return attr
    }
}
