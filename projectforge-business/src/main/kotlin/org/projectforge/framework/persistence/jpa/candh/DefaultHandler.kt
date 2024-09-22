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

import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.history.PropertyOpType
import org.projectforge.framework.persistence.jpa.candh.CandHMaster.propertyWasModified
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.jvmErasure

/**
 * Used for primitive types, String, Integer, LocalDate etc. Simply sets the destValue to srcValue if not equals.
 */
open class DefaultHandler : CandHIHandler {
    override fun accept(property: KMutableProperty1<*, *>): Boolean {
        return true
    }

    override fun process(propertyContext: PropertyContext<*>, context: CandHContext): Boolean {
        var modified = false
        propertyContext.apply {
            if (destPropertyValue == null || srcPropertyValue == null) {
                if (destPropertyValue != srcPropertyValue) {
                    modified = true
                }
            } else if (!propertyValuesEqual(srcPropertyValue, destPropertyValue)) {
                modified = true
            }
            if (modified) {
                context.debugContext?.add(
                    "$kClass.$propertyName",
                    srcVal = srcPropertyValue,
                    destVal = destPropertyValue,
                    msg = "Field of type ${property.returnType.jvmErasure} modified.",
                )
                context.addHistoryEntry(propertyContext)
                @Suppress("UNCHECKED_CAST")
                property as KMutableProperty1<BaseDO<*>, Any?>
                property.set(dest, srcPropertyValue)
                propertyWasModified(context, propertyContext, PropertyOpType.Update)
            }
        }
        return true
    }

    open fun propertyValuesEqual(srcValue: Any, destValue: Any): Boolean {
        return srcValue == destValue
    }
}
