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
import org.projectforge.carddav.CardDavXmlUtils.appendMultiStatusEnd
import org.projectforge.carddav.CardDavXmlUtils.appendMultiStatusStart
import org.projectforge.carddav.model.Contact
import org.projectforge.rest.utils.ResponseUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

private val log = KotlinLogging.logger {}

internal object ReportRequestHandler {
    /**
     * Handles a PROPFIND request for the current user principal.
     * This is the initial call to the CardDAV server for getting the
     *
     * Example 1:
     * ```
     *   <sync-collection xmlns="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/" xmlns:d="DAV:">
     *     <sync-token>
     *           https://www.projectforge.org/ns/sync/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
     *         </sync-token>
     *     <sync-level>1</sync-level>
     *     <prop>
     *       <getetag/>
     *       <card:address-data/>
     *     </prop>
     *   </sync-collection>
     * ```
     *
     * @param requestWrapper The request wrapper.
     * @param response The response.
     * @param user The user.
     * @see CardDavXmlUtils.generatePropFindResponse
     */
    fun handleSyncReportCall(
        requestWrapper: RequestWrapper,
        response: HttpServletResponse,
        contactList: List<Contact>
    ) {
        log.debug { "handleReportCall:  ${requestWrapper.request.method}: '${requestWrapper.requestURI}' body=[${requestWrapper.body}]" }
        val props = CardDavUtils.handleProps(requestWrapper, response) ?: return // No properties response is handled in handleProps.
        val rootElement = CardDavXmlUtils.getRootElement(requestWrapper.body)
        val sb = StringBuilder()
        appendMultiStatusStart(sb)
        if (rootElement == "sync-collection") {
            val syncToken = System.currentTimeMillis().toString() // Nothing better for now.
            sb.appendLine("  <sync-token>$syncToken</sync-token>")
            contactList.forEach { contact ->
                appendPropfindContact(sb, requestWrapper.requestURI, contact, false)
            }
        } else if (rootElement == "addressbook-multiget") {
            val requestedAddressIds = CardDavXmlUtils.extractAddressIds(requestWrapper.body)
            val notFoundContactIds = requestedAddressIds.filter { addressId -> contactList.none { it.id == addressId } }
            notFoundContactIds.forEach { notFoundId ->
                generateNotFoundContact(sb, CardDavUtils.getVcfFileName(Contact(notFoundId)))
            }
            val requestedContacts = contactList.filter { contact -> requestedAddressIds.contains(contact.id) }
            requestedContacts.forEach { contact ->
                appendPropfindContact(sb, requestWrapper.requestURI, contact, true)
            }
        } else {
            ResponseUtils.setValues(
                response, HttpStatus.BAD_REQUEST, contentType = MediaType.TEXT_PLAIN_VALUE,
                content = "Unknown root element for REPORT-call '$rootElement'."
            )
            return
        }
        appendMultiStatusEnd(sb)
        val content = sb.toString()
        log.debug { "handleReportCall: response=[$content]" }
        CardDavUtils.setMultiStatusResponse(response, content)
    }

    /**
     * Generates a response for a not found contact.
     * ```
     * <response>
     *    <href>/1733697201904/ProjectForge-7833476.vcf</href>
     *    <status>HTTP/1.1 404 Not Found</status>
     * </response>
     * ```
     */
    fun generateNotFoundContact(sb: StringBuilder, href: String) {
        sb.appendLine(
            """
            |  <response>
            |    <href>$href</href>
            |    <status>HTTP/1.1 404 Not Found</status>
            |  </response>
        """.trimMargin()
        )
    }

    fun appendPropfindContact(sb: StringBuilder, href: String, contact: Contact, fullVCards: Boolean) {
        sb.appendLine(
            """
            |  <response>
            |    <href>${href}${CardDavUtils.getVcfFileName(contact)}</href>
            |    <propstat>
            |        <prop>
            |          <getetag>"${CardDavUtils.getETag(contact)}"</getetag>""".trimMargin()
        )
        if (fullVCards) {
            sb.append("        <cr:address-data>")
            contact.vcardData?.let {
                CardDavXmlUtils.appendEscapedXml(sb, it) // No indent here!!!
            }
            CardDavXmlUtils.appendLines(sb, "</cr:address-data>")
        } else {
            CardDavXmlUtils.appendLines(sb, "<cr:address-data />")
        }
        sb.appendLine(
            """
            |        </prop>
            |        <status>HTTP/1.1 200 OK</status>
            |    </propstat>
            |  </response>""".trimMargin()
        )
    }
}
