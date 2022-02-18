/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.utils

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
enum class RoundUnit {
    /**
     * Round to integers.
     */
    INT,
    /**
     * Round to 0, 0.5, 1, 1.5, ...
     */
    HALF,
    /**
     * Round to 0, 0.25, 0.5, 0.75, 1, ...
     */
    QUARTER,
    /**
     * Round to 0, 0.2, 0.4, 0.6, 0.8, 1, ...
     */
    FIFTH,
    /**
     * Round to 0, 0.1, 0.2, 0.3, 0.4, 0.5, ..., 1, ...
     */
    TENTH
}
