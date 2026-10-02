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

    /**
     * The entries removed from a list property (user/group id lists or entity collections), so clients are able to
     * show only the removed and added entries instead of the whole lists. Null, if the property isn't a list.
     * @see setListValues
     */
    var removedValues: List<String>? = null

    /**
     * The entries added to a list property. See [removedValues].
     */
    var addedValues: List<String>? = null

    /**
     * Sets [removedValues] and [addedValues] as the difference of the given old and new entries of a list property.
     * Leaves both null, if there is no difference (e.g. only the order changed).
     */
    fun setListValues(oldValues: List<String>, newValues: List<String>) {
        val removed = oldValues.filter { it !in newValues }
        val added = newValues.filter { it !in oldValues }
        if (removed.isEmpty() && added.isEmpty()) {
            return
        }
        removedValues = removed
        addedValues = added
    }

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
