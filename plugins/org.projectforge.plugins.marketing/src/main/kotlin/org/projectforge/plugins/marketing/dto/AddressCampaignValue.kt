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

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.Transient
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.address.MailingAddress
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import org.projectforge.plugins.marketing.AddressCampaignValueDO
import org.projectforge.rest.dto.BaseDTO
import java.util.*

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

  @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
  val contactStatusAsString: String?
    get() {
      contactStatus?.let { return translate(it.i18nKey) }
      return null
    }

  @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
  val addressStatusAsString: String?
    get() {
      addressStatus?.let { return translate(it.i18nKey) }
      return null
    }

  @get:Transient
  @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
  val timeAgo: String?
    get() = TimeAgo.getMessage(lastUpdate)

  override fun copyFrom(src: AddressCampaignValueDO) {
    this.id = src.id // Campaign value ID (including negative synthetic IDs for multi-selection)

    // Copy address campaign relationship
    src.addressCampaign?.let { campaignDO ->
      val campaignDto = AddressCampaign()
      campaignDto.copyFrom(campaignDO)
      this.addressCampaign = campaignDto
    }

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
      this.lastUpdate = addressDO.lastUpdate

      // Combine all emails as CSV (business, private)
      val emails = mutableListOf<String>()
      addressDO.email?.let { if (it.isNotBlank()) emails.add(it) }
      addressDO.privateEmail?.let { if (it.isNotBlank()) emails.add(it) }
      this.email = if (emails.isNotEmpty()) emails.joinToString(", ") else null

      // Use MailingAddress only for formatting, don't store the object
      this.formattedAddress = MailingAddress(addressDO).formattedAddress
    }

    // For deleted campaign values: keep ID but clear value and comment
    // This makes them appear as "not set" to the user while preserving the entity for updates
    if (src.deleted) {
      this.value = null
      this.comment = null
    } else {
      this.value = src.value
      this.comment = src.comment
    }
  }

  override fun copyTo(dest: AddressCampaignValueDO) {
    dest.id = this.id
    dest.value = this.value
    dest.comment = this.comment
    // Note: address and addressCampaign relationships should be set by the caller
    // as they require proper entity references from the database
  }

  /**
   * Populate address fields from an AddressDO.
   * Used when creating a new campaign value for an address that doesn't have one yet.
   */
  fun populateFromAddress(addressDO: org.projectforge.business.address.AddressDO) {
    this.addressId = addressDO.id
    this.name = addressDO.name
    this.fullLastName = addressDO.fullLastName
    this.firstName = addressDO.firstName
    this.organization = addressDO.organization
    this.contactStatus = addressDO.contactStatus
    this.addressStatus = addressDO.addressStatus
    this.lastUpdate = addressDO.lastUpdate

    // Combine all emails as CSV (business, private)
    val emails = mutableListOf<String>()
    addressDO.email?.let { if (it.isNotBlank()) emails.add(it) }
    addressDO.privateEmail?.let { if (it.isNotBlank()) emails.add(it) }
    this.email = if (emails.isNotEmpty()) emails.joinToString(", ") else null

    // Format mailing address
    this.formattedAddress = MailingAddress(addressDO).formattedAddress
  }
}
