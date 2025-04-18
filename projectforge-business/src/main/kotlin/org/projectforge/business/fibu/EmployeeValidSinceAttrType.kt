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

package org.projectforge.business.fibu

import org.projectforge.common.i18n.I18nEnum

enum class EmployeeValidSinceAttrType(override val i18nKey: String) : I18nEnum {
    ANNUAL_LEAVE("fibu.employee.urlaubstage"),
    STATUS("fibu.employee.status"),
    WEEKLY_HOURS("fibu.employee.wochenstunden");

    companion object {
        fun safeValueOf(name: String?): EmployeeValidSinceAttrType? {
            name ?: return null
            return EmployeeValidSinceAttrType.entries.firstOrNull { it.name == name }
        }
    }
}
