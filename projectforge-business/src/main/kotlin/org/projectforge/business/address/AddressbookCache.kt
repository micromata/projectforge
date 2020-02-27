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

import org.projectforge.framework.cache.AbstractCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import javax.persistence.LockModeType

/**
 * The address book entries will be cached.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
open class AddressbookCache : AbstractCache() {
    @Autowired
    private lateinit var em: EntityManager

    private lateinit var addressBookList: List<AddressbookDO>

    open fun getAddressbook(id: Int): AddressbookDO? {
        checkRefresh()
        return addressBookList.find { it.id == id }
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
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing AddressbookCache ...")
        // This method must not be synchronized because it works with a new copy of maps.
        val newList = mutableListOf<AddressbookDO>()
        val list = em.createQuery("from AddressbookDO t", AddressbookDO::class.java)
                .setLockMode(LockModeType.NONE)
                .resultList
        list.forEach {
            if (!it.isDeleted) {
                newList.add(it)
            }
        }
        addressBookList = newList
        log.info("Initializing of AddressbookCache done.")
    }

    companion object {
        private val log = LoggerFactory.getLogger(AddressbookCache::class.java)
    }
}
