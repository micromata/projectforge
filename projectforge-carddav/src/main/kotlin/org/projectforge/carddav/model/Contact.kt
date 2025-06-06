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

package org.projectforge.carddav.model

import org.projectforge.business.address.ImageType
import org.projectforge.carddav.CardDavUtils
import org.projectforge.framework.time.PFDateTime
import java.security.MessageDigest
import java.util.Date

data class Contact(
    val id: Long? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val lastUpdated: Date? = null,
    val imageLastUpdate: Date? = null,
    val imageType: ImageType? = null,
    var vcardData: String? = null,
) {
    val displayName = "$lastName, $firstName"

    /**
     * A unique identifier for this version of the resource. This allows clients to detect changes efficiently.
     * The hashcode of the vcard combined with last update of image, embedded in quotes.
     * Please note: the etag is embedded in quotes. Must be used in xml as well as in http response of GET call.
     * @return The ETag.
     */
    val etag: String by lazy {
        vcardData?.let {
            CardDavUtils.getEtag("$it:imageLastUpdate=$imageLastUpdate".toByteArray())
        } ?: "\"null\""
    }

    /**
     * The last modified date of the resource as Http date.
     * @return The last modified date as Http date
     * @see PFDateTime.formatAsHttpDate
     */
    val lastModifiedAsHttpDate: String
        get() = PFDateTime.fromOrNow(lastUpdated).formatAsHttpDate()

    override fun toString(): String {
        return "Contact[id=$id, name=[$displayName], lastUpdated=$lastUpdated, vcardData-size=${vcardData?.length}]"
    }
}
