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
import org.projectforge.business.address.*
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.utils.MarkdownBuilder
import org.projectforge.plugins.marketing.AddressCampaignDO
import org.projectforge.plugins.marketing.AddressCampaignDao
import org.projectforge.plugins.marketing.AddressCampaignValueDO
import org.projectforge.plugins.marketing.AddressCampaignValueDao
import org.projectforge.plugins.marketing.dto.AddressCampaign
import org.projectforge.plugins.marketing.dto.AddressCampaignValue
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.multiselect.MultiSelectionSupport
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/addressCampaignValue")
class AddressCampaignValuePagesRest :
    AbstractDTOPagesRest<AddressCampaignValueDO, AddressCampaignValue, AddressCampaignValueDao>(
        baseDaoClazz = AddressCampaignValueDao::class.java,
        i18nKeyPrefix = "plugins.marketing.addressCampaignValue.title"
    ) {
    @Autowired
    private lateinit var addressCampaignDao: AddressCampaignDao

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    companion object {
        private const val USER_PREF_SELECTED_CAMPAIGN_ID = "AddressCampaignValuePagesRest.selectedCampaignId"
        private const val FILTER_CAMPAIGN_ID = "AddressCampaignValuePagesRest.campaignId"
    }

    /**
     * Add campaign selector and filter elements
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        // Campaign selector (required for all queries)
        val campaigns = addressCampaignDao.select(deleted = false)
        val campaignValues = campaigns.map { UISelectValue(it.id.toString(), it.title ?: "unknown") }

        val campaignFilter = UIFilterListElement(
            id = FILTER_CAMPAIGN_ID,
            values = campaignValues,
            label = translate("plugins.marketing.addressCampaign"),
            multi = false,
            defaultFilter = true
        )
        elements.add(campaignFilter)

        // Dynamic value filter based on selected campaign
        val currentCampaignId = userPrefService.getEntry(
            category,
            USER_PREF_SELECTED_CAMPAIGN_ID,
            Long::class.java
        )
        if (currentCampaignId != null) {
            val campaign = addressCampaignDao.find(currentCampaignId)
            val valuesList = mutableListOf<UISelectValue<String>>()

            // Add option for empty values
            valuesList.add(UISelectValue("__EMPTY__", translate("filter.emptyValue")))

            // Add values from campaign's valuesArray
            campaign?.valuesArray?.forEach { value ->
                valuesList.add(UISelectValue(value, value))
            }

            if (valuesList.size > 1) { // More than just __EMPTY__
                elements.add(
                    UIFilterListElement(
                        id = "value",
                        values = valuesList,
                        label = translate("value"),
                        multi = true,
                        defaultFilter = true
                    )
                )
            }
        }

        // Boolean filters (synthetic - use CustomResultFilter)
        elements.add(
            UIFilterElement(
                "isFavorite",
                UIFilterElement.FilterType.BOOLEAN,
                translate("address.filter.myFavorites"),
                defaultFilter = false
            )
        )
        elements.add(
            UIFilterElement(
                "doublets",
                UIFilterElement.FilterType.BOOLEAN,
                translate("address.filter.doublets"),
                defaultFilter = false
            )
        )

        // Enum filters (database field predicates)
        val contactStatusValues = ContactStatus.entries.map {
            UISelectValue(it.name, translate(it.i18nKey))
        }
        elements.add(
            UIFilterListElement(
                id = "contactStatus",
                values = contactStatusValues,
                label = translate("address.contactStatus"),
                multi = true,
                defaultFilter = true
            )
        )

        val addressStatusValues = AddressStatus.entries.map {
            UISelectValue(it.name, translate(it.i18nKey))
        }
        elements.add(
            UIFilterListElement(
                id = "addressStatus",
                values = addressStatusValues,
                label = translate("address.addressStatus"),
                multi = true,
                defaultFilter = true
            )
        )

        // Text filters (database field predicates)
        elements.add(
            UIFilterElement(
                "organization",
                UIFilterElement.FilterType.STRING,
                translate("organization"),
                defaultFilter = true
            )
        )
    }

    /**
     * Pre-process magic filter to set up synthetic filters and field predicates
     */
    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter
    ): List<CustomResultFilter<AddressCampaignValueDO>>? {
        val filters = mutableListOf<CustomResultFilter<AddressCampaignValueDO>>()

        // Process synthetic filters (delegated to address filters via adapter)
        val isFavoriteEntry = source.entries.find { it.field == "isFavorite" }
        isFavoriteEntry?.synthetic = true
        if (isFavoriteEntry?.isTrueValue == true) {
            filters.add(CampaignValueFilterAdapter(FavoritesResultFilter(personalAddressDao)))
        }

        val doubletsEntry = source.entries.find { it.field == "doublets" }
        doubletsEntry?.synthetic = true
        if (doubletsEntry?.isTrueValue == true) {
            filters.add(CampaignValueFilterAdapter(DoubletsResultFilter()))
        }

        // Process field-based filters (add as QueryFilter predicates for AddressDO)
        val contactStatusEntry = source.entries.find { it.field == "contactStatus" }
        if (contactStatusEntry != null && !contactStatusEntry.value.values.isNullOrEmpty()) {
            val statuses = contactStatusEntry.value.values?.mapNotNull { value ->
                try {
                    ContactStatus.valueOf(value)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            if (!statuses.isNullOrEmpty()) {
                target.add(QueryFilter.isIn("contactStatus", statuses))
            }
        }

        val addressStatusEntry = source.entries.find { it.field == "addressStatus" }
        if (addressStatusEntry != null && !addressStatusEntry.value.values.isNullOrEmpty()) {
            val statuses = addressStatusEntry.value.values?.mapNotNull { value ->
                try {
                    AddressStatus.valueOf(value)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            if (!statuses.isNullOrEmpty()) {
                target.add(QueryFilter.isIn("addressStatus", statuses))
            }
        }

        val organizationEntry = source.entries.find { it.field == "organization" }
        organizationEntry?.value?.value?.let { orgValue ->
            if (orgValue.isNotBlank()) {
                target.add(QueryFilter.like("organization", orgValue, autoWildcardSearch = true))
            }
        }

        // Value filter - applied after merge as CustomResultFilter
        val valueEntry = source.entries.find { it.field == "value" }
        valueEntry?.synthetic = true
        if (valueEntry != null && !valueEntry.value.values.isNullOrEmpty()) {
            val selectedValues = valueEntry.value.values?.toMutableList() ?: mutableListOf()

            // Check if __EMPTY__ is selected
            val includeEmpty = selectedValues.remove("__EMPTY__")

            // Add ValueResultFilter for post-merge filtering
            if (selectedValues.isNotEmpty() || includeEmpty) {
                filters.add(ValueResultFilter(selectedValues, includeEmpty))
            }
        }

        return filters.ifEmpty { null }
    }

    /**
     * Process magic filter to handle campaign selection
     */
    override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
        super.postProcessMagicFilter(target, source)

        val previousCampaignId = userPrefService.getEntry(category, USER_PREF_SELECTED_CAMPAIGN_ID, Long::class.java)
        val campaignEntry = source.entries.find { it.field == FILTER_CAMPAIGN_ID }

        val campaignId = campaignEntry?.value?.values?.firstOrNull()?.toLongOrNull()
        if (campaignId != null) {
            // Check if campaign changed
            if (previousCampaignId != campaignId) {
                source.extended["reloadUI"] = true
            }
            // Save to user preferences
            userPrefService.putEntry(category, USER_PREF_SELECTED_CAMPAIGN_ID, campaignId)
            // Store for DAO
            source.extended["campaignId"] = campaignId
        } else if (previousCampaignId != null) {
            // Initial load - restore from user prefs or use first available
            source.extended["campaignId"] = previousCampaignId
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
        // Add alert box for selected campaign
        val campaign = getAddressCampaignDO(request)
        if (campaign != null) {
            layout.add(buildCampaignTitle(campaign.title))
        } else {
            layout.add(
                UIAlert(
                    message = "plugins.marketing.addressCampaign.noCampaignSelected",
                    color = UIColor.DANGER,
                    markdown = false
                )
            )
        }

        // Build custom row click URL with campaignId and addressId parameters
        // AG Grid will replace 'addressId' placeholder with actual row's addressId field value
        val campaignId = campaign?.id
        val rowClickUrl = if (campaignId != null) {
            "${
                PagesResolver.getEditPageUrl(
                    this::class.java,
                    absolute = true
                )
            }/id?campaignId=$campaignId&addressId=addressId"
        } else {
            null
        }

        agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            AddressCampaignValueMultiSelectedPageRest::class.java,
            userAccess = userAccess,
            rowClickUrl = rowClickUrl,
        )
            .add(lc, "name", headerName = "contact.name", pinnedAndLocked = UIAgGridColumnDef.Orientation.LEFT)
            .add(
                lc,
                "firstName",
                headerName = "contact.firstname",
                pinnedAndLocked = UIAgGridColumnDef.Orientation.LEFT
            )
            .add(lc, "value", headerName = "value")
            .add(lc, "organization", headerName = "organization")
            .add(lc, "formattedAddress", headerName = "address", wrapText = true, cellRenderer = "multilineCell")
            .add(
                UIAgGridColumnDef(
                    "lastUpdate",
                    headerName = "modified",
                    valueFormatter = "data.timeAgo",
                    sortable = true,
                    width = UIAgGridColumnDef.DATE_WIDTH
                )
            )
            .add("contactStatusAsString", headerName = "address.contactStatus", width = 110)
            .add("addressStatusAsString", headerName = "address.addressStatus", width = 110)
            .add(lc, "comment")
            .withMultiRowSelection(request, magicFilter)
            .withGetRowClass(
                """if (params.node.data.isFavoriteCard) { return 'ag-row-blue'; }"""
            )
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: AddressCampaignValue, userAccess: UILayout.UserAccess): UILayout {
        // Build campaign values for UISelect
        val campaignValues = mutableListOf<UISelectValue<String>>()
        campaignValues.add(UISelectValue("", "--")) // Empty option

        // Add campaign values from the DTO (already parsed)
        dto.addressCampaign?.values?.forEach { value ->
            campaignValues.add(UISelectValue(value, value))
        }

        // Build business card style address display
        val addressCard = MarkdownBuilder()

        // Name (bold)
        val fullName = buildString {
            if (!dto.firstName.isNullOrBlank()) {
                append(dto.firstName)
                append(" ")
            }
            if (!dto.name.isNullOrBlank()) {
                append(dto.name)
            }
        }.trim()

        if (fullName.isNotBlank()) {
            addressCard.appendLine(fullName, bold = true)
        }

        // Organization
        if (!dto.organization.isNullOrBlank()) {
            addressCard.appendLine(dto.organization)
        }

        // Mailing address (formatted with newlines)
        addressCard.appendMultilineText(dto.formattedAddress)

        // Email
        if (!dto.email.isNullOrBlank()) {
            addressCard.appendLine(dto.email)
        }

        val layout = super.createEditLayout(dto, userAccess)
            .add(buildCampaignTitle(dto.addressCampaign?.title))
            .add(
                UIAlert(
                    message = addressCard.toString(),
                    color = UIColor.LIGHT,
                    markdown = true
                )
            )
            .add(
                UIFieldset(UILength(12), title = translate("plugins.marketing.addressCampaignValue"))
                    .add(
                        UIRow()
                            .add(
                                UICol(12)
                                    .add(
                                        UISelect(
                                            "value",
                                            lc,
                                            values = campaignValues,
                                            label = translate("value")
                                        )
                                    )
                            )
                    )
                    .add(
                        UIRow()
                            .add(
                                UICol(12)
                                    .add(UITextArea("comment", lc))
                            )
                    )
            )

        return LayoutUtils.processEditPage(layout, dto, this)
    }

    /**
     * Override to handle negative IDs from transient campaign values.
     * Negative IDs are synthetic IDs used for multi-selection tracking.
     * When a negative ID is passed in the URL, treat it as a new object.
     */
    override fun getItemAndLayout(
        request: HttpServletRequest,
        @RequestParam("id") id: String?,
        @RequestParam("returnToCaller") returnToCaller: String?
    ): ResponseEntity<FormLayoutData> {
        // If id is negative (synthetic ID from transient campaign value),
        // treat it as a new object by passing null
        val effectiveId = id?.toLongOrNull()?.let { longId ->
            if (longId < 0) null else id
        } ?: id

        return super.getItemAndLayout(request, effectiveId, returnToCaller)
    }

    /**
     * Override to handle campaignId and addressId parameters for new campaign values.
     * When creating a new campaign value (transient object with no ID), we need to know
     * which campaign to associate it with and which address it belongs to.
     */
    override fun onBeforeGetItemAndLayout(
        request: HttpServletRequest,
        dto: AddressCampaignValue,
        userAccess: UILayout.UserAccess
    ) {
        super.onBeforeGetItemAndLayout(request, dto, userAccess)

        // Extract parameters from request
        val campaignIdParam = request.getParameter("campaignId")
        val campaignId = campaignIdParam?.toLongOrNull()
        val addressIdParam = request.getParameter("addressId")
        val addressId = addressIdParam?.toLongOrNull()

        // If this is a new object (no id), populate campaign and address data
        if (dto.id == null) {
            // Set the campaign
            if (campaignId != null) {
                val campaign = addressCampaignDao.find(campaignId)
                if (campaign != null) {
                    val campaignDto = AddressCampaign()
                    campaignDto.copyFrom(campaign)
                    dto.addressCampaign = campaignDto
                }
            }

            // Set the address data
            if (addressId != null) {
                val addressDO = addressDao.find(addressId)
                if (addressDO != null) {
                    dto.populateFromAddress(addressDO)
                }
            }
        }
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
        resultSet: ResultSet<AddressCampaignValueDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        val newResultSet = super.postProcessResultSet(resultSet, request, magicFilter)
        @Suppress("UNCHECKED_CAST")
        processList(request, newResultSet.resultSet as List<AddressCampaignValue>)

        // Check if UI reload is needed (campaign changed)
        if (magicFilter.extended["reloadUI"] == true) {
            // Signal to frontend to reload UI
            newResultSet.reloadUI = true
        }

        return newResultSet
    }

    fun processList(request: HttpServletRequest, list: List<AddressCampaignValue>) {
        val personalAddressMap = personalAddressDao.personalAddressByAddressId
        list.forEach { entry ->
            // Set the favoriteAddress flag from personal address data
            entry.isFavoriteCard = personalAddressMap[entry.addressId]?.isFavorite
        }
    }

    override fun transformForDB(dto: AddressCampaignValue): AddressCampaignValueDO {
        // For updates, load existing entity to preserve relationships
        val dbObj = if (dto.id != null) {
            baseDao.find(dto.id) ?: AddressCampaignValueDO()
        } else {
            AddressCampaignValueDO()
        }

        // Copy editable fields
        dto.copyTo(dbObj)

        // Set the address relationship (required foreign key) - only for new records
        if (dto.id == null && dto.addressId != null) {
            val address = AddressDO()
            address.id = dto.addressId
            dbObj.address = address
        }

        // Set the campaign relationship (required foreign key) - only for new records
        if (dto.id == null && dto.addressCampaign?.id != null) {
            val campaign = AddressCampaignDO()
            campaign.id = dto.addressCampaign!!.id
            dbObj.addressCampaign = campaign
        }

        return dbObj
    }

    override fun transformFromDB(obj: AddressCampaignValueDO, editMode: Boolean): AddressCampaignValue {
        val dto = AddressCampaignValue()
        dto.copyFrom(obj)
        return dto
    }

    /**
     * Override to handle both real campaign value IDs (positive) and synthetic IDs (negative).
     * Negative IDs represent addressIds for transient campaign values (addresses without campaign values).
     */
    override fun getListByIds(entityIds: Collection<java.io.Serializable>?): List<AddressCampaignValueDO> {
        if (entityIds.isNullOrEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<AddressCampaignValueDO>()
        val realIds = mutableListOf<Long>()
        val addressIds = mutableListOf<Long>()

        // Separate real IDs from synthetic IDs
        entityIds.forEach { id ->
            val longId = (id as? Long) ?: (id as? String)?.toLongOrNull()
            if (longId != null) {
                if (longId > 0) {
                    realIds.add(longId)
                } else {
                    // Negative ID = synthetic ID, convert back to addressId
                    addressIds.add(-longId)
                }
            }
        }

        // Load real campaign values from database
        if (realIds.isNotEmpty()) {
            result.addAll(baseDao.select(realIds) ?: emptyList())
        }

        // Create transient campaign values for addresses without campaign values
        if (addressIds.isNotEmpty()) {
            // Get the current campaign
            val campaignId = getCurrentFilter().extended["campaignId"] as? Long
            val campaign = if (campaignId != null) {
                addressCampaignDao.find(campaignId)
            } else {
                null
            }

            // Load addresses and create transient campaign values
            addressIds.forEach { addressId ->
                val address = addressDao.find(addressId)
                if (address != null) {
                    val transientValue = AddressCampaignValueDO()
                    transientValue.id = -addressId // Synthetic ID
                    transientValue.address = address
                    transientValue.addressCampaign = campaign
                    transientValue.value = null
                    transientValue.comment = null
                    result.add(transientValue)
                }
            }
        }

        return result
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
            val registeredData =
                MultiSelectionSupport.getRegisteredData(request, AddressCampaignValuePagesRest::class.java)
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

    private fun buildCampaignTitle(campaignTitle: String?): UIAlert {
        val mb = MarkdownBuilder()
            .append(translate("plugins.marketing.addressCampaign"))
            .append(": ")
            .append(campaignTitle, MarkdownBuilder.Color.RED, bold = true)
        return UIAlert(
            message = mb.toString(),
            color = UIColor.LIGHT,
            markdown = true
        )
    }
}
