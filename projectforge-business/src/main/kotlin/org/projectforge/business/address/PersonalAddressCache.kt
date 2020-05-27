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
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * Caches the personal addresses of users for faster access.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class PersonalAddressCache : AbstractCache() {
    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    /**
     * Map key is the user id, the entry is a map (key = address id, entry is the PersonalAddressDO).
     */
    private var ownersMap = mutableMapOf<Int, Map<Int, PersonalAddressDO>>()

    @JvmOverloads
    fun getByAddressId(addressId: Int, owner: PFUserDO? = ThreadLocalUserContext.getUser()): PersonalAddressDO? {
        owner?.id?.let { ownerId ->
            return getPersonalAddressList(ownerId)[addressId]
        }
        return null
    }

    @JvmOverloads
    fun isPersonalAddress(addressId: Int, owner: PFUserDO? = ThreadLocalUserContext.getUser()): Boolean {
        return getByAddressId(addressId, owner)?.isFavorite == true
    }

    private fun getPersonalAddressList(ownerId: Int): Map<Int, PersonalAddressDO> {
        // Try to get map form cache first.
        synchronized(ownersMap) {
            ownersMap[ownerId]?.let {
                return it
            }
        }
        // Read list of personal addresses from data base:
        val list = personalAddressDao.list
        val map = mutableMapOf<Int, PersonalAddressDO>()
        list.forEach { personalAddress ->
            personalAddress.addressId?.let { addressId ->
                map.put(addressId, personalAddress)
            }
        }
        synchronized(ownersMap) {
            ownersMap[ownerId] = map
        }
        return map
    }

    fun setAsExpired(userId: Int) {
        synchronized(ownersMap) {
            ownersMap.remove(userId)
        }
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Refreshing PersonalAddressCache...")
        synchronized(ownersMap) {
            ownersMap.clear()
        }
    }
}
