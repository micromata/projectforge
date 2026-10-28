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

package org.projectforge.business.availability

import org.projectforge.common.CSVParser
import org.projectforge.common.i18n.I18nEnum
import java.io.StringReader

/**
 * What the shared view of absences (calendar, team calendars) may show: the vacations and the availability entries
 * by their state (see [AvailabilityStatus.kind]).
 */
enum class AbsenceKind(override val i18nKey: String) : I18nEnum {
    VACATION("vacation"),
    NOT_AVAILABLE("availability.status.notAvailable"),
    REMOTE_REACHABLE("availability.status.remoteReachable"),
    REACHABLE_IF_NEEDED("availability.status.reachableIfNeeded");

    companion object {
        /** All kinds, for a filter that doesn't choose (null). */
        fun orAll(kinds: Collection<AbsenceKind>?): Set<AbsenceKind> = kinds?.toSet() ?: entries.toSet()

        /** The kinds stored as comma separated names (e.g. TeamCalDO.includeAbsenceKinds); null or blank: null (all). */
        fun parse(csv: String?): Set<AbsenceKind>? {
            if (csv.isNullOrBlank()) {
                return null
            }
            val names = CSVParser(StringReader(csv)).also { it.setCsvSeparatorChar(',') }.parseLine().orEmpty()
            return names.mapNotNull { name -> entries.find { it.name == name.trim() } }.toSet()
        }

        /** The kinds to store ([parse]): null for none given or all of them, which is the same. */
        fun toCsv(kinds: Collection<AbsenceKind>?): String? {
            if (kinds.isNullOrEmpty() || kinds.toSet().containsAll(entries)) {
                return null
            }
            return entries.filter { it in kinds }.joinToString(",") { it.name }
        }
    }
}
