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

package org.projectforge.plugins.ihk

import java.time.LocalDate

/**
 * The apprentice's training settings, printed on every report. Stored as user pref (see [IHKService]); the
 * fields need defaults, the pref is deserialized via the no-arg constructor.
 */
class IHKSettings(
    /** The first day of the apprenticeship. Null: not set up yet. */
    var ausbildungsbeginn: LocalDate? = null,
    /** The training year, or [AUSBILDUNGSJAHR_AUTO] to calculate it from [ausbildungsbeginn]. */
    var ausbildungsjahr: Int = AUSBILDUNGSJAHR_AUTO,
    /** The team or department. */
    var teamname: String? = null,
) {
    companion object {
        const val AUSBILDUNGSJAHR_AUTO = -1

        /** The training years that may be set explicitly (e.g. for a shortened apprenticeship). */
        val AUSBILDUNGSJAHRE = 1..4
    }
}
