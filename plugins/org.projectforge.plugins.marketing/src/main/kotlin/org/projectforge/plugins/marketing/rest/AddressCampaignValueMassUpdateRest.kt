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

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.address.AddressDao
import org.projectforge.plugins.marketing.AddressCampaignValueDO
import org.projectforge.plugins.marketing.AddressCampaignValueDao
import org.projectforge.plugins.marketing.dto.AddressCampaignValue
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.AbstractMassUpdateRest
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateFieldDeclaration
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.i18n.translate
import org.projectforge.common.i18n.UserException
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable

/**
 * Mass update of the address campaign values after selection, for the hand-built projectforge-next page
 * ([AddressCampaignValueEntityRest]). The selection may contain synthetic ids (negative address ids) of addresses
 * without a value in the selected campaign yet: their values are created.
 */
@RestController
@RequestMapping("${Rest.URL}/addressCampaignValue${AbstractMassUpdateRest.URL_SUFFIX_SELECTED}")
class AddressCampaignValueMassUpdateRest : AbstractMassUpdateRest<AddressCampaignValue>() {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressCampaignValueDao: AddressCampaignValueDao

    @Autowired
    private lateinit var addressCampaignValueEntityRest: AddressCampaignValueEntityRest

    override val layoutContext: LayoutContext = LayoutContext(AddressCampaignValueDO::class.java)

    override fun getId(obj: AddressCampaignValue): Long {
        return obj.id ?: obj.addressId?.let { -it } ?: -1
    }

    @PostConstruct
    private fun postConstruct() {
        pagesRest = addressCampaignValueEntityRest
    }

    /**
     * The value as select of the values of the selected campaign (built at runtime, hence
     * [MassUpdateFieldDeclaration.values]), and the comment. With a comparison campaign selected in the list,
     * also the adoption of its values ([ADOPT_COMPARISON], rendered by the client itself).
     */
    override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
        val values = addressCampaignValueEntityRest.getAddressCampaignDO()?.valuesArray?.map { UISelectValue(it, it) }
        val declarations = mutableListOf(
            MassUpdateFieldDeclaration("value", showDeleteOption = true, values = values ?: emptyList()),
            MassUpdateFieldDeclaration("comment", showAppendOption = true),
        )
        if (addressCampaignValueEntityRest.getComparisonCampaignDO() != null) {
            declarations.add(MassUpdateFieldDeclaration(ADOPT_COMPARISON, custom = true))
        }
        return declarations
    }

    /** The selected campaign and the comparison campaign, shown above the selection (as above the list). */
    override fun getStatisticsData(selectedIds: Collection<Serializable>?): Any {
        val campaign = addressCampaignValueEntityRest.getAddressCampaignDO()
        val comparison = addressCampaignValueEntityRest.getComparisonCampaignDO()
        return AddressCampaignValueEntityRest.Statistics(
            campaign?.id,
            campaign?.title,
            campaign?.active,
            comparison?.id,
            comparison?.title,
        )
    }

    /** The adoption has no entity property, so no label of the registry. */
    override fun getFieldTranslation(field: String): String {
        if (field == ADOPT_COMPARISON) {
            return translate("plugins.marketing.addressCampaignValue.comparisonCampaign")
        }
        return super.getFieldTranslation(field)
    }

    override fun describePreviewChange(
        field: String,
        param: MassUpdateParameter,
        label: String,
        value: String?,
    ): String? {
        if (field != ADOPT_COMPARISON) {
            return null
        }
        val options = AdoptOptions.of(param) ?: return null
        val comparisonTitle = addressCampaignValueEntityRest.getComparisonCampaignDO()?.title ?: ""
        return listOfNotNull(
            translateMsg("plugins.marketing.addressCampaignValue.adoptPreview", comparisonTitle),
            if (options.comment) translate("plugins.marketing.addressCampaignValue.adoptComments") else null,
            if (options.onlyEmpty) translate("plugins.marketing.addressCampaignValue.adoptOnlyEmpty") else null,
        ).joinToString(", ")
    }

    override fun proceedMassUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<AddressCampaignValue>,
    ): ResponseEntity<*>? {
        val params = massUpdateContext.massUpdateParams
        val addressCampaignDO = addressCampaignValueEntityRest.getAddressCampaignDO()
            ?: return showValidationErrors(
                ValidationError.create("plugins.marketing.addressCampaignValue.error.addressOrCampaignNotGiven")
            )
        if (!addressCampaignDO.active) {
            // Refused by AddressCampaignValueRight row by row anyway; said once, and why.
            return showValidationErrors(ValidationError.create("plugins.marketing.addressCampaign.error.inactive"))
        }
        val adopt = params[ADOPT_COMPARISON]?.let { AdoptOptions.of(it) }
        if (adopt != null && params["value"]?.hasAction == true) {
            return showValidationErrors(ValidationError.create("plugins.marketing.addressCampaignValue.error.adoptAndSet"))
        }
        val adoption = adopt?.let {
            Adoption(it, addressCampaignValueEntityRest.getComparisonValues(), addressCampaignDO.valuesArray)
        }
        // No property of the value: whether a row changed is told by the adoption (markCurrentModified).
        massUpdateContext.ignoreFieldsForModificationCheck = listOf(ADOPT_COMPARISON)
        params["value"]?.textValue?.let { value ->
            if (value.isNotEmpty() && addressCampaignDO.valuesArray?.contains(value) != true) {
                return showValidationErrors(
                    ValidationError.create("plugins.marketing.addressCampaignValue.error.unknownValue", "value.textValue")
                )
            }
        }
        // Positive ids are existing campaign values, negative ones synthetic ids (negated address ids).
        val realCampaignValueIds = mutableListOf<Long>()
        val addressIds = mutableListOf<Long>()
        selectedIds.forEach { id ->
            val longId = (id as? Number)?.toLong() ?: (id as? String)?.toLongOrNull()
            if (longId != null) {
                if (longId > 0) realCampaignValueIds.add(longId) else addressIds.add(-longId)
            }
        }
        if (realCampaignValueIds.isNotEmpty()) {
            addressCampaignValueDao.select(realCampaignValueIds)?.forEach { addressCampaignValueDO ->
                update(addressCampaignValueDO, params, adoption, massUpdateContext) {
                    addressCampaignValueDO.deleted = false
                    addressCampaignValueDao.update(addressCampaignValueDO)
                }
            }
        }
        if (addressIds.isNotEmpty()) {
            addressDao.select(addressIds)?.forEach { address ->
                // A value may have been created meanwhile.
                val addressCampaignValueDO = addressCampaignValueDao.get(address.id, addressCampaignDO.id)
                    ?: AddressCampaignValueDO().also {
                        addressCampaignValueDao.setAddress(it, address.id!!)
                        it.addressCampaign = addressCampaignDO
                    }
                update(addressCampaignValueDO, params, adoption, massUpdateContext) {
                    if ((addressCampaignValueDO.id ?: 0) > 0) {
                        addressCampaignValueDO.deleted = false
                        addressCampaignValueDao.update(addressCampaignValueDO)
                    } else if (addressCampaignValueDO.value != null || addressCampaignValueDO.comment != null) {
                        // Not an empty value for an address the run had nothing for (e.g. nothing to adopt).
                        addressCampaignValueDao.insert(addressCampaignValueDO)
                    }
                }
            }
        }
        return null
    }

    private fun update(
        addressCampaignValueDO: AddressCampaignValueDO,
        params: Map<String, MassUpdateParameter>,
        adoption: Adoption?,
        massUpdateContext: MassUpdateContext<AddressCampaignValue>,
        update: () -> Unit,
    ) {
        val addressCampaignValue = AddressCampaignValue()
        addressCampaignValue.copyFrom(addressCampaignValueDO)
        massUpdateContext.startUpdate(addressCampaignValue)
        val oldValue = addressCampaignValue.value
        val oldComment = addressCampaignValue.comment
        processTextParameter(addressCampaignValueDO, "comment", params)
        addressCampaignValue.comment = addressCampaignValueDO.comment
        params["value"]?.let { param ->
            if (param.delete == true) {
                addressCampaignValueDO.value = null
                addressCampaignValue.value = null
            }
            param.textValue?.let { value ->
                addressCampaignValueDO.value = value
                addressCampaignValue.value = value
            }
        }
        val adopted = adoption?.apply(addressCampaignValueDO, addressCampaignValue)
        massUpdateContext.commitUpdate(
            identifier4Message = listOfNotNull(
                addressCampaignValue.firstName,
                addressCampaignValue.fullLastName,
                addressCampaignValue.organization,
            ).joinToString(" "),
            addressCampaignValue,
            update = {
                // Reported for this row (MassUpdateContext.addError), the rest of the run goes on.
                adopted?.unknownValue?.let {
                    throw UserException("plugins.marketing.addressCampaignValue.error.unknownComparisonValue", it)
                }
                update()
                if (adopted?.changed == true) {
                    // The adoption has no field of its own the modification check could compare.
                    massUpdateContext.markCurrentModified()
                    // Fields without a param of their own are no Excel columns otherwise.
                    if (!params.containsKey("value")) {
                        massUpdateContext.recordCurrentModification("value", oldValue, addressCampaignValue.value)
                    }
                    if (!params.containsKey("comment")) {
                        massUpdateContext.recordCurrentModification("comment", oldComment, addressCampaignValue.comment)
                    }
                }
            },
        )
    }

    /**
     * The choices of the client's [ADOPT_COMPARISON] control, posted as the param's text value: `value`, plus
     * `comment` (adopt the comment as well) and `onlyEmpty` (only rows without a value of their own), comma separated.
     */
    private class AdoptOptions(val comment: Boolean, val onlyEmpty: Boolean) {
        companion object {
            fun of(param: MassUpdateParameter): AdoptOptions? {
                val options = param.textValue?.split(",")?.map { it.trim() } ?: return null
                if (!options.contains("value")) {
                    return null
                }
                return AdoptOptions(comment = options.contains("comment"), onlyEmpty = options.contains("onlyEmpty"))
            }
        }
    }

    /**
     * Adopts the value (and the comment) the address has in the comparison campaign: a value only where the
     * comparison campaign has one; a comment is appended to the row's own on a new line (see [appendComment]).
     */
    private class Adoption(
        val options: AdoptOptions,
        val comparisonValues: Map<Long, AddressCampaignValueDO>,
        val campaignValues: Array<String>?,
    ) {
        /**
         * @return Whether anything was adopted, or the comparison value the campaign doesn't know (nothing
         * adopted then).
         */
        fun apply(dbObj: AddressCampaignValueDO, dto: AddressCampaignValue): Result {
            val comparison = dbObj.address?.id?.let { comparisonValues[it] } ?: return Result()
            if (options.onlyEmpty && dbObj.shownValue != null) {
                return Result()
            }
            var changed = false
            comparison.shownValue?.let { value ->
                if (campaignValues?.contains(value) != true) {
                    return Result(unknownValue = value)
                }
                changed = dbObj.shownValue != value
                dbObj.value = value
                dto.value = value
            }
            appendComment(dbObj.comment, comparison.comment)?.takeIf { options.comment }?.let { comment ->
                dbObj.comment = comment
                dto.comment = comment
                changed = true
            }
            return Result(changed = changed)
        }

        class Result(val changed: Boolean = false, val unknownValue: String? = null)
    }

    companion object {
        /**
         * The comment with the comparison's appended on a new line (or the comparison's alone, without one of its
         * own), null if there is nothing to adopt: no comparison comment, or one the comment already contains
         * (adopted before).
         */
        internal fun appendComment(own: String?, comparison: String?): String? {
            val adopt = comparison?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val current = own?.trimEnd()?.takeIf { it.isNotBlank() } ?: return adopt
            if (current.contains(adopt)) {
                return null
            }
            return "$current\n$adopt"
        }

        /** The client's control adopting the values of the comparison campaign, see [AdoptOptions]. */
        const val ADOPT_COMPARISON = "adoptComparison"
    }
}
