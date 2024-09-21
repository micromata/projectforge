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

package org.projectforge.plugins.marketing

import jakarta.persistence.EntityManager
import org.jetbrains.kotlin.builtins.StandardNames.FqNames.list
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.history.HistProp
import org.projectforge.framework.persistence.history.HistoryEntry
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class AddressCampaignValueDao : BaseDao<AddressCampaignValueDO>(AddressCampaignValueDO::class.java) {
    @Autowired
    private var addressDao: AddressDao? = null

    init {
        userRightId = MarketingPluginUserRightId.PLUGIN_MARKETING_ADDRESS_CAMPAIGN_VALUE
    }

    fun get(addressId: Long?, addressCampaignId: Long?): AddressCampaignValueDO? {
        return persistenceService.selectNamedSingleResult(
            AddressCampaignValueDO.FIND_BY_ADDRESS_AND_CAMPAIGN,
            AddressCampaignValueDO::class.java,
            Pair("addressId", addressId), Pair("addressCampaignId", addressCampaignId),
        )
    }

    override fun getList(filter: BaseSearchFilter): List<AddressCampaignValueDO> {
        val myFilter = if (filter is AddressCampaignValueFilter) {
            filter
        } else {
            AddressCampaignValueFilter(filter)
        }
        val queryFilter = QueryFilter(myFilter)
        if (myFilter.addressCampaign != null) {
            queryFilter.add(eq("address_campaign_fk", myFilter.addressCampaign.id!!))
        }
        if (myFilter.addressCampaignValue != null) {
            queryFilter.add(eq("value", myFilter.addressCampaign.id!!))
        }
        return getList(queryFilter)
    }

    /**
     * @see BaseDao.getOrLoad
     */
    fun setAddress(addressCampaignValue: AddressCampaignValueDO, addressId: Long) {
        val address = addressDao!!.getOrLoad(addressId)
        addressCampaignValue.address = address
    }

    override fun newInstance(): AddressCampaignValueDO {
        return AddressCampaignValueDO()
    }

    fun getAddressCampaignValuesByAddressId(
        searchFilter: AddressCampaignValueFilter
    ): Map<Long?, AddressCampaignValueDO> {
        val map = HashMap<Long?, AddressCampaignValueDO>()
        return getAddressCampaignValuesByAddressId(map, searchFilter)
    }

    fun getAddressCampaignValuesByAddressId(
        map: MutableMap<Long?, AddressCampaignValueDO>,
        searchFilter: AddressCampaignValueFilter
    ): Map<Long?, AddressCampaignValueDO> {
        map.clear()
        val addressCampaignId = searchFilter.addressCampaignId ?: return map
        val list: List<AddressCampaignValueDO> = persistenceService.namedQuery(
            AddressCampaignValueDO.FIND_BY_CAMPAIGN,
            AddressCampaignValueDO::class.java,
            Pair("addressCampaignId", searchFilter.addressCampaignId))
        if (CollectionUtils.isEmpty(list)) {
            return map
        }
        for (addressCampaignValue in list) {
            map[addressCampaignValue.addressId] = addressCampaignValue
        }
        return map
    }

    fun getAddressCampaignValuesByAddressId(
        map: MutableMap<Long, AddressCampaignValueDO>,
        addressCampaignId: Long?
    ): Map<Long, AddressCampaignValueDO> {
        map.clear()
        if (addressCampaignId == null) {
            return map
        }
        val list: List<AddressCampaignValueDO> = persistenceService.namedQuery(
            AddressCampaignValueDO.FIND_BY_CAMPAIGN,
            AddressCampaignValueDO::class.java,
            Pair("addressCampaignId", addressCampaignId),
        )
        if (CollectionUtils.isEmpty(list)) {
            return map
        }
        for (addressCampaignValue in list) {
            map[addressCampaignValue.addressId!!] = addressCampaignValue
        }
        return map
    }

    override fun convert(context: PfPersistenceContext, entry: HistoryEntry<*>): List<DisplayHistoryEntry> {
        if (entry.diffEntries!!.isEmpty()) {
            val se = DisplayHistoryEntry(userGroupCache, entry)
            return listOf(se)
        }
        val result: MutableList<DisplayHistoryEntry> = ArrayList()
        for (prop in entry.diffEntries!!) {
            val se: DisplayHistoryEntry = object : DisplayHistoryEntry(userGroupCache, entry, prop, context.em) {
                override fun getObjectValue(userGroupCache: UserGroupCache, em: EntityManager, prop: HistProp?): Any? {
                    if (prop == null) {
                        return null
                    }

                    val type = prop.type

                    if (AddressDO::class.java.name == type) {
                        return prop.value
                    }
                    if (AddressCampaignDO::class.java.name == type) {
                        return prop.value
                    }

                    return super.getObjectValue(userGroupCache, em, prop)
                }
            }
            result.add(se)
        }

        return result
    }

    fun setAddressDao(addressDao: AddressDao?) {
        this.addressDao = addressDao
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AddressCampaignValueDao::class.java)
    }
}
