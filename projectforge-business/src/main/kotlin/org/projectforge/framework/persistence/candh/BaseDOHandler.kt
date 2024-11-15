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

import mu.KotlinLogging
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.candh.CandHMaster.propertyWasModified
import org.projectforge.framework.persistence.history.PropertyOpType
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

private val log = KotlinLogging.logger {}

/**
 * Used for objects of type BaseDO.
 */
class BaseDOHandler : DefaultHandler() {
    override fun accept(property: KMutableProperty1<*, *>): Boolean {
        return property.returnType.jvmErasure.isSubclassOf(BaseDO::class)
    }

    override fun propertyValuesEqual(srcValue: Any, destValue: Any): Boolean {
        return !idModified(srcValue, destValue)
    }

   /* Identical to DefaultHandler:
   override fun process(propertyContext: PropertyContext, context: CandHContext): Boolean {
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
                /*val useSrc = if (srcPropertyValue == null) {
                    null
                } else {
                    context.persistenceService.runReadOnly { ctx ->
                        ctx.selectById(propertyContext.kClass.java, HibernateUtils.getIdentifier(srcPropertyValue))
                    }
                }*/
                context.debugContext?.add(
                    "$kClass.$propertyName",
                    srcVal = srcPropertyValue,
                    destVal = destPropertyValue,
                    msg = "Field of type ${property.returnType.jvmErasure} modified.",
                )
                @Suppress("UNCHECKED_CAST")
                property as KMutableProperty1<BaseDO<*>, Any?>
                property.set(dest, srcPropertyValue)
                propertyWasModified(context, propertyContext, PropertyOpType.Update)
            }
        }
        return true
    }*/

    companion object {
        internal fun idModified(srcObject: Any, destObject: Any): Boolean {
            val srcFieldValueId = HibernateUtils.getIdentifier(srcObject)
            val destFieldValueId = HibernateUtils.getIdentifier(destObject)
            if (srcFieldValueId != null) {
                return srcFieldValueId != destFieldValueId
            }
            return false
        }
    }
}
