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

import org.projectforge.business.PfCaches
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrCaseKind
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.model.CvrPriority
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.User

/**
 * A case as the projectforge-next pages read and write it (see [CvrCaseEntityRest]). The values are copied one by
 * one: the assignee as id and display name from the user cache.
 */
class CvrCase(
    var code: String? = null,
    var kind: CvrCaseKind? = null,
    var title: String? = null,
    var description: String? = null,
    var status: CvrCaseStatus? = null,
    var priority: CvrPriority? = null,
    var assignee: User? = null,
    var techMeasures: String? = null,
    var nonTechMeasures: String? = null,
    var clientAdvisory: Boolean = false,
    var minClientAgentVersion: String? = null,
    var clientAgentCheckId: String? = null,
    var scanPluginIds: String? = null,
    var cveIds: String? = null,
    var closeReason: String? = null,
) : BaseDTO<CvrCaseDO>() {
    override fun copyFrom(src: CvrCaseDO) {
        copyFrom4ListRow(src)
        description = src.description
        techMeasures = src.techMeasures
        nonTechMeasures = src.nonTechMeasures
        minClientAgentVersion = src.minClientAgentVersion
        clientAgentCheckId = src.clientAgentCheckId
        scanPluginIds = src.scanPluginIds
        cveIds = src.cveIds
        closeReason = src.closeReason
    }

    override fun copyFrom4ListRow(src: CvrCaseDO) {
        id = src.id
        deleted = src.deleted
        copyAuditFieldsFrom(src)
        code = src.code
        kind = src.kind
        title = src.title
        status = src.status
        priority = src.priority
        clientAdvisory = src.clientAdvisory
        assignee = PfCaches.instance.getUser(src.assigneeId)?.let { User(id = it.id, displayName = it.displayName) }
    }

    override fun copyTo(dest: CvrCaseDO) {
        dest.id = id
        dest.deleted = deleted
        dest.code = code
        dest.kind = kind
        dest.title = title
        dest.description = description
        dest.status = status
        dest.priority = priority
        dest.assignee = assignee?.id?.let { PFUserDO().also { user -> user.id = it } }
        dest.techMeasures = techMeasures
        dest.nonTechMeasures = nonTechMeasures
        dest.clientAdvisory = clientAdvisory
        dest.minClientAgentVersion = minClientAgentVersion
        dest.clientAgentCheckId = clientAgentCheckId
        dest.scanPluginIds = scanPluginIds
        dest.cveIds = cveIds
        dest.closeReason = closeReason
    }
}
