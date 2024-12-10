/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.carddav.service

import mu.KotlinLogging
import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.business.address.vcard.VCardUtils
import org.projectforge.carddav.model.AddressBook
import org.projectforge.carddav.model.Contact
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class AddressService {
    @Autowired
    private lateinit var addressDAVCache: AddressDAVCache

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    fun getContactList(addressBook: AddressBook): List<Contact> {
        val favorites = personalAddressDao.favoriteAddressIdList
        return addressDAVCache.getContacts(addressBook, favorites)
    }

    fun getContact(id: Long): Contact? {
        val favorites = personalAddressDao.favoriteAddressIdList
        if (!favorites.contains(id)) {
            log.info { "Contact with id=$id is not in favorites. Don't returning contact." }
            return null
        }
        return addressDAVCache.getContact(id)
    }

    @Suppress("UNUSED_PARAMETER")
    fun createContact(ab: AddressBook, vcardBytearray: ByteArray): Contact {
        log.warn("Creation of contacts not supported.")
        /*try {
            val vcard = vCardService.getVCardFromByteArray(vcardBytearray) ?: return Contact()
            val address = vCardService.buildAddressDO(vcard)
            addressDao.save(address)
            personalAddressDao.save(...) // Add this new address to user's favorites.
        } catch (e: Exception) {
            log.error("Exception while creating contact.", e)
        }*/
        return Contact()
    }

    @Suppress("UNUSED_PARAMETER")
    fun updateContact(contact: Contact, vcardBytearray: ByteArray?): Contact {
        log.warn("Updating of contacts not supported.")
        /*try {
            val vcard = vCardService.getVCardFromByteArray(vcardBytearray) ?: return Contact()
            val address = vCardService.buildAddressDO(vcard)
            addressDao.update(address)
        } catch (e: Exception) {
            log.error("Exception while updating contact: " + contact.name, e)
        }*/
        return Contact()
    }

    fun deleteContact(contact: Contact) {
        try {
            val list = VCardUtils.parseVCardsFromByteArray(contact.vcardData)
            if (list.isEmpty()) {
                return // Nothing to do.
            }
            if (list.size > 1) {
                log.warn { "More than one vcard found in contact. Deleting only the first one." }
            }
            val vcard = list[0]
            val personalAddress = personalAddressDao.getByAddressUid(vcard.uid.value)
            if (personalAddress?.isFavorite == true) {
                personalAddress.isFavoriteCard = false
                personalAddressDao.saveOrUpdate(personalAddress)
                log.info("Contact '${vcard.formattedName.value} removed from ${ThreadLocalUserContext.loggedInUser!!.username}'s favorite list.")
            }
        } catch (e: Exception) {
            log.error(e) { "Exception while deleting contact: id=${contact.id}, name=[${contact.lastName}, ${contact.firstName}]" }
        }
    }
}
