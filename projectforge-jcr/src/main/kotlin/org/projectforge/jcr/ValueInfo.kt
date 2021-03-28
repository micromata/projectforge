/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.jcr

import java.math.BigDecimal
import java.util.*
import javax.jcr.Value

/**
 * For information.
 */
class ValueInfo() {
    internal constructor(value: Value?): this() {
        value ?: return
        type = PropertyTypeEnum.convert(value.type)
        when (type) {
            PropertyTypeEnum.BOOLEAN -> boolean = value.boolean
            PropertyTypeEnum.STRING -> string = value.string
            PropertyTypeEnum.DATE -> date = value.date
            PropertyTypeEnum.DECIMAL -> decimal = value.decimal
            PropertyTypeEnum.DOUBLE -> double = value.double
            PropertyTypeEnum.LONG -> long = value.long
            PropertyTypeEnum.BINARY -> {}
            else -> string = value.string
        }
    }

    var type: PropertyTypeEnum? = null
    var boolean: Boolean? = null
    var string: String? = null
    var date: Calendar? = null
    var decimal: BigDecimal? = null
    var double: Double? = null
    var long: Long? = null

    override fun toString(): String {
        return when (type) {
            PropertyTypeEnum.BOOLEAN -> "$boolean"
            PropertyTypeEnum.DATE -> "$date"
            PropertyTypeEnum.DECIMAL -> "$decimal"
            PropertyTypeEnum.DOUBLE -> "$double"
            PropertyTypeEnum.LONG -> "$long"
            PropertyTypeEnum.BINARY -> "<binary>"
            else -> "$string"
        }
    }
}
