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

package org.projectforge.business.address

import jakarta.annotation.PostConstruct
import jakarta.persistence.Tuple
import mu.KotlinLogging
import org.hibernate.Hibernate
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.database.TupleUtils.getLong
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * The address book entries will be cached.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
open class AddressbookCache : AbstractCache() {
    // Will be set by AddressDao (cycle dependency).
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    private var addressBookList = listOf<AddressbookDO>() // Mustn't be synchronized, it's only read.

    // key is the addres.id, value is a set of addressbook's assigned to the address.
    private var addressBookByAddressMap = mapOf<Long, List<AddressbookDO>>() // Mustn't be synchronized, it's only read.

    fun getAll(): List<AddressbookDO> {
        checkRefresh()
        return addressBookList
    }

    open fun getAddressbook(id: Long?): AddressbookDO? {
        id ?: return null
        checkRefresh()
        return addressBookList.find { it.id == id }
    }

    /**
     * Returns the AddressbookDO if it is initialized (Hibernate). Otherwise, it will be loaded from the database.
     * Prevents lazy loadings.
     */
    fun getAddressbookIfNotInitialized(ab: AddressbookDO?): AddressbookDO? {
        ab ?: return null
        if (Hibernate.isInitialized(ab)) {
            return ab
        }
        return getAddressbook(ab.id)
    }


    /**
     * @param ab Address book to search for (only id must be given).
     * @return address book from cache.
     */
    open fun getAddressbook(ab: AddressbookDO): AddressbookDO? {
        checkRefresh()
        return addressBookList.find { it.id == ab.id }
    }

    /**
     * Gets all address books for the given address.
     * @param addressId Address id.
     */
    open fun getAddressbooksForAddress(addressId: Long?): List<AddressbookDO>? {
        addressId ?: return null
        checkRefresh()
        return addressBookByAddressMap[addressId]
    }

    /**
     * Gets all address books for the given address.
     */
    open fun getAddressbooksForAddress(address: AddressDO?): List<AddressbookDO>? {
        return getAddressbooksForAddress(address?.id)
    }

    internal fun setAddressDao(addressDao: AddressDao) {
        this.addressDao = addressDao
        addressDao.register(object : BaseDOModifiedListener<AddressDO> {
            override fun afterInsertOrModify(obj: AddressDO, operationType: OperationType) {
                setExpired()
            }
        })
    }

    @PostConstruct
    private fun postConstruct() {
        instance = this
        addressbookDao.register(object : BaseDOModifiedListener<AddressbookDO> {
            override fun afterInsertOrModify(obj: AddressbookDO, operationType: OperationType) {
                setExpired()
            }
        })
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing AddressbookCache ...")
        persistenceService.runIsolatedReadOnly(recordCallStats = true) { context ->
            // This method must not be synchronized because it works with a new copy of maps.
            val newList = mutableListOf<AddressbookDO>()
            addressbookDao.selectAll(checkAccess = false).forEach {
                if (it.deleted != true) {
                    newList.add(it)
                }
            }
            val em = context.em
            val newMap = em.createQuery(SELECT_ADDRESS_ID_WITH_ADDRESSBOOK_IDS, Tuple::class.java).resultList
                .groupBy(
                    { getLong(it, "addressId")!! },
                    { tuple -> newList.find { it.id == getLong(tuple, "addressbookId") }!! }
                )
            addressBookList = newList
            addressBookByAddressMap = newMap
            log.info { "Initializing of AddressbookCache done. ${context.formatStats(true)}" }
        }
    }

    companion object {
        lateinit var instance: AddressbookCache
            private set

        private val SELECT_ADDRESS_ID_WITH_ADDRESSBOOK_IDS = """
            SELECT a.id as addressId,b.id as addressbookId
            FROM ${AddressDO::class.simpleName} a
            JOIN a.addressbookList b
            WHERE a.deleted = false
        """.trimIndent()

    }
}
