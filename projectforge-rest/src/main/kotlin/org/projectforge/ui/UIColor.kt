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

import com.fasterxml.jackson.annotation.JsonProperty

enum class UIColor {
    @JsonProperty("danger")
    DANGER,
    @JsonProperty("dark")
    DARK,
    @JsonProperty("info")
    INFO,
    @JsonProperty("light")
    LIGHT,

    /**
     * Blue text with white background and without border.
     */
    @JsonProperty("link")
    LINK,

    /**
     * Blue
     */
    @JsonProperty("primary")
    PRIMARY,
    @JsonProperty("secondary")
    /**
     * Grey
     */
    SECONDARY,
    @JsonProperty("success")
    SUCCESS,
    @JsonProperty("warning")
    WARNING
}
