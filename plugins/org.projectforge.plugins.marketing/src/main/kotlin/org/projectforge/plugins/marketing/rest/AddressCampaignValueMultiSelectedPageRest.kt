/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.address.AddressDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.marketing.*
import org.projectforge.plugins.marketing.dto.AddressCampaignValue
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.rest.multiselect.MultiSelectionSupport
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UILayout
import org.projectforge.ui.UISelect
import org.projectforge.ui.UISelectValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import javax.servlet.http.HttpServletRequest

/**
 * Mass update after selection.
 */
@RestController
@RequestMapping("${Rest.URL}/addressCampaignValue${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class AddressCampaignValueMultiSelectedPageRest : AbstractMultiSelectedPage<AddressCampaignValue>() {

  @Autowired
  private lateinit var addressDao: AddressDao

  @Autowired
  private lateinit var addressCampaignValuePagesRest: AddressCampaignValuePagesRest

  override val layoutContext: LayoutContext = LayoutContext(AddressCampaignValueDO::class.java)

  override fun getTitleKey(): String {
    return "plugins.marketing.addressCampaignValue.multiselected.title"
  }

  override val listPageUrl: String = "/${MenuItemDefId.OUTGOING_INVOICE_LIST.url}"

  override val pagesRestClass: Class<out AbstractPagesRest<*, *, *>>
    get() = AddressCampaignValuePagesRest::class.java

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
    val values = addressCampaign?.values?.map { UISelectValue(it, it) }
    layout.add(
      createInputFieldRow(
        "value",
        UISelect("value.textValue", values = values),
        massUpdateData,
        showDeleteOption = true
      )
    )
    createAndAddFields(lc, massUpdateData, layout, "comment", append = true)
  }

  override fun proceedMassUpdate(
    request: HttpServletRequest,
    selectedIds: Collection<Serializable>,
    massUpdateContext: MassUpdateContext<AddressCampaignValue>,
  ): ResponseEntity<*>? {
    val addressList = addressDao.getListByIds(selectedIds)
    val list = addressCampaignValuePagesRest.convertList(request, addressList)
    val params = massUpdateContext.massUpdateData
    val addressCampaign = addressCampaignValuePagesRest.getAddressCampaign(request)
    list.forEach { entry ->
      massUpdateContext.startUpdate(entry)
      processTextParameter(entry, "comment", params)
      params["value"]?.let { param ->
        if (param.delete == true) {
          entry.value = null
        }
        param.textValue?.let {
          entry.value = it
        }
      }
    }
    return null
  }
}
