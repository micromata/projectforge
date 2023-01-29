/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.sipgate

import org.projectforge.business.address.AddressDO

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
object SipgateContactSync {
  fun from(address: AddressDO): SipgateContact {
    val contact = SipgateContact()
    // contact.id
    contact.name = address.fullNameWithTitleAndForm
    contact.family = address.name
    contact.given = address.firstName
    // var picture: String? = null
    val mails = mutableListOf<SipgateEmail>()
    address.email?.let { mails.add(SipgateEmail(it, SipgateContact.EmailType.WORK)) }
    address.privateEmail?.let { mails.add(SipgateEmail(it, SipgateContact.EmailType.HOME)) }
    if (mails.isNotEmpty()) {
      contact.emails = mails.toTypedArray()
    }
    val numbers = mutableListOf<SipgateNumber>()
    address.businessPhone?.let { numbers.add(SipgateNumber(it).setWork()) }
    address.mobilePhone?.let { numbers.add(SipgateNumber(it).setCell()) }
    address.privatePhone?.let { numbers.add(SipgateNumber(it).setHome()) }
    address.privateMobilePhone?.let { numbers.add(SipgateNumber(it).setOther()) }
    address.fax?.let { numbers.add(SipgateNumber(it).setFaxWork()) }
    if (numbers.isNotEmpty()) {
      contact.numbers = numbers.toTypedArray()
    }

    val addresses = mutableListOf<SipgateAddress>()
    createAddress(
      addressText = address.addressText,
      addressText2 = address.addressText2,
      zipCode = address.zipCode,
      city = address.city,
      state = address.state,
      country = address.country,
    )?.let { addresses.add(it) }
    createAddress(
      addressText = address.privateAddressText,
      addressText2 = address.privateAddressText2,
      zipCode = address.privateZipCode,
      city = address.privateCity,
      state = address.privateState,
      country = address.privateCountry,
    )?.let { addresses.add(it) }
    if (addresses.isNotEmpty()) {
      contact.addresses = addresses.toTypedArray()
    }

    contact.organization = address.organization
    contact.division = address.division
    contact.scope = SipgateContact.Scope.SHARED
    return contact
  }

  private fun createAddress(
    addressText: String?, addressText2: String?, zipCode: String?, city: String?, state: String?, country: String?,
  ): SipgateAddress? {
    if (addressText.isNullOrBlank() && addressText2.isNullOrBlank() && zipCode.isNullOrBlank() && city.isNullOrBlank() && state.isNullOrBlank() && country.isNullOrBlank()) {
      return null
    }
    val address = SipgateAddress()
    address.streetAddress = addressText?.trim()
    address.extendedAddress = addressText2?.trim()
    address.postalCode = zipCode?.trim()
    address.locality = city?.trim()
    // address.city = city?.trim() // Sync api uses locality!?
    address.region = state?.trim()
    // address.state = state?.trim() // Sync api uses region!?
    address.country = country?.trim()
    return address
  }
}
