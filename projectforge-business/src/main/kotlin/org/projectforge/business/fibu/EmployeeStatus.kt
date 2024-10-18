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

package org.projectforge.business.fibu

import org.projectforge.common.i18n.I18nEnum
import java.util.*

enum class EmployeeStatus(
    /**
     * The key will be used e. g. for i18n.
     *
     * @return
     */
    val key: String
) : I18nEnum {
    FEST_ANGESTELLTER("festAngestellter"), BEFRISTET_ANGESTELLTER("befristetAngestellter"), FREELANCER("freelancer"), AUSHILFE(
        "aushilfe"
    ),
    STUDENTISCHE_HILFSKRAFT(
        "studentischeHilfskraft"
    ),
    STUD_ABSCHLUSSARBEIT("studentischeAbschlussarbeit"), PRAKTIKANT("praktikant"), AZUBI("azubi");

    override val i18nKey: String
        /**
         * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
         */
        get() = "fibu.employee.status.$key"

    companion object {
        fun safeValueOf(name: String): EmployeeStatus? {
            return EmployeeStatus.entries.firstOrNull { it.name == name }
        }

        fun findByi18nKey(i18nKey: String): EmployeeStatus? {
            return entries.firstOrNull { it.i18nKey == i18nKey }
        }
    }
}
