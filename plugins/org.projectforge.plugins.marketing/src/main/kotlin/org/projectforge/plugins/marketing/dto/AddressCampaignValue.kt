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

package org.projectforge.plugins.marketing.dto

import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.address.MailingAddress
import org.projectforge.plugins.marketing.AddressCampaignValueDO
import org.projectforge.rest.dto.BaseDTO

class AddressCampaignValue(
    var addressCampaign: AddressCampaign? = null,
    var addressId: Long? = null,
    var name: String? = null,
    var fullLastName: String? = null,
    var firstName: String? = null,
    var organization: String? = null,
    var email: String? = null,
    var contactStatus: ContactStatus? = null,
    var addressStatus: AddressStatus? = null,
    var formattedAddress: String? = null,
    var value: String? = null,
    var comment: String? = null,
    var isFavoriteCard: Boolean? = null,
) : BaseDTO<AddressCampaignValueDO>() {
  override fun copyFrom(src: AddressCampaignValueDO) {
    this.id = src.id // Campaign value ID
    src.address?.let { addressDO ->
      // Store address ID for favorite lookup
      this.addressId = addressDO.id

      // Copy address fields directly
      this.name = addressDO.name
      this.fullLastName = addressDO.fullLastName
      this.firstName = addressDO.firstName
      this.organization = addressDO.organization
      this.contactStatus = addressDO.contactStatus
      this.addressStatus = addressDO.addressStatus

      // Combine all emails as CSV (business, private)
      val emails = mutableListOf<String>()
      addressDO.email?.let { if (it.isNotBlank()) emails.add(it) }
      addressDO.privateEmail?.let { if (it.isNotBlank()) emails.add(it) }
      this.email = if (emails.isNotEmpty()) emails.joinToString(", ") else null

      // Use MailingAddress only for formatting, don't store the object
      this.formattedAddress = MailingAddress(addressDO).formattedAddress
    }
    this.value = src.value
    this.comment = src.comment
  }

  override fun copyTo(dest: AddressCampaignValueDO) {
    dest.id = this.id
    dest.value = this.value
    dest.comment = this.comment
    // Note: address and addressCampaign relationships should be set by the caller
    // as they require proper entity references from the database
  }
}
