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

package org.projectforge.rest

import org.projectforge.business.testdata.TestDataContext
import org.projectforge.business.testdata.TestDataContributor
import org.projectforge.business.testdata.UserTestDataContributor
import org.projectforge.business.user.UserPrefCacheDataKey
import org.projectforge.business.user.UserPrefDao
import org.projectforge.framework.persistence.database.DatabaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The team vacation calendar ([VacationExportRest]) of the administrator and of Laura (website screenshots) opens
 * with the whole team and its availability entries, so it isn't empty after the first login.
 */
@Service
open class VacationExportTestDataContributor : TestDataContributor {
    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    override val order = 65

    override fun seed(ctx: TestDataContext) {
        val groupId = ctx.groups[UserTestDataContributor.ORGANIZATION]?.id ?: return
        listOf(DatabaseService.DEFAULT_ADMIN_USER, "laura").forEach { username ->
            val pref = VacationExportRest.VacationExportUserPref(groupIds = listOf(groupId), showAvailabilities = true)
            // Same key as VacationExportRest.getUserPref, written directly, because nobody is logged in.
            userPrefDao.insertOrUpdate(
                ctx.user(username).id!!,
                UserPrefCacheDataKey(VacationExportRest.USER_PREF_AREA, VacationExportRest.USER_PREF_NAME),
                pref,
                checkAccess = false,
            )
        }
    }
}
