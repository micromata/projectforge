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

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressImageDao
import org.projectforge.business.address.vcard.ImageType
import org.projectforge.business.address.vcard.VCardUtils
import org.projectforge.carddav.CardDavConfig
import org.projectforge.carddav.CardDavUtils
import org.projectforge.carddav.model.Contact
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Cache needed, because vcard generation takes a lot of cpu power....
 */
@Service
open class AddressDAVCache : AbstractCache(TICKS_PER_HOUR), BaseDOModifiedListener<AddressDO> {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressImageDao: AddressImageDao

    @Autowired
    private lateinit var cardDavConfig: CardDavConfig

    private var contactMap = mutableMapOf<Long, Contact>()

    fun getContact(id: Long): Contact? {
        return getCachedAddress(id)
    }

    fun getContacts(ids: List<Long>): List<Contact> {
        val result = mutableListOf<Contact>()
        val missedInCache = mutableListOf<Long>()
        ids.forEach {
            val contact = getCachedAddress(it)
            if (contact != null) {
                result.add(contact)
            } else {
                missedInCache.add(it)
            }
        }
        log.info { "Got ${result.size} addresses from cache and must load ${missedInCache.size} from data base..." }
        if (missedInCache.size > 0) {
            addressDao.select(missedInCache, checkAccess = false)?.forEach {
                val imageType = ImageType.PNG
                val imageUrl = if(it.image == true) {
                    CardDavUtils.getImageUrl(it.id!!, imageType)
                } else {
                    null
                }
                val vcard = VCardUtils.buildVCardString(it, cardDavConfig.vcardVersion, imageUrl = imageUrl, imageType = imageType)
                val contact =
                    Contact(
                        it.id,
                        firstName = it.firstName,
                        lastName = it.name,
                        lastUpdated = it.lastUpdate,
                        hasImage = it.image == true,
                        imageLastUpdate = it.imageLastUpdate,
                        vcardData = vcard,
                    )
                addCachedContact(it.id!!, contact)
                val copy = contact
                result.add(copy)
            }
        }
        return result
    }

    private fun getCachedAddress(id: Long): Contact? {
        synchronized(contactMap) {
            return contactMap[id]
        }
    }

    private fun addCachedContact(id: Long, contact: Contact) {
        synchronized(contactMap) {
            contactMap[id] = contact
        }
    }

    /**
     * After modification of any address (insert, update, delete, undelete) this address should be removed from
     * this cache.
     */
    override fun afterInsertOrModify(changedObject: AddressDO, operationType: OperationType) {
        synchronized(contactMap) {
            contactMap.remove(changedObject.id)
        }
    }

    @PostConstruct
    fun postConstruct() {
        addressDao.register(this)
    }

    override fun refresh() {
        log.info("Clearing cache ${this::class.java.simpleName}.")
        synchronized(contactMap) {
            contactMap.clear()
        }
    }
}
