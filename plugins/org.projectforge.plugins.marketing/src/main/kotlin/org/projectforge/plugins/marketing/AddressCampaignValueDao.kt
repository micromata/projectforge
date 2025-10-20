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

import mu.KotlinLogging
import org.projectforge.business.address.AddressDao
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class AddressCampaignValueDao : BaseDao<AddressCampaignValueDO>(AddressCampaignValueDO::class.java) {
    @Autowired
    private var addressDao: AddressDao? = null

    @Autowired
    private lateinit var addressCampaignDao: AddressCampaignDao

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

    /**
     * Selects all addresses for a given campaign, including addresses without campaign values.
     * Uses AddressDao for proper access control and filtering, then merges with campaign values.
     *
     * @param campaignId The campaign ID to load values for
     * @param filter QueryFilter for address filtering (contactStatus, addressStatus, organization, etc.)
     * @param customResultFilters CustomResultFilters for addresses (favorites, doublets, images, etc.)
     * @param checkAccess Whether to check access rights
     * @return List of AddressCampaignValueDO with address data (value/comment null if no campaign value exists)
     */
    fun selectAddressesForCampaign(
        campaignId: Long,
        filter: QueryFilter,
        customResultFilters: List<CustomResultFilter<AddressCampaignValueDO>>?,
        checkAccess: Boolean = true,
    ): List<AddressCampaignValueDO> {
        // 1. Load all addresses matching the filter (with access control)
        val addressFilters = customResultFilters?.mapNotNull { campaignFilter ->
            // Extract address filters from campaign value filter adapters
            if (campaignFilter is org.projectforge.plugins.marketing.rest.CampaignValueFilterAdapter) {
                // Use reflection to get the wrapped address filter
                try {
                    val field = campaignFilter.javaClass.getDeclaredField("addressFilter")
                    field.isAccessible = true
                    field.get(campaignFilter) as? CustomResultFilter<org.projectforge.business.address.AddressDO>
                } catch (e: Exception) {
                    log.warn { "Could not extract address filter from CampaignValueFilterAdapter: ${e.message}" }
                    null
                }
            } else {
                null
            }
        }

        val addresses = addressDao!!.select(filter, addressFilters, checkAccess)

        // 2. Load campaign values for this campaign (by ID map)
        // Include deleted values so they keep their real IDs instead of getting synthetic negative IDs
        val campaignValuesMap = getAddressCampaignValuesByAddressId(
            mutableMapOf(),
            campaignId,
            includeDeleted = true
        )

        // 3. Load the campaign object once
        val campaign = addressCampaignDao.find(campaignId)

        // 4. Merge: For each address, use existing campaign value or create transient one
        val result = addresses.map { address ->
            val existingValue = campaignValuesMap[address.id]
            if (existingValue != null) {
                existingValue
            } else {
                // Create transient campaign value (not persisted)
                // Assign synthetic ID using negative addressId to enable multi-selection tracking
                val newValue = AddressCampaignValueDO()
                newValue.id = -address.id!! // Synthetic ID: negative addressId
                newValue.address = address
                newValue.addressCampaign = campaign
                newValue.value = null
                newValue.comment = null
                newValue
            }
        }

        // 5. Apply remaining custom filters (value filters that are not address filters)
        val remainingFilters = customResultFilters?.filter { filter ->
            filter !is org.projectforge.plugins.marketing.rest.CampaignValueFilterAdapter
        }

        if (!remainingFilters.isNullOrEmpty()) {
            val mutableResult = result.toMutableList()
            remainingFilters.forEach { filter ->
                mutableResult.removeIf { !filter.match(mutableResult, it) }
            }
            return mutableResult
        }

        return result
    }

    override fun select(
        filter: QueryFilter,
        customResultFilters: List<CustomResultFilter<AddressCampaignValueDO>>?,
        checkAccess: Boolean,
    ): List<AddressCampaignValueDO> {
        // Safety check: AddressCampaignValue must always be filtered by campaign
        // to avoid loading all values across all campaigns (performance + data isolation)
        val campaignId = filter.extended["campaignId"] as? Long
        if (campaignId == null) {
            log.warn { "AddressCampaignValueDao.select called without campaignId. Returning empty list to prevent loading all campaign values." }
            return emptyList()
        }

        // Use address-based query with merge to include addresses without campaign values
        return selectAddressesForCampaign(campaignId, filter, customResultFilters, checkAccess)
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
        } else {
            return emptyList()
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
        addressCampaignId: Long?,
        includeDeleted: Boolean = false
    ): Map<Long, AddressCampaignValueDO> {
        map.clear()
        if (addressCampaignId == null) {
            return map
        }
        val queryName = if (includeDeleted) {
            AddressCampaignValueDO.FIND_BY_CAMPAIGN_INCLUDING_DELETED
        } else {
            AddressCampaignValueDO.FIND_BY_CAMPAIGN
        }
        val list: List<AddressCampaignValueDO> = persistenceService.executeNamedQuery(
            queryName,
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
}
