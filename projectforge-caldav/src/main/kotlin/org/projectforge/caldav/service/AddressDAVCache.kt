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

package org.projectforge.caldav.service

import mu.KotlinLogging
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressImageDao
import org.projectforge.caldav.model.AddressBook
import org.projectforge.caldav.model.Contact
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOChangedListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct
import org.projectforge.framework.persistence.jpa.PfPersistenceContext

private val log = KotlinLogging.logger {}

/**
 * Cache needed, because vcard generation takes lot of cpu power....
 */
@Service
open class AddressDAVCache : AbstractCache(TICKS_PER_HOUR), BaseDOChangedListener<AddressDO> {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressImageDao: AddressImageDao

    @Autowired
    private lateinit var vCardService: VCardService

    private var contactMap = mutableMapOf<Long, Contact>()

    open fun getContacts(addressBook: AddressBook, ids: List<Long>): List<Contact> {
        val result = mutableListOf<Contact>()
        val missedInCache = mutableListOf<Long>()
        ids.forEach {
            val contact = getCachedAddress(it)
            if (contact != null) {
                val copy = Contact(contact, addressBook)
                result.add(copy)
            } else {
                missedInCache.add(it)
            }
        }
        log.info("Got ${result.size} addresses from cache and must load ${missedInCache.size} from data base...")
        if (missedInCache.size > 0) {
            addressDao.internalLoad(missedInCache)?.forEach {
                val vcard = vCardService.buildVCardByteArray(it, addressImageDao)
                val contact = Contact(it.id, it.fullName, it.lastUpdate, vcard)
                addCachedContact(it.id!!, contact)
                val copy = Contact(contact, addressBook)
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
    override fun afterSaveOrModify(changedObject: AddressDO, operationType: OperationType, context: PfPersistenceContext) {
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
