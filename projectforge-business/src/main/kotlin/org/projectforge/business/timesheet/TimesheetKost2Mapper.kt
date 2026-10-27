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

package org.projectforge.business.timesheet

import org.projectforge.business.fibu.kost.Kost2DO

/**
 * Maps the cost unit (kost2) of a time sheet to the cost unit of the same type (kost2Art, the last two digits) among
 * the ones of another task or project, e. g. 5.999.11.02 -> 7.000.10.02. Used by the mass update of time sheets and by
 * the Kost2 reallocation.
 */
object TimesheetKost2Mapper {
    /**
     * @return The cost unit of the same type as [kost2] out of [available], or null if there is none (or [kost2] is
     * null). A cost unit without type matches one without type (as the mass update always did).
     */
    fun sameKost2Art(kost2: Kost2DO?, available: Collection<Kost2DO>?): Kost2DO? {
        kost2 ?: return null
        val kost2ArtId = kost2.kost2Art?.id
        return available?.find { it.kost2Art?.id == kost2ArtId }
    }
}
