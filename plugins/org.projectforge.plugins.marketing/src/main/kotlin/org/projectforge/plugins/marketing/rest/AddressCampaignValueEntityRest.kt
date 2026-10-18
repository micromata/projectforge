/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.address.DoubletsResultFilter
import org.projectforge.business.address.FavoritesResultFilter
import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.common.FilenameUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUserId
import org.projectforge.framework.time.DateHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.plugins.marketing.AddressCampaignDO
import org.projectforge.plugins.marketing.AddressCampaignDao
import org.projectforge.plugins.marketing.AddressCampaignValueDO
import org.projectforge.plugins.marketing.AddressCampaignValueDao
import org.projectforge.plugins.marketing.AddressCampaignValueExport
import org.projectforge.plugins.marketing.AddressCampaignValueMyEntriesResultFilter
import org.projectforge.plugins.marketing.InvalidAddressesResultFilter
import org.projectforge.plugins.marketing.dto.AddressCampaignValue
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.ResultSet
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * The layout-free REST endpoint of the address campaign values, serving the hand-built projectforge-next page
 * (see components/features/address-campaign-value). The list shows one row per address of the selected campaign:
 * an address without a value of its own yet comes as a transient value with the negative address id as synthetic
 * id (see [AddressCampaignValueDao.selectAddressesForCampaign]), opened as a new entry via `newEntry?campaignId=&addressId=`.
 *
 * The successor of [AddressCampaignValuePagesRest], which is still served under `addressCampaignValueLegacy` for the
 * running campaign and shares the selected campaign with this page. Everything but the layout is taken over from
 * there; the copy goes away with the legacy page.
 */
@RestController
@RequestMapping("${Rest.URL}/addressCampaignValue")
class AddressCampaignValueEntityRest :
    AbstractDTOEntityRest<AddressCampaignValueDO, AddressCampaignValue, AddressCampaignValueDao>(
        baseDaoClazz = AddressCampaignValueDao::class.java,
        i18nKeyPrefix = "plugins.marketing.addressCampaignValue.title"
    ) {
    /**
     * What the list shows above the table (`ResultSet.statistics`): the selected campaign, or none if no
     * campaign is selected yet (the list is empty then).
     */
    class Statistics(val campaignId: Long?, val campaignTitle: String?)

    @Autowired
    private lateinit var addressCampaignDao: AddressCampaignDao

    @Autowired
    private lateinit var addressCampaignValueExport: AddressCampaignValueExport

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    companion object {
        /**
         * The user pref area of the selected campaign, shared with the React page ([AddressCampaignValuePagesRest]),
         * so both show the same campaign. The former category of the React page, so its users keep their selection.
         */
        internal const val USER_PREF_AREA = "addressCampaignValue"
        internal const val USER_PREF_SELECTED_CAMPAIGN_ID = "AddressCampaignValuePagesRest.selectedCampaignId"
        private const val FILTER_CAMPAIGN_ID = "campaignId"
        private const val EMPTY_VALUE = "__EMPTY__"
    }

    /**
     * The campaign selector (required for all queries), the values of the selected campaign and the address filters.
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        val campaigns = addressCampaignDao.select(deleted = false)
        elements.add(
            UIFilterListElement(
                id = FILTER_CAMPAIGN_ID,
                values = campaigns.map { UISelectValue(it.id.toString(), it.title ?: "unknown") },
                label = translate("plugins.marketing.addressCampaign"),
                multi = false,
                defaultFilter = true,
            )
        )
        // The values depend on the selected campaign, so the list meta is reloaded when it changes (reloadUI).
        getSelectedCampaignId()?.let { addressCampaignDao.find(it) }?.valuesArray?.let { values ->
            if (values.isNotEmpty()) {
                val valuesList = mutableListOf(UISelectValue(EMPTY_VALUE, translate("filter.emptyValue")))
                values.forEach { valuesList.add(UISelectValue(it, it)) }
                elements.add(
                    UIFilterListElement(
                        id = "value",
                        values = valuesList,
                        label = translate("value"),
                        multi = true,
                        defaultFilter = true,
                    )
                )
            }
        }
        // Boolean filters (synthetic, CustomResultFilter).
        elements.add(
            UIFilterElement(
                "isFavorite",
                UIFilterElement.FilterType.BOOLEAN,
                translate("address.filter.myFavorites"),
                defaultFilter = false,
            )
        )
        elements.add(
            UIFilterElement(
                "doublets",
                UIFilterElement.FilterType.BOOLEAN,
                translate("address.filter.doublets"),
                defaultFilter = false,
            )
        )
        elements.add(
            UIFilterElement(
                "myEntries",
                UIFilterElement.FilterType.BOOLEAN,
                translate("address.filter.myEntries"),
                tooltip = translate("address.filter.myEntries.tooltip"),
                defaultFilter = true,
            )
        )
        elements.add(
            UIFilterElement(
                "invalidAddresses",
                UIFilterElement.FilterType.BOOLEAN,
                translate("address.filter.invalidAddresses"),
                defaultFilter = false,
            )
        )
        // Enum and text filters (predicates on the address).
        elements.add(
            UIFilterListElement(
                id = "contactStatus",
                values = ContactStatus.entries.map { UISelectValue(it.name, translate(it.i18nKey)) },
                label = translate("address.contactStatus"),
                multi = true,
                defaultFilter = true,
            )
        )
        elements.add(
            UIFilterListElement(
                id = "addressStatus",
                values = AddressStatus.entries.map { UISelectValue(it.name, translate(it.i18nKey)) },
                label = translate("address.addressStatus"),
                multi = true,
                defaultFilter = true,
            )
        )
        elements.add(
            UIFilterElement(
                "organization",
                UIFilterElement.FilterType.STRING,
                translate("organization"),
                defaultFilter = true,
            )
        )
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<AddressCampaignValueDO>>? {
        val filters = mutableListOf<CustomResultFilter<AddressCampaignValueDO>>()
        // Handled in postProcessMagicFilter via the extended map.
        source.entries.find { it.field == FILTER_CAMPAIGN_ID }?.synthetic = true

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
        val myEntriesEntry = source.entries.find { it.field == "myEntries" }
        myEntriesEntry?.synthetic = true
        if (myEntriesEntry?.isTrueValue == true) {
            filters.add(AddressCampaignValueMyEntriesResultFilter(persistenceService, requiredLoggedInUserId))
        }
        val invalidAddressesEntry = source.entries.find { it.field == "invalidAddresses" }
        invalidAddressesEntry?.synthetic = true
        if (invalidAddressesEntry?.isTrueValue == true) {
            filters.add(InvalidAddressesResultFilter())
        }

        val contactStatusEntry = source.entries.find { it.field == "contactStatus" }
        contactStatusEntry?.synthetic = true
        contactStatusEntry?.value?.values?.mapNotNull { value -> ContactStatus.entries.find { it.name == value } }
            ?.let { if (it.isNotEmpty()) target.add(QueryFilter.isIn("contactStatus", it)) }

        val addressStatusEntry = source.entries.find { it.field == "addressStatus" }
        addressStatusEntry?.synthetic = true
        addressStatusEntry?.value?.values?.mapNotNull { value -> AddressStatus.entries.find { it.name == value } }
            ?.let { if (it.isNotEmpty()) target.add(QueryFilter.isIn("addressStatus", it)) }

        val organizationEntry = source.entries.find { it.field == "organization" }
        organizationEntry?.synthetic = true
        organizationEntry?.value?.value?.let { orgValue ->
            if (orgValue.isNotBlank()) {
                target.add(QueryFilter.like("organization", orgValue, autoWildcardSearch = true))
            }
        }

        // The value is filtered after the merge of values and addresses.
        val valueEntry = source.entries.find { it.field == "value" }
        valueEntry?.synthetic = true
        valueEntry?.value?.values?.let { values ->
            if (values.isNotEmpty()) {
                val selectedValues = values.toMutableList()
                val includeEmpty = selectedValues.remove(EMPTY_VALUE)
                filters.add(ValueResultFilter(selectedValues, includeEmpty))
            }
        }
        return filters.ifEmpty { null }
    }

    /**
     * The selected campaign: the one of the filter (remembered as user pref, shared with the React page), or the
     * remembered one on an initial load. Stored in the extended map for the DAO.
     */
    override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
        super.postProcessMagicFilter(target, source)
        val previousCampaignId = getSelectedCampaignId()
        val campaignId = source.entries.find { it.field == FILTER_CAMPAIGN_ID }
            ?.value?.values?.firstOrNull()?.toLongOrNull()
        if (campaignId != null) {
            if (previousCampaignId != campaignId) {
                // The value filter offers the values of the selected campaign.
                source.extended["reloadUI"] = true
            }
            userPrefService.putEntry(USER_PREF_AREA, USER_PREF_SELECTED_CAMPAIGN_ID, campaignId)
            source.extended["campaignId"] = campaignId
        } else if (previousCampaignId != null) {
            source.extended["campaignId"] = previousCampaignId
        }
    }

    /**
     * Every column is sorted in memory: the query runs on the addresses (see
     * [AddressCampaignValueDao.selectAddressesForCampaign]), which have neither the value nor the comment, and
     * `created`/`lastUpdate` are the ones of the value, not of the address.
     */
    override val computedSortProperties: Map<String, (AddressCampaignValueDO) -> Comparable<*>?> = mapOf(
        "name" to { it.address?.name },
        "firstName" to { it.address?.firstName },
        "organization" to { it.address?.organization },
        "value" to { if (it.deleted) null else it.value },
        "comment" to { if (it.deleted) null else it.comment },
        "created" to { if ((it.id ?: 0) > 0) it.created else null },
        "lastUpdate" to { if ((it.id ?: 0) > 0) it.lastUpdate else null },
        "lastUpdateOfAddress" to { it.address?.lastUpdate },
        "contactStatus" to { it.address?.contactStatus?.let { status -> translate(status.i18nKey) } },
        "addressStatus" to { it.address?.addressStatus?.let { status -> translate(status.i18nKey) } },
    )

    /** Whether the Excel export is offered (the former `excelExportSupported` of the React list). */
    override fun addVariablesForListPage(): Map<String, Any> {
        return mapOf("excelExportSupported" to isExcelExportAllowed())
    }

    override fun postProcessResultSet(
        resultSet: ResultSet<AddressCampaignValueDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        val campaign = (magicFilter.extended["campaignId"] as? Long)?.let { addressCampaignDao.find(it) }
        resultSet.statistics = Statistics(campaign?.id, campaign?.title)
        val newResultSet = super.postProcessResultSet(resultSet, request, magicFilter)
        val personalAddressMap = personalAddressDao.personalAddressByAddressId
        @Suppress("UNCHECKED_CAST")
        (newResultSet.resultSet as List<AddressCampaignValue>).forEach { entry ->
            entry.isFavoriteCard = personalAddressMap[entry.addressId]?.isFavorite
        }
        if (magicFilter.extended["reloadUI"] == true) {
            newResultSet.reloadUI = true
        }
        return newResultSet
    }

    /**
     * A new value is the one of an address without a value in a campaign yet (a row with a synthetic id): the
     * address and the campaign come as request parameters `addressId` and `campaignId`.
     */
    override fun newBaseDO(request: HttpServletRequest?): AddressCampaignValueDO {
        val obj = super.newBaseDO(request)
        request?.getParameter("campaignId")?.toLongOrNull()?.let { campaignId ->
            obj.addressCampaign = addressCampaignDao.find(campaignId)
        }
        request?.getParameter("addressId")?.toLongOrNull()?.let { addressId ->
            obj.address = addressDao.find(addressId)
        }
        return obj
    }

    override fun transformForDB(dto: AddressCampaignValue): AddressCampaignValueDO {
        // For updates, load the existing entity to preserve its relationships.
        val dbObj = dto.id?.let { baseDao.find(it) } ?: AddressCampaignValueDO()
        dto.copyTo(dbObj)
        // Editing a deleted campaign value restores it (it is shown as "not set").
        if (dto.id != null && dbObj.deleted && (dbObj.value != null || dbObj.comment != null)) {
            dbObj.deleted = false
        }
        // The address and the campaign (required foreign keys), only for new records.
        if (dto.id == null) {
            dto.addressId?.let { addressId ->
                dbObj.address = AddressDO().also { it.id = addressId }
            }
            dto.addressCampaign?.id?.let { campaignId ->
                dbObj.addressCampaign = AddressCampaignDO().also { it.id = campaignId }
            }
        }
        return dbObj
    }

    override fun transformFromDB(obj: AddressCampaignValueDO, editMode: Boolean): AddressCampaignValue {
        val dto = AddressCampaignValue()
        dto.copyFrom(obj)
        return dto
    }

    /**
     * Both real campaign value ids (positive) and synthetic ids (negative address ids of addresses without a
     * value), e.g. of a selection for the mass update.
     */
    override fun getListByIds(entityIds: Collection<Serializable>?): List<AddressCampaignValueDO> {
        if (entityIds.isNullOrEmpty()) {
            return emptyList()
        }
        val realIds = mutableListOf<Long>()
        val addressIds = mutableListOf<Long>()
        entityIds.forEach { id ->
            val longId = (id as? Long) ?: (id as? Number)?.toLong() ?: (id as? String)?.toLongOrNull()
            if (longId != null) {
                if (longId > 0) realIds.add(longId) else addressIds.add(-longId)
            }
        }
        val result = mutableListOf<AddressCampaignValueDO>()
        if (realIds.isNotEmpty()) {
            result.addAll(baseDao.select(realIds) ?: emptyList())
        }
        if (addressIds.isNotEmpty()) {
            val campaign = getAddressCampaignDO()
            addressIds.forEach { addressId ->
                addressDao.find(addressId)?.let { address ->
                    result.add(AddressCampaignValueDO().also {
                        it.id = -addressId // Synthetic id.
                        it.address = address
                        it.addressCampaign = campaign
                    })
                }
            }
        }
        return result
    }

    /** The selected campaign: the one of the current filter, otherwise the remembered one. */
    internal fun getAddressCampaignDO(): AddressCampaignDO? {
        val campaignId = getCurrentFilter().extended["campaignId"] as? Long ?: getSelectedCampaignId()
        return campaignId?.let { addressCampaignDao.find(it) }
    }

    private fun getSelectedCampaignId(): Long? {
        return userPrefService.getEntry(USER_PREF_AREA, USER_PREF_SELECTED_CAMPAIGN_ID, Long::class.java)
    }

    private fun isExcelExportAllowed(): Boolean {
        return accessChecker.isLoggedInUserMemberOfGroup(
            ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.MARKETING_GROUP,
        )
    }

    /**
     * The filtered list of the selected campaign as Excel file, for the finance and marketing staff. No campaign
     * selected or an empty result answers 404 rather than a file.
     */
    @AccessChecked("FINANCE_GROUP, MARKETING_GROUP; DAO: select access (list result filtered by baseDao)")
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        if (!isExcelExportAllowed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Any>()
        }
        val campaignValueList = getResultList(filter)
        val campaign = (filter.extended["campaignId"] as? Long)?.let { addressCampaignDao.find(it) } ?: run {
            log.warn { "No campaign selected for Excel export." }
            return ResponseEntity.notFound().build<Any>()
        }
        log.info { "Exporting address campaign values as Excel file for campaign '${campaign.title}'." }
        val addressIds = campaignValueList.mapNotNull { it.address?.id }
        if (addressIds.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val addressList = addressDao.select(addressIds) ?: emptyList()
        val campaignValueMap = campaignValueList.filter { it.address?.id != null }.associateBy { it.address!!.id }
        val xls = addressCampaignValueExport.export(
            addressList,
            personalAddressDao.personalAddressByAddressId,
            campaignValueMap,
            campaign.title ?: "Campaign",
        )
        if (xls == null || xls.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val campaignName = FilenameUtils.createSafeFilename(campaign.title ?: "Campaign", maxlength = 200)
        val filename =
            "ProjectForge-AddressCampaignExport_${campaignName}_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
        return RestUtils.downloadFile(filename, xls)
    }
}
