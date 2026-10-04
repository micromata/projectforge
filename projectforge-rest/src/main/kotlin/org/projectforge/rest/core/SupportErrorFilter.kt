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

import org.projectforge.common.i18n.UserException
import org.projectforge.framework.integration.IntegrationErrors
import org.springframework.beans.TypeMismatchException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.context.request.async.AsyncRequestTimeoutException

/**
 * Decides which exceptions of a request go into the support error digest
 * ([org.projectforge.framework.support.SupportErrorDigest]).
 */
object SupportErrorFilter {
    enum class Kind {
        /** A remote system the request needed isn't reachable (connection error, timeout). */
        EXTERNAL,

        /** An unexpected server error, i.e. a bug. */
        REQUEST,
    }

    /**
     * Null for everything that isn't a server problem:
     * - a [UserException] anywhere in the cause chain (shown to the user, as Wicket's error page exempted it),
     * - a request the client got wrong (malformed body, wrong parameter type, unknown method or media type,
     *   missing parameter: Spring's [ErrorResponse] exceptions, [HttpMessageNotReadableException],
     *   [TypeMismatchException]),
     * - a client that went away (Tomcat's ClientAbortException, matched by name; async request timeouts),
     * - every known exception [GlobalExceptionRegistry] exempts, unless it's an unreachable remote system.
     */
    fun classify(ex: Throwable): Kind? {
        val chain = generateSequence(ex) { it.cause.takeIf { cause -> cause !== it } }.take(20).toList()
        if (chain.any { it is UserException }) {
            return null
        }
        if (chain.any { it is ErrorResponse || it is HttpMessageNotReadableException || it is TypeMismatchException }) {
            return null
        }
        if (chain.any {
                it is AsyncRequestTimeoutException || it is AsyncRequestNotUsableException ||
                        it::class.java.simpleName == "ClientAbortException"
            }) {
            return null
        }
        if (IntegrationErrors.isConnectionError(ex)) {
            return Kind.EXTERNAL
        }
        return if (GlobalExceptionRegistry.sendMailToDevelopers(ex)) Kind.REQUEST else null
    }
}
