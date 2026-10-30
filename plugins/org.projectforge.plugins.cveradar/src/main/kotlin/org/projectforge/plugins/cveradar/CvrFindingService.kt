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

import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.model.CvrFindingDO
import org.projectforge.plugins.cveradar.model.CvrFindingStatus
import org.projectforge.plugins.cveradar.model.CvrPriority
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.projectforge.plugins.cveradar.rest.FindingList
import org.projectforge.plugins.cveradar.rest.FindingStatusRequest
import org.projectforge.plugins.cveradar.rest.FindingView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * The findings of a vulnerability for the user (MAT-02, MAT-04 – MAT-06). Which one a user sees follows the inventory
 * (VIS-02): in an instance he may not see, a finding is only counted (VIS-04). The security team sets the status and
 * discards a false positive, in the instances it may see. A risk acceptance is requested by the team and approved by
 * the security officer, from priority P2 on by the management as well (MAT-05). Everything is taken from the database,
 * never from the request.
 */
@Service
class CvrFindingService {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var findingDao: CvrFindingDao

    @Autowired
    private lateinit var inventoryService: CvrInventoryService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    fun findings(user: PFUserDO, vulnerabilityId: Long): FindingList {
        access.checkReader(user)
        vulnerabilityDao.find(vulnerabilityId, checkAccess = false)?.takeIf { !it.deleted }
            ?: throw UserException(CveRadarService.ERROR_NOT_FOUND)
        return list(user, vulnerabilityId)
    }

    fun discard(user: PFUserDO, id: Long, reason: String?): FindingList {
        val text = reason?.trim()?.takeIf { it.isNotEmpty() } ?: throw UserException(ERROR_REASON)
        val finding = changeable(user, id)
        reset(finding, CvrFindingStatus.DISCARDED)
        finding.reason = text.take(TEXT_LENGTH)
        findingDao.update(finding, checkAccess = false)
        return list(user, finding.vulnerabilityId!!)
    }

    fun restore(user: PFUserDO, id: Long): FindingList {
        val finding = changeable(user, id)
        reset(finding, CvrFindingStatus.NEW)
        findingDao.update(finding, checkAccess = false)
        return list(user, finding.vulnerabilityId!!)
    }

    /**
     * Sets the status of several findings of one vulnerability (MAT-04, MAT-09 mass action). All are checked before
     * the first one is changed, so a refusal changes nothing. "Risk accepted" is a request (MAT-05): the findings stay
     * in review until the approvals are in.
     */
    fun setStatus(user: PFUserDO, request: FindingStatusRequest): FindingList {
        val status = request.status?.takeIf { it in SETTABLE } ?: throw UserException(ERROR_STATUS)
        val ids = request.ids?.distinct()?.takeIf { it.isNotEmpty() } ?: throw UserException(CveRadarService.ERROR_NOT_FOUND)
        val reason = request.reason?.trim()?.takeIf { it.isNotEmpty() }?.take(TEXT_LENGTH)
        val fixedVersion = request.fixedVersion?.trim()?.takeIf { it.isNotEmpty() }?.take(VERSION_LENGTH)
        if ((status == CvrFindingStatus.NOT_AFFECTED || status == CvrFindingStatus.RISK_ACCEPTED) && reason == null) {
            throw UserException(ERROR_REASON_REQUIRED)
        }
        if (status == CvrFindingStatus.FIXED && fixedVersion == null) {
            throw UserException(ERROR_FIXED_VERSION)
        }
        val today = LocalDate.now()
        if (status == CvrFindingStatus.RISK_ACCEPTED && !(request.riskExpires?.isAfter(today) ?: false)) {
            throw UserException(ERROR_EXPIRY)
        }
        val findings = ids.map { changeable(user, it) }
        if (findings.map { it.vulnerabilityId }.distinct().size > 1) {
            throw UserException(ERROR_STATUS)
        }
        findings.forEach { finding ->
            reset(finding, if (status == CvrFindingStatus.RISK_ACCEPTED) CvrFindingStatus.IN_REVIEW else status)
            finding.reason = reason
            when (status) {
                CvrFindingStatus.FIXED -> {
                    finding.fixedVersion = fixedVersion
                    finding.fixedDate = request.fixedDate ?: today
                }

                CvrFindingStatus.FIX_PLANNED -> finding.fixedVersion = fixedVersion
                CvrFindingStatus.RISK_ACCEPTED -> {
                    finding.riskExpires = request.riskExpires
                    finding.riskRequestedBy = user
                }

                else -> {}
            }
            findingDao.update(finding, checkAccess = false)
        }
        return list(user, findings.first().vulnerabilityId!!)
    }

    /**
     * Approves a requested risk acceptance as the security officer or, where the priority is P1 or P2, as the
     * management; with the approvals in, the finding is "risk accepted" (MAT-05). The two approvals come from two
     * persons.
     */
    fun approveRisk(user: PFUserDO, id: Long): FindingList {
        val finding = requested(user, id)
        val needsManagement = needsManagement(finding.vulnerabilityId!!)
        if (!finding.riskExpires!!.isAfter(LocalDate.now())) {
            throw UserException(ERROR_EXPIRY)
        }
        when {
            access.isSecurityOfficer(user) && finding.riskApprovedBySo == null -> finding.riskApprovedBySo = user
            needsManagement && access.isManagement(user) && finding.riskApprovedByMg == null &&
                    finding.riskApprovedBySo?.id != user.id -> finding.riskApprovedByMg = user

            else -> throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        if (finding.riskApprovedBySo != null && (!needsManagement || finding.riskApprovedByMg != null)) {
            finding.status = CvrFindingStatus.RISK_ACCEPTED
        }
        findingDao.update(finding, checkAccess = false)
        return list(user, finding.vulnerabilityId!!)
    }

    /** Refuses a requested risk acceptance (security officer, management from P2 on): the finding is affected again. */
    fun rejectRisk(user: PFUserDO, id: Long): FindingList {
        val finding = requested(user, id)
        reset(finding, CvrFindingStatus.AFFECTED)
        findingDao.update(finding, checkAccess = false)
        return list(user, finding.vulnerabilityId!!)
    }

    /** The risk acceptances that ended are affected again (MAT-05); returns their number. */
    fun expireRiskAcceptances(today: LocalDate = LocalDate.now()): Int {
        val expired = findingDao.selectExpiredRiskAcceptances(today)
        expired.forEach {
            val reason = it.reason
            reset(it, CvrFindingStatus.AFFECTED)
            it.reason = reason
            findingDao.update(it, checkAccess = false)
        }
        return expired.size
    }

    /** The status with everything that belongs to another status removed. */
    private fun reset(finding: CvrFindingDO, status: CvrFindingStatus) {
        finding.status = status
        finding.reason = null
        finding.fixedVersion = null
        finding.fixedDate = null
        finding.riskExpires = null
        finding.riskRequestedBy = null
        finding.riskApprovedBySo = null
        finding.riskApprovedByMg = null
    }

    /** The finding, if the user belongs to the security team and sees its instance. */
    private fun changeable(user: PFUserDO, id: Long): CvrFindingDO {
        access.checkSecurityTeam(user)
        return visible(user, id)
    }

    /** The finding with a risk acceptance awaiting approval, for the officer and the management who see its instance. */
    private fun requested(user: PFUserDO, id: Long): CvrFindingDO {
        if (!access.isSecurityOfficer(user) && !access.isManagement(user)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        val finding = visible(user, id)
        if (finding.status != CvrFindingStatus.IN_REVIEW || finding.riskRequestedBy == null || finding.riskExpires == null) {
            throw UserException(ERROR_STATUS)
        }
        return finding
    }

    private fun visible(user: PFUserDO, id: Long): CvrFindingDO {
        val finding = findingDao.find(id, checkAccess = false)?.takeIf { !it.deleted }
            ?: throw UserException(CveRadarService.ERROR_NOT_FOUND)
        if (finding.instanceId !in inventoryService.readableInstances(user)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        return finding
    }

    /** The binding priority of the case, otherwise the suggested one (PRIO-01). */
    private fun priorityOf(vulnerabilityId: Long): CvrPriority? {
        caseDao.findByVulnerabilityId(vulnerabilityId)?.priority?.let { return it }
        val vulnerability = vulnerabilityDao.find(vulnerabilityId, checkAccess = false) ?: return null
        return CvrPriorityRule.suggest(vulnerability.cvssScore, vulnerability.kevAdded != null).priority
    }

    /** From priority P2 on the management approves a risk acceptance as well (MAT-05). */
    private fun needsManagement(vulnerabilityId: Long): Boolean =
        priorityOf(vulnerabilityId).let { it == CvrPriority.P1 || it == CvrPriority.P2 }

    private fun list(user: PFUserDO, vulnerabilityId: Long): FindingList {
        val all = findingDao.selectByVulnerability(vulnerabilityId)
        val instances = inventoryService.readableInstances(user)
        val (visible, hidden) = all.partition { it.instanceId in instances }
        val components = byId(CvrComponentDO::class.java, visible.mapNotNull { it.componentId })
        val technologies = byId(CvrTechnologyDO::class.java, visible.mapNotNull { it.technologyId })
        val needsManagement = needsManagement(vulnerabilityId)
        val views = visible.map { finding ->
            val instance = instances[finding.instanceId]!!
            val component = components[finding.componentId]
            val technology = technologies[finding.technologyId]
            FindingView(
                id = finding.id!!, certainty = finding.certainty, status = finding.status, matched = finding.matched,
                reason = finding.reason, area = instance.area, system = instance.system,
                instance = instance.instance, component = component?.let { "${it.code} ${it.name}" },
                technology = technology?.let { listOfNotNull(it.product, it.version).joinToString(" ") },
                fixedVersion = finding.fixedVersion, fixedDate = finding.fixedDate, riskExpires = finding.riskExpires,
                riskRequestedBy = name(finding.riskRequestedBy), riskApprovedBySo = name(finding.riskApprovedBySo),
                riskApprovedByMg = name(finding.riskApprovedByMg), riskNeedsManagement = needsManagement,
                canApproveRisk = canApprove(user, finding, needsManagement),
            )
        }.sortedWith(compareBy({ it.status }, { it.certainty }, { it.system }, { it.instance ?: "" }))
        // Only open ones count as hidden: a discarded or done finding says nothing about a system.
        val hiddenOpen = hidden.count { it.status in CvrFindingStatus.OPEN }
        return FindingList(views, hiddenOpen, access.isSecurityTeam(user))
    }

    private fun canApprove(user: PFUserDO, finding: CvrFindingDO, needsManagement: Boolean): Boolean {
        if (finding.status != CvrFindingStatus.IN_REVIEW || finding.riskRequestedBy == null) {
            return false
        }
        return (access.isSecurityOfficer(user) && finding.riskApprovedBySo == null) ||
                (needsManagement && access.isManagement(user) && finding.riskApprovedByMg == null &&
                        finding.riskApprovedBySo?.id != user.id)
    }

    private fun name(user: PFUserDO?): String? = userGroupCache.getUser(user?.id)?.displayName

    private fun <T : org.projectforge.framework.persistence.api.BaseDO<Long>> byId(clazz: Class<T>, ids: Collection<Long>): Map<Long, T> {
        return persistenceService.executeQueryBatched(
            "from ${clazz.simpleName} e where e.id in :ids", clazz, "ids", ids.toSet(),
        ).associateBy { it.id!! }
    }

    companion object {
        private const val TEXT_LENGTH = 4000
        private const val VERSION_LENGTH = 100

        /** What the security team sets by hand; a risk acceptance only as a request. */
        private val SETTABLE = CvrFindingStatus.entries - CvrFindingStatus.DISCARDED

        const val ERROR_REASON = "plugins.cveradar.finding.error.reason"
        const val ERROR_REASON_REQUIRED = "plugins.cveradar.finding.error.reasonRequired"
        const val ERROR_FIXED_VERSION = "plugins.cveradar.finding.error.fixedVersion"
        const val ERROR_EXPIRY = "plugins.cveradar.finding.error.expiry"
        const val ERROR_STATUS = "plugins.cveradar.finding.error.status"
    }
}
