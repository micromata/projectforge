/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

private val log = KotlinLogging.logger {}

internal class Prop(val type: PropType, val value: String? = null) {
    val supported: Boolean
        get() = type.supported
    val tag: String
        get() = type.tag
    val xmlns: String
        get() = type.xmlns

    companion object {
        /**
         * List of all available properties.
         * A typical request body looks like this:
         * ```
         * <propfind xmlns="DAV:">
         *   <d:prop>
         *     <d:resourcetype/>
         *     <d:displayname/>
         *     <d:current-user-principal/>
         *     <d:current-user-privilege-set/>
         *     <card:address-data />
         *   </d:prop>
         * </d:propfind>
         * ```
         * @param xml The XML string to extract the properties from.
         * @return The list of found/requested properties.
         */
        fun extractProps(xml: String): List<Prop> {
            val props = mutableListOf<Prop>()
            // First, get the element name of the prop element:
            val value = CardDavXmlUtils.extractElementValue(xml, "prop")
            if (value == null) {
                log.warn { "Invalid Props request (no <prop>...</prop> found): $xml" }
                return props
            }
            PropType.entries.forEach { propType ->
                if ("<${propType.tag}" in value || ":${propType.tag}" in value) { // e.g. <card:address-data /> or <getetag/>
                    val propValue = CardDavXmlUtils.extractElementValue(value, propType.tag)
                    props.add(Prop(propType, propValue))
                }
            }
            return props
        }
    }
}
