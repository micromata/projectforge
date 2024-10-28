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

import java.math.BigDecimal
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.jvmErasure

/**
 * Used for BigDecimal, ignores the scale on comparison.
 */
class BigDecimalHandler : DefaultHandler() {
    override fun accept(property: KMutableProperty1<*, *>): Boolean {
        return property.returnType.jvmErasure == BigDecimal::class
    }

    override fun propertyValuesEqual(srcValue: Any, destValue: Any): Boolean {
        srcValue as BigDecimal
        destValue as BigDecimal
        return srcValue.compareTo(destValue) == 0
    }
}