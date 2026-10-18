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
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateFieldDeclaration
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
@RequestMapping("${Rest.URL}/addressCampaignValue${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class AddressCampaignValueMultiSelectedPageRest : AbstractMultiSelectedPage<AddressCampaignValue>() {
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
     * [MassUpdateFieldDeclaration.values]), and the comment.
     */
    override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
        val values = addressCampaignValueEntityRest.getAddressCampaignDO()?.valuesArray?.map { UISelectValue(it, it) }
        return listOf(
            MassUpdateFieldDeclaration("value", showDeleteOption = true, values = values ?: emptyList()),
            MassUpdateFieldDeclaration("comment", showAppendOption = true),
        )
    }

    /** The selected campaign, shown above the selection (the same line as above the list). */
    override fun getStatisticsData(selectedIds: Collection<Serializable>?): Any {
        val campaign = addressCampaignValueEntityRest.getAddressCampaignDO()
        return AddressCampaignValueEntityRest.Statistics(campaign?.id, campaign?.title)
    }

    override fun proceedMassUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<AddressCampaignValue>,
    ): ResponseEntity<*>? {
        val params = massUpdateContext.massUpdateParams
        val addressCampaignDO = addressCampaignValueEntityRest.getAddressCampaignDO()
            ?: return showValidationErrors(
                ValidationError("plugins.marketing.addressCampaignValue.error.addressOrCampaignNotGiven")
            )
        params["value"]?.textValue?.let { value ->
            if (value.isNotEmpty() && addressCampaignDO.valuesArray?.contains(value) != true) {
                return showValidationErrors(
                    ValidationError("plugins.marketing.addressCampaignValue.error.unknownValue", "value.textValue")
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
                update(addressCampaignValueDO, params, massUpdateContext) {
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
                update(addressCampaignValueDO, params, massUpdateContext) {
                    if ((addressCampaignValueDO.id ?: 0) > 0) {
                        addressCampaignValueDO.deleted = false
                        addressCampaignValueDao.update(addressCampaignValueDO)
                    } else {
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
        massUpdateContext: MassUpdateContext<AddressCampaignValue>,
        update: () -> Unit,
    ) {
        val addressCampaignValue = AddressCampaignValue()
        addressCampaignValue.copyFrom(addressCampaignValueDO)
        massUpdateContext.startUpdate(addressCampaignValue)
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
        massUpdateContext.commitUpdate(
            identifier4Message = "${addressCampaignValue.firstName} ${addressCampaignValue.fullLastName} ${addressCampaignValue.organization}",
            addressCampaignValue,
            update = update,
        )
    }
}
