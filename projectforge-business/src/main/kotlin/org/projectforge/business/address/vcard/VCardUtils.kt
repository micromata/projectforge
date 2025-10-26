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
import org.projectforge.business.address.FormOfAddress
import org.projectforge.business.address.ImageType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.LocalDate
import java.util.*

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
        // Add form (salutation) first with i18n: "Herr", "Frau", "Firma", etc.
        addressDO.form?.let {
            n.prefixes.add(ThreadLocalUserContext.getLocalizedString(it.i18nKey))
        }
        // Add title (academic title) second: "Dr.", "Prof.", etc.
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
            val languageTag = communicationLanguage.toLanguageTag()
            // Use standard LANG property (works in V4.0)
            vcard.addLanguage(languageTag)
            vcard.structuredName.language = languageTag
            // Also add X-LANG custom property for V3.0 compatibility
            vcard.addExtendedProperty("X-LANG", languageTag)
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
        vcard.addOrganization(organisation)
        // Position/title as separate TITLE property
        addressDO.positionText?.let { vcard.addTitle(it) }
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
        // Add birthName as custom property
        addressDO.birthName?.let { vcard.addExtendedProperty("X-BIRTHNAME", it) }
        // Add PGP public key as base64-encoded binary data
        addressDO.publicKey?.let { keyText ->
            // Store as binary data (ez-vcard will base64-encode it automatically)
            val keyData = keyText.toByteArray(Charsets.UTF_8)
            val keyProperty = Key(keyData, ezvcard.parameter.KeyType.PGP)
            vcard.addKey(keyProperty)
        }
        // Add PGP fingerprint as custom property
        addressDO.fingerprint?.let { vcard.addExtendedProperty("X-PGP-FPR", it) }
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
        // Parse prefixes: form (salutation) and title (academic title)
        vcard.structuredName?.prefixes?.forEach { prefix ->
            val parsedForm = parseFormOfAddress(prefix)
            if (parsedForm != null) {
                address.form = parsedForm
            } else {
                // Not a known form, treat as title (academic title like "Dr.", "Prof.")
                address.title = prefix
            }
        }
        for (vcardAddress in vcard.addresses) {
            if (vcardAddress.types.contains(AddressType.HOME)) {
                address.privateAddressText = vcardAddress.streetAddress
                address.privateAddressText2 = vcardAddress.extendedAddress
                address.privateZipCode = vcardAddress.postalCode
                address.privateCity = vcardAddress.locality
                address.privateState = vcardAddress.region
                address.privateCountry = vcardAddress.country
            }
            if (vcardAddress.types.contains(AddressType.WORK)) {
                address.addressText = vcardAddress.streetAddress
                address.addressText2 = vcardAddress.extendedAddress
                address.zipCode = vcardAddress.postalCode
                address.city = vcardAddress.locality
                address.state = vcardAddress.region
                address.country = vcardAddress.country
            }
            if (vcardAddress.types.contains(AddressType.POSTAL)) {
                address.postalAddressText = vcardAddress.streetAddress
                address.postalAddressText2 = vcardAddress.extendedAddress
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
                    // Legacy support: old exports had positionText as 3rd value
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
        // Parse positionText from TITLE property (preferred over ORG 3rd value)
        vcard.titles.firstOrNull()?.let { title ->
            address.positionText = title.value
        }
        // Parse birthName from custom property
        vcard.getExtendedProperty("X-BIRTHNAME")?.let { extProp ->
            address.birthName = extProp.value
        }
        // Parse PGP public key from KEY property (base64-decoded binary data)
        vcard.keys.firstOrNull()?.let { key ->
            // ez-vcard stores keys - just take the first one and assume it's a PGP key
            // Decode from binary data back to text
            address.publicKey = key.data?.toString(Charsets.UTF_8) ?: key.text
        }
        // Parse PGP fingerprint from custom property
        vcard.getExtendedProperty("X-PGP-FPR")?.let { extProp ->
            address.fingerprint = extProp.value
        }
        // Parse communicationLanguage from X-LANG (V3.0) or LANG property (V4.0)
        val languageTag = vcard.getExtendedProperty("X-LANG")?.value
            ?: vcard.languages.firstOrNull()?.value
        languageTag?.let { tag ->
            try {
                address.communicationLanguage = Locale.forLanguageTag(tag)
            } catch (e: Exception) {
                log.warn("Failed to parse language tag: $tag", e)
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

    /**
     * Parse a localized form of address string back to FormOfAddress enum.
     * E.g. "Herr" -> MISTER, "Frau" -> MISS, "Firma" -> COMPANY
     */
    private fun parseFormOfAddress(localizedString: String): FormOfAddress? {
        return FormOfAddress.values().firstOrNull { form ->
            ThreadLocalUserContext.getLocalizedString(form.i18nKey) == localizedString
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
