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

package org.projectforge.rest.dto

/**
 * One configurable right of a user as the user edit page of projectforge-next renders it: its label, whether it
 * is a checkbox or a choice, the choices this user may get (they depend on the assigned groups) and the current
 * value. Built by [org.projectforge.rest.UserRightsHandler.getRightInfos].
 */
class UserRightInfo(
    val rightId: String,
    val label: String,
    /** A checkbox ([UserRightDto.booleanValue]) rather than a choice of [values] ([UserRightDto.value]). */
    val booleanType: Boolean,
    /** The selectable values of a non boolean right, empty for a boolean one. */
    val values: List<Option>,
    val value: String? = null,
    val booleanValue: Boolean? = null,
) {
    class Option(val value: String, val label: String)
}
