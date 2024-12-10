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
import org.jetbrains.kotlin.utils.addToStdlib.countOccurrencesOf

private val log = KotlinLogging.logger {}

/**
 * Properties that can be requested in a PROPFIND request.
 */
internal enum class Prop(val str: String, val xmlns: String = CardDavUtils.D) {
    ADDRESSBOOK_HOME_SET("addressbook-home-set", CardDavUtils.CARD),
    ADDRESS_DATA("caddress-data"),
    CURRENT_USER_PRINCIPAL("current-user-principal"),
    CURRENT_USER_PRIVILEGE_SET("current-user-privilege-set"),
    DIRECTORY_GATEWAY("directory-gateway", CardDavUtils.CARD), // 404 - Not found.
    DISPLAYNAME("displayname"),
    EMAIL_ADDRESS_SET("email-address-set"),
    GETCTAG("getctag"),
    GETETAG("getetag"),
    RESOURCETYPE("resourcetype"),
    PRINCIPAL_COLLECTION_SET("principal-collection-set"),
    PRINCIPAL_URL("principal-URL"),
    RESOURCE_ID("resource-id"),
    SUPPORTED_REPORT_SET("supported-report-set"),
    SYNCTOKEN("sync-token");

    companion object {
        /**
         * <propfind xmlns="DAV:">
         *   <prop>
         *     <resourcetype/>
         *     <displayname/>
         *     <current-user-principal/>
         *     <current-user-privilege-set/>
         *     <card:address-data />
         *   </prop>
         * </propfind>
         */
        /**
         * List of all available properties.
         * @param xml The XML string to extract the properties from.
         * @return The list of found/requested properties.
         */
        fun extractProps(xml: String): List<Prop> {
            val props = mutableListOf<Prop>()
            // First, get the element name of the prop element:
            val propElement = CardDavXmlUtils.getElementName(xml, "prop") ?: "prop"
            val count = xml.countOccurrencesOf("<$propElement>")
            if (count == 0) {
                log.warn { "Invalid Props request (no <$propElement>...</$propElement> found): $xml" }
                return props
            }
            if (count > 1) {
                log.warn { "Invalid Props request (multiple entries of <$propElement>...</$propElement> found, first is used): $xml" }
            }
            val propXml = xml.substringAfter("<$propElement>").substringBefore("</$propElement>")
            Prop.entries.forEach { prop ->
                if ("<${prop.str}" in propXml || ":${prop.str}" in propXml) { // e.g. <card:address-data /> or <getetag/>
                    props.add(prop)
                }
            }
            return props
        }
    }
}
