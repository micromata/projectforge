/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import org.projectforge.business.address.*
import org.projectforge.framework.utils.LabelValueBean
import java.sql.Date
import java.util.*

class Address(var contactStatus: ContactStatus? = null,
              var addressStatus: AddressStatus? = null,
              var uid: String? = null,
              var name: String? = null,
              var firstName: String? = null,
              var form: FormOfAddress? = null,
              var title: String? = null,
              var positionText: String? = null,
              var organization: String? = null,
              var division: String? = null,
              var businessPhone: String? = null,
              var mobilePhone: String? = null,
              var fax: String? = null,
              var addressText: String? = null,
              var zipCode: String? = null,
              var city: String? = null,
              var country: String? = null,
              var state: String? = null,
              var postalAddressText: String? = null,
              var postalZipCode: String? = null,
              var postalCity: String? = null,
              var postalCountry: String? = null,
              var postalState: String? = null,
              var email: String? = null,
              var website: String? = null,
              var communicationLanguage: Locale? = null,
              var privatePhone: String? = null,
              var privateMobilePhone: String? = null,
              var privateAddressText: String? = null,
              var privateZipCode: String? = null,
              var privateCity: String? = null,
              var privateCountry: String? = null,
              var privateState: String? = null,
              var privateEmail: String? = null,
              var publicKey: String? = null,
              var fingerprint: String? = null,
              var comment: String? = null,
              var birthday: Date? = null,
              var imageUrl: String? = null,
              var previewImageUrl: String? = null,
              var instantMessaging: MutableList<LabelValueBean<InstantMessagingType, String>>? = null,
              var addressbookList: MutableSet<Addressbook>? = null,
              /**
               * Is this address a personal favorite of the current logged-in user?
               */
              var isFavoriteCard: Boolean = false,
              var isFavoriteBusinessPhone: Boolean = false,
              var isFavoritePrivatePhone: Boolean = false,
              var isFavoriteMobilePhone: Boolean = false,
              var isFavoritePrivateMobilePhone: Boolean = false,
              var isFavoriteFax: Boolean = false
) : BaseHistorizableDTO<AddressDO>() {

    override fun copyFrom(src: AddressDO) {
        super.copyFrom(src)
        if (src.imageData != null) {
            imageUrl = "address/image/$id"
        }
        if (src.imageDataPreview != null) {
            previewImageUrl = "address/imagePreview/$id}"
        }
        if (!src.addressbookList.isNullOrEmpty()) {
            addressbookList = mutableSetOf()
            src.addressbookList?.forEach { srcAddressbook ->
                val addressbook = Addressbook()
                addressbook.copyFromMinimal(srcAddressbook)
                addressbookList!!.add(addressbook)
            }
        }
    }

    override fun copyTo(dest: AddressDO) {
        super.copyTo(dest)
        if (!addressbookList.isNullOrEmpty()) {
            dest.addressbookList = mutableSetOf()
            addressbookList?.forEach { srcAddressbook ->
                val addressbook = AddressbookDO()
                srcAddressbook.copyTo(addressbook)
                dest.addressbookList!!.add(addressbook)
            }
        }
    }
}
