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

package org.projectforge.common.html

enum class CssClass(val cls: String) {
    ERROR("error"),
    SUCCESS("success"),
    WARNING("warning"),
    BOLD("bold"),

    /**
     * Fixed width for th and td (minimal with, nowrap).
     */
    FIXED_WIDTH_NO_WRAP("fixed-width-no-wrap"),
    /**
     * Expand for th and td (minimal with, nowrap).
     */
    EXPAND("expand"),

    ALIGN_RIGHT("align-right"),
}
