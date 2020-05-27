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

package org.projectforge.business.address

import mu.KotlinLogging
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOChangedListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * The address book entries will be cached.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class AddressCache : AbstractCache(), BaseDOChangedListener<AddressDO> {
    @Autowired
    private lateinit var addressDao: AddressDao

    /**
     * List of address books per address (by address.id).
     */
    private val addressMap = mutableMapOf<Int, MutableSet<AddressbookDO>>()

    /**
     * @param address: must be attqched to entity manager for lazy loading of address books.
     */
    fun getAddressbooks(address: AddressDO): Set<AddressbookDO>? {
        val id = address.id ?: return null
        addressMap[id]?.let {
            return it
        }
        val result = address.addressbookList ?: mutableSetOf()
        synchronized(addressMap) {
            addressMap[id] = result
        }
        return result
    }

    internal fun setAddressExpired(addressId: Int) {
        synchronized(addressMap) {
            addressMap.remove(addressId)
        }
    }

    @PostConstruct
    private fun postConstruct() {
        addressDao.register(this)
    }

    override fun afterSaveOrModifify(changedObject: AddressDO, operationType: OperationType) {
        synchronized(addressMap) {
            addressMap.remove(changedObject.id)
        }
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Refreshing AddressCache ...")
        synchronized(addressMap) {
            addressMap.clear()
        }
    }
}
