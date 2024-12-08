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

import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.projectforge.carddav.CardDavXmlWriter.appendMultiStatusEnd
import org.projectforge.carddav.CardDavXmlWriter.appendMultiStatusStart
import org.projectforge.carddav.model.Contact
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.utils.ResponseUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

private val log = KotlinLogging.logger {}

internal object ReportRequestHandler {
    /**
     * Handles a PROPFIND request for the current user principal.
     * This is the initial call to the CardDAV server for getting the
     * @param requestWrapper The request wrapper.
     * @param response The response.
     * @param user The user.
     * @see CardDavXmlWriter.generatePropFindResponse
     */
    fun handleSyncReportCall(
        requestWrapper: RequestWrapper,
        response: HttpServletResponse,
        contactList: List<Contact>
    ) {
        log.debug { "handleReportCall:  ${requestWrapper.request.method}: '${requestWrapper.requestURI}' body=[${requestWrapper.body}]" }
        val props = Prop.extractProps(requestWrapper.body)
        if (props.isEmpty()) {
            ResponseUtils.setValues(
                response, HttpStatus.BAD_REQUEST, contentType = MediaType.TEXT_PLAIN_VALUE,
                content = "No properties found in PROPFIND request."
            )
        }
        val syncToken = System.currentTimeMillis().toString() // Nothing better for now.
        val content = generateSyncReportResponse(syncToken, requestWrapper.requestURI, contactList)
        log.debug { "handleReportCall: response=[$content]" }
        ResponseUtils.setValues(
            response,
            HttpStatus.MULTI_STATUS,
            contentType = MediaType.APPLICATION_XML_VALUE,
            content = content,
        )
    }

    fun generateSyncReportResponse(href: String, syncToken: String? = null, contacts: List<Contact>): String {
        val sb = StringBuilder()
        appendMultiStatusStart(sb)
        sb.appendLine("  <sync-token>$syncToken</sync-token>")
        contacts.forEach { contact ->
            appendPropfindContact(sb, href, contact)
        }
        appendMultiStatusEnd(sb)
        return sb.toString()
    }

    fun appendPropfindContact(sb: StringBuilder, href: String, contact: Contact) {
        sb.appendLine(
            """
          <response>
            <href>${href}/ProjectForge-${contact.id ?: -1}.vcf</href>
            <propstat>
                <prop>
            <getetag>"${CardDavUtils.getETag(contact)}"</getetag>
                    <cr:address-data />""".trimIndent()
        )
        //                    <getetag>"${CardDavUtils.getETag(contact)}"</getetag>
        //            <cr:address-data>""".trimIndent()
        /*contact.vcardDataAsString.let { vcardData ->
            sb.appendLine(vcardData)
        }*/
        //            </cr:address-data>
        sb.appendLine(
            """
                </prop>
                <status>HTTP/1.1 200 OK</status>
            </propstat>
          </response>""".trimIndent()
        )
    }
}
