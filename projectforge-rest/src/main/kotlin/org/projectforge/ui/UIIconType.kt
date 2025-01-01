/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.ui

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Defined as Fontawesome icons. Solid style for default.
 * Check https://fontawesome.com/icons/ for available non-pro icons.
 */
@JsonFormat
enum class UIIconType(@JsonValue val icon: Array<String>) {
    /**
     * https://fontawesome.com/icons/check?style=solid
     */
    CHECKED(arrayOf("fas", "check")),

    /**
     * https://fontawesome.com/icons/info?style=solid
     */
    INFO(arrayOf("fas", "info")),

    /**
     * https://fontawesome.com/icons/paperclip?style=solid
     */
    PAPER_CLIP(arrayOf("fas", "paperclip")),

    /**
     * https://fontawesome.com/icons/star?style=regular
     */
    STAR_REGULAR(arrayOf("far", "star")),

    /**
     * https://fontawesome.com/icons/times?style=solid
     */
    TIMES(arrayOf("fas", "times")),

    /**
     * https://fontawesome.com/icons/user-lock?style=solid
     */
    USER_LOCK(arrayOf("fas", "user-lock")),
}
