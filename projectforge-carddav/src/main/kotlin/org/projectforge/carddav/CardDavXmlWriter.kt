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
        val syncTokenLine = if (syncToken != null) "\n                <d:sync-token>$syncToken</d:sync-token>" else ""
        val ctagLine = if (ctag != null) "\n                <cs:getctag>$ctag</cs:getctag>" else ""
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <d:multistatus xmlns:d="DAV:" xmlns:cs="http://calendarserver.org/ns/">
            <d:response>
                <d:href>$href</d:href>
                <d:propstat>
                    <d:prop>
                        <d:displayname>$displayname</d:displayname>
                        <d:getcontenttype/>$ctagLine$syncTokenLine
                    </d:prop>
                    <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
            </d:response>
        </d:multistatus>
    """.trimIndent()
        // Chat-GPT:
        // <d:multistatus xmlns:d="DAV:" xmlns:cs="http://calendarserver.org/ns/">
        //    <d:response>
        //        <d:href>/carddav/users/joe/</d:href>
        //        <d:propstat>
        //            <d:prop>
        //                <d:displayname>Joe's Address Book</d:displayname>
        //                <d:resourcetype>
        //                    <d:collection />
        //                    <cs:addressbook />
        //                </d:resourcetype>
        //                <cs:getctag>1234567890</cs:getctag>
        //                <d:sync-token>https://example.com/carddav/users/joe/sync-token</d:sync-token>
        //            </d:prop>
        //            <d:status>HTTP/1.1 200 OK</d:status>
        //        </d:propstat>
        //    </d:response>
        //</d:multistatus>
    }

    fun appendPropfindContact(sb: StringBuilder, user: User, contact: Contact) {
        sb.appendLine(
            """  <d:response>
    <d:href>/carddav/${user.userName}/addressbook/contact${contact.id}.vcf</d:href>
    <d:propstat>
      <d:prop>
        <d:getetag>"${CardDavUtils.getETag(contact)}"</d:getetag>
        <d:displayname>${contact.displayName}</d:displayname>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>"""
        )
    }

    fun appendXmlPrefix(sb: StringBuilder) {
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    }

    fun appendMultiStatusStart(sb: StringBuilder, server: String, prependXmlPrefix: Boolean = true) {
        if (prependXmlPrefix) {
            appendXmlPrefix(sb)
        }
        sb.append("<d:multistatus xmlns:d=\"DAV:\" xmlns:cs=\"https://")
            .append(server)
            .appendLine("/ns/\">")
    }

    fun appendMultiStatusEnd(sb: StringBuilder) {
        sb.appendLine("</d:multistatus>")
    }

    fun generateCurrentUserPrincipal(requestWrapper: RequestWrapper, user: PFUserDO, props: List<PropFindUtils.Prop>): String {
        CardDavInit.cardDavUseRootPath
        val href = "${requestWrapper.baseUrl}/users/${user.username}/"
        val sb = StringBuilder()
        sb.appendLine("""
            <?xml version="1.0" encoding="UTF-8"?>
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:href>$href</d:href>
                    <d:propstat>
                        <d:prop>""".trimIndent())
        if (props.contains(PropFindUtils.Prop.RESOURCETYPE)) {
            appendPropLines(sb, "<d:resourcetype>")
            appendPropLines(sb, "  <d:collection />")
            appendPropLines(sb, "  <cs:addressbook />")
            appendPropLines(sb, "</d:resourcetype>")
        }
        if (props.contains(PropFindUtils.Prop.DISPLAYNAME)) {
            appendPropLines(sb, "<d:displayname>CardDAV Root</d:displayname>")
        }
        if (props.contains(PropFindUtils.Prop.CURRENT_USER_PRINCIPAL)) {
            appendPropLines(sb, "<d:current-user-principal>")
            appendPropLines(sb, "  <d:href>$href</d:href>")
            appendPropLines(sb, "</d:current-user-principal>")
        }
        if (props.contains(PropFindUtils.Prop.CURRENT_USER_PRIVILEGE_SET)) {
            appendPropLines(sb, "<d:current-user-privilege-set>")
            appendPropLines(sb, "  <d:privilege><d:read /></d:privilege>")
            // appendPropLines(sb, "  <d:privilege><d:write /></d:privilege>")
            appendPropLines(sb, "</d:current-user-privilege-set>")
        }
        sb.appendLine("""
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent())
        return sb.toString()
    }

    private fun appendPropLines(sb: StringBuilder, vararg lines: String) {
        lines.forEach { line ->
            sb.appendLine("              $line")
        }
    }

    /*
    fun generateSyncReportResponse(
        displayname: String,
        href: String,
        ctag: String? = null,
        syncToken: String? = null,
    ): String {
        val syncTokenLine = if (syncToken != null) "\n                <d:sync-token>$syncToken</d:sync-token>" else ""
        val ctagLine = if (ctag != null) "\n                <cs:getctag>$ctag</cs:getctag>" else ""
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <d:multistatus xmlns:d="DAV:" xmlns:card="urn:ietf:params:xml:ns:carddav">
            <d:response>
                <d:href>/users/joe/addressBooks/default/contact1.vcf</d:href>
                <d:propstat>
                    <d:prop>
                        <d:getetag>"123456789"</d:getetag>
                    </d:prop>
                    <d:status>HTTP/1.1 200 OK</d:status>
                </d:response>
            <d:response>
                <d:href>/users/joe/addressBooks/default/contact2.vcf</d:href>
                <d:propstat>
                    <d:prop>
                        <d:getetag>"987654321"</d:getetag>
                    </d:prop>
                    <d:status>HTTP/1.1 200 OK</d:status>
                </d:response>
            <d:sync-token>https://example.com/carddav/users/joe/new-sync-token</d:sync-token>
        </d:multistatus>""".trimIndent()
    }*/
}
