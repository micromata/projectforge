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

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrCaseKind
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.model.CvrPriority
import org.projectforge.plugins.cveradar.model.CvrRatingDO
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.projectforge.plugins.cveradar.rest.CaseFile
import org.projectforge.plugins.cveradar.rest.CaseFileView
import org.projectforge.plugins.cveradar.rest.FilePermissions
import org.projectforge.plugins.cveradar.rest.FileTeamCheckView
import org.projectforge.plugins.cveradar.rest.MassRequest
import org.projectforge.plugins.cveradar.rest.MassResult
import org.projectforge.plugins.cveradar.rest.MeasuresRequest
import org.projectforge.plugins.cveradar.rest.NvdReference
import org.projectforge.plugins.cveradar.rest.RatingRequest
import org.projectforge.plugins.cveradar.rest.RatingView
import org.projectforge.plugins.cveradar.rest.ReferenceLinkView
import org.projectforge.plugins.cveradar.rest.SuggestionView
import org.projectforge.plugins.cveradar.rest.TriageRequest
import org.projectforge.plugins.cveradar.rest.VulnerabilityView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.util.Date
import java.util.zip.GZIPInputStream

private val log = KotlinLogging.logger {}

/**
 * The file of a CVE (TRI-01 – TRI-07, PRIO-01 – PRIO-05, ACT-01): the NVD data, the case with its triage, the ratings of
 * the security team members and the measures. Everything is taken from the database and the logged-in user, never from
 * the request. Readers read; the security team edits; the binding priority is set by the assignee or a security
 * officer. Every change of a case carries the `lastUpdate` the editor loaded and is refused if the case changed since
 * (TRI-04).
 */
@Service
class CvrCaseFileService {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var ratingDao: CvrRatingDao

    @Autowired
    private lateinit var service: CveRadarService

    @Autowired
    private lateinit var teamCheckDao: CvrTeamCheckDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    /** The file of the vulnerability, with its case if one was opened. */
    fun viewVulnerability(user: PFUserDO, vulnerabilityId: Long): CaseFile {
        access.checkReader(user)
        val vulnerability = vulnerabilityDao.find(vulnerabilityId, checkAccess = false)?.takeIf { !it.deleted }
            ?: throw UserException(CveRadarService.ERROR_NOT_FOUND)
        return view(user, vulnerability, caseDao.findByVulnerabilityId(vulnerabilityId))
    }

    /** The file of the case (a vulnerability or an incident). */
    fun viewCase(user: PFUserDO, caseId: Long): CaseFile {
        val case = service.getCase(user, caseId)
        val vulnerability = case.vulnerabilityId?.let { vulnerabilityDao.find(it, checkAccess = false) }
        return view(user, vulnerability, case)
    }

    /** Opens the file of the vulnerability: its case, once (idempotent); readers may, to start the discussion. */
    fun openCase(user: PFUserDO, vulnerabilityId: Long): CaseFile {
        access.checkReader(user)
        val vulnerability = vulnerabilityDao.find(vulnerabilityId, checkAccess = false)?.takeIf { !it.deleted }
            ?: throw UserException(CveRadarService.ERROR_NOT_FOUND)
        ensureCase(vulnerability)
        return viewVulnerability(user, vulnerabilityId)
    }

    private fun ensureCase(vulnerability: CvrVulnerabilityDO): CvrCaseDO {
        caseDao.findByVulnerabilityId(vulnerability.id!!)?.let { return it }
        // A deleted case or one not linked yet (codes are unique, deleted ones included): revive and link it.
        caseDao.findByCode(vulnerability.cveId!!)?.let { stored ->
            if (stored.deleted) {
                caseDao.undelete(stored, checkAccess = false)
            }
            if (stored.vulnerabilityId != vulnerability.id) {
                caseDao.update(stored, checkAccess = false) // Links the vulnerability (CvrCaseDao.onInsertOrModify).
            }
            return caseDao.find(stored.id, checkAccess = false)!!
        }
        val case = CvrCaseDO().also {
            it.kind = CvrCaseKind.CVE
            it.code = vulnerability.cveId
            it.title = vulnerability.cveId
            it.status = CvrCaseStatus.NEW
        }
        try {
            caseDao.insert(case, checkAccess = false)
        } catch (ex: Exception) {
            // Opened at the same time by someone else: the unique constraints decide, take theirs.
            return caseDao.findByVulnerabilityId(vulnerability.id!!) ?: throw ex
        }
        return case
    }

    fun triage(user: PFUserDO, caseId: Long, request: TriageRequest): CaseFile {
        access.checkSecurityTeam(user)
        val case = editableCase(caseId, request.lastUpdate, openRequired = true)
        applyTriage(user, case, request.action, request.assigneeId, request.status, request.priority, request.reason)
        return viewCase(user, caseId)
    }

    private fun applyTriage(
        user: PFUserDO,
        case: CvrCaseDO,
        action: TriageRequest.Action?,
        assigneeId: Long?,
        status: CvrCaseStatus?,
        priority: CvrPriority?,
        reason: String?,
    ) {
        when (action ?: throw UserException(ERROR_ACTION)) {
            TriageRequest.Action.TAKE_OVER -> {
                checkReassign(user, case)
                assign(case, user)
            }

            TriageRequest.Action.ASSIGN -> {
                checkReassign(user, case)
                val assignee = assigneeId?.let { userGroupCache.getUser(it) }
                if (assignee == null || assignee.deactivated || !access.isSecurityTeam(assignee)) {
                    throw UserException(ERROR_ASSIGNEE)
                }
                assign(case, assignee)
            }

            TriageRequest.Action.SET_STATUS -> {
                // Closing has its own rules: not relevant with a reason, closed from the closure overview.
                if (status == null || status == CvrCaseStatus.CLOSED || status == CvrCaseStatus.NOT_RELEVANT) {
                    throw UserException(ERROR_STATUS)
                }
                case.status = status
            }

            TriageRequest.Action.SET_PRIORITY -> {
                val text = reason?.trim()?.takeIf { it.isNotEmpty() } ?: throw UserException(ERROR_PRIORITY_REASON)
                priority ?: throw UserException(ERROR_ACTION)
                if (!canSetPriority(user, case)) {
                    throw UserException(ERROR_PRIORITY_RIGHTS)
                }
                if (case.assignee == null) {
                    assign(case, user) // Setting it on an unassigned case makes the acting person its assignee (TRI-03).
                }
                case.priority = priority
                case.priorityReason = text.take(TEXT_LENGTH)
                if (case.status == CvrCaseStatus.NEW || case.status == CvrCaseStatus.IN_TRIAGE) {
                    case.status = CvrCaseStatus.RATED
                }
            }

            TriageRequest.Action.NOT_RELEVANT -> {
                service.checkClosing(user, case, reason)
                case.status = CvrCaseStatus.NOT_RELEVANT
                case.closeReason = reason!!.trim().take(TEXT_LENGTH)
            }
        }
        caseDao.update(case, checkAccess = false)
    }

    private fun assign(case: CvrCaseDO, assignee: PFUserDO) {
        case.assignee = PFUserDO().also { it.id = assignee.id }
        if (case.status == CvrCaseStatus.NEW) {
            case.status = CvrCaseStatus.IN_TRIAGE
        }
    }

    /**
     * A case assigned to somebody else is handed over by its assignee or a security officer only: otherwise anybody of
     * the security team could take it over and set the binding priority reserved for the assignee (TRI-03).
     */
    private fun checkReassign(user: PFUserDO, case: CvrCaseDO) {
        val assigneeId = case.assigneeId ?: return
        if (assigneeId != user.id && !access.isSecurityOfficer(user)) {
            throw UserException(ERROR_REASSIGN_RIGHTS)
        }
    }

    /** The assignee, a security officer, or any security team member while the case is unassigned (TRI-03). */
    fun canSetPriority(user: PFUserDO, case: CvrCaseDO): Boolean {
        return access.isSecurityOfficer(user) || case.assigneeId == null || case.assigneeId == user.id
    }

    /** Saves the own rating of the logged-in user, a new one or the change of the existing one (TRI-03). */
    fun rate(user: PFUserDO, caseId: Long, request: RatingRequest): CaseFile {
        access.checkSecurityTeam(user)
        val case = editableCase(caseId, request.lastUpdate, openRequired = true)
        val priority = request.priority ?: throw UserException(ERROR_ACTION)
        val exploitability = request.exploitability ?: throw UserException(ERROR_ACTION)
        // Deleted ones included: one row per case and user (unique), a removed rating is revived.
        val existing = ratingDao.findByCaseAndUser(caseId, user.id!!)
        if (existing?.deleted == true) {
            ratingDao.undelete(existing, checkAccess = false)
        }
        val rating = existing ?: CvrRatingDO().also {
            it.cvrCase = case
            it.user = PFUserDO().also { u -> u.id = user.id }
        }
        rating.priority = priority
        rating.exploitability = exploitability
        rating.reason = request.reason?.trim()?.take(TEXT_LENGTH)?.takeIf { it.isNotEmpty() }
        if (existing == null) {
            ratingDao.insert(rating, checkAccess = false)
        } else {
            ratingDao.update(rating, checkAccess = false)
        }
        if (case.status == CvrCaseStatus.NEW) {
            case.status = CvrCaseStatus.IN_TRIAGE
            caseDao.update(case, checkAccess = false)
        }
        return viewCase(user, caseId)
    }

    /** Removes the own rating. */
    fun deleteRating(user: PFUserDO, caseId: Long): CaseFile {
        access.checkSecurityTeam(user)
        editableCase(caseId, null, checkConflict = false, openRequired = true)
        ratingDao.selectByCase(caseId).firstOrNull { it.user?.id == user.id }?.let { ratingDao.markAsDeleted(it, checkAccess = false) }
        return viewCase(user, caseId)
    }

    /** The measures of the case (PRIO-05, ACT-01); rich text is sanitized when rendered, links are checked here. */
    fun saveMeasures(user: PFUserDO, caseId: Long, request: MeasuresRequest): CaseFile {
        access.checkSecurityTeam(user)
        val case = editableCase(caseId, request.lastUpdate, openRequired = true)
        case.techMeasures = request.techMeasures?.take(CvrCaseDO.LENGTH_MARKDOWN)
        case.nonTechMeasures = request.nonTechMeasures?.take(CvrCaseDO.LENGTH_MARKDOWN)
        case.workaround = request.workaround?.take(CvrCaseDO.LENGTH_MARKDOWN)
        case.fixVersion = request.fixVersion
        case.referenceLinks = request.referenceLinks
        caseDao.update(case, checkAccess = false) // Normalises the version and the links.
        return viewCase(user, caseId)
    }

    /** The same action for several vulnerabilities (TRI-07); the files are opened where missing. */
    fun mass(user: PFUserDO, request: MassRequest): List<MassResult> {
        access.checkSecurityTeam(user)
        val action = request.action
        if (action == null || action == TriageRequest.Action.SET_STATUS) {
            throw UserException(ERROR_ACTION)
        }
        if (request.vulnerabilityIds.size > MAX_MASS) {
            throw UserException(ERROR_MASS_LIMIT, MAX_MASS)
        }
        return request.vulnerabilityIds.distinct().map { id ->
            val vulnerability = vulnerabilityDao.find(id, checkAccess = false)?.takeIf { !it.deleted }
            if (vulnerability == null) {
                return@map MassResult(id, null, false, translate(UserException(CveRadarService.ERROR_NOT_FOUND)))
            }
            try {
                val case = ensureCase(vulnerability)
                val fresh = caseDao.find(case.id, checkAccess = false)!!
                if (fresh.status?.isClosed == true) {
                    throw UserException(ERROR_CLOSED)
                }
                applyTriage(user, fresh, action, request.assigneeId, null, request.priority, request.reason)
                MassResult(id, vulnerability.cveId, true, null)
            } catch (ex: UserException) {
                MassResult(id, vulnerability.cveId, false, translate(ex))
            } catch (ex: Exception) {
                // One failing entry doesn't abort the others: each one is saved on its own.
                log.error(ex) { "Mass action $action failed for vulnerability $id: ${ex.message}" }
                MassResult(id, vulnerability.cveId, false, translate(UserException(ERROR_MASS_FAILED)))
            }
        }
    }

    /** The case for a change by the security team: not changed since the editor loaded it, not closed if required. */
    private fun editableCase(
        caseId: Long,
        loadedLastUpdate: Date?,
        checkConflict: Boolean = true,
        openRequired: Boolean = false,
    ): CvrCaseDO {
        if (checkConflict) {
            caseDao.checkNotChanged(caseId, loadedLastUpdate)
        }
        val case = caseDao.find(caseId, checkAccess = false)?.takeIf { !it.deleted }
            ?: throw UserException(CveRadarService.ERROR_NOT_FOUND)
        if (openRequired && case.status?.isClosed == true) {
            throw UserException(ERROR_CLOSED)
        }
        return case
    }

    private fun view(user: PFUserDO, vulnerability: CvrVulnerabilityDO?, case: CvrCaseDO?): CaseFile {
        val ratings = case?.id?.let { ratingDao.selectByCase(it) } ?: emptyList()
        val suggestion = vulnerability?.let {
            CvrPriorityRule.suggest(it.cvssScore, it.kevAdded != null)
        }?.let { SuggestionView(it.priority, it.reasons.map { reason -> reason.key }) }
        val teamChecks = case?.id?.let { teamCheckDao.selectByCase(it) } ?: emptyList()
        return CaseFile(
            vulnerability = vulnerability?.let { vulnerabilityView(it) },
            case = case?.let { caseView(it) },
            suggestion = suggestion,
            ratings = ratings.map { ratingView(user, it, case?.priority) },
            teamChecks = teamChecks.map {
                FileTeamCheckView(it.id!!, userGroupCache.getGroup(it.groupId)?.name, it.subject, it.answer, it.answerDate)
            },
            permissions = FilePermissions(
                canEdit = access.isSecurityTeam(user) && case?.status?.isClosed != true,
                canSetPriority = access.isSecurityTeam(user) && case?.status?.isClosed != true &&
                        (case == null || canSetPriority(user, case)),
                isSecurityOfficer = access.isSecurityOfficer(user),
                canOpen = case == null && vulnerability != null,
            ),
        )
    }

    private fun caseView(case: CvrCaseDO): CaseFileView {
        return CaseFileView(
            id = case.id!!,
            code = case.code,
            kind = case.kind,
            title = case.title,
            status = case.status,
            priority = case.priority,
            priorityReason = case.priorityReason,
            assigneeId = case.assigneeId,
            assigneeName = userGroupCache.getUser(case.assigneeId)?.displayName,
            techMeasures = case.techMeasures,
            nonTechMeasures = case.nonTechMeasures,
            fixVersion = case.fixVersion,
            workaround = case.workaround,
            referenceLinks = CvrReferenceLinks.parse(case.referenceLinks).map { ReferenceLinkView(it.url, it.label) },
            closeReason = case.closeReason,
            lastUpdate = case.lastUpdate,
        )
    }

    private fun ratingView(user: PFUserDO, rating: CvrRatingDO, binding: CvrPriority?): RatingView {
        return RatingView(
            id = rating.id!!,
            userId = rating.user?.id,
            userName = userGroupCache.getUser(rating.user?.id)?.displayName,
            priority = rating.priority,
            exploitability = rating.exploitability,
            reason = rating.reason,
            lastUpdate = rating.lastUpdate,
            deviates = binding != null && rating.priority != binding,
            mine = rating.user?.id == user.id,
        )
    }

    private fun vulnerabilityView(vulnerability: CvrVulnerabilityDO): VulnerabilityView {
        val references = references(vulnerability)
        val (remediation, other) = references.partition { reference -> reference.tags.any { it in REMEDIATION_TAGS } }
        return VulnerabilityView(
            id = vulnerability.id!!,
            cveId = vulnerability.cveId,
            nvdStatus = vulnerability.nvdStatus,
            rejected = vulnerability.nvdStatus.equals("Rejected", ignoreCase = true),
            published = vulnerability.published,
            nvdLastModified = vulnerability.nvdLastModified,
            description = vulnerability.description,
            cvssScore = vulnerability.cvssScore,
            severity = vulnerability.severity,
            cvss31Score = vulnerability.cvss31Score,
            cvss31Vector = vulnerability.cvss31Vector,
            cvss40Score = vulnerability.cvss40Score,
            cvss40Vector = vulnerability.cvss40Vector,
            cwes = vulnerability.cwes?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            kevAdded = vulnerability.kevAdded,
            remediationReferences = remediation,
            otherReferences = other,
        )
    }

    /** The references of the NVD document: untrusted, so only http(s) links, limited in number and length. */
    internal fun references(vulnerability: CvrVulnerabilityDO): List<NvdReference> {
        val raw = vulnerability.rawDocument ?: return emptyList()
        return try {
            val json = GZIPInputStream(ByteArrayInputStream(raw)).use { String(it.readNBytes(MAX_RAW_BYTES), Charsets.UTF_8) }
            OBJECT_MAPPER.readTree(json).path("references").mapNotNull { node ->
                val url = node.path("url").asText("").trim()
                if (url.length > CvrReferenceLinks.MAX_URL_LENGTH || !CvrReferenceLinks.parse(url).any { it.url == url }) {
                    return@mapNotNull null
                }
                NvdReference(
                    url,
                    node.path("source").asText(null)?.take(CvrReferenceLinks.MAX_LABEL_LENGTH),
                    node.path("tags").mapNotNull { it.asText(null)?.take(50) }.take(10),
                )
            }.take(CvrReferenceLinks.MAX_LINKS * 2)
        } catch (ex: Exception) {
            log.warn { "Can't read the references of ${vulnerability.cveId}: ${ex.message}" }
            emptyList()
        }
    }

    private fun translate(ex: UserException): String = translateMsg(ex)

    companion object {
        private val OBJECT_MAPPER = ObjectMapper()
        private val REMEDIATION_TAGS = setOf("Patch", "Vendor Advisory", "Mitigation")
        private const val MAX_RAW_BYTES = 5_000_000
        private const val MAX_MASS = 200
        private const val TEXT_LENGTH = 4000

        const val ERROR_ACTION = "plugins.cveradar.file.error.action"
        const val ERROR_ASSIGNEE = "plugins.cveradar.file.error.assignee"
        const val ERROR_STATUS = "plugins.cveradar.file.error.status"
        const val ERROR_PRIORITY_REASON = "plugins.cveradar.file.error.priorityReason"
        const val ERROR_PRIORITY_RIGHTS = "plugins.cveradar.file.error.priorityRights"
        const val ERROR_CLOSED = "plugins.cveradar.file.error.closed"
        const val ERROR_MASS_FAILED = "plugins.cveradar.file.error.massFailed"
        const val ERROR_MASS_LIMIT = "plugins.cveradar.file.error.massLimit"
        const val ERROR_REASSIGN_RIGHTS = "plugins.cveradar.file.error.reassignRights"
    }
}
