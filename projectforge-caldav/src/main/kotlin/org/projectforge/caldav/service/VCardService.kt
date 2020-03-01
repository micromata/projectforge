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

package org.projectforge.caldav.service

import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.io.text.VCardReader
import ezvcard.parameter.AddressType
import ezvcard.parameter.EmailType
import ezvcard.parameter.ImageType
import ezvcard.parameter.TelephoneType
import ezvcard.property.*
import ezvcard.util.PartialDate
import org.projectforge.business.address.AddressDO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.LocalDate

@Service
class VCardService {
    fun buildVCard(addressDO: AddressDO): ByteArray { //See: https://github.com/mangstadt/ez-vcard
        val vcard = VCard()
        val uid = Uid("urn:uuid:" + addressDO.uid)
        vcard.uid = uid
        vcard.setFormattedName(addressDO.fullName)
        val n = StructuredName()
        n.family = addressDO.name
        n.given = addressDO.firstName
        if (StringUtils.isEmpty(addressDO.title) == false) {
            n.prefixes.add(addressDO.title)
        }
        vcard.structuredName = n
        //Home address
        val homeAddress = Address()
        homeAddress.types.add(AddressType.HOME)
        homeAddress.streetAddress = addressDO.privateAddressText
        homeAddress.postalCode = addressDO.privateZipCode
        homeAddress.locality = addressDO.privateCity
        homeAddress.region = addressDO.privateState
        homeAddress.country = addressDO.privateCountry
        if (addressDO.communicationLanguage != null) {
            vcard.addLanguage(addressDO.communicationLanguage!!.getDisplayLanguage(addressDO.communicationLanguage))
            vcard.structuredName.language = addressDO.communicationLanguage!!.getDisplayLanguage(addressDO.communicationLanguage)
        }
        //adr.setLabel("123 Main St.\nAlbany, NY 54321\nUSA");
        vcard.addAddress(homeAddress)
        vcard.addTelephoneNumber(addressDO.privatePhone, TelephoneType.HOME)
        vcard.addTelephoneNumber(addressDO.privateMobilePhone, TelephoneType.HOME, TelephoneType.PAGER)
        vcard.addEmail(addressDO.privateEmail, EmailType.HOME)
        //Business address
        val businessAddress = Address()
        businessAddress.types.add(AddressType.WORK)
        businessAddress.streetAddress = addressDO.addressText
        businessAddress.postalCode = addressDO.zipCode
        businessAddress.locality = addressDO.city
        businessAddress.region = addressDO.state
        businessAddress.country = addressDO.country
        //adr.setLabel("123 Main St.\nAlbany, NY 54321\nUSA");
        vcard.addAddress(businessAddress)
        vcard.addTelephoneNumber(addressDO.businessPhone, TelephoneType.WORK)
        vcard.addTelephoneNumber(addressDO.mobilePhone, TelephoneType.WORK, TelephoneType.CELL)
        vcard.addTelephoneNumber(addressDO.fax, TelephoneType.WORK, TelephoneType.FAX)
        vcard.addEmail(addressDO.email, EmailType.WORK)
        val organisation = Organization()
        organisation.values.add(if (StringUtils.isEmpty(addressDO.organization) == false) addressDO.organization else "")
        organisation.values.add(if (StringUtils.isEmpty(addressDO.division) == false) addressDO.division else "")
        organisation.values.add(if (StringUtils.isEmpty(addressDO.positionText) == false) addressDO.positionText else "")
        vcard.addOrganization(organisation)
        //Home address
        val postalAddress = Address()
        postalAddress.types.add(AddressType.POSTAL)
        postalAddress.streetAddress = addressDO.postalAddressText
        postalAddress.postalCode = addressDO.postalZipCode
        postalAddress.locality = addressDO.postalCity
        postalAddress.region = addressDO.postalState
        postalAddress.country = addressDO.postalCountry
        val birthday = addressDO.birthday
        if (birthday != null) {
            val date = PartialDate.Builder().year(birthday.year).month(birthday.monthValue).date(birthday.dayOfMonth).build()
            vcard.birthday = Birthday(date)
        }
        vcard.addUrl(addressDO.website)
        vcard.addNote(addressDO.comment)
        if (addressDO.imageData != null) {
            val photo = Photo(addressDO.imageData, ImageType.JPEG)
            vcard.addPhoto(photo)
        }
        return Ezvcard.write(vcard).version(VCardVersion.V3_0).go().toByteArray()
    }

    fun buildAddressDO(vcard: VCard): AddressDO {
        val address = AddressDO()
        address.uid = vcard.uid.value
        address.name = vcard.structuredName.family
        address.firstName = vcard.structuredName.given
        if (vcard.structuredName.prefixes != null && vcard.structuredName.prefixes.size > 0) {
            address.title = vcard.structuredName.prefixes[0]
        }
        for (vcardAddress in vcard.addresses) {
            if (vcardAddress.types.contains(AddressType.HOME)) {
                address.privateAddressText = vcardAddress.streetAddressFull
                address.privateZipCode = vcardAddress.postalCode
                address.privateCity = vcardAddress.locality
                address.privateState = vcardAddress.region
                address.privateCountry = vcardAddress.country
            }
            if (vcardAddress.types.contains(AddressType.WORK)) {
                address.addressText = vcardAddress.streetAddressFull
                address.zipCode = vcardAddress.postalCode
                address.city = vcardAddress.locality
                address.state = vcardAddress.region
                address.country = vcardAddress.country
            }
            if (vcardAddress.types.contains(AddressType.POSTAL)) {
                address.postalAddressText = vcardAddress.streetAddressFull
                address.postalZipCode = vcardAddress.postalCode
                address.postalCity = vcardAddress.locality
                address.postalState = vcardAddress.region
                address.postalCountry = vcardAddress.country
            }
        }
        for (telephone in vcard.telephoneNumbers) {
            if (telephone.types.contains(TelephoneType.PAGER)) {
                address.privateMobilePhone = addCountryCode(telephone.text)
            }
            if (telephone.types.contains(TelephoneType.HOME) && telephone.types.contains(TelephoneType.VOICE)) {
                address.privatePhone = addCountryCode(telephone.text)
            }
            if (telephone.types.contains(TelephoneType.CELL) && telephone.types.contains(TelephoneType.VOICE)) {
                address.mobilePhone = addCountryCode(telephone.text)
            }
            if (telephone.types.contains(TelephoneType.WORK) && telephone.types.contains(TelephoneType.VOICE)) {
                address.businessPhone = addCountryCode(telephone.text)
            }
            if (telephone.types.contains(TelephoneType.WORK) && telephone.types.contains(TelephoneType.FAX)) {
                address.fax = addCountryCode(telephone.text)
            }
        }
        for (email in vcard.emails) {
            if (email.types.contains(EmailType.HOME)) {
                address.privateEmail = email.value
            }
            if (email.types.contains(EmailType.WORK)) {
                address.email = email.value
            }
        }
        address.website = if (vcard.urls != null && vcard.urls.size > 0) vcard.urls[0].value else null
        val birthday = vcard.birthday
        val partialDate = birthday?.partialDate
        if (partialDate != null) {
            val year = partialDate.year
            val month = partialDate.month
            val date = partialDate.date
            address.birthday = LocalDate.of(year ?: 2000, month ?: 1, date ?: 1)
        }
        for (note in vcard.notes) {
            address.comment = if (address.comment != null) address.comment else "" + note.value + " "
        }
        address.imageData = if (!vcard.photos.isNullOrEmpty()) vcard.photos[0].data else null
        if (vcard.organization?.values?.isNotEmpty() == true) {
            when (vcard.organization.values.size) {
                3 -> {
                    address.positionText = vcard.organization.values[2]
                    address.division = vcard.organization.values[1]
                    address.organization = vcard.organization.values[0]
                }
                2 -> {
                    address.division = vcard.organization.values[1]
                    address.organization = vcard.organization.values[0]
                }
                1 -> address.organization = vcard.organization.values[0]
            }
        }
        return address
    }

    private fun addCountryCode(phonenumber: String?): String? {
        var result: String? = ""
        result = if (phonenumber != null && phonenumber.startsWith("0")) {
            phonenumber.replaceFirst("0".toRegex(), "+49")
        } else {
            phonenumber
        }
        return result
    }

    fun getVCardFromByteArray(vcardBytearray: ByteArray?): VCard? {
        val bais = ByteArrayInputStream(vcardBytearray)
        val reader = VCardReader(bais)
        var vcard: VCard? = null
        try {
            vcard = reader.readNext()
        } catch (e: IOException) {
            log.error("An exception accured while parsing vcard from byte array: " + e.message, e)
        }
        return vcard
    }

    companion object {
        private val log = LoggerFactory.getLogger(VCardService::class.java)
    }
}
