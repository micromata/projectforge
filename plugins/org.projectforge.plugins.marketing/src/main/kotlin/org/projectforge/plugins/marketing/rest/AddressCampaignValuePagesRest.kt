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

import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.plugins.marketing.AddressCampaignValueDO
import org.projectforge.plugins.marketing.AddressCampaignValueDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AGGridSupport
import org.projectforge.rest.core.AbstractDOPagesRest
import org.projectforge.rest.fibu.RechnungMultiSelectedPageRest
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UIAgGridColumnDef
import org.projectforge.ui.UILabel
import org.projectforge.ui.UILayout
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/addressCampaignValue")
class AddressCampaignValuePagesRest : AbstractDOPagesRest<AddressCampaignValueDO, AddressCampaignValueDao>(
  baseDaoClazz = AddressCampaignValueDao::class.java,
  i18nKeyPrefix = "plugins.marketing.addressCampaignValue.title"
) {

  /**
   * LAYOUT List page
   */
  override fun createListLayout(request: HttpServletRequest, magicFilter: MagicFilter): UILayout {
    /*    val layout = super.createListLayout(request, magicFilter)
        .add(
          UITable.createUIResultSetTable()
            .add(
              lc, "created", "address.name", "address.firstName", "address.organization",
              "address.contactStatus", "address.addressText", "address.addressStatus", "value", "comment"
            )
        )*/
    val layout = super.createListLayout(request, magicFilter)
    AGGridSupport.prepareUIGrid4ListPage(
      request,
      layout,
      RechnungMultiSelectedPageRest::class.java,
      magicFilter,
    )
      .add(lc, "created", "address.name", "address.firstName", "address.organization")
      .add(lc, "address.contactStatus", "address.email", "address.addressText", "address.addressStatus")
      .add(lc, "value", "comment")
      .withMultiRowSelection(request, magicFilter)
      .withPinnedLeft(4)
    return LayoutUtils.processListPage(layout, this)
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: AddressCampaignValueDO, userAccess: UILayout.UserAccess): UILayout {
    val layout = super.createEditLayout(dto, userAccess)
      .add(UILabel("TODO"))
    return LayoutUtils.processEditPage(layout, dto, this)
  }
}
