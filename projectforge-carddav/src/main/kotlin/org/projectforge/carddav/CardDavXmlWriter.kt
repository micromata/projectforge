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

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.entities.PFUserDO

internal object CardDavXmlWriter {
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

    fun appendPropLines(sb: StringBuilder, vararg lines: String) {
        lines.forEach { line ->
            sb.appendLine("          $line")
        }
    }

    fun getUsersAddressbookDisplayName(user: PFUserDO): String {
        return translate("address.cardDAV.addressbook.displayName")
    }

    const val XML_NS =
        "xmlns:d=\"DAV:\" xmlns:cr=\"urn:ietf:params:xml:ns:carddav\" xmlns:cs=\"http://calendarserver.org/ns/\""
}
