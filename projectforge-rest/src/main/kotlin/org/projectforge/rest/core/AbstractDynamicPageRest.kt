/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.core

import mu.KotlinLogging
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.ServerData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * For single dynamic React pages. Use this class especially for CSRF protection.
 */
abstract class AbstractDynamicPageRest {
    @Autowired
    protected lateinit var sessionCsrfCache: SessionCsrfCache

    /**
     * Creates new server data object with csrfToken.
     */
    protected fun createServerData(request: HttpServletRequest): ServerData {
        return ServerData(csrfToken = sessionCsrfCache.ensureAndGetToken(request))
    }

    protected fun validateCsrfToken(request: HttpServletRequest, postData: PostData<*>): ResponseEntity<ResponseAction>? {
        if (sessionCsrfCache.checkToken(request, postData.serverData?.csrfToken)) {
            // Check OK.
            return null
        }
        log.warn("Check of CSRF token failed, a validation error will be shown. Upsert of data declined: ${postData.data}")
        val validationErrors = mutableListOf<ValidationError>()
        validationErrors.add(ValidationError.create("errorpage.csrfError"))
        return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
    }
}
