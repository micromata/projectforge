/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that IT will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.ui

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Defined as Fontawesome icons.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class UIIconType(val icon: Array<String>) {
    /**
     * https://fontawesome.com/icons/check?style=solid
     */
    CHECKED(arrayOf("check")),
    /**
     * https://fontawesome.com/icons/times?style=solid
     */
    TIMES(arrayOf("times")),
    /**
     * https://fontawesome.com/icons/times?style=regular
     */
    TIMES_REGULAR(arrayOf("far", "times"))
}
