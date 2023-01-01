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

package org.projectforge.jcr

import javax.jcr.PropertyType

enum class PropertyTypeEnum(val value: Int) {
    BINARY(PropertyType.BINARY),
    BOOLEAN(PropertyType.BOOLEAN),
    DATE(PropertyType.DATE),
    DECIMAL(PropertyType.DECIMAL),
    DOUBLE(PropertyType.DOUBLE),
    LONG(PropertyType.LONG),
    NAME(PropertyType.NAME),
    PATH(PropertyType.PATH),
    REFERENCE(PropertyType.REFERENCE),
    STRING(PropertyType.STRING),
    UNDEFINED(PropertyType.UNDEFINED),
    URI(PropertyType.URI),
    WEAKREFERENCE(PropertyType.WEAKREFERENCE);

    companion object {
        fun convert(type: Int?): PropertyTypeEnum? {
            type ?: return null
            values().forEach {
                if (type == it.value) {
                    return it
                }
            }
            return null
        }
    }
}
