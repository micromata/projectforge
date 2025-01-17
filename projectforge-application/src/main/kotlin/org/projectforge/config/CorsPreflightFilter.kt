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

package org.projectforge.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import java.io.IOException


@Component
class CorsPreflightFilter : Filter {
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        // Überprüfe den Origin-Header der Anfrage
        val origin = httpRequest.getHeader("Origin")

        // Nur Anfragen mit einem Origin behandeln
        if (origin != null) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin) // Setze den Origin der Anfrage
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With")
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true") // Erlaubt Credentials
            httpResponse.setHeader("Access-Control-Max-Age", "3600")
        }

        // Preflight-Request (OPTIONS) direkt beantworten
        if ("OPTIONS".equals(httpRequest.method, ignoreCase = true)) {
            httpResponse.status = HttpServletResponse.SC_OK
        } else {
            // Andere Anfragen weiterleiten
            chain.doFilter(request, response)
        }
    }
}
