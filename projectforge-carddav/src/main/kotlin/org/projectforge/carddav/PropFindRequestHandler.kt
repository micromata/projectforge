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
import org.projectforge.framework.persistence.user.entities.PFUserDO

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
        val props = CardDavUtils.handleProps(requestWrapper, response)
            ?: return // No properties response is handled in handleProps.
        val content = generatePropFindResponse(requestWrapper, user, props)
        log.debug { "handlePropFindCall: response=[$content]" }
        CardDavUtils.setMultiStatusResponse(response, content)
    }

    /**
     * /principals/ or /carddav/principals/
     */
    fun handlePropFindPrincipalsCall(requestWrapper: RequestWrapper, response: HttpServletResponse, user: PFUserDO) {
        log.debug { "handlePropFindPrincipalsCall: ${requestWrapper.request.method}: '${requestWrapper.requestURI}' body=[${requestWrapper.body}]" }
        val props = CardDavUtils.handleProps(requestWrapper, response)
            ?: return // No properties response is handled in handleProps.
        val content = generatePropFindResponse(requestWrapper, user, props)
        log.debug { "handlePropFindPrincipalsCall: response=[$content]" }
        CardDavUtils.setMultiStatusResponse(response, content)
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
        val href = requestWrapper.requestURI
        val sb = StringBuilder()
        appendMultiStatusStart(sb)
        sb.appendLine(
            """
                |  <d:response>
                |    <d:href>$href</d:href>
                |    <d:propstat>
                |      <d:prop>
            """.trimMargin()
        )
        if (props.contains(Prop.RESOURCETYPE)) {
            sb.appendLine("        <d:resourcetype>")
            if (!props.contains(Prop.PRINCIPAL_URL)) {
                // Apple-client requests PRINCIPAL_URL and doesn't expect <cr:addressbook/> in the response. But Thunderbird expects it.
                sb.appendLine("          <cr:addressbook />")
            }
            sb.appendLine("          <d:collection />")
            sb.appendLine("        </d:resourcetype>")
        }
        if (props.contains(Prop.GETCTAG)) {
            sb.appendLine("        <cs:getctag>\"88d6c17fa866ef38e6e0122a59bf3da10a66daa042860116c88979a50c025eb9\"</cs:getctag>")
        }
        if (props.contains(Prop.GETETAG)) {
            sb.appendLine("        <d:getetag>\"88d6c17fa866ef38e6e0122a59bf3da10a66daa042860116c88979a50c025eb9\"</d:getetag>")
        }
        if (props.contains(Prop.SYNCTOKEN)) {
            // This sync token is just a random constant string for testing.
            sb.appendLine(
                """
                |        <d:sync-token>
                |          https://www.projectforge.org/ns/sync/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
                |        </d:sync-token>
                """.trimMargin()
            )
        }
        if (props.contains(Prop.DISPLAYNAME)) {
            sb.appendLine("        <d:displayname>${getUsersAddressbookDisplayName(user)}</d:displayname>")
            // appendPropLines(sb, "<getcontenttype>text/vcard</getcontenttype>")
        }
        if (props.contains(Prop.CURRENT_USER_PRINCIPAL)) {
            sb.appendLine(
                """
                |        <d:current-user-principal>
                |          <d:href>${CardDavUtils.getPrincipalsUsersUrl(href, user)}</d:href>
                |        </d:current-user-principal>
                """.trimMargin()
            )
        }
        if (props.contains(Prop.PRINCIPAL_URL)) {
            sb.appendLine(
                """
                |        <d:principal-URL>
                |          <d:href>${CardDavUtils.getPrincipalsUsersUrl(href, user)}</d:href>
                |        </d:principal-URL>
                """.trimMargin()
            )
        }
        if (props.contains(Prop.CURRENT_USER_PRIVILEGE_SET)) {
            sb.appendLine(
                """
                |        <d:current-user-privilege-set>
                |          <d:privilege><d:read /></d:privilege>
                |          <d:privilege><d:all /></d:privilege>
                |          <d:privilege><d:write /></d:privilege>
                |          <d:privilege><d:write-properties /></d:privilege>
                |          <d:privilege><d:write-content /></d:privilege>
                |        </d:current-user-privilege-set>
                """.trimMargin()
            )
        }
        sb.appendLine(
            """
                |      </d:prop>
                |      <d:status>HTTP/1.1 200 OK</d:status>
                |    </d:propstat>
                |  </d:response>
            """.trimMargin()
        )
        appendMultiStatusEnd(sb)
        return sb.toString()
    }

    // <?xml version="1.0" encoding="UTF-8"?>
    //<d:multistatus xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav" xmlns:cs="http://calendarserver.org/ns/">
    //  <d:response>
    //    <d:href>/principals/users/kai/</d:href>
    //    <d:propstat>
    //      <d:prop>
    //        <d:current-user-principal>
    //          <d:href>/principals/users/kai/</d:href>
    //        </d:current-user-principal>
    //        <d:principal-URL>
    //          <d:href>/principals/users/kai/</d:href>
    //        </d:principal-URL>
    //        <cs:email-address-set>
    //          <d:href>mailto:kai@example.com</d:href>
    //        </cs:email-address-set>
    //      </d:prop>
    //      <d:status>HTTP/1.1 200 OK</d:status>
    //    </d:propstat>
    //  </d:response>
    //</d:multistatus>
}
