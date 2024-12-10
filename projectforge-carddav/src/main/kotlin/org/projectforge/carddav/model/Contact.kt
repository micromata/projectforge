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

package org.projectforge.carddav.model

data class Contact(
    val id: Long? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val lastUpdated: java.util.Date? = null,
    var vcardData: ByteArray? = null,
) {
    val displayName = "$lastName, $firstName"
    override fun toString(): String {
        return "Contact[id=$id, name=[$displayName], lastUpdated=$lastUpdated, vcardData-size=${vcardData?.size}]"
    }
}
