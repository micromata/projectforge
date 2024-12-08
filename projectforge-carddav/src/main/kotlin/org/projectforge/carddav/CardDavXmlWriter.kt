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

import org.projectforge.carddav.model.Contact
import org.projectforge.carddav.model.User
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.entities.PFUserDO

internal object CardDavXmlWriter {
    /**
     * Generates a response for a PROPFIND request for a user.
     * @param displayname The display name of the user.
     * @param href The href of the user.
     * @param ctag A change tag that clients can use to detect whether content has changed (not standardized, but common).
     * @param syncToken A token to track incremental changes (optional if the server supports synchronization).
     * @return The response as a string.
     */
    fun generatePropfindUserResponse(
        displayname: String,
        href: String,
        ctag: String? = null,
        syncToken: String? = null,
    ): String {
        val syncTokenLine = if (syncToken != null) "\n                <sync-token>$syncToken</sync-token>" else ""
        val ctagLine = if (ctag != null) "\n                <cs:getctag>$ctag</cs:getctag>" else ""
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <multistatus $XML_NS>
            <response>
                <href>$href</href>
                <propstat>
                    <prop>
                        <displayname>$displayname</displayname>
                        <getcontenttype>text/vcard</getcontenttype>$ctagLine$syncTokenLine
                    </prop>
                    <status>HTTP/1.1 200 OK</status>
                </propstat>
            </response>
        </multistatus>
    """.trimIndent()
        // Chat-GPT:
        // <multistatus xmlns:d="DAV:" xmlns:cs="http://calendarserver.org/ns/">
        //    <response>
        //        <href>/carddav/users/joe/</href>
        //        <propstat>
        //            <prop>
        //                <displayname>Joe's Address Book</displayname>
        //                <resourcetype>
        //                    <collection />
        //                    <cs:addressbook />
        //                </resourcetype>
        //                <cs:getctag>1234567890</cs:getctag>
        //                <sync-token>https://example.com/carddav/users/joe/sync-token</sync-token>
        //            </prop>
        //            <status>HTTP/1.1 200 OK</status>
        //        </propstat>
        //    </response>
        //</multistatus>
    }

    fun appendPropfindContact(sb: StringBuilder, user: User, contact: Contact) {
        sb.appendLine(
            """  <response>
    <href>/carddav/${user.userName}/addressbook/contact${contact.id}.vcf</href>
    <propstat>
      <prop>
        <getetag>"${CardDavUtils.getETag(contact)}"</getetag>
        <displayname>${contact.displayName}</displayname>
      </prop>
      <status>HTTP/1.1 200 OK</status>
    </propstat>
  </response>"""
        )
    }

    fun appendXmlPrefix(sb: StringBuilder) {
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    }

    fun appendMultiStatusStart(sb: StringBuilder, prependXmlPrefix: Boolean = true) {
        if (prependXmlPrefix) {
            appendXmlPrefix(sb)
        }
        sb.appendLine("<multistatus $XML_NS>")
    }

    fun appendMultiStatusEnd(sb: StringBuilder) {
        sb.appendLine("</multistatus>")
    }

    /**
     * Generates a response for a PROPFIND request for the current user principal.
     * This is the initial call to the CardDAV server for getting the principal.
     * Information about resources and privileges are returned, if requested.
     * @param requestWrapper The request wrapper.
     * @param user The user.
     * @param props The properties to include in the response.
     * @return The response as a string.
     */
    fun generatePropFindResponse(
        requestWrapper: RequestWrapper,
        user: PFUserDO,
        props: List<PropFindUtils.Prop>
    ): String {
        val href = "${requestWrapper.baseUrl}/users/${user.username}/"
        val sb = StringBuilder()
        appendMultiStatusStart(sb)
        sb.appendLine(
            """
                <response>
                    <href>$href</href>
                    <propstat>
                        <prop>""".trimIndent()
        )
        if (props.contains(PropFindUtils.Prop.RESOURCETYPE)) {
            appendPropLines(sb, "<resourcetype>")
            appendPropLines(sb, "  <cr:addressbook />")
            appendPropLines(sb, "  <collection />")
            appendPropLines(sb, "</resourcetype>")
        }
        if (props.contains(PropFindUtils.Prop.GETCTAG)) {
            appendPropLines(sb, "<cs:getctag>\"88d6c17fa866ef38e6e0122a59bf3da10a66daa042860116c88979a50c025eb9\"</cs:getctag>")
        }
        if (props.contains(PropFindUtils.Prop.GETETAG)) {
            appendPropLines(sb, "<getetag>\"88d6c17fa866ef38e6e0122a59bf3da10a66daa042860116c88979a50c025eb9\"</getetag>")
        }
        if (props.contains(PropFindUtils.Prop.SYNCTOKEN)) {
            appendPropLines(sb, "<sync-token>")
            appendPropLines(sb, "  https://www.projectforge.org/ns/sync/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
            appendPropLines(sb, "</sync-token>")
        }
        if (props.contains(PropFindUtils.Prop.DISPLAYNAME)) {
            appendPropLines(sb, "<displayname>${getUsersAddressbookDisplayName(user)}</displayname>")
            // appendPropLines(sb, "<getcontenttype>text/vcard</getcontenttype>")
        }
        if (props.contains(PropFindUtils.Prop.CURRENT_USER_PRINCIPAL)) {
            appendPropLines(sb, "<current-user-principal>")
            appendPropLines(sb, "  <href>$href</href>")
            appendPropLines(sb, "</current-user-principal>")
        }
        if (props.contains(PropFindUtils.Prop.CURRENT_USER_PRIVILEGE_SET)) {
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
                    </prop>
                    <status>HTTP/1.1 200 OK</status>
                </propstat>
            </response>
        """.trimIndent()
        )
        appendMultiStatusEnd(sb)
        return sb.toString()
    }

    /**
     * Generates a response for a PROPFIND request for the current user directory.
     * Handle PROPFIND requests: /carddav/users/<username>
     * The client expects information about the address book of the given user.
     * @param requestWrapper The request wrapper.
     * @param user The user.
     * @return The response as a string.
     */
    fun generatePropfindUserDirectory(requestWrapper: RequestWrapper, user: PFUserDO): String {
        val href = "${requestWrapper.baseUrl}/users/${user.username}/"
        val sb = StringBuilder()
        appendMultiStatusStart(sb)
        sb.appendLine(
            """
            <response>
              <href>$href</href>
              <propstat>
                <prop>
                  <resourcetype>
                    <collection />
                  </resourcetype>
                  <displayname>Kai's Home Collection</displayname>
                  <getcontenttype>text/vcard</getcontenttype>
                  <cr:addressbook-home-set>
                    <href>${href}addressBooks/</href>
                  </cr:addressbook-home-set>
                </prop>
                <status>HTTP/1.1 200 OK</status>
              </propstat>
            </response>
            <response>
                <href>${href}addressBooks/</href>
                <propstat>
                    <prop>
                        <resourcetype>
                            <collection />
                            <cr:addressbook />
                        </resourcetype>
                            <getcontenttype>text/vcard</getcontenttype>
                        <displayname>${getUsersAddressbookDisplayName(user)}</displayname>
                        <getcontenttype>text/vcard</getcontenttype>
                        <getetag>"test-e-123"</getetag>
                        <cr:getctag>"test-124"</cr:getctag>
                    </prop>
                    <status>HTTP/1.1 200 OK</status>
                </propstat>
            </response>""".trimIndent()
        )
        /*sb.appendLine("""
                <response>
                    <href>${href}contacts/john_doe.vcf</href>
                    <propstat>
                        <prop>
                            <getetag>"12345"</getetag>
                            <getcontenttype>text/vcard</getcontenttype>
                            <resourcetype />
                        </prop>
                        <status>HTTP/1.1 200 OK</status>
                    </propstat>
                </response>
        """.trimIndent())*/
        appendMultiStatusEnd(sb)
        return sb.toString()
    }

    private fun appendPropLines(sb: StringBuilder, vararg lines: String) {
        lines.forEach { line ->
            sb.appendLine("          $line")
        }
    }

    /*
    fun generateSyncReportResponse(
        displayname: String,
        href: String,
        ctag: String? = null,
        syncToken: String? = null,
    ): String {
        val syncTokenLine = if (syncToken != null) "\n                <sync-token>$syncToken</sync-token>" else ""
        val ctagLine = if (ctag != null) "\n                <cs:getctag>$ctag</cs:getctag>" else ""
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <multistatus $XML_NS>
            <response>
                <href>/users/joe/addressBooks/default/contact1.vcf</href>
                <propstat>
                    <prop>
                        <getetag>"123456789"</getetag>
                    </prop>
                    <status>HTTP/1.1 200 OK</status>
                </response>
            <response>
                <href>/users/joe/addressBooks/default/contact2.vcf</href>
                <propstat>
                    <prop>
                        <getetag>"987654321"</getetag>
                    </prop>
                    <status>HTTP/1.1 200 OK</status>
                </response>
            <sync-token>https://example.com/carddav/users/joe/new-sync-token</sync-token>
        </multistatus>""".trimIndent()
    }*/

    private fun getUsersAddressbookDisplayName(user: PFUserDO): String {
        return translate("address.cardDAV.addressbook.displayName")
    }

    private const val XML_NS =
        "xmlns:d=\"DAV:\" xmlns:cr=\"urn:ietf:params:xml:ns:carddav\" xmlns:cs=\"http://calendarserver.org/ns/\""
}
