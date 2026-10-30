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

package org.projectforge.plugins.cveradar.model

/**
 * What the list of the vulnerabilities shows of the case of a vulnerability: filled per page by
 * [org.projectforge.plugins.cveradar.rest.CvrVulnerabilityEntityRest], not stored. Without a case only the suggested
 * priority is set.
 */
class CvrTriageInfo(
    val caseId: Long? = null,
    val status: CvrCaseStatus? = null,
    val priority: CvrPriority? = null,
    val suggestedPriority: CvrPriority? = null,
    val assigneeId: Long? = null,
    val assigneeName: String? = null,
    val ratingCount: Int = 0,
)

/**
 * The findings of a vulnerability in all instances (MAT-02), for the list only: counts, so everybody sees them
 * (VIS-04). Of the open ones [affected] are sure, [possible] are those without an exact version or only by name
 * (MAT-03, MAT-12); [inProgress] and [done] are counted by status (TRI-10).
 */
class CvrFindingCounts(val affected: Int = 0, val possible: Int = 0, val inProgress: Int = 0, val done: Int = 0)
