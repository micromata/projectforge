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

/**
 * A digital business card of a user: a snapshot of the contact data the user wants to publish (taken over from an
 * address of the address book and editable by hand), so a later change of the address doesn't change the published
 * card unnoticed. A user may have several cards (e.g. with and without the mobile number).
 *
 * Stored as json in the user preferences (see [BusinessCards]), not in an own table: the cards are personal data
 * only their owner works with. The gateway doesn't know them, it only delivers the contact image of the address.
 */
class BusinessCard {
    /**
     * Identifies the card in the list of its owner (select, update, delete) and is the uid of its vCard. Assigned by
     * [BusinessCardService], never taken over from a request. Not a secret: the cards are only reachable by the owner.
     */
    var id: String? = null

    /** Name of the card for the owner only (e.g. "With mobile"). */
    var cardName: String? = null

    /** The address the data was taken over from (a reference only, the card keeps its own copy). */
    var addressId: Long? = null

    /** Publish the photo of the address (via the gateway). */
    var includePhoto: Boolean = false

    var firstName: String? = null
    var lastName: String? = null
    var title: String? = null
    var organization: String? = null
    var division: String? = null
    var positionText: String? = null
    var email: String? = null
    var businessPhone: String? = null
    var mobilePhone: String? = null
    var fax: String? = null
    var website: String? = null
    var street: String? = null
    var zipCode: String? = null
    var city: String? = null
    var country: String? = null

    // The private data of the address: taken over only if the user ticks it (unticked by default in the editor).
    var privateEmail: String? = null
    var privatePhone: String? = null
    var privateMobilePhone: String? = null
    var privateStreet: String? = null
    var privateZipCode: String? = null
    var privateCity: String? = null
    var privateCountry: String? = null

    /** ISO date (yyyy-MM-dd), as text: the card is only a copy for the vCard. */
    var birthday: String? = null
    var comment: String? = null

    internal fun copy(): BusinessCard = BusinessCard().also {
        it.id = id
        it.copyEditableFrom(this)
    }

    /**
     * Copies everything the user may edit (all but the [id]) from the given card.
     */
    internal fun copyEditableFrom(src: BusinessCard) {
        cardName = src.cardName
        addressId = src.addressId
        includePhoto = src.includePhoto
        firstName = src.firstName
        lastName = src.lastName
        title = src.title
        organization = src.organization
        division = src.division
        positionText = src.positionText
        email = src.email
        businessPhone = src.businessPhone
        mobilePhone = src.mobilePhone
        fax = src.fax
        website = src.website
        street = src.street
        zipCode = src.zipCode
        city = src.city
        country = src.country
        privateEmail = src.privateEmail
        privatePhone = src.privatePhone
        privateMobilePhone = src.privateMobilePhone
        privateStreet = src.privateStreet
        privateZipCode = src.privateZipCode
        privateCity = src.privateCity
        privateCountry = src.privateCountry
        birthday = src.birthday
        comment = src.comment
    }
}

/**
 * All business cards of a user, the value of the user preference ([BusinessCardService.PREF_AREA]).
 */
class BusinessCards {
    var cards: MutableList<BusinessCard> = mutableListOf()
}
