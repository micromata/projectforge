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

import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.model.CvrFindingDO
import org.projectforge.plugins.cveradar.model.CvrFindingStatus
import org.projectforge.plugins.cveradar.model.CvrTechnologyDO
import org.projectforge.plugins.cveradar.rest.FindingList
import org.projectforge.plugins.cveradar.rest.FindingView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The findings of a vulnerability for the user (MAT-02, MAT-06). Which one a user sees follows the inventory (VIS-02):
 * in an instance he may not see, a finding is only counted (VIS-04). The security team discards a false positive and
 * takes it back, in the instances it may see. Everything is taken from the database, never from the request.
 */
@Service
class CvrFindingService {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var findingDao: CvrFindingDao

    @Autowired
    private lateinit var inventoryService: CvrInventoryService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

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
        finding.status = CvrFindingStatus.DISCARDED
        finding.discardReason = text.take(TEXT_LENGTH)
        findingDao.update(finding, checkAccess = false)
        return list(user, finding.vulnerabilityId!!)
    }

    fun restore(user: PFUserDO, id: Long): FindingList {
        val finding = changeable(user, id)
        finding.status = CvrFindingStatus.NEW
        finding.discardReason = null
        findingDao.update(finding, checkAccess = false)
        return list(user, finding.vulnerabilityId!!)
    }

    /** The finding, if the user belongs to the security team and sees its instance. */
    private fun changeable(user: PFUserDO, id: Long): CvrFindingDO {
        access.checkSecurityTeam(user)
        val finding = findingDao.find(id, checkAccess = false)?.takeIf { !it.deleted }
            ?: throw UserException(CveRadarService.ERROR_NOT_FOUND)
        if (finding.instanceId !in inventoryService.readableInstances(user)) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        return finding
    }

    private fun list(user: PFUserDO, vulnerabilityId: Long): FindingList {
        val all = findingDao.selectByVulnerability(vulnerabilityId)
        val instances = inventoryService.readableInstances(user)
        val (visible, hidden) = all.partition { it.instanceId in instances }
        val components = byId(CvrComponentDO::class.java, visible.mapNotNull { it.componentId })
        val technologies = byId(CvrTechnologyDO::class.java, visible.mapNotNull { it.technologyId })
        val views = visible.map { finding ->
            val instance = instances[finding.instanceId]!!
            val component = components[finding.componentId]
            val technology = technologies[finding.technologyId]
            FindingView(
                id = finding.id!!, certainty = finding.certainty, status = finding.status, matched = finding.matched,
                discardReason = finding.discardReason, area = instance.area, system = instance.system,
                instance = instance.instance, component = component?.let { "${it.code} ${it.name}" },
                technology = technology?.let { listOfNotNull(it.product, it.version).joinToString(" ") },
            )
        }.sortedWith(compareBy({ it.status }, { it.certainty }, { it.system }, { it.instance ?: "" }))
        // Only open ones count as hidden: a discarded finding says nothing about a system.
        return FindingList(views, hidden.count { it.status == CvrFindingStatus.NEW }, access.isSecurityTeam(user))
    }

    private fun <T : org.projectforge.framework.persistence.api.BaseDO<Long>> byId(clazz: Class<T>, ids: Collection<Long>): Map<Long, T> {
        return persistenceService.executeQueryBatched(
            "from ${clazz.simpleName} e where e.id in :ids", clazz, "ids", ids.toSet(),
        ).associateBy { it.id!! }
    }

    companion object {
        private const val TEXT_LENGTH = 4000
        const val ERROR_REASON = "plugins.cveradar.finding.error.reason"
    }
}
