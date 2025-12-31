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

package org.projectforge.rest.core

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.IOException
import java.net.NoRouteToHostException
import java.util.Collections.emptyEnumeration

class GlobalExceptionRegistryTest {
    class StalePageException(msg: String) : Exception(msg)

    val request = Mockito.mock(HttpServletRequest::class.java)

    init {
        Mockito.`when`(request.attributeNames).thenReturn(emptyEnumeration())
        Mockito.`when`(request.headerNames).thenReturn(emptyEnumeration())
        Mockito.`when`(request.parameterNames).thenReturn(emptyEnumeration())
    }

    @Test
    fun testExceptionHandling() {
        GlobalExceptionRegistry.registerExInfo(StalePageException::class.java, "Stale page")
        check(StalePageException("msg"), "Stale page: msg")
        check(IOException("Die Verbindung wurde vom Kommunikationspartner zurückgesetzt"), "Connection lost: Die Verbindung wurde vom Kommunikationspartner zurückgesetzt")
        check(NoRouteToHostException("msg"), "No route to host: msg")
        Assertions.assertTrue(GlobalExceptionRegistry.sendMailToDevelopers(NullPointerException()))
        Assertions.assertFalse(GlobalExceptionRegistry.sendMailToDevelopers(IllegalStateException("Cannot start async")))
        Assertions.assertTrue(GlobalExceptionRegistry.sendMailToDevelopers(IllegalStateException("Unknown illegal state")))
    }

    fun check(ex: Throwable, expectedMessage: String, status: HttpStatus = HttpStatus.BAD_REQUEST) {
        Assertions.assertNotNull(GlobalExceptionRegistry.findExInfo(ex)?.also {
            Assertions.assertEquals(status, it.status)
        })
        Assertions.assertFalse(GlobalExceptionRegistry.sendMailToDevelopers(ex))
        GlobalDefaultExceptionHandler().also { handler ->
            handler.defaultErrorHandler(request, ex).let { response ->
                response as ResponseEntity<*>
                Assertions.assertEquals(expectedMessage, response.body)
            }
        }
    }
}
