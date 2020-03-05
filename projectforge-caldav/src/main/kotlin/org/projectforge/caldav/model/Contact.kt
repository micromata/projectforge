/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.caldav.model

import io.milton.annotations.ModifiedDate
import io.milton.annotations.Name
import io.milton.annotations.UniqueId
import java.util.*

class Contact() {
    constructor(id: Int?, fullname: String?, modifiedDate: Date?, vcardData: ByteArray?) : this() {
        this.id = id?.toLong() ?: -1
        this.name = fullname ?: "untitled"
        this.modifiedDate = modifiedDate
        this.vcardData = vcardData
    }

    constructor(contact: Contact, addressBook: AddressBook?) : this() {
        this.id = contact.id
        this.name = contact.name
        this.modifiedDate = contact.modifiedDate
        this.vcardData = contact.vcardData
        this.addressBook = addressBook
    }

    @get:UniqueId
    var id: Long = 0

    @get:Name
    var name: String? = null
    @get:ModifiedDate
    var modifiedDate: Date? = null
    var vcardData: ByteArray? = null
    var addressBook: AddressBook? = null
}
