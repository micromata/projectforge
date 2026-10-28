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

import org.projectforge.common.i18n.I18nEnum

/**
 * @param absent The user is away, as on vacation: a substitute with such an entry isn't on duty (see
 * VacationService.getVacationOverlaps). The other states are shown only, they don't make a conflict.
 */
enum class AvailabilityStatus(val key: String, val absent: Boolean = false) : I18nEnum {
    NOT_AVAILABLE("notAvailable", absent = true),
    REMOTE_REACHABLE("remoteReachable"),
    REACHABLE_IF_NEEDED("reachableIfNeeded");

    /** The kind of absence the calendar filter offers for entries of this state. */
    val kind: AbsenceKind
        get() = when (this) {
            NOT_AVAILABLE -> AbsenceKind.NOT_AVAILABLE
            REMOTE_REACHABLE -> AbsenceKind.REMOTE_REACHABLE
            REACHABLE_IF_NEEDED -> AbsenceKind.REACHABLE_IF_NEEDED
        }

    override val i18nKey: String
        get() = "availability.status.$key"
}
