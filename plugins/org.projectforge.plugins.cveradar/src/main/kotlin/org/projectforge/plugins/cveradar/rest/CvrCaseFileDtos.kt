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

import org.projectforge.plugins.cveradar.model.CvrCaseKind
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.model.CvrExploitability
import org.projectforge.plugins.cveradar.model.CvrPriority
import org.projectforge.plugins.cveradar.model.CvrSource
import org.projectforge.plugins.cveradar.model.CvrTeamAnswer
import java.math.BigDecimal
import java.util.Date

// The JSON of CvrCaseFileRest (the file of a CVE: NVD data, triage, ratings, measures), mirrored by
// projectforge-next/lib/rs/cve-radar-file.ts.

/** A reference of the NVD document (http(s) links only), with its tags (Patch, Vendor Advisory, Mitigation, …). */
class NvdReference(val url: String, val source: String?, val tags: List<String>)

class VulnerabilityView(
    val id: Long,
    val cveId: String?,
    val nvdStatus: String?,
    val rejected: Boolean,
    val published: Date?,
    val nvdLastModified: Date?,
    val description: String?,
    val cvssScore: BigDecimal?,
    val severity: String?,
    val cvss31Score: BigDecimal?,
    val cvss31Vector: String?,
    val cvss40Score: BigDecimal?,
    val cvss40Vector: String?,
    val cwes: List<String>,
    val kevAdded: Date?,
    /** The id in the EUVD, its EPSS and the day since the EUVD knows it to be exploited (IMP-16). */
    val euvdId: String?,
    val epss: BigDecimal?,
    val exploitedSince: Date?,
    /** Known to the EUVD only (no CPE of the NVD): a match is possible by the product names only. */
    val euvdOnly: Boolean,
    /** Where the data comes from: the NVD, OSV (advisories) and the EUVD. */
    val sources: List<CvrSource>,
    /** References for remediation (tags Patch, Vendor Advisory, Mitigation, PRIO-04), shown apart from the others. */
    val remediationReferences: List<NvdReference>,
    val otherReferences: List<NvdReference>,
)

class ReferenceLinkView(val url: String, val label: String?)

class CaseFileView(
    val id: Long,
    val code: String?,
    val kind: CvrCaseKind?,
    val title: String?,
    /** Markdown (import) or HTML (rich text editor), to render sanitized (AI-09). */
    val description: String?,
    val status: CvrCaseStatus?,
    val priority: CvrPriority?,
    val priorityReason: String?,
    val assigneeId: Long?,
    val assigneeName: String?,
    val techMeasures: String?,
    val nonTechMeasures: String?,
    val fixVersion: String?,
    val workaround: String?,
    val referenceLinks: List<ReferenceLinkView>,
    val closeReason: String?,
    /** To send back with every change: the editor's conflict detection (TRI-04). */
    val lastUpdate: Date?,
)

class SuggestionView(val priority: CvrPriority, val reasons: List<String>)

class RatingView(
    val id: Long,
    val userId: Long?,
    val userName: String?,
    val priority: CvrPriority?,
    val exploitability: CvrExploitability?,
    val reason: String?,
    val lastUpdate: Date?,
    /** Deviates from the binding priority (TRI-03). */
    val deviates: Boolean,
    val mine: Boolean,
)

class FileTeamCheckView(
    val id: Long,
    val groupName: String?,
    val subject: String?,
    /** For the group only; empty if every member answers. */
    val answer: CvrTeamAnswer?,
    val answerDate: java.time.LocalDate?,
    /** Every member answers: the active members and how many of them answered. */
    val perPerson: Boolean,
    val members: Int,
    val answered: Int,
)

class FilePermissions(
    /** Security team: triage, rating, measures. */
    val canEdit: Boolean,
    /** Sets the binding priority: the assignee, or any security officer; an unassigned case: any security team member. */
    val canSetPriority: Boolean,
    val isSecurityOfficer: Boolean,
    /** Opens the file of a CVE without one (starts the discussion). */
    val canOpen: Boolean,
)

class CaseFile(
    val vulnerability: VulnerabilityView?,
    /** Null: no file opened for the vulnerability yet. */
    val case: CaseFileView?,
    val suggestion: SuggestionView?,
    val ratings: List<RatingView>,
    val teamChecks: List<FileTeamCheckView>,
    val permissions: FilePermissions,
)

/** One of the actions of [CvrCaseFileRest.triage]; only the fields of the action are read. */
class TriageRequest(
    val lastUpdate: Date? = null,
    val action: Action? = null,
    val assigneeId: Long? = null,
    val status: CvrCaseStatus? = null,
    val priority: CvrPriority? = null,
    val reason: String? = null,
) {
    enum class Action { TAKE_OVER, ASSIGN, SET_STATUS, SET_PRIORITY, NOT_RELEVANT }
}

class RatingRequest(
    val lastUpdate: Date? = null,
    val priority: CvrPriority? = null,
    val exploitability: CvrExploitability? = null,
    val reason: String? = null,
)

class DescriptionRequest(
    val lastUpdate: Date? = null,
    val description: String? = null,
)

class MeasuresRequest(
    val lastUpdate: Date? = null,
    val techMeasures: String? = null,
    val nonTechMeasures: String? = null,
    val fixVersion: String? = null,
    val workaround: String? = null,
    val referenceLinks: String? = null,
)

class MassRequest(
    val vulnerabilityIds: List<Long> = emptyList(),
    val action: TriageRequest.Action? = null,
    val assigneeId: Long? = null,
    val priority: CvrPriority? = null,
    val reason: String? = null,
)

class MassResult(val vulnerabilityId: Long, val cveId: String?, val ok: Boolean, val message: String?)

/** The answer of a member to a team check asking every member. */
class TeamCheckAnswerView(
    val user: String?,
    val answer: CvrTeamAnswer?,
    val comment: String?,
    val answerDate: java.time.LocalDate?,
    val lastUpdate: Date?,
)

/**
 * The file of a team check (CLS-03): what is asked, the answer, and the case it belongs to. If every member answers
 * ([perPerson]), answer, comment, date and answeredBy are the own ones of the user.
 */
class TeamCheckFile(
    val id: Long,
    val caseId: Long?,
    val caseCode: String?,
    val caseTitle: String?,
    val caseStatus: CvrCaseStatus?,
    val groupName: String?,
    val subject: String?,
    val answer: CvrTeamAnswer?,
    val comment: String?,
    val answerDate: java.time.LocalDate?,
    val answeredBy: String?,
    val canAnswer: Boolean,
    val perPerson: Boolean,
    /** The active members (every member answers). */
    val members: Int,
    /** The active members who answered. */
    val answered: Int,
    /** The active members without answer or in progress. */
    val openMembers: Int,
    /** All answers, for the security team only. */
    val answers: List<TeamCheckAnswerView>?,
    /** The active members without answer, for the security team only. */
    val missing: List<String>?,
)

// The findings of a vulnerability (MAT-02), mirrored by projectforge-next/lib/rs/cve-radar-file.ts.

/** A finding in an instance the user may see (VIS-02). */
class FindingView(
    val id: Long,
    val certainty: org.projectforge.plugins.cveradar.model.CvrFindingCertainty?,
    val status: org.projectforge.plugins.cveradar.model.CvrFindingStatus?,
    /** What matched, e.g. the CPE with its version range. */
    val matched: String?,
    val discardReason: String?,
    val area: String?,
    val system: String,
    /** The code of the instance, null for the default instance of the system. */
    val instance: String?,
    val component: String?,
    /** The technology that matched, with its version. */
    val technology: String?,
)

/** The findings of a vulnerability; those in instances the user may not see are only counted (VIS-04). */
class FindingList(
    val findings: List<FindingView>,
    val hiddenCount: Int,
    /** Whether the user may discard and restore (the security team). */
    val canChange: Boolean,
)

class DiscardRequest(var reason: String? = null)

/**
 * What the research of a new case by an AI system starts from (AI-01): the NVD data of the CVE if it was imported, and
 * the case if one exists for it already.
 */
class AiCaseContext(val vulnerability: VulnerabilityView?, val caseId: Long?)

/**
 * A new case: from the answer of an AI system (AI-04 ff.) or from a [CaseExport], as shown to the security team before:
 * the values of the case, the measures and the workaround as html. Checked again by
 * [org.projectforge.plugins.cveradar.CvrCaseFileService.importCase].
 */
class CaseImportRequest(
    val kind: CvrCaseKind? = null,
    val code: String? = null,
    val title: String? = null,
    val description: String? = null,
    val priority: CvrPriority? = null,
    val priorityReason: String? = null,
    val fixVersion: String? = null,
    val workaround: String? = null,
    val techMeasures: String? = null,
    val nonTechMeasures: String? = null,
    val referenceLinks: String? = null,
    val clientAdvisory: Boolean = false,
    val minClientAgentVersion: String? = null,
    val clientAgentCheckId: String? = null,
)

/**
 * A case as a file, to import it again, e.g. into another instance of ProjectForge: the values of the case as they are
 * stored, without its history (status, assignee, ratings, team checks, discussion).
 */
class CaseExport(
    val format: String,
    val schemaVersion: String,
    val exported: java.util.Date,
    val case: CaseImportRequest,
)
