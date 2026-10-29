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

import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired

/**
 * The daos of the CVE-Radar: select access for the readers, everything else for the security team, decided by the
 * groups of [CveRadarAccess] (no user right of its own). Overrides `hasAccess` *and* `hasInsertAccess`: without a
 * user right the base class would allow everybody.
 */
abstract class CvrBaseDao<O : ExtendedBaseDO<Long>>(clazz: Class<O>) : BaseDao<O>(clazz) {
    @Autowired
    protected lateinit var cveRadarAccess: CveRadarAccess

    override fun hasAccess(
        user: PFUserDO,
        obj: O?,
        oldObj: O?,
        operationType: OperationType,
        throwException: Boolean,
    ): Boolean {
        val allowed = if (operationType == OperationType.SELECT) {
            cveRadarAccess.isReader(user)
        } else {
            cveRadarAccess.isSecurityTeam(user)
        }
        if (!allowed && throwException) {
            throw AccessException(user, CveRadarAccess.NO_ACCESS)
        }
        return allowed
    }

    override fun hasInsertAccess(user: PFUserDO): Boolean = cveRadarAccess.isSecurityTeam(user)
}
