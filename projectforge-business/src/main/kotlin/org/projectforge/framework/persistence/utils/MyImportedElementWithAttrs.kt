/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.utils

import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow
import de.micromata.genome.db.jpa.tabattr.api.TimeableService
import de.micromata.merlin.excel.importer.ImportedSheet
import de.micromata.merlin.importer.PropertyDelta
import org.projectforge.export.AttrColumnDescription
import java.io.Serializable
import java.util.*

class MyImportedElementWithAttrs<PK : Serializable?, T : TimeableAttrRow<PK>?, E : EntityWithTimeableAttr<PK, T>?>(
        importedSheet: ImportedSheet<E>,
        row: Int,
        clazz: Class<E>?,
        private val attrDiffProperties: List<AttrColumnDescription>?,
        private val dateToSelectAttrRow: Date,
        private val timeableService: TimeableService,
        vararg diffProperties: String) : MyImportedElement<E>(importedSheet, row, clazz!!, *diffProperties) {

    override fun addAdditionalPropertyDeltas(): Collection<PropertyDelta> {
        @Suppress("UNCHECKED_CAST")
        return if (attrDiffProperties == null) {
            emptyList()
        } else attrDiffProperties.map { colDesc: AttrColumnDescription -> this.createPropertyDelta(colDesc) }
                .filter{it != null } as List<PropertyDelta>
    }

    private fun createPropertyDelta(colDesc: AttrColumnDescription): PropertyDelta? {
        val fieldName = colDesc.groupName + "." + colDesc.propertyName
        val attrRow = timeableService.getAttrRowForSameMonth(value, colDesc.groupName, dateToSelectAttrRow)
        val oldAttrRow = timeableService.getAttrRowForSameMonth(oldValue, colDesc.groupName, dateToSelectAttrRow)
        if (attrRow == null) {
            return null
        }
        val newVal = attrRow.getAttribute(colDesc.propertyName)
        val oldVal = oldAttrRow?.getAttribute(colDesc.propertyName)
        if (newVal == null && oldVal == null) {
            return null
        }
        val type: Class<*> = newVal?.javaClass ?: oldVal?.javaClass ?: return null
        return createPropertyDelta(fieldName, newVal, oldVal, type)
    }
}
