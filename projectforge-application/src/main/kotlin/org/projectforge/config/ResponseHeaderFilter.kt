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

package org.projectforge.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletResponse

/**
 * Sets every init parameter of the filter registration as a response header (name = header, value = header value),
 * e.g. `Cache-Control: public, max-age=7200` for the static resources (see [WebXMLInitializer]).
 */
class ResponseHeaderFilter : Filter {
    private var headers: Map<String, String> = emptyMap()

    override fun init(filterConfig: FilterConfig) {
        headers = filterConfig.initParameterNames.toList().associateWith { filterConfig.getInitParameter(it) }
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpResponse = response as HttpServletResponse
        headers.forEach { (name, value) -> httpResponse.setHeader(name, value) }
        chain.doFilter(request, response)
    }
}
