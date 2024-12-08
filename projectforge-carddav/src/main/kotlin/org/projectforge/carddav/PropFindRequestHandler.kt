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
import org.projectforge.carddav.CardDavUtils.getUsersAddressbookDisplayName
import org.projectforge.carddav.CardDavXmlUtils.appendMultiStatusEnd
import org.projectforge.carddav.CardDavXmlUtils.appendMultiStatusStart
import org.projectforge.carddav.CardDavXmlUtils.appendPropLines
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.utils.ResponseUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

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
    fun handlePropFindCall(requestWrapper: RequestWrapper, response: HttpServletResponse, user: PFUserDO) {
        log.debug { "handlePropFindCall: ${requestWrapper.request.method}: '${requestWrapper.requestURI}' body=[${requestWrapper.body}]" }
        val props = Prop.extractProps(requestWrapper.body)
        if (props.isEmpty()) {
            ResponseUtils.setValues(
                response, HttpStatus.BAD_REQUEST, contentType = MediaType.TEXT_PLAIN_VALUE,
                content = "No properties found in PROPFIND request."
            )
        }
        val content = generatePropFindResponse(requestWrapper, user, props)
        log.debug { "handlePropFindCall: response=[$content]" }
        ResponseUtils.setValues(
            response,
            HttpStatus.MULTI_STATUS,
            contentType = MediaType.APPLICATION_XML_VALUE,
            content = content,
        )
    }

    /**
     * Generates a response for a PROPFIND request.
     * Information about resources and privileges are returned, if requested.
     * @param requestWrapper The request wrapper.
     * @param user The user.
     * @param props The properties to include in the response.
     * @return The response as a string.
     */
    fun generatePropFindResponse(
        requestWrapper: RequestWrapper,
        user: PFUserDO,
        props: List<Prop>
    ): String {
        val href = "${requestWrapper.baseUrl}/users/${user.username}/"
        val sb = StringBuilder()
        appendMultiStatusStart(sb)
        sb.appendLine(
            """
            |    <response>
            |        <href>$href</href>
            |        <propstat>
            |            <prop>""".trimMargin()
        )
        if (props.contains(Prop.RESOURCETYPE)) {
            appendPropLines(sb, "<resourcetype>")
            appendPropLines(sb, "  <cr:addressbook />")
            appendPropLines(sb, "  <collection />")
            appendPropLines(sb, "</resourcetype>")
        }
        if (props.contains(Prop.GETCTAG)) {
            appendPropLines(
                sb,
                "<cs:getctag>\"88d6c17fa866ef38e6e0122a59bf3da10a66daa042860116c88979a50c025eb9\"</cs:getctag>"
            )
        }
        if (props.contains(Prop.GETETAG)) {
            appendPropLines(
                sb,
                "<getetag>\"88d6c17fa866ef38e6e0122a59bf3da10a66daa042860116c88979a50c025eb9\"</getetag>"
            )
        }
        if (props.contains(Prop.SYNCTOKEN)) {
            appendPropLines(sb, "<sync-token>")
            appendPropLines(
                sb,
                "  https://www.projectforge.org/ns/sync/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            )
            appendPropLines(sb, "</sync-token>")
        }
        if (props.contains(Prop.DISPLAYNAME)) {
            appendPropLines(sb, "<displayname>${getUsersAddressbookDisplayName(user)}</displayname>")
            // appendPropLines(sb, "<getcontenttype>text/vcard</getcontenttype>")
        }
        if (props.contains(Prop.CURRENT_USER_PRINCIPAL)) {
            appendPropLines(sb, "<current-user-principal>")
            appendPropLines(sb, "  <href>$href</href>")
            appendPropLines(sb, "</current-user-principal>")
        }
        if (props.contains(Prop.CURRENT_USER_PRIVILEGE_SET)) {
            appendPropLines(sb, "<current-user-privilege-set>")
            appendPropLines(sb, "  <privilege><read /></privilege>")
            appendPropLines(sb, "  <privilege><all /></privilege>")
            appendPropLines(sb, "  <privilege><write /></privilege>")
            appendPropLines(sb, "  <privilege><write-properties /></privilege>")
            appendPropLines(sb, "  <privilege><write-content /></privilege>")
            appendPropLines(sb, "</current-user-privilege-set>")
        }
        sb.appendLine(
            """
            |          </prop>
            |          <status>HTTP/1.1 200 OK</status>
            |      </propstat>
            |  </response>
        """.trimMargin()
        )
        appendMultiStatusEnd(sb)
        return sb.toString()
    }
}
