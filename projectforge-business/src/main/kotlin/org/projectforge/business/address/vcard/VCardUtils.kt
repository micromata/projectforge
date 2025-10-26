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

package org.projectforge.business.address.vcard

import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.parameter.AddressType
import ezvcard.parameter.EmailType
import ezvcard.parameter.TelephoneType
import ezvcard.property.*
import mu.KotlinLogging
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressImageDO
import org.projectforge.business.address.ImageType
import org.projectforge.framework.time.PFDateTime
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.LocalDate

private val log = KotlinLogging.logger {}

object VCardUtils {
    @JvmStatic
    fun buildVCard(
        addressDO: AddressDO,
        imageUrl: String? = null,
        imageType: ImageType? = null,
    ): VCard { //See: https://github.com/mangstadt/ez-vcard
        val vcard = VCard()
        val uid = Uid("urn:uuid:" + addressDO.uid)
        vcard.uid = uid
        vcard.setFormattedName(addressDO.fullName)
        val n = StructuredName()
        n.family = addressDO.name
        n.given = addressDO.firstName
        addressDO.title?.let { n.prefixes.add(it) }
        vcard.structuredName = n
        //Home address
        val homeAddress = Address()
        homeAddress.types.add(AddressType.HOME)
        homeAddress.streetAddress = addressDO.privateAddressText
        homeAddress.extendedAddress = addressDO.privateAddressText2
        homeAddress.postalCode = addressDO.privateZipCode
        homeAddress.locality = addressDO.privateCity
        homeAddress.region = addressDO.privateState
        homeAddress.country = addressDO.privateCountry
        addressDO.communicationLanguage?.let { communicationLanguage ->
            vcard.addLanguage(addressDO.communicationLanguage!!.getDisplayLanguage(communicationLanguage))
            vcard.structuredName.language =
                addressDO.communicationLanguage!!.getDisplayLanguage(communicationLanguage)
        }
        //adr.setLabel("123 Main St.\nAlbany, NY 54321\nUSA");
        vcard.addAddress(homeAddress)
        vcard.addTelephoneNumber(addressDO.privatePhone, TelephoneType.HOME)
        vcard.addTelephoneNumber(addressDO.privateMobilePhone, TelephoneType.CELL, TelephoneType.HOME)
        vcard.addEmail(addressDO.privateEmail, EmailType.HOME)
        //Business address
        val businessAddress = Address()
        businessAddress.types.add(AddressType.WORK)
        businessAddress.streetAddress = addressDO.addressText
        businessAddress.extendedAddress = addressDO.addressText2
        businessAddress.postalCode = addressDO.zipCode
        businessAddress.locality = addressDO.city
        businessAddress.region = addressDO.state
        businessAddress.country = addressDO.country
        //adr.setLabel("123 Main St.\nAlbany, NY 54321\nUSA");
        vcard.addAddress(businessAddress)
        vcard.addTelephoneNumber(addressDO.businessPhone, TelephoneType.WORK)
        vcard.addTelephoneNumber(addressDO.mobilePhone, TelephoneType.CELL)
        vcard.addTelephoneNumber(addressDO.fax, TelephoneType.FAX, TelephoneType.WORK)
        vcard.addEmail(addressDO.email, EmailType.WORK)
        val organisation = Organization()
        organisation.values.add(addressDO.organization ?: "")
        organisation.values.add(addressDO.division ?: "")
        organisation.values.add(addressDO.positionText ?: "")
        vcard.addOrganization(organisation)
        //Postal address
        val postalAddress = Address()
        postalAddress.types.add(AddressType.POSTAL)
        postalAddress.streetAddress = addressDO.postalAddressText
        postalAddress.extendedAddress = addressDO.postalAddressText2
        postalAddress.postalCode = addressDO.postalZipCode
        postalAddress.locality = addressDO.postalCity
        postalAddress.region = addressDO.postalState
        postalAddress.country = addressDO.postalCountry
        vcard.addAddress(postalAddress)
        addressDO.birthday?.let { birthday ->
            // PartialDate is not supported in V3.0, using java.util.Date:
            vcard.birthday = Birthday(birthday)
        }
        vcard.addUrl(addressDO.website)
        vcard.addNote(addressDO.comment)
        // Handle photo - support both URL and embedded image from transient attribute
        if (imageUrl != null) {
            Photo(imageUrl, (imageType ?: ImageType.PNG).asVCardImageType()).let {
                vcard.addPhoto(it)
            }
        } else {
            // Check for embedded image from AddressImageDO in transient attributes
            (addressDO.getTransientAttribute("image") as? AddressImageDO)?.let { imageData ->
                imageData.image?.let { imageBytes ->
                    Photo(imageBytes, (imageData.imageType ?: ImageType.PNG).asVCardImageType()).let {
                        vcard.addPhoto(it)
                    }
                }
            }
        }
        return vcard
    }

    @JvmStatic
    @JvmOverloads
    fun buildVCardByteArray(
        addressDO: AddressDO,
        vCardVersion: VCardVersion = VCardVersion.V_4_0,
        imageUrl: String? = null,
        imageType: ImageType? = null,
    ): ByteArray { //See: https://github.com/mangstadt/ez-vcard
        return buildVCardString(
            addressDO,
            vCardVersion,
            imageUrl,
            imageType
        ).toByteArray()
    }

    @JvmStatic
    @JvmOverloads
    fun buildVCardString(
        addressDO: AddressDO,
        vCardVersion: VCardVersion = VCardVersion.V_4_0,
        imageUrl: String? = null,
        imageType: ImageType? = null,
    ): String { //See: https://github.com/mangstadt/ez-vcard
        val vcard = buildVCard(addressDO, imageUrl, imageType)
        return Ezvcard.write(vcard).version(vCardVersion.ezVCardType).go()
    }

    @JvmStatic
    fun buildAddressDO(vcard: VCard): AddressDO {
        val address = AddressDO()
        address.uid = vcard.uid?.value
        address.name = vcard.structuredName?.family
        address.firstName = vcard.structuredName?.given
        if (vcard.structuredName?.prefixes?.isNotEmpty() == true) {
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
            if (telephone.types.contains(TelephoneType.HOME)) {
                if (telephone.types.contains(TelephoneType.CELL)) {
                    address.privateMobilePhone = addCountryCode(telephone.text)
                } else {
                    address.privatePhone = addCountryCode(telephone.text)
                }
            } else {
                when {
                    telephone.types.contains(TelephoneType.FAX) -> {
                        address.fax = addCountryCode(telephone.text)
                    }

                    telephone.types.contains(TelephoneType.CELL) -> {
                        address.mobilePhone = addCountryCode(telephone.text)
                    }

                    else -> {
                        address.businessPhone = addCountryCode(telephone.text)
                    }
                }
            }
        }
        for (email in vcard.emails) {
            if (email.types.contains(EmailType.HOME)) {
                address.privateEmail = email.value
            } else {
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
        } else {
            address.birthday = PFDateTime.fromTemporalOrNull(birthday?.date)?.localDate
        }
        for (note in vcard.notes) {
            var noteValue = note.value ?: ""
            // Remove "CLASS: WORK" that ProjectForge adds during export to avoid false change detection
            // Case 1: "\nCLASS: WORK" at the end (after actual note text)
            noteValue = noteValue.replace("\nCLASS: WORK", "")
            // Case 2: Only "CLASS: WORK" without any note text
            if (noteValue.trim() == "CLASS: WORK") {
                noteValue = ""
            }
            noteValue = noteValue.trim()
            if (noteValue.isNotBlank()) {
                address.comment = if (address.comment != null) address.comment else "" + noteValue + " "
            }
        }
        vcard.photos.firstOrNull { it.data != null }?.let { photo ->
            address.setTransientAttribute("image", AddressImageDO().also {
                it.image = photo.data
                photo.contentType?.let { contentType ->
                    it.imageType = ImageType.from(contentType) ?: ImageType.PNG
                }
            })
        }
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
        return if (phonenumber != null && phonenumber.startsWith("0")) {
            phonenumber.replaceFirst("0".toRegex(), "+49")
        } else {
            phonenumber
        }
    }

    @JvmStatic
    fun parseVCardsFromByteArray(vcardByteArray: ByteArray?): List<VCard> {
        vcardByteArray ?: return emptyList()
        ByteArrayInputStream(vcardByteArray).use { stream ->
            try {
                return Ezvcard.parse(stream).all()
            } catch (e: IOException) {
                log.error("An exception occurred while parsing vcard from byte array: " + e.message, e)
                return emptyList()
            }
        }
    }

    @JvmStatic
    fun parseVCardsFromString(vcardString: String?): List<VCard> {
        vcardString ?: return emptyList()
        try {
            return Ezvcard.parse(vcardString).all()
        } catch (e: IOException) {
            log.error("An exception occurred while parsing vcard from byte array: " + e.message, e)
            return emptyList()
        }
    }

    @JvmStatic
    fun parseFromByteArray(vcardByteArray: ByteArray?): List<AddressDO> {
        vcardByteArray ?: return emptyList()
        ByteArrayInputStream(vcardByteArray).use { stream ->
            try {
                return Ezvcard.parse(stream).all().map { buildAddressDO(it) }
            } catch (e: IOException) {
                log.error("An exception occurred while parsing vcard from byte array: " + e.message, e)
                return emptyList()
            }
        }
    }
}
