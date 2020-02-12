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
import org.projectforge.model.rest.AddressObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.sql.Date
import java.time.LocalDate
import java.util.*

@Service
class VCardService {
    fun getVCard(addressDO: AddressDO): ByteArray { //See: https://github.com/mangstadt/ez-vcard
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

    fun getAddressObject(vcard: VCard): AddressObject {
        val ao = AddressObject()
        ao.uid = vcard.uid.value
        ao.name = vcard.structuredName.family
        ao.firstName = vcard.structuredName.given
        if (vcard.structuredName.prefixes != null && vcard.structuredName.prefixes.size > 0) {
            ao.title = vcard.structuredName.prefixes[0]
        }
        for (address in vcard.addresses) {
            if (address.types.contains(AddressType.HOME)) {
                ao.privateAddressText = address.streetAddressFull
                ao.privateZipCode = address.postalCode
                ao.privateCity = address.locality
                ao.privateState = address.region
                ao.privateCountry = address.country
            }
            if (address.types.contains(AddressType.WORK)) {
                ao.addressText = address.streetAddressFull
                ao.zipCode = address.postalCode
                ao.city = address.locality
                ao.state = address.region
                ao.country = address.country
            }
            if (address.types.contains(AddressType.POSTAL)) {
                ao.postalAddressText = address.streetAddressFull
                ao.postalZipCode = address.postalCode
                ao.postalCity = address.locality
                ao.postalState = address.region
                ao.postalCountry = address.country
            }
        }
        for (telephone in vcard.telephoneNumbers) {
            if (telephone.types.contains(TelephoneType.PAGER)) {
                ao.privateMobilePhone = addCountryCode(telephone.text)
            }
            if (telephone.types.contains(TelephoneType.HOME) && telephone.types.contains(TelephoneType.VOICE)) {
                ao.privatePhone = addCountryCode(telephone.text)
            }
            if (telephone.types.contains(TelephoneType.CELL) && telephone.types.contains(TelephoneType.VOICE)) {
                ao.mobilePhone = addCountryCode(telephone.text)
            }
            if (telephone.types.contains(TelephoneType.WORK) && telephone.types.contains(TelephoneType.VOICE)) {
                ao.businessPhone = addCountryCode(telephone.text)
            }
            if (telephone.types.contains(TelephoneType.WORK) && telephone.types.contains(TelephoneType.FAX)) {
                ao.fax = addCountryCode(telephone.text)
            }
        }
        for (email in vcard.emails) {
            if (email.types.contains(EmailType.HOME)) {
                ao.privateEmail = email.value
            }
            if (email.types.contains(EmailType.WORK)) {
                ao.email = email.value
            }
        }
        ao.website = if (vcard.urls != null && vcard.urls.size > 0) vcard.urls[0].value else null
        val birthday = vcard.birthday
        if (birthday != null) {
            ao.birthday = Date(birthday.date.time)
        }
        for (note in vcard.notes) {
            ao.comment = if (ao.comment != null) ao.comment else "" + note.value + " "
        }
        ao.image = if (vcard.photos != null && vcard.photos.size > 0) Arrays.toString(vcard.photos[0].data) else null
        if (vcard.organization != null && vcard.organization.values != null && vcard.organization.values.size > 0) {
            when (vcard.organization.values.size) {
                3 -> {
                    ao.positionText = vcard.organization.values[2]
                    ao.division = vcard.organization.values[1]
                    ao.organization = vcard.organization.values[0]
                }
                2 -> {
                    ao.division = vcard.organization.values[1]
                    ao.organization = vcard.organization.values[0]
                }
                1 -> ao.organization = vcard.organization.values[0]
            }
        }
        return ao
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

    private fun convertBirthday(birthday: LocalDate?): Birthday? {
        if (birthday == null) {
            return null
        }
        val date = PartialDate.Builder().year(birthday.year)
                .month(birthday.monthValue)
                .date(birthday.dayOfMonth)
                .build()
        return Birthday(date)
    }

    companion object {
        private val log = LoggerFactory.getLogger(VCardService::class.java)
    }
}
