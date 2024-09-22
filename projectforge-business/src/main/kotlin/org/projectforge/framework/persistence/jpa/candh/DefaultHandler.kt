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

package org.projectforge.framework.persistence.jpa.candh

import org.projectforge.framework.persistence.jpa.candh.CandHMaster.setModificationStatusOnChange
import java.io.Serializable
import java.lang.reflect.Field

/**
 * Used for primitive types, String, Integer, LocalDate etc. Simply sets the destFieldValue to srcFieldalue if not equals.
 */
open class DefaultHandler : CandHIHandler {
    override fun accept(field: Field): Boolean {
        return true
    }

    override fun process(fieldContext: FieldContext<*>, context: CandHContext): Boolean {
        var modified = false
        fieldContext.apply {
            if (destFieldValue == null || srcFieldValue == null) {
                if (destFieldValue != srcFieldValue) {
                    modified = true
                }
            } else if (!fieldValuesEqual(srcFieldValue!!, destFieldValue!!)) {
                modified = true
            }
            if (modified) {
                context.debugContext?.add(
                    "$srcClazz.$fieldName",
                    srcVal = srcFieldValue,
                    destVal = destFieldValue,
                    msg = "Field of type ${field.type} modified.",
                )
                context.addHistoryEntry(fieldContext)
                synchronized(field) {
                    val wasAccessible = field.canAccess(dest)
                    try {
                        field.isAccessible = true
                        field[dest] = srcFieldValue
                    } finally {
                        field.isAccessible = wasAccessible
                    }
                }
                setModificationStatusOnChange(context, src, fieldName)
            }
        }
        return true
    }

    open fun fieldValuesEqual(srcFieldValue: Any, destFieldValue: Any): Boolean {
        return srcFieldValue == destFieldValue
    }
}
