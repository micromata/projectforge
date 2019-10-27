/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.config

import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.RequestDispatcher
import javax.servlet.http.HttpServletRequest


@Controller
class MyErrorController : ErrorController {

    override fun getErrorPath(): String {
        return "/error"
    }

    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest): String {
        val status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)

        if (status != null) {
            val statusCode = Integer.valueOf(status!!.toString())

            if (statusCode === HttpStatus.NOT_FOUND.value()) {
                // return "error-404"
            } else if (statusCode === HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                // return "error-500"
            }
        }
        return "/index.html"
    }
}
