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
import org.projectforge.carddav.CardDavUtils.CARD
import org.projectforge.carddav.CardDavUtils.CS
import org.projectforge.carddav.CardDavUtils.D
import org.projectforge.carddav.CardDavUtils.getUsersAddressbookDisplayName

private val log = KotlinLogging.logger {}

/**
 * Writes properties to a StringBuilder.
 */
internal object PropWriter {
    fun appendSupportedProps(sb: StringBuilder, writerContext: WriterContext) {
        val props = writerContext.props ?: return
        props.filter { it.supported }.forEach { prop ->
            appendSupportedProp(sb, prop, writerContext)
        }
    }

    /**
     * Appends the supported properties to the StringBuilder.
     * @param sb The StringBuilder.
     * @param prop The property to append.
     * @param href The href.
     * @param user The user.
     * the <D:resourcetype> tag.
     */
    fun appendSupportedProp(sb: StringBuilder, prop: Prop, writerContext: WriterContext) {
        val href = writerContext.href
        val user = writerContext.userDO
        when (prop) {
            Prop.ADDRESSBOOK_HOME_SET -> {
                appendMultilineProp(
                    sb, prop,
                    "<$D:href>${CardDavUtils.getUsersUrl(href, user, "addressbooks/")}</$D:href>"
                )
            }

            Prop.CURRENT_USER_PRINCIPAL -> {
                appendMultilineProp(
                    sb, prop,
                    "<$D:href>${CardDavUtils.getPrincipalsUsersUrl(href, user)}</$D:href>"
                )
            }

            Prop.CURRENT_USER_PRIVILEGE_SET -> {
                appendMultilineProp(
                    sb, prop,
                    """
                |          <$D:privilege><$D:read /></$D:privilege>
                |          <$D:privilege><$D:write /></$D:privilege>
                |          <$D:privilege><$D:write-properties /></$D:privilege>
                |          <$D:privilege><$D:write-content /></$D:privilege>
                """.trimMargin()
                )
            }

            Prop.DISPLAYNAME -> {
                if (href.contains("addressbooks")) {
                    appendProp(sb, prop, getUsersAddressbookDisplayName(user))
                } else {
                    appendProp(sb, prop, user.getFullname())
                }
            }

            Prop.EMAIL_ADDRESS_SET -> {
                appendMultilineProp(
                    sb, prop,
                    "<$D:href>mailto:${user.email ?: "${user.username}@example.com"}</$D:href>"
                )
            }

            Prop.GETCTAG, Prop.GETETAG -> {
                appendProp(sb, prop, CardDavUtils.getEtag(writerContext.contactList))
            }

            Prop.MAX_IMAGE_SIZE -> appendProp(sb, prop, CardDavInit.MAX_IMAGE_SIZE)

            Prop.MAX_RESOURCE_SIZE -> appendProp(sb, prop, CardDavInit.MAX_RESOURCE_SIZE)

            Prop.PRINCIPAL_COLLECTION_SET -> {
                appendMultilineProp(
                    sb, prop,
                    "<$D:href>${CardDavUtils.getUrl(href, "/principals")}</$D:href>"
                )
            }

            Prop.PRINCIPAL_URL, Prop.OWNER -> {
                appendMultilineProp(
                    sb, prop,
                    "<$D:href>${CardDavUtils.getPrincipalsUsersUrl(href, user)}</$D:href>"
                )
            }

            Prop.QUOTA_AVAILABLE_BYTES -> appendProp(sb, prop, CardDavInit.QUOTA_AVAILABLE_BYTES)

            Prop.QUOTA_USED_BYTES -> appendProp(sb, prop, "1000") // Dummy value.

            Prop.RESOURCE_ID -> appendProp(sb, prop, CardDavUtils.generateDeterministicUUID(user))

            Prop.RESOURCETYPE -> {
                appendMultilineProp(
                    sb, prop,
                    if (href.contains("addressbooks")) {
                        """
                        |          <$D:collection />
                        |          <$CARD:addressbook />
                        """.trimMargin()
                    } else {
                        "<$D:collection />"
                    }
                )
            }

            Prop.SUPPORTED_REPORT_SET -> {
                appendMultilineProp(
                    sb, prop,
                    """
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
                    """.trimMargin()
                )
            }

            Prop.SYNCTOKEN -> {
                // TODO: This sync token is just a random constant string for testing.
                appendProp(
                    sb, prop,
                    "76731ß1284"
                )
            }

            else -> log.warn { "Unsupported prop '<${prop.xmlns}:${prop.tag}>'" }
        }
    }

    private fun appendMultilineProp(sb: StringBuilder, prop: Prop, value: String) {
        sb.appendLine("        <${prop.xmlns}:${prop.tag}>")
        if (value.contains("\n")) {
            sb.appendLine(value)
        } else {
            sb.appendLine("          $value")
        }
        sb.appendLine("        </${prop.xmlns}:${prop.tag}>")
    }

    private fun appendProp(sb: StringBuilder, prop: Prop, value: String, multiLine: Boolean = true) {
        sb.appendLine("        <${prop.xmlns}:${prop.tag}>$value</${prop.xmlns}:${prop.tag}>")
    }
}
