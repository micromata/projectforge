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

package org.projectforge.business.fibu.datev

import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessChecker

/**
 * The access right of the DATEV import (accounting records and chart of accounts). The import itself lives in
 * projectforge-rest (`DatevRecordImportRest`, `DatevAccountImportRest`); the accounting-record pages are gated
 * by the same right.
 */
object DatevImportService {
    val USER_RIGHT_ID: UserRightId = UserRightId.FIBU_DATEV_IMPORT

    /**
     * Has the user the right FIBU_DATEV_IMPORT (value true)?
     *
     * @param accessChecker
     * @see UserRightId.FIBU_DATEV_IMPORT
     */
    fun hasRight(accessChecker: AccessChecker): Boolean {
        return hasRight(accessChecker, false)
    }

    /**
     * Has the user the right FIBU_DATEV_IMPORT (value true)?
     *
     * @param accessChecker
     * @throws AccessException
     * @see UserRightId.FIBU_DATEV_IMPORT
     */
    fun checkLoggedinUserRight(accessChecker: AccessChecker): Boolean {
        return hasRight(accessChecker, true)
    }

    private fun hasRight(accessChecker: AccessChecker, throwException: Boolean): Boolean {
        return accessChecker.hasLoggedInUserRight(USER_RIGHT_ID, throwException, UserRightValue.TRUE)
    }
}
