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

import org.projectforge.birthdaybutler.BirthdayButlerConfiguration
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressImageDao
import org.projectforge.business.address.AddressUserMatcher
import org.projectforge.business.address.ContactImageUrl
import org.projectforge.business.address.ImageType
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * The business cards of the logged-in user, kept in the user preferences. There is no way to reach the cards of
 * another user: everything works on the list of the logged-in user (there is no user parameter on purpose), a posted
 * id only selects among those.
 *
 * The photo is published (via the gateway) only from the user's own address (see [photoSource]): otherwise anybody
 * who may read an address could make the photo of a colleague public.
 *
 * Writes are serialized: the cards of a user are one preference, read, changed and written back.
 */
@Service
open class BusinessCardService {
    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressImageDao: AddressImageDao

    @Autowired
    private lateinit var contactImageUrl: ContactImageUrl

    @Autowired
    private lateinit var birthdayButlerConfiguration: BirthdayButlerConfiguration

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * The cards of the logged-in user, sorted by name. Copies of the stored cards: changes have to be saved with
     * [save].
     */
    open fun list(): List<BusinessCard> {
        return load().cards.map { it.copy() }.sortedBy { it.cardName?.lowercase() ?: "" }
    }

    open fun get(id: String?): BusinessCard? {
        id ?: return null
        return load().cards.firstOrNull { it.id == id }?.copy()
    }

    /**
     * Inserts the card (no id given) or updates the own card with the given id. The id is never taken over:
     * a new card gets a new one, an updated card keeps its own.
     * @return The stored card.
     * @throws UserException if the name is missing, the limit is reached, the id is unknown (for this user) or the
     * photo is wanted from an address that isn't the user's own one.
     */
    open fun save(card: BusinessCard): BusinessCard {
        if (card.cardName.isNullOrBlank()) {
            throw UserException(ERROR_NAME_REQUIRED)
        }
        validate(card)
        if (card.includePhoto && photoSource(card) == null) {
            throw UserException(ERROR_PHOTO_NOT_OWN_ADDRESS)
        }
        synchronized(this) {
            return saveInternal(card)
        }
    }

    private fun saveInternal(card: BusinessCard): BusinessCard {
        val data = load()
        val stored = if (card.id.isNullOrBlank()) {
            if (data.cards.size >= MAX_CARDS_PER_USER) {
                throw UserException(ERROR_LIMIT_REACHED)
            }
            BusinessCard().also {
                it.id = createId()
                data.cards.add(it)
            }
        } else {
            data.cards.firstOrNull { it.id == card.id } ?: throw UserException(ERROR_UNKNOWN_CARD)
        }
        stored.copyEditableFrom(card)
        userPrefService.putEntry(PREF_AREA, PREF_NAME, data, true)
        return stored.copy()
    }

    /**
     * @return true, if a card was deleted.
     */
    open fun delete(id: String?): Boolean {
        id ?: return false
        synchronized(this) {
            val data = load()
            val removed = data.cards.removeIf { it.id == id }
            if (removed) {
                userPrefService.putEntry(PREF_AREA, PREF_NAME, data, true)
            }
            return removed
        }
    }

    /**
     * The client checks the lengths too, but the request may carry anything: a huge text would bloat the
     * preference and the QR code, a birthday that isn't an ISO date would be dropped from the vCard silently.
     */
    private fun validate(card: BusinessCard) {
        val texts = listOf(
            card.cardName, card.title, card.firstName, card.lastName, card.organization, card.division,
            card.positionText, card.email, card.businessPhone, card.mobilePhone, card.fax, card.website, card.street,
            card.zipCode, card.city, card.country, card.privateEmail, card.privatePhone, card.privateMobilePhone,
            card.privateStreet, card.privateZipCode, card.privateCity, card.privateCountry,
        )
        val birthdayValid = card.birthday.isNullOrBlank() ||
                runCatching { java.time.LocalDate.parse(card.birthday!!.trim()) }.isSuccess
        if (texts.any { (it?.length ?: 0) > MAX_LENGTH } || (card.comment?.length ?: 0) > MAX_COMMENT_LENGTH ||
            !birthdayValid
        ) {
            throw UserException(ERROR_INVALID_FIELD)
        }
    }

    /**
     * The addresses (readable for the user) that probably are the user's own one, best match first, see
     * [AddressUserMatcher.suggest]. The organization of the birthday butler is taken as hint, if configured.
     */
    open fun suggestAddresses(): List<AddressDO> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val firstName = user.firstname?.trim()?.lowercase()
        val lastName = user.lastname?.trim()?.lowercase()
        if (firstName.isNullOrEmpty() || lastName.isNullOrEmpty()) {
            return emptyList()
        }
        // Only the few addresses of the user's name, not the whole address book.
        val candidates = persistenceService.executeQuery(
            "SELECT a FROM AddressDO a WHERE a.deleted = false" +
                    " AND lower(trim(a.firstName)) = :firstName AND lower(trim(a.name)) = :lastName",
            AddressDO::class.java,
            Pair("firstName", firstName),
            Pair("lastName", lastName),
        ).filter { addressDao.hasLoggedInUserSelectAccess(it, false) }
        val organization = birthdayButlerConfiguration.takeIf { it.isConfigured() }?.organization
        return AddressUserMatcher.suggest(user, candidates, organization)
    }

    /**
     * The vCard of the card (also the content of the QR code). The photo is a url on the gateway, only if the card
     * wants it, a gateway is configured and the source address is the user's own one (see [photoSource]) and has an
     * image. Otherwise the vCard has no photo (never an error: the address may have become unreadable meanwhile).
     */
    @JvmOverloads
    open fun buildVCard(card: BusinessCard, photoSource: AddressDO? = photoSource(card)): String {
        var photoUrl: String? = null
        var photoType: ImageType? = null
        if (card.includePhoto) {
            photoSource?.let { address ->
                addressImageDao.findImage(address.id!!, checkAccess = false)?.let { image ->
                    photoUrl = contactImageUrl.url(address)
                    photoType = image.imageType
                }
            }
        }
        return BusinessCardVCard.build(card, photoUrl, photoType)
    }

    /**
     * The source address of the card, if its photo may be published: not deleted, readable for the logged-in user and
     * the user's own address. Own means one of the user's e-mail addresses is the address's (see
     * [AddressUserMatcher.emailMatches]); the name isn't enough: the user can change it (My account), and two
     * colleagues may share one. The e-mail addresses of a user are administered, not set by the user.
     */
    open fun photoSource(card: BusinessCard): AddressDO? {
        val addressId = card.addressId ?: return null
        val address = addressDao.find(addressId, checkAccess = false)?.takeIf { !it.deleted } ?: return null
        if (!addressDao.hasLoggedInUserSelectAccess(address, false)) {
            return null
        }
        val user = ThreadLocalUserContext.requiredLoggedInUser
        return address.takeIf { AddressUserMatcher.emailMatches(it, user) }
    }

    private fun load(): BusinessCards {
        return userPrefService.getEntry(PREF_AREA, PREF_NAME, BusinessCards::class.java) ?: BusinessCards()
    }

    companion object {
        const val PREF_AREA = "businessCard"
        const val PREF_NAME = "cards"
        const val MAX_CARDS_PER_USER = 20
        const val ERROR_NAME_REQUIRED = "businessCard.error.nameRequired"
        const val ERROR_LIMIT_REACHED = "businessCard.error.limitReached"
        const val ERROR_UNKNOWN_CARD = "businessCard.error.unknownCard"
        const val ERROR_PHOTO_NOT_OWN_ADDRESS = "businessCard.error.photoNotOwnAddress"
        const val ERROR_INVALID_FIELD = "businessCard.error.invalidField"
        private const val MAX_LENGTH = 255
        private const val MAX_COMMENT_LENGTH = 1000

        internal fun createId(): String = UUID.randomUUID().toString()

        /**
         * A new card (not stored) with the data of the given address as its own copy, the private data included: the
         * user may read the address anyway. Which of it goes on the card is the user's choice (the editor ticks only
         * the business data by default), and everything can be changed afterwards.
         */
        fun createFrom(address: AddressDO, cardName: String? = null): BusinessCard {
            return BusinessCard().also {
                it.cardName = cardName ?: address.organization
                it.addressId = address.id
                it.firstName = address.firstName
                it.lastName = address.name
                it.title = address.title
                it.organization = address.organization
                it.division = address.division
                it.positionText = address.positionText
                it.email = address.email
                it.businessPhone = address.businessPhone
                it.mobilePhone = address.mobilePhone
                it.fax = address.fax
                it.website = address.website
                it.street = address.addressText
                it.zipCode = address.zipCode
                it.city = address.city
                it.country = address.country
                it.privateEmail = address.privateEmail
                it.privatePhone = address.privatePhone
                it.privateMobilePhone = address.privateMobilePhone
                it.privateStreet = address.privateAddressText
                it.privateZipCode = address.privateZipCode
                it.privateCity = address.privateCity
                it.privateCountry = address.privateCountry
                it.birthday = address.birthday?.toString()
            }
        }
    }
}
