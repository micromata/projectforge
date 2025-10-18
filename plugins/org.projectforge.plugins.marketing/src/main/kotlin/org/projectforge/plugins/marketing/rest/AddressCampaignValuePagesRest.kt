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

package org.projectforge.plugins.marketing.rest

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.PersonalAddressDO
import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.plugins.marketing.AddressCampaignDO
import org.projectforge.plugins.marketing.AddressCampaignDao
import org.projectforge.plugins.marketing.AddressCampaignValueDO
import org.projectforge.plugins.marketing.AddressCampaignValueDao
import org.projectforge.plugins.marketing.dto.AddressCampaign
import org.projectforge.plugins.marketing.dto.AddressCampaignValue
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.multiselect.MultiSelectionSupport
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/addressCampaignValue")
class AddressCampaignValuePagesRest :
    AbstractDTOPagesRest<AddressDO, AddressCampaignValue, AddressDao>(
        baseDaoClazz = AddressDao::class.java,
        i18nKeyPrefix = "plugins.marketing.addressCampaignValue.title"
    ) {
    @Autowired
    private lateinit var addressCampaignDao: AddressCampaignDao

    @Autowired
    private lateinit var addressCampaignValueDao: AddressCampaignValueDao

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    companion object {
        private const val USER_PREF_SELECTED_CAMPAIGN_ID = "AddressCampaignValuePagesRest.selectedCampaignId"
        private const val FILTER_CAMPAIGN_ID = "AddressCampaignValuePagesRest.campaignId"
    }

    /**
     * ########################################
     * # Force usage only for selection mode: #
     * ########################################
     */
    override fun getInitialList(request: HttpServletRequest): InitialListData {
        MultiSelectionSupport.ensureMultiSelectionOnly(request, this, "/wa/addressCampaignValuesList")
        return super.getInitialList(request)
    }

    /**
     * Add campaign selector to magic filter elements
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        val campaigns = addressCampaignDao.select(deleted = false)
        val campaignValues = campaigns.map { UISelectValue(it.id.toString(), it.title ?: "unknown") }

        val campaignFilter = UIFilterListElement(
            id = FILTER_CAMPAIGN_ID,
            values = campaignValues,
            label = "plugins.marketing.addressCampaign.title",
            multi = false,
            defaultFilter = true
        )

        elements.add(campaignFilter)
    }

    /**
     * Process magic filter to handle campaign selection
     */
    override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
        super.postProcessMagicFilter(target, source)

        // Find the campaign ID from filter entries
        val campaignEntry = source.entries.find { it.field == FILTER_CAMPAIGN_ID }
        var campaignId: Long? = null

        if (campaignEntry != null && !campaignEntry.value.values.isNullOrEmpty()) {
            // User has selected a campaign from the filter
            val campaignIdStr = campaignEntry.value.values?.firstOrNull()
            campaignId = campaignIdStr?.toLongOrNull()

            if (campaignId != null) {
                // Save to user preferences for persistence across sessions
                userPrefService.putEntry(category, USER_PREF_SELECTED_CAMPAIGN_ID, campaignId)
            }
        } else {
            // No filter entry, restore from user prefs or use first available
            campaignId = userPrefService.getEntry(category, USER_PREF_SELECTED_CAMPAIGN_ID, Long::class.java)
            if (campaignId == null) {
                // Use first available campaign as default
                campaignId = addressCampaignDao.selectAll(checkAccess = false).firstOrNull()?.id
            }
        }

        // Store in extended map for getAddressCampaignDO() to use
        if (campaignId != null) {
            source.extended["campaignId"] = campaignId
        }
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            AddressCampaignValueMultiSelectedPageRest::class.java,
            userAccess = userAccess,
        )
            .add(lc, "address.name", "address.firstName", pinnedAndLocked = UIAgGridColumnDef.Orientation.LEFT)
            .add(lc, "address.organization")
            .add(lc, "address.contactStatus", "address.email", "address.addressText", "address.addressStatus")
            .add(lc, "value", "comment")
            .withMultiRowSelection(request, magicFilter)
            .withGetRowClass(
                """if (params.node.data.favoriteAddress) { return 'ag-row-red'; }"""
            )
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: AddressCampaignValue, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
            .add(UILabel("TODO"))
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    /*
    override fun getListByIds(entityIds: Collection<Serializable>?): List<AddressCampaignValueDO> {
      // return baseDao.getListByIds(entityIds) ?: listOf()
      val addressList = baseDao.getListByIds(entityIds)
      return addressList?.map {
        val campaignValue = AddressCampaignValueDO()
        campaignValue.address = it
        campaignValue
      } ?: emptyList()
    }*/

    override fun postProcessResultSet(
        resultSet: ResultSet<AddressDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        val newResultSet = super.postProcessResultSet(resultSet, request, magicFilter)
        @Suppress("UNCHECKED_CAST")
        if (!processList(request, newResultSet.resultSet as List<AddressCampaignValue>)) {
            newResultSet.resultSet = emptyList()
        }
        return newResultSet
    }

    fun processList(request: HttpServletRequest, list: List<AddressCampaignValue>): Boolean {
        val addressCampaign = getAddressCampaign(request) ?: return false
        val addressCampaignValueMap = getAddressCampaignValueMap(addressCampaign.id)
        val personalAddressMap = personalAddressDao.personalAddressByAddressId
        list.forEach { entry ->
            fillValues(entry, addressCampaignValueMap, personalAddressMap)
        }
        return true
    }

    private fun getAddressCampaignValueMap(addressCampaignId: Long?): Map<Long, AddressCampaignValueDO> {
        val addressCampaignValueMap = mutableMapOf<Long, AddressCampaignValueDO>()
        addressCampaignValueDao.getAddressCampaignValuesByAddressId(addressCampaignValueMap, addressCampaignId)
        return addressCampaignValueMap
    }

    private fun fillValues(
        dest: AddressCampaignValue,
        addressCampaignValueMap: Map<Long, AddressCampaignValueDO>,
        personalAddressMap: Map<Long, PersonalAddressDO>,
        addressDO: AddressDO? = null,
    ) {
        if (addressDO != null) {
            dest.copyFrom(addressDO)
        }
        dest.favoriteAddress = personalAddressMap[dest.id]?.isFavorite
        addressCampaignValueMap[dest.address?.id]?.let { value ->
            dest.value = value.value
            dest.comment = value.comment
        }
    }

    override fun transformForDB(dto: AddressCampaignValue): AddressDO {
        val dbObj = AddressDO()
        dto.copyTo(dbObj)
        return dbObj
    }

    override fun transformFromDB(obj: AddressDO, editMode: Boolean): AddressCampaignValue {
        val dto = AddressCampaignValue()
        dto.copyFrom(obj)
        return dto
    }

    internal fun getAddressCampaignDO(request: HttpServletRequest): AddressCampaignDO? {
        // Try to get campaign ID from multiple sources in priority order:
        // 1. Current filter's extended map (current session)
        val currentFilter = getCurrentFilter()
        var addressCampaignId = currentFilter.extended["campaignId"] as? Long

        // 2. User preferences (saved across sessions)
        if (addressCampaignId == null) {
            addressCampaignId = userPrefService.getEntry(category, USER_PREF_SELECTED_CAMPAIGN_ID, Long::class.java)
        }

        // 3. MultiSelectionSupport (legacy/compatibility for old workflow)
        if (addressCampaignId == null) {
            val registeredData = MultiSelectionSupport.getRegisteredData(request, AddressCampaignValuePagesRest::class.java)
            if (registeredData is Long) {
                addressCampaignId = registeredData
            }
        }

        if (addressCampaignId != null) {
            return addressCampaignDao.find(addressCampaignId)
        }
        return null
    }

    internal fun getAddressCampaign(request: HttpServletRequest): AddressCampaign? {
        val addressCampaignDO = getAddressCampaignDO(request) ?: return null
        val campaign = AddressCampaign()
        campaign.copyFrom(addressCampaignDO)
        return campaign
    }
}
