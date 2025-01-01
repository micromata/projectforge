/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.history.FlatDisplayHistoryEntry
import org.projectforge.framework.persistence.history.HistoryEntry
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

    override fun select(filter: BaseSearchFilter): List<AddressCampaignValueDO> {
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
        return select(queryFilter)
    }

    /**
     * @see BaseDao.findOrLoad
     */
    fun setAddress(addressCampaignValue: AddressCampaignValueDO, addressId: Long) {
        val address = addressDao!!.findOrLoad(addressId)
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
        searchFilter.addressCampaignId ?: return map
        val list: List<AddressCampaignValueDO> = persistenceService.executeNamedQuery(
            AddressCampaignValueDO.FIND_BY_CAMPAIGN,
            AddressCampaignValueDO::class.java,
            Pair("addressCampaignId", searchFilter.addressCampaignId)
        )
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
        val list: List<AddressCampaignValueDO> = persistenceService.executeNamedQuery(
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

    fun setAddressDao(addressDao: AddressDao?) {
        this.addressDao = addressDao
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AddressCampaignValueDao::class.java)
    }
}
