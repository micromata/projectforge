/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.DateHolder
import org.projectforge.registry.Registry
import java.util.*

class BirthdayCache() : AbstractCache() {
    private var addressDao: AddressDao

    private var cacheList = mutableListOf<BirthdayAddress>()

    init {
        val registryEntry = Registry.instance.getEntry(AddressDao::class.java)
        addressDao = registryEntry.dao as AddressDao;
    }

    /**
     * Get the birthdays of address entries.
     *
     * @param fromDate Search for birthdays from given date (ignoring the year).
     * @param toDate   Search for birthdays until given date (ignoring the year).
     * @param all      If false, only the birthdays of favorites will be returned.
     * @return The entries are ordered by date of year and name.
     */
    fun getBirthdays(fromDate: Date, toDate: Date, all: Boolean, favorites: List<Int>)
            : Set<BirthdayAddress> {
        checkRefresh()
        // Uses not Collections.sort because every comparison needs Calendar.getDayOfYear().
        val set = TreeSet<BirthdayAddress>()
        val from = DateHolder(fromDate)
        val to = DateHolder(toDate)
        var dh: DateHolder
        val fromMonth = from.month
        val fromDayOfMonth = from.dayOfMonth
        val toMonth = to.month
        val toDayOfMonth = to.dayOfMonth
        for (birthdayAddress in cacheList) {
            val address = birthdayAddress.address
            if (!addressDao.hasLoggedInUserSelectAccess(address,false)) {
                // User has no access to the given address.
                continue
            }
            if (!all && !favorites.contains(address.id)) {
                // Address is not a favorite address, so ignore it.
                continue
            }
            dh = DateHolder(address.birthday)
            val month = dh.month
            val dayOfMonth = dh.dayOfMonth
            if (!DateHelper.dateOfYearBetween(month, dayOfMonth, fromMonth, fromDayOfMonth, toMonth, toDayOfMonth)) {
                continue
            }
            val ba = BirthdayAddress(address)
            ba.isFavorite = favorites.contains(address.getId())
            set.add(ba)
        }
        return set
    }

    override fun refresh() {
        val filter = QueryFilter()
        filter.add(QueryFilter.isNotNull("birthday"))
        val addressList = addressDao.internalGetList(filter)
        val newList = mutableListOf<BirthdayAddress>()
        addressList.forEach {
            newList.add(BirthdayAddress(it))
        }
        cacheList = newList
    }
}
