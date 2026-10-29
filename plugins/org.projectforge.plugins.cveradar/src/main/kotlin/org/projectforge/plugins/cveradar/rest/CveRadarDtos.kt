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

package org.projectforge.plugins.cveradar.rest

import org.projectforge.plugins.cveradar.CvrCaseStatusService
import org.projectforge.plugins.cveradar.CvrStatisticsService
import org.projectforge.plugins.cveradar.model.CvrCaseKind
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.model.CvrClientAnswer
import org.projectforge.plugins.cveradar.model.CvrPriority
import org.projectforge.plugins.cveradar.model.CvrTeamAnswer
import java.time.LocalDate
import java.util.Date

// The JSON of CveRadarRest, mirrored by projectforge-next/lib/rs/cve-radar-live.ts.

class CaseRef(
    val id: Long,
    val code: String?,
    val title: String?,
    val kind: CvrCaseKind?,
    val status: CvrCaseStatus?,
    val priority: CvrPriority?,
    val clientAdvisory: Boolean,
    val minClientAgentVersion: String?,
    val clientAgentCheckId: String?,
    val closeReason: String?,
)

class OpenItem(val key: String, val count: Int)

class DeviceCounts(val fixed: Int, val open: Int, val confirmed: Int, val noData: Int, val inactive: Int)

class DeviceRow(
    val id: Long,
    val username: String?,
    val userDisplayName: String?,
    val hostname: String?,
    val serialNumber: String?,
    val agentVersion: String?,
    val agentVersionAt: Date?,
    val lastSeen: Date?,
    /** Within the closure overview: the state of the device for the case. */
    val state: CvrCaseStatusService.DeviceState? = null,
    val checkPassed: Boolean? = null,
    val confirmation: CvrClientAnswer? = null,
)

class TeamCheckRow(
    val id: Long,
    val caseId: Long?,
    val caseCode: String?,
    val caseTitle: String?,
    val groupId: Long?,
    val groupName: String?,
    val subject: String?,
    val answer: CvrTeamAnswer?,
    val comment: String?,
    val answerDate: LocalDate?,
    val answeredBy: String?,
    val lastUpdate: Date?,
    /** Whether the logged-in user may answer: a member of the group or the security team. */
    val canAnswer: Boolean,
)

class ConfirmationCounts(val done: Int, val notAffected: Int, val question: Int)

class QuestionRow(val user: String?, val comment: String?, val lastUpdate: Date?)

class ClosureView(
    val case: CaseRef,
    val closable: Boolean,
    val openItems: List<OpenItem>,
    val clientsRelevant: Boolean,
    val devices: DeviceCounts,
    /** The devices with their persons: for the security team only (VIS-15), null otherwise. */
    val deviceRows: List<DeviceRow>?,
    val teamChecks: List<TeamCheckRow>,
    val confirmations: ConfirmationCounts,
    /** The questions of the employees: for the security team only, null otherwise. */
    val questions: List<QuestionRow>?,
    val canEdit: Boolean,
    /** Whether the user may close the case despite open items (security officer). */
    val canCloseWithOpenItems: Boolean,
)

class Advisory(
    val id: Long,
    val code: String?,
    val title: String?,
    val nonTechMeasures: String?,
    val myAnswer: CvrClientAnswer?,
    val myComment: String?,
    /** Whether the own active devices prove the fix already (CLS-04), so no confirmation is needed. */
    val fixedByEvidence: Boolean,
)

class MyView(
    val devices: List<DeviceRow>,
    val advisories: List<Advisory>,
    val teamChecks: List<TeamCheckRow>,
    /** Whether the user has the right of the CVE-Radar (shows the links to cases and devices). */
    val reader: Boolean,
    val securityTeam: Boolean,
    /** The PF groups behind the roles, for the help texts of the pages (empty: not configured). */
    val groups: RoleGroups,
)

class RoleGroups(val read: String, val write: String, val securityOfficer: String)

class CloseRequest(var reason: String? = null)

class TeamCheckRequest(var groupId: Long? = null, var subject: String? = null)

class TeamCheckAnswerRequest(var answer: CvrTeamAnswer? = null, var comment: String? = null, var date: LocalDate? = null)

class ConfirmRequest(var answer: CvrClientAnswer? = null, var comment: String? = null)

class CveRadarError(val message: String)

/** The cases created and the NVD vulnerabilities published per day, week or month (the statistics tab). */
class ReportedStatistics(
    val granularity: CvrStatisticsService.Granularity,
    val cases: List<CvrStatisticsService.Bucket>,
    val vulnerabilities: List<CvrStatisticsService.Bucket>,
)
