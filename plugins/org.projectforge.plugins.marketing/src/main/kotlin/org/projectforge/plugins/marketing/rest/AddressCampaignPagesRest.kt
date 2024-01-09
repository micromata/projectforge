/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.plugins.marketing.AddressCampaignDO
import org.projectforge.plugins.marketing.AddressCampaignDao
import org.projectforge.plugins.marketing.dto.AddressCampaign
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UILayout
import org.projectforge.ui.UITable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/addressCampaign")
class AddressCampaignPagesRest: AbstractDTOPagesRest<AddressCampaignDO, AddressCampaign, AddressCampaignDao>(baseDaoClazz = AddressCampaignDao::class.java, i18nKeyPrefix = "plugins.marketing.addressCampaign.title") {
    override fun transformForDB(dto: AddressCampaign): AddressCampaignDO {
        val addressCampaignDO = AddressCampaignDO()
        dto.copyTo(addressCampaignDO)
        return addressCampaignDO
    }

    override fun transformFromDB(obj: AddressCampaignDO, editMode: Boolean): AddressCampaign {
        val addressCampaign = AddressCampaign()
        addressCampaign.copyFrom(obj)
        return addressCampaign
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
        layout.add(UITable.createUIResultSetTable()
                        .add(lc, "created", "lastUpdate", "title", "values", "comment"))
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: AddressCampaign, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(lc, "title", "values", "comment")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
