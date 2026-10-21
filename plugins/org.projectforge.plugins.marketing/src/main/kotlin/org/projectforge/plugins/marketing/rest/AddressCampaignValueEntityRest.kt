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
import org.projectforge.ui.ValidationError
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
     * campaign is selected yet (the list is empty then), whether its values may still be changed, and the
     * comparison campaign, if one is selected.
     */
    class Statistics(
        val campaignId: Long?,
        val campaignTitle: String?,
        val campaignActive: Boolean? = null,
        val comparisonCampaignId: Long? = null,
        val comparisonCampaignTitle: String? = null,
    )

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
         * The id of the former React page's campaign filter, kept: the stored current filter and the filter
         * favorites of the users are kept under the category `addressCampaignValue`, so they keep selecting their
         * campaign.
         */
        private const val FILTER_CAMPAIGN_ID = "AddressCampaignValuePagesRest.campaignId"
        private const val EMPTY_VALUE = "__EMPTY__"

        /** A second campaign whose values are shown beside the selected one's (e.g. last year's), see [comparisonValuesOf]. */
        private const val FILTER_COMPARISON_CAMPAIGN_ID = "comparisonCampaignId"
        private const val FILTER_COMPARISON_VALUE = "comparisonValue"
        private const val FILTER_ONLY_DIFFERENCES = "onlyDifferences"
    }

    /**
     * The campaign selector (required for all queries), the values of the selected campaign, the comparison campaign
     * with the filters on its values, and the address filters.
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        // The active campaigns first: an inactive one is closed (marked by the client), and mostly of interest as
        // comparison.
        val campaigns = addressCampaignDao.select(deleted = false).sortedBy { !it.active }
        val campaignValues = campaigns.map {
            UISelectValue(it.id.toString(), it.title ?: "unknown", inactive = if (it.active) null else true)
        }
        elements.add(
            UIFilterListElement(
                id = FILTER_CAMPAIGN_ID,
                values = campaignValues,
                label = translate("plugins.marketing.addressCampaign"),
                multi = false,
                defaultFilter = true,
            )
        )
        val currentFilter = getCurrentFilter()
        val campaignId = campaignIdOf(currentFilter)
        elements.add(
            UIFilterListElement(
                id = FILTER_COMPARISON_CAMPAIGN_ID,
                values = campaignValues.filter { it.id != campaignId?.toString() },
                label = translate("plugins.marketing.addressCampaignValue.comparisonCampaign"),
                multi = false,
                defaultFilter = true,
            )
        )
        comparisonCampaignIdOf(currentFilter)?.let { addressCampaignDao.find(it) }?.let { comparison ->
            comparison.valuesArray?.takeIf { it.isNotEmpty() }?.let { values ->
                elements.add(
                    UIFilterListElement(
                        id = FILTER_COMPARISON_VALUE,
                        values = listOf(UISelectValue(EMPTY_VALUE, translate("filter.emptyValue"))) +
                                values.map { UISelectValue(it, it) },
                        label = translate("plugins.marketing.addressCampaignValue.comparisonValue"),
                        multi = true,
                        defaultFilter = true,
                    )
                )
            }
            elements.add(
                UIFilterElement(
                    FILTER_ONLY_DIFFERENCES,
                    UIFilterElement.FilterType.BOOLEAN,
                    translate("plugins.marketing.addressCampaignValue.onlyDifferences"),
                    defaultFilter = true,
                )
            )
        }
        // The values depend on the selected campaign, so the list meta is reloaded when it changes (reloadUI).
        getAddressCampaignDO()?.valuesArray?.let { values ->
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
        // The comparison campaign: its values are looked up once for both of its filters.
        source.entries.find { it.field == FILTER_COMPARISON_CAMPAIGN_ID }?.synthetic = true
        val comparisonValueEntry = source.entries.find { it.field == FILTER_COMPARISON_VALUE }
        comparisonValueEntry?.synthetic = true
        val onlyDifferencesEntry = source.entries.find { it.field == FILTER_ONLY_DIFFERENCES }
        onlyDifferencesEntry?.synthetic = true
        comparisonCampaignIdOf(source)?.let { comparisonId ->
            val comparisonValues by lazy { comparisonValuesOf(comparisonId) }
            comparisonValueEntry?.value?.values?.takeIf { it.isNotEmpty() }?.let { values ->
                val selectedValues = values.toMutableList()
                val includeEmpty = selectedValues.remove(EMPTY_VALUE)
                filters.add(ComparisonValueResultFilter(comparisonValues, selectedValues, includeEmpty))
            }
            if (onlyDifferencesEntry?.isTrueValue == true) {
                filters.add(ComparisonDifferenceResultFilter(comparisonValues))
            }
        }
        return filters.ifEmpty { null }
    }

    /**
     * The selected campaign is the one of the campaign filter, stored in the extended map for the DAO. Without one
     * the list is empty ("no campaign selected"), as the filter shows: unlike the React page, there is no fallback
     * to the remembered campaign, which would list a campaign the filter doesn't name.
     */
    override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
        super.postProcessMagicFilter(target, source)
        // The stored filter is still the previous one here (the list saves the new one afterwards).
        val previousFilter = getCurrentFilter()
        val campaignId = campaignIdOf(source)
        val comparisonCampaignId = comparisonCampaignIdOf(source)
        if (campaignIdOf(previousFilter) != campaignId || comparisonCampaignIdOf(previousFilter) != comparisonCampaignId) {
            // The value filters offer the values of the selected campaigns (or none without one), and the comparison
            // campaign is chosen among the others.
            source.extended["reloadUI"] = true
        }
        if (campaignId != null) {
            source.extended["campaignId"] = campaignId
        }
        comparisonCampaignId?.let { source.extended["comparisonCampaignId"] = it }
    }

    private fun campaignIdOf(filter: MagicFilter): Long? = selectedIdOf(filter, FILTER_CAMPAIGN_ID)

    /** The comparison campaign of the filter, unless it is the selected campaign itself. */
    private fun comparisonCampaignIdOf(filter: MagicFilter): Long? =
        selectedIdOf(filter, FILTER_COMPARISON_CAMPAIGN_ID)?.takeIf { it != campaignIdOf(filter) }

    private fun selectedIdOf(filter: MagicFilter, field: String): Long? {
        return filter.entries.find { it.field == field }?.value?.values?.firstOrNull()?.toLongOrNull()
    }

    /** The (not deleted) values of a campaign by address id. */
    private fun comparisonValuesOf(campaignId: Long): Map<Long, AddressCampaignValueDO> {
        return baseDao.getAddressCampaignValuesByAddressId(mutableMapOf(), campaignId)
    }

    /** The comparison campaign of the user's current filter, if any (see [AddressCampaignValueMultiSelectedPageRest]). */
    internal fun getComparisonCampaignDO(): AddressCampaignDO? {
        return comparisonCampaignIdOf(getCurrentFilter())?.let { addressCampaignDao.find(it) }
    }

    /** The values of the comparison campaign of the user's current filter by address id, empty without one. */
    internal fun getComparisonValues(): Map<Long, AddressCampaignValueDO> {
        return comparisonCampaignIdOf(getCurrentFilter())?.let { comparisonValuesOf(it) } ?: emptyMap()
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
        val comparison = (magicFilter.extended["comparisonCampaignId"] as? Long)?.let { addressCampaignDao.find(it) }
        resultSet.statistics = Statistics(
            campaign?.id,
            campaign?.title,
            campaign?.active,
            comparison?.id,
            comparison?.title,
        )
        val newResultSet = super.postProcessResultSet(resultSet, request, magicFilter)
        val personalAddressMap = personalAddressDao.personalAddressByAddressId
        val comparisonValues = comparison?.id?.let { comparisonValuesOf(it) } ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        (newResultSet.resultSet as List<AddressCampaignValue>).forEach { entry ->
            entry.isFavoriteCard = personalAddressMap[entry.addressId]?.isFavorite
            entry.addressId?.let { comparisonValues[it] }?.let { entry.setComparison(it) }
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
        dto.id?.let { id ->
            val dbObj = baseDao.find(id) ?: AddressCampaignValueDO()
            dto.copyTo(dbObj)
            // Editing a deleted campaign value restores it (it is shown as "not set").
            if (dbObj.deleted && (dbObj.value != null || dbObj.comment != null)) {
                dbObj.deleted = false
            }
            return dbObj
        }
        // A value of the address may have been created meanwhile (by another user, from a stale list): it is updated
        // then, as the mass update does, instead of inserted a second time, which the unique constraint on address
        // and campaign would refuse.
        val addressId = dto.addressId
        val campaignId = dto.addressCampaign?.id
        if (addressId != null && campaignId != null) {
            baseDao.get(addressId, campaignId)?.let { existing ->
                val existingId = existing.id
                dto.copyTo(existing) // Copies the id (null) as well.
                existing.id = existingId
                existing.deleted = false
                return existing
            }
        }
        // The address and the campaign (required foreign keys) of a new record.
        val dbObj = AddressCampaignValueDO()
        dto.copyTo(dbObj)
        addressId?.let { dbObj.address = AddressDO().also { it.id = addressId } }
        campaignId?.let { dbObj.addressCampaign = AddressCampaignDO().also { it.id = campaignId } }
        return dbObj
    }

    /**
     * The form (an edited value, or a new one) shows the value the address has in the comparison campaign of the
     * list, to be adopted (see the edit intro of projectforge-next). A list row gets it in [postProcessResultSet],
     * looked up once for all rows.
     */
    override fun transformFromDB(obj: AddressCampaignValueDO, editMode: Boolean): AddressCampaignValue {
        val dto = AddressCampaignValue()
        dto.copyFrom(obj)
        if (obj.id == null && obj.addressCampaign?.active == false) {
            // A new value of an inactive campaign can't be saved (AddressCampaignValueRight); a stored one gets
            // its flags from getById.
            dto.writeAccess = false
            dto.deleteAccess = false
        }
        if (editMode || obj.id == null) {
            getComparisonCampaignDO()?.takeIf { it.id != obj.addressCampaign?.id }?.let { comparison ->
                dto.comparisonCampaignTitle = comparison.title
                baseDao.get(obj.address?.id, comparison.id)?.takeIf { !it.deleted }?.let { dto.setComparison(it) }
            }
        }
        return dto
    }

    /**
     * A new value belongs to an address and a campaign (see [newBaseDO]): without them, there is nothing to save.
     * The value of an inactive campaign can't be changed: refused by [AddressCampaignValueRight] anyway, said here
     * with the reason instead of the general missing right.
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: AddressCampaignValue) {
        if (dto.id == null && (dto.addressId == null || dto.addressCampaign?.id == null)) {
            validationErrors.add(ValidationError.create("plugins.marketing.addressCampaignValue.error.addressOrCampaignNotGiven"))
            return
        }
        // The stored campaign of a stored value, not the posted one.
        val campaignId = dto.id?.takeIf { it > 0 }?.let { baseDao.find(it)?.addressCampaign?.id }
            ?: dto.addressCampaign?.id
        if (campaignId?.let { addressCampaignDao.find(it) }?.active == false) {
            validationErrors.add(ValidationError.create("plugins.marketing.addressCampaign.error.inactive"))
        }
    }

    /** A value is only created from the row of its address (synthetic id), not from a blank form. */
    override fun listInsertAccess(): Boolean = false

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

    /** The selected campaign: the one of the user's current filter, if any. */
    internal fun getAddressCampaignDO(): AddressCampaignDO? {
        return campaignIdOf(getCurrentFilter())?.let { addressCampaignDao.find(it) }
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
