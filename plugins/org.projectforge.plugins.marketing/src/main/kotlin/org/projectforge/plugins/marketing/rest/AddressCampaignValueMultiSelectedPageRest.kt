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

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.address.AddressDao
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.plugins.marketing.AddressCampaignValueDO
import org.projectforge.plugins.marketing.AddressCampaignValueDao
import org.projectforge.plugins.marketing.dto.AddressCampaignValue
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable


/**
 * Mass update after selection.
 */
@RestController
@RequestMapping("${Rest.URL}/addressCampaignValue${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class AddressCampaignValueMultiSelectedPageRest : AbstractMultiSelectedPage<AddressCampaignValue>() {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressCampaignValueDao: AddressCampaignValueDao

    @Autowired
    private lateinit var addressCampaignValuePagesRest: AddressCampaignValuePagesRest

    override val layoutContext: LayoutContext = LayoutContext(AddressCampaignValueDO::class.java)

    override fun getId(obj: AddressCampaignValue): Long {
        return obj.id ?: obj.addressId ?: -1
    }

    override fun getTitleKey(): String {
        return "plugins.marketing.addressCampaignValue.multiselected.title"
    }

    @PostConstruct
    private fun postConstruct() {
        pagesRest = addressCampaignValuePagesRest
    }

    override fun fillForm(
        request: HttpServletRequest,
        layout: UILayout,
        massUpdateData: MutableMap<String, MassUpdateParameter>,
        selectedIds: Collection<Serializable>?,
        variables: MutableMap<String, Any>,
    ) {
        /*
              // Heading
          gridBuilder.newFormHeading(
              getString("plugins.marketing.addressCampaignValue") + ": " + data.getAddressCampaign().getTitle());

         */
        val lc = LayoutContext(AddressCampaignValueDO::class.java)

        val addressCampaign = addressCampaignValuePagesRest.getAddressCampaign(request)
        if (addressCampaign != null) {
            layout.add(UIReadOnlyField(label = "plugins.marketing.addressCampaign", value = addressCampaign.title))
        }
        val values = addressCampaign?.values?.map { UISelectValue(it, it) }
        layout.add(
            createInputFieldRow(
                "value",
                UISelect("value.textValue", values = values),
                massUpdateData,
                showDeleteOption = true
            )
        )
        createAndAddFields(lc, massUpdateData, layout, "comment", showAppendOption = true)
    }

    override fun proceedMassUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<AddressCampaignValue>,
    ): ResponseEntity<*>? {
        val params = massUpdateContext.massUpdateParams
        val addressCampaign = addressCampaignValuePagesRest.getAddressCampaign(request)
        params["value"]?.let { param ->
            param.textValue?.let { value ->
                if (!value.isEmpty() && addressCampaign?.values?.contains(value) != true) {
                    return showValidationErrors(
                        ValidationError(
                            "plugins.marketing.addressCampaignValue.error.unknownValue",
                            "value.textValue"
                        )
                    )
                }
            }
        }
        val addressCampaignDO = addressCampaignValuePagesRest.getAddressCampaignDO(request)
        if (addressCampaignDO == null) {
            return showValidationErrors(
                ValidationError("plugins.marketing.addressCampaignValue.error.addressOrCampaignNotGiven")
            )
        }

        // Split selected IDs into positive (existing campaign values) and negative (address IDs)
        val realCampaignValueIds = mutableListOf<Long>()
        val addressIdsFromNegative = mutableListOf<Long>()

        selectedIds.forEach { id ->
            val longId = (id as? Long) ?: (id as? String)?.toLongOrNull()
            if (longId != null) {
                if (longId > 0) {
                    realCampaignValueIds.add(longId)
                } else {
                    // Negative ID = synthetic ID, convert back to addressId
                    addressIdsFromNegative.add(-longId)
                }
            }
        }

        // 1. Load existing campaign values (positive IDs)
        val existingCampaignValues = if (realCampaignValueIds.isNotEmpty()) {
            addressCampaignValueDao.select(realCampaignValueIds) ?: emptyList()
        } else {
            emptyList()
        }

        // Process existing campaign values
        existingCampaignValues.forEach { addressCampaignValueDO ->
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
                update = {
                    addressCampaignValueDO.deleted = false
                    addressCampaignValueDao.update(addressCampaignValueDO)
                },
            )
        }

        // 2. Load addresses for negative IDs and create/update campaign values
        if (addressIdsFromNegative.isNotEmpty()) {
            val addresses = addressDao.select(addressIdsFromNegative) ?: emptyList()
            addresses.forEach { address ->
                // Check if campaign value exists for this address
                var addressCampaignValueDO = addressCampaignValueDao.get(address.id, addressCampaignDO.id)
                if (addressCampaignValueDO == null) {
                    // Create new campaign value
                    addressCampaignValueDO = AddressCampaignValueDO()
                    addressCampaignValueDao.setAddress(addressCampaignValueDO, address.id!!)
                    addressCampaignValueDO.addressCampaign = addressCampaignDO
                }

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
                    update = {
                        if (addressCampaignValueDO.id != null && addressCampaignValueDO.id!! > 0) {
                            addressCampaignValueDO.deleted = false
                            addressCampaignValueDao.update(addressCampaignValueDO)
                        } else {
                            addressCampaignValueDao.insert(addressCampaignValueDO)
                            EntityCopyStatus.MAJOR
                        }
                    },
                )
            }
        }
        return null
    }
}
