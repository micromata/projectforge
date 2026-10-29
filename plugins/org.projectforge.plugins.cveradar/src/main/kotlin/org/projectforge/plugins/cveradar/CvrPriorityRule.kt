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

package org.projectforge.plugins.cveradar

import org.projectforge.plugins.cveradar.model.CvrPriority
import java.math.BigDecimal

/**
 * The suggested priority of a vulnerability (PRIO-01): CVSS as the starting point, one step higher for a vulnerability
 * listed in the CISA KEV catalogue (actively exploited). EPSS and the criticality and exposure of the affected
 * instances join with the inventory. Only a suggestion: the binding priority is set by a person (TRI-03).
 */
object CvrPriorityRule {
    /** The [key] is the last part of the i18n key `plugins.cveradar.priorityReason.<key>`. */
    enum class Reason(val key: String) { CVSS_P2("cvssP2"), CVSS_P3("cvssP3"), CVSS_P4("cvssP4"), KEV("kev") }

    data class Suggestion(val priority: CvrPriority, val reasons: List<Reason>)

    fun suggest(cvss: BigDecimal?, kev: Boolean): Suggestion {
        val reasons = mutableListOf<Reason>()
        var level = when {
            cvss != null && cvss >= BigDecimal(9) -> 1.also { reasons.add(Reason.CVSS_P2) }
            cvss != null && cvss >= BigDecimal(7) -> 2.also { reasons.add(Reason.CVSS_P3) }
            else -> 3.also { reasons.add(Reason.CVSS_P4) }
        }
        if (kev) {
            level--
            reasons.add(Reason.KEV)
        }
        return Suggestion(CvrPriority.entries[level], reasons)
    }
}
