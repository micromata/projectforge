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

package org.projectforge.framework.persistence.history

class DisplayHistoryEntryAttr {
    var id: Long? = null
    var operationType: PropertyOpType? = null
        set(value) {
            field = value
            operation = HistoryFormatService.translate(value)
        }
    var operation: String? = null
    var propertyName: String? = null
    var displayPropertyName: String? = null
    var oldValue: String? = null
    var newValue: String? = null

    companion object {
        fun create(attr: HistoryEntryAttrDO, context: HistoryLoadContext): DisplayHistoryEntryAttr {
            val entry = context.requiredHistoryEntry
            val entityClass = HistoryValueService.instance.getClass(entry.entityName)
            return DisplayHistoryEntryAttr().also {
                it.id = attr.id
                it.operationType = attr.opType
                it.propertyName = HistoryFormatUtils.getPlainPropertyName(attr)
                it.displayPropertyName = attr.displayPropertyName
                if (it.displayPropertyName == null && entityClass != null) {
                    it.displayPropertyName = HistoryFormatUtils.translatePropertyName(entityClass, attr.propertyName)
                }
                it.oldValue = attr.oldValue
                it.newValue = attr.value
            }
        }
    }
}
