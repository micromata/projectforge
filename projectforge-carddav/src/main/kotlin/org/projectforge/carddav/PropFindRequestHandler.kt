/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.carddav.CardDavUtils.D
import org.projectforge.carddav.CardDavXmlUtils.appendMultiStatusEnd
import org.projectforge.carddav.CardDavXmlUtils.appendMultiStatusStart

private val log = KotlinLogging.logger {}

internal object PropFindRequestHandler {
    /**
     * Handles a PROPFIND request for the current user principal.
     * This is the initial call to the CardDAV server for getting the
     * @param requestWrapper The request wrapper.
     * @param response The response.
     * @param user The user.
     * @see CardDavXmlUtils.generatePropFindResponse
     */
    fun handlePropFindCall(writerContext: WriterContext) {
        val requestWrapper = writerContext.requestWrapper
        val response = writerContext.response
        log.debug { "handlePropFindCall: ${requestWrapper.request.method}: '${requestWrapper.requestURI}' body=[${requestWrapper.body}]" }
        CardDavUtils.handleProps(requestWrapper, response) ?: return // No properties response is handled in handleProps.
        val content = generatePropFindResponse(writerContext)
        log.debug { "handlePropFindCall: response.content=[${CardDavServerDebugWriter.sanitizeContent(content)}]" }
        CardDavUtils.setMultiStatusResponse(response, content)
        CardDavServerDebugWriter.writeRequestResponseLogInTestMode(requestWrapper, response, content)
    }

    /**
     * /principals/ or /carddav/principals/
     */
    fun handlePropFindPrincipalsCall(writerContext: WriterContext) {
        val requestWrapper = writerContext.requestWrapper
        val response = writerContext.response
        log.debug { "handlePropFindPrincipalsCall: ${requestWrapper.request.method}: '${requestWrapper.requestURI}' body=[${requestWrapper.body}]" }
        CardDavUtils.handleProps(requestWrapper, response) ?: return // No properties response is handled in handleProps.
        val content = generatePropFindResponse(writerContext)
        log.debug { "handlePropFindPrincipalsCall: response=[${CardDavServerDebugWriter.sanitizeContent(content)}]" }
        CardDavUtils.setMultiStatusResponse(response, content)
        CardDavServerDebugWriter.writeRequestResponseLogInTestMode(requestWrapper, response, content)
    }

    /**
     * Generates a response for a PROPFIND request.
     * Information about resources and privileges are returned, if requested.
     * @param requestWrapper The request wrapper.
     * @param user The user.
     * @param props The properties to include in the response.
     * @return The response as a string.
     */
    fun generatePropFindResponse(writerContext: WriterContext): String {
        val href = writerContext.href
        val sb = StringBuilder()
        appendMultiStatusStart(sb)
        sb.appendLine(
            """
                |  <$D:response>
                |    <$D:href>$href</$D:href>
                |    <$D:propstat>
                |      <$D:prop>
            """.trimMargin()
        )
        PropWriter.appendSupportedProps(sb, writerContext)
        sb.appendLine(
            """
                |      </$D:prop>
                |      <$D:status>HTTP/1.1 200 OK</$D:status>
                |    </$D:propstat>
            """.trimMargin()
        )
        /*
        val unsupportedProps = props.filter { it.supported.not() }
        if (unsupportedProps.isNotEmpty()) {
            sb.appendLine("    <$D:propstat>")
            sb.appendLine("      <$D:prop>")
            unsupportedProps.forEach { prop ->
                sb.appendLine("        <${prop.xmlns}:${prop.tag} />")
            }
            sb.appendLine("      </$D:prop>")
            sb.appendLine("      <$D:status>HTTP/1.1 404 Not Found</$D:status>")
            sb.appendLine("    </$D:propstat>")
        }*/
        sb.appendLine("  </$D:response>")
        appendMultiStatusEnd(sb)
        return sb.toString()
    }
}
