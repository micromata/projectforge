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

import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.plugins.marketing.AddressCampaignValueDO
import org.projectforge.plugins.marketing.AddressCampaignValueDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UILayout
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
class AddressCampaignValueMultiSelectedPageRest : AbstractMultiSelectedPage<AddressCampaignValueDO>() {

  @Autowired
  private lateinit var addressCampaignValueDao: AddressCampaignValueDao

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
    createAndAddFields(lc, massUpdateData, layout, "values")
    createAndAddFields(lc, massUpdateData, layout, "comment", append = true)
    /*
        {
      // Value
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("value"));
      final AddressCampaignDO addressCampaign = data.getAddressCampaign();
      final LabelValueChoiceRenderer<String> valueChoiceRenderer = new LabelValueChoiceRenderer<>(
          addressCampaign.getValuesArray());
      fs.addDropDownChoice(new PropertyModel<>(data, "value"), valueChoiceRenderer.getValues(),
          valueChoiceRenderer).setNullValid(
              false);
    }
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("comment"));
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "comment"))).setAutogrow();
    }

     */
  }

  override fun proceedMassUpdate(
    selectedIds: Collection<Serializable>,
    massUpdateContext: MassUpdateContext<AddressCampaignValueDO>,
  ): ResponseEntity<*>? {
    val campaignValues = addressCampaignValueDao.getListByIds(selectedIds)
    if (campaignValues.isNullOrEmpty()) {
      return null
    }
    val params = massUpdateContext.massUpdateData
    campaignValues.forEach { invoice ->
      massUpdateContext.startUpdate(invoice)
      processTextParameter(invoice, "comment", params)
    }
    return null
  }
}
