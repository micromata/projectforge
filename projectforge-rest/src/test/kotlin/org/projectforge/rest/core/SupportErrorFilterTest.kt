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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.springframework.beans.TypeMismatchException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import java.net.ConnectException
import java.net.SocketTimeoutException

class SupportErrorFilterTest {
    @Test
    fun `only server problems are reported`() {
        Assertions.assertEquals(SupportErrorFilter.Kind.REQUEST, SupportErrorFilter.classify(NullPointerException()))
        Assertions.assertEquals(
            SupportErrorFilter.Kind.REQUEST,
            SupportErrorFilter.classify(RuntimeException("wrapped", IllegalStateException("bug"))),
        )
        // Known exceptions exempted by the registry.
        Assertions.assertNull(SupportErrorFilter.classify(IllegalStateException("Cannot start async")))
        // Shown to the user, also when wrapped (e.g. by a transaction).
        Assertions.assertNull(SupportErrorFilter.classify(UserException("some.key")))
        Assertions.assertNull(SupportErrorFilter.classify(RuntimeException(AccessException("access.exception.noAccess"))))
        // The client's fault.
        Assertions.assertNull(
            SupportErrorFilter.classify(HttpMessageNotReadableException("JSON parse error", MockHttpInputMessage(ByteArray(0))))
        )
        Assertions.assertNull(SupportErrorFilter.classify(TypeMismatchException("x", Long::class.java)))
        Assertions.assertNull(SupportErrorFilter.classify(HttpRequestMethodNotSupportedException("PATCH")))
        Assertions.assertNull(SupportErrorFilter.classify(MissingServletRequestParameterException("id", "Long")))
        Assertions.assertNull(SupportErrorFilter.classify(HttpMediaTypeNotSupportedException("text/plain")))
        Assertions.assertNull(SupportErrorFilter.classify(ClientAbortException()))
        Assertions.assertNull(SupportErrorFilter.classify(AsyncRequestTimeoutException()))
    }

    @Test
    fun `an unreachable remote system is reported even though the registry knows it`() {
        Assertions.assertEquals(
            SupportErrorFilter.Kind.EXTERNAL,
            SupportErrorFilter.classify(IllegalStateException("LDAP down", ConnectException("Connection refused"))),
        )
        Assertions.assertEquals(SupportErrorFilter.Kind.EXTERNAL, SupportErrorFilter.classify(ConnectException("refused")))
        Assertions.assertEquals(SupportErrorFilter.Kind.EXTERNAL, SupportErrorFilter.classify(SocketTimeoutException("Read timed out")))
    }

    /** Stands in for Tomcat's exception, which is matched by its simple name. */
    private class ClientAbortException : java.io.IOException("Broken pipe")
}
