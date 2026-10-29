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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.cveradar.CveRadarAccess
import org.projectforge.plugins.cveradar.CveRadarPlugin
import org.projectforge.plugins.cveradar.CveRadarService
import org.projectforge.plugins.cveradar.CvrCaseDao
import org.projectforge.plugins.cveradar.CvrCaseFileService
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrCaseKind
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the cases (`/rs/cvrcase`), serving the list and the edit form of
 * projectforge-next (components/features/cve-radar/cases). Access by the dao: read with the read-only right, write
 * by the security team ([org.projectforge.plugins.cveradar.CveRadarAccess]).
 *
 * Closing through the form follows the rules of the close action (CLS-05): with a reason, and with open items by a
 * security officer only.
 */
@RestController
@RequestMapping("${Rest.URL}/${CveRadarPlugin.CASE_CATEGORY}")
class CvrCaseEntityRest : AbstractDTOEntityRest<CvrCaseDO, CvrCase, CvrCaseDao>(
    CvrCaseDao::class.java,
    "plugins.cveradar.cases",
) {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var fileService: CvrCaseFileService

    @Autowired
    private lateinit var cveRadarService: CveRadarService

    /** Called by Spring before every endpoint of this controller. */
    @ModelAttribute
    fun checkPluginActive() = access.checkPluginActive()

    override fun newDTO(): CvrCase = CvrCase()

    /** A new case is an incident, new, assigned to the logged-in user. */
    override fun newBaseDO(request: HttpServletRequest?): CvrCaseDO {
        val case = super.newBaseDO(request)
        case.kind = CvrCaseKind.INCIDENT
        case.status = CvrCaseStatus.NEW
        case.assignee = ThreadLocalUserContext.loggedInUser
        return case
    }

    override fun transformFromDB(obj: CvrCaseDO, editMode: Boolean): CvrCase {
        return CvrCase().also { it.copyFrom(obj) }
    }

    override fun transformForDB(dto: CvrCase): CvrCaseDO {
        return CvrCaseDO().also { dto.copyTo(it) }
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: CvrCase) {
        super.validate(validationErrors, dto)
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val existing = dto.id?.let { baseDao.find(it, checkAccess = false) }
        if (existing == null) {
            if (dto.priority != null && dto.priorityReason.isNullOrBlank()) {
                validationErrors.add(
                    ValidationError(translateMsg(UserException(CvrCaseFileService.ERROR_PRIORITY_REASON)), fieldId = "priorityReason"),
                )
            }
        } else {
            try {
                // Conflict detection (TRI-04): the editor sends the lastUpdate it loaded, without it nothing is saved.
                baseDao.checkNotChanged(existing.id!!, dto.lastUpdate)
            } catch (ex: UserException) {
                validationErrors.add(ValidationError(translateMsg(ex)))
                return
            }
            if (dto.priority != existing.priority) {
                // The binding priority (PRIO-02, TRI-03): with a reason, by the assignee or a security officer.
                if (dto.priorityReason.isNullOrBlank()) {
                    validationErrors.add(
                        ValidationError(translateMsg(UserException(CvrCaseFileService.ERROR_PRIORITY_REASON)), fieldId = "priorityReason"),
                    )
                } else if (!fileService.canSetPriority(user, existing)) {
                    validationErrors.add(
                        ValidationError(translateMsg(UserException(CvrCaseFileService.ERROR_PRIORITY_RIGHTS)), fieldId = "priority"),
                    )
                }
            }
        }
        // NOT_RELEVANT closes the case as well (CvrCaseStatus.isClosed): same rules as CLOSED (CLS-05).
        if (dto.status?.isClosed != true) {
            return
        }
        val stored = dto.id?.let { baseDao.find(it, checkAccess = false) }
        if (stored?.status?.isClosed == true) {
            return
        }
        try {
            cveRadarService.checkClosing(
                ThreadLocalUserContext.requiredLoggedInUser,
                stored ?: transformForDB(dto),
                dto.closeReason,
            )
        } catch (ex: UserException) {
            validationErrors.add(ValidationError(translateMsg(ex), fieldId = "closeReason"))
        }
    }
}
