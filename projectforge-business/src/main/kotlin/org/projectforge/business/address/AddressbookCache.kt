/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * The address book entries will be cached.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
open class AddressbookCache : AbstractCache(), BaseDOChangedListener<AddressbookDO> {
    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    private lateinit var addressBookList: List<AddressbookDO>

    open fun getAddressbook(id: Int): AddressbookDO? {
        checkRefresh()
        synchronized(addressBookList) {
            return addressBookList.find { it.id == id }
        }
    }

    /**
     * @param ab Address book to search for (only id must be given).
     * @return address book from cache.
     */
    open fun getAddressbook(ab: AddressbookDO): AddressbookDO? {
        checkRefresh()
        synchronized(addressBookList) {
            return addressBookList.find { it.id == ab.id }
        }
    }

    /**
     * After modification of any address (insert, update, delete, undelete) this address should be removed from
     * this cache.
     */
    override fun afterSaveOrModifify(changedObject: AddressbookDO, operationType: OperationType) {
        setExpired()
    }

    @PostConstruct
    private fun postConstruct() {
        addressbookDao.register(this)
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing AddressbookCache ...")
        // This method must not be synchronized because it works with a new copy of maps.
        val newList = mutableListOf<AddressbookDO>()
        val list = addressbookDao.internalLoadAll()
        list.forEach {
            if (!it.isDeleted) {
                newList.add(it)
            }
        }
        addressBookList = newList
        log.info("Initializing of AddressbookCache done.")
    }
}
