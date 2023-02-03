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
import org.projectforge.framework.utils.NumberHelper

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
object SipgateContactSyncService {
  internal var countryPrefixForTestcases: String? = null

  fun from(address: AddressDO): SipgateContact {
    val contact = SipgateContact()
    // contact.id
    contact.name = getName(address)
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

  fun from(contact: SipgateContact): AddressDO {
    val address = extractName(contact.name)
    // contact.id
    // var picture: String? = null
    address.email = contact.emails?.firstOrNull { it.type == SipgateContact.EmailType.WORK }?.email
    address.privateEmail = contact.emails?.firstOrNull { it.type == SipgateContact.EmailType.HOME }?.email
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
    return address
  }

  fun getName(address: AddressDO): String {
    val sb = StringBuilder()
    /*address.title?.let {
      sb.append(it.trim()).append(" ")
    }*/
    address.firstName?.let {
      sb.append(it.trim()).append(" ")
    }
    address.name?.let {
      sb.append(it.trim()).append(" ")
    }
    return sb.toString().trim()
  }

  fun extractName(name: String?): AddressDO {
    val address = AddressDO()
    if (name.isNullOrBlank()) {
      return address
    }
    val names = name.split(" ")
    address.name = names.last().trim()
    address.firstName = names.take(names.size - 1).joinToString(" ")
    return address
  }

  /**
   * Tries to find the contact with the best match (
   */
  fun findBestMatch(contacts: List<SipgateContact>, address: AddressDO): SipgateContact? {
    val matches =
      contacts.filter { it.name?.trim()?.lowercase() == getName(address).lowercase() }
    if (matches.isEmpty()) {
      return null
    }
    if (matches.size == 1) {
      return matches.first()
    }
    return matches.maxBy { matchScore(it, address) }
  }

  internal fun matchScore(contact: SipgateContact, address: AddressDO): Int {
    if (contact.name?.trim()?.lowercase() != getName(address).lowercase()) {
      return -1
    }
    var counter = 1
    val numbers = arrayOf(
      extractNumber(address.businessPhone),
      extractNumber(address.mobilePhone),
      extractNumber(address.privateMobilePhone),
      extractNumber(address.privatePhone),
      extractNumber(address.fax),
    )
    contact.numbers?.forEach { number ->
      val extractedNumber = extractNumber(number.number)
      numbers.forEach { if (it != null && extractedNumber == it) ++counter }
    }
    contact.emails?.forEach { email ->
      val str = email.email?.trim()?.lowercase()
      if (str != null && str == address.email?.trim()?.lowercase() || str == address.privateEmail?.trim()
          ?.lowercase()
      ) {
        ++counter
      }
    }
    contact.addresses?.forEach { adr ->
      val str = adr.postalCode?.trim()?.lowercase()
      if (str != null && str == address.zipCode?.trim() || str == address.privateZipCode) {
        ++counter
      }
    }
    if (address.division != null && contact.division?.trim()?.lowercase() == address.division?.trim()?.lowercase()) {
      ++counter
    }
    if (address.organization != null && contact.organization?.trim()?.lowercase() == address.organization?.trim()
        ?.lowercase()
    ) {
      ++counter
    }
    return counter
  }

  private fun extractNumber(number: String?): String? {
    number ?: return null
    if (countryPrefixForTestcases != null) {
      return NumberHelper.extractPhonenumber(number, countryPrefixForTestcases)
    }
    return NumberHelper.extractPhonenumber(number)
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
