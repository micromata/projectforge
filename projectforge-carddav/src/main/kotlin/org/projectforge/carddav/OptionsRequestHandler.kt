/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.carddav

import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.projectforge.rest.utils.ResponseUtils
import org.springframework.http.HttpStatus

private val log = KotlinLogging.logger {}

internal object OptionsRequestHandler {
    /**
     * Handles the initial OPTIONS request to indicate supported methods and DAV capabilities.
     * This is the initial request to determine the allowed methods and DAV capabilities by the client.
     *
     * @return ResponseEntity with allowed methods and DAV capabilities in the headers.
     */
    fun handleDynamicOptions(requestWrapper: RequestWrapper, response: HttpServletResponse) {
        log.debug { "handlePropFindCall: ${requestWrapper.request.method}: '${requestWrapper.requestURI}' body=[${requestWrapper.body}]" }
        val requestedPath = requestWrapper.requestURI
        // Indicate DAV capabilities
        response.addHeader("DAV", "1, 2, 3, addressbook")

        // Indicate allowed HTTP methods
        // add("Allow", "OPTIONS, GET, HEAD, POST, PUT, DELETE, PROPFIND, REPORT")
        response.addHeader("Allow", "OPTIONS, GET, DELETE, PROPFIND, REPORT")

        // Expose additional headers for client visibility
        response.addHeader("Access-Control-Expose-Headers", "DAV, Allow")

        // Additional headers for user-specific paths
        if (requestedPath.contains("/users/")) {
            // Example: You might add user-specific behavior here
            response.addHeader("Content-Type", "application/xml")
        }
        ResponseUtils.setValues(response, status = HttpStatus.OK)
        CardDavServerDebugWriter.writeRequestResponseLogInTestMode(requestWrapper, response)
    }
}
