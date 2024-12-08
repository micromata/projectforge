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

internal enum class Prop(val str: String) {
    RESOURCETYPE("resourcetype"),
    DISPLAYNAME("displayname"),
    GETETAG("getetag"),
    GETCTAG("getctag"),
    SYNCTOKEN("sync-token"),
    CURRENT_USER_PRINCIPAL("current-user-principal"),
    CURRENT_USER_PRIVILEGE_SET("current-user-privilege-set"),
    ADDRESS_DATA("caddress-data");

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
        fun extractProps(xml: String): List<Prop> {
            val props = mutableListOf<Prop>()
            val count = xml.countOccurrencesOf("<prop>")
            if (count == 0) {
                log.warn { "Invalid Props request (no <prop>...</prop> found): $xml" }
                return props
            }
            if (count > 1) {
                log.warn { "Invalid Props request (multiple entries of <prop>...</prop> found, first is used): $xml" }
            }
            val propXml = xml.substringAfter("<prop>").substringBefore("</prop>")
            Prop.entries.forEach { prop ->
                if ("<${prop.str}" in propXml || ":${prop.str}" in propXml) { // e.g. <card:address-data /> or <getetag/>
                    props.add(prop)
                }
            }
            return props
        }
    }
}
