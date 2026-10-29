/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.address.businesscard

import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.parameter.AddressType
import ezvcard.parameter.EmailType
import ezvcard.parameter.TelephoneType
import ezvcard.property.Address
import ezvcard.property.Birthday
import ezvcard.property.Organization
import ezvcard.property.Photo
import ezvcard.property.StructuredName
import ezvcard.property.Uid
import org.projectforge.business.address.ImageType
import java.time.LocalDate
import java.util.UUID

/**
 * Builds the vCard of a business card. Compact on purpose (only the fields of the card, no empty properties, no
 * embedded photo): it is the content of a QR code, which gets dense quickly.
 */
object BusinessCardVCard {
    /**
     * @param photoUrl Public url of the photo (gateway). The photo itself is never embedded.
     * @return vCard 3.0 (understood by the camera apps of iOS and Android as well as by the address books).
     */
    fun build(card: BusinessCard, photoUrl: String? = null, photoType: ImageType? = null): String {
        val vcard = VCard()
        // Stable for a card, but not the uid of the address: the card is a copy.
        vcard.uid = Uid("urn:uuid:" + UUID.nameUUIDFromBytes((card.id ?: card.cardName ?: "").toByteArray()))
        val n = StructuredName()
        n.family = card.lastName?.trim()
        n.given = card.firstName?.trim()
        card.title.clean()?.let { n.prefixes.add(it) }
        vcard.structuredName = n
        // FN is mandatory and mustn't be empty: a card without a name shows the organization or the card name.
        vcard.setFormattedName(
            listOfNotNull(card.title.clean(), card.firstName.clean(), card.lastName.clean()).joinToString(" ")
                .ifEmpty { card.organization.clean() ?: card.cardName.clean() ?: "" }
        )
        card.organization.clean()?.let {
            val organization = Organization()
            organization.values.add(it)
            card.division.clean()?.let { division -> organization.values.add(division) }
            vcard.addOrganization(organization)
        }
        card.positionText.clean()?.let { vcard.addTitle(it) }
        card.businessPhone.clean()?.let { vcard.addTelephoneNumber(it, TelephoneType.WORK) }
        card.mobilePhone.clean()?.let { vcard.addTelephoneNumber(it, TelephoneType.CELL) }
        card.fax.clean()?.let { vcard.addTelephoneNumber(it, TelephoneType.FAX, TelephoneType.WORK) }
        card.privatePhone.clean()?.let { vcard.addTelephoneNumber(it, TelephoneType.HOME) }
        card.privateMobilePhone.clean()?.let { vcard.addTelephoneNumber(it, TelephoneType.CELL, TelephoneType.HOME) }
        card.email.clean()?.let { vcard.addEmail(it, EmailType.WORK) }
        card.privateEmail.clean()?.let { vcard.addEmail(it, EmailType.HOME) }
        card.website.clean()?.let { vcard.addUrl(it) }
        addAddress(vcard, AddressType.WORK, card.street, card.zipCode, card.city, card.country)
        addAddress(vcard, AddressType.HOME, card.privateStreet, card.privateZipCode, card.privateCity, card.privateCountry)
        card.birthday.clean()?.let { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()?.let { vcard.birthday = Birthday(it) }
        }
        card.comment.clean()?.let { vcard.addNote(it) }
        photoUrl?.takeIf { it.isNotBlank() }?.let {
            vcard.addPhoto(Photo(it, (photoType ?: ImageType.JPEG).asVCardImageType()))
        }
        // Apple expects upper case types (TYPE=JPEG), see VCardUtils.buildVCardString. Only in the PHOTO line, the
        // texts of the card stay as they are.
        return Ezvcard.write(vcard).version(VCardVersion.V3_0).go().lines().joinToString("\r\n") { line ->
            if (line.startsWith("PHOTO")) PHOTO_TYPE_REGEX.replace(line) { it.value.uppercase() } else line
        }
    }

    private val PHOTO_TYPE_REGEX = Regex("TYPE=(jpeg|png|gif)")

    private fun addAddress(
        vcard: VCard, type: AddressType, street: String?, zipCode: String?, city: String?, country: String?,
    ) {
        if (listOf(street, zipCode, city, country).none { it.clean() != null }) {
            return
        }
        val address = Address()
        address.types.add(type)
        address.streetAddress = street.clean()
        address.postalCode = zipCode.clean()
        address.locality = city.clean()
        address.country = country.clean()
        vcard.addAddress(address)
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
