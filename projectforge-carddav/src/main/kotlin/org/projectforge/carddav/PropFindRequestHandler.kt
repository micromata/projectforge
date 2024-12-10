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
import org.projectforge.carddav.CardDavUtils.CARD
import org.projectforge.carddav.CardDavUtils.CS
import org.projectforge.carddav.CardDavUtils.D
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
                |  <$D:response>
                |    <$D:href>$href</$D:href>
                |    <$D:propstat>
                |      <$D:prop>
            """.trimMargin()
        )
        if (props.contains(Prop.ADDRESSBOOK_HOME_SET)) {
            sb.appendLine(
                """
                |        <$CARD:addressbook-home-set>
                |          <$D:href>${CardDavUtils.getUsersUrl(href, user, "addressbooks/")}</$D:href>
                |        </$CARD:addressbook-home-set>
                """.trimMargin()
            )
        }
        if (props.contains(Prop.EMAIL_ADDRESS_SET)) {
            sb.appendLine(
                """
                |        <$CS:email-address-set>
                |          <$D:href>mailto:${user.email ?: "${user.username}@example.com"}</$D:href>
                |        </$CS:email-address-set>
                """.trimMargin()
            )
        }
        if (props.contains(Prop.RESOURCETYPE)) {
            sb.appendLine("        <$D:resourcetype>")
            if (!props.contains(Prop.PRINCIPAL_URL)) {
                // Apple-client requests PRINCIPAL_URL and doesn't expect <card:addressbook/> in the response. But Thunderbird expects it.
                sb.appendLine("          <$CARD:addressbook />")
            }
            sb.appendLine("          <$D:collection />")
            sb.appendLine("        </$D:resourcetype>")
        }
        if (props.contains(Prop.GETCTAG)) {
            sb.appendLine("        <$CS:getctag>\"88d6c17fa866ef38e6e0122a59bf3da10a66daa042860116c88979a50c025eb9\"</$CS:getctag>")
        }
        if (props.contains(Prop.GETETAG)) {
            sb.appendLine("        <$D:getetag>\"88d6c17fa866ef38e6e0122a59bf3da10a66daa042860116c88979a50c025eb9\"</$D:getetag>")
        }
        if (props.contains(Prop.SYNCTOKEN)) {
            // This sync token is just a random constant string for testing.
            sb.appendLine(
                """
                |        <$D:sync-token>
                |          https://www.projectforge.org/ns/sync/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
                |        </$D:sync-token>
                """.trimMargin()
            )
        }
        if (props.contains(Prop.DISPLAYNAME)) {
            sb.appendLine("        <$D:displayname>${getUsersAddressbookDisplayName(user)}</$D:displayname>")
            // appendPropLines(sb, "<getcontenttype>text/vcard</getcontenttype>")
        }
        if (props.contains(Prop.CURRENT_USER_PRINCIPAL)) {
            sb.appendLine(
                """
                |        <$D:current-user-principal>
                |          <$D:href>${CardDavUtils.getPrincipalsUsersUrl(href, user)}</$D:href>
                |        </$D:current-user-principal>
                """.trimMargin()
            )
        }
        if (props.contains(Prop.PRINCIPAL_URL)) {
            sb.appendLine(
                """
                |        <$D:principal-URL>
                |          <$D:href>${CardDavUtils.getPrincipalsUsersUrl(href, user)}</$D:href>
                |        </$D:principal-URL>
                """.trimMargin()
            )
        }
        if (props.contains(Prop.PRINCIPAL_COLLECTION_SET)) {
            sb.appendLine(
                """
                |        <$D:principal-collection-set>
                |          <$D:href>${CardDavUtils.getUrl(href, "/principals")}</$D:href>
                |        </$D:principal-collection-set>
                """.trimMargin()
            )
        }
        if (props.contains(Prop.CURRENT_USER_PRIVILEGE_SET)) {
            sb.appendLine(
                """
                |        <$D:current-user-privilege-set>
                |          <$D:privilege><$D:read /></$D:privilege>
                |          <$D:privilege><$D:all /></$D:privilege>
                |          <$D:privilege><$D:write /></$D:privilege>
                |          <$D:privilege><$D:write-properties /></$D:privilege>
                |          <$D:privilege><$D:write-content /></$D:privilege>
                |        </$D:current-user-privilege-set>
                """.trimMargin()
            )
        }
        if (props.contains(Prop.RESOURCE_ID)) {
            // A unique, immutable identifier for the user or resource. Typically, a urn:uuid.
            sb.appendLine("        <$D:resource-id>${CardDavUtils.generateDeterministicUUID(user)}</$D:resource-id>")
        }
        if (props.contains(Prop.SUPPORTED_REPORT_SET)) {
            sb.appendLine(
                """
                |        <$D:supported-report-set>
                |          <$D:supported-report>
                |            <$D:report>
                |              <$CARD:addressbook-query />
                |            </$D:report>
                |          </$D:supported-report>
                |          <$D:supported-report>
                |            <$D:report>
                |              <$D:sync-collection />
                |            </$D:report>
                |          </$D:supported-report>
                |        </$D:supported-report-set>
                """.trimMargin()
            )
        }
        sb.appendLine(
            """
                |      </$D:prop>
                |      <$D:status>HTTP/1.1 200 OK</$D:status>
                |    </$D:propstat>
            """.trimMargin()
        )
        if (props.contains(Prop.DIRECTORY_GATEWAY)) {
            // addUnsupportedProp(sb, Prop.DIRECTORY_GATEWAY)
        }
        sb.appendLine("  </$D:response>")
        appendMultiStatusEnd(sb)
        return sb.toString()
    }

    fun addUnsupportedProp(sb: StringBuilder, prop: Prop) {
        sb.appendLine(
            """
                |    <$D:propstat>
                |      <$D:prop>
                |        <${prop.xmlns}:${prop.str} />
                |      </$D:prop>
                |      <$D:status>HTTP/1.1 404 Not Found</$D:status>
                |    </$D:propstat>
                """.trimMargin()
        )
    }
}
