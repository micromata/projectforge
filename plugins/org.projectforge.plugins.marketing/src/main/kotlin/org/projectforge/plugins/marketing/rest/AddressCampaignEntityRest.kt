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

import org.projectforge.framework.i18n.translate
import org.projectforge.plugins.marketing.AddressCampaignDO
import org.projectforge.plugins.marketing.AddressCampaignDao
import org.projectforge.plugins.marketing.dto.AddressCampaign
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.ui.ValidationError
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the address campaigns, serving the hand-built projectforge-next page
 * (see components/features/address-campaign). Replaces the retired `AddressCampaignPagesRest`, whose
 * server-side `UILayout` moved onto the frontend. The Wicket pages `AddressCampaignListPage`/`EditPage`
 * stay as the classic version.
 */
@RestController
@RequestMapping("${Rest.URL}/addressCampaign")
class AddressCampaignEntityRest : AbstractDTOEntityRest<AddressCampaignDO, AddressCampaign, AddressCampaignDao>(
    baseDaoClazz = AddressCampaignDao::class.java,
    i18nKeyPrefix = "plugins.marketing.addressCampaign.title"
) {
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
     * The format check of Wicket's `AddressCampaignEditForm`: values like "Value 1; Value 2; Value 3", at least
     * one of them. Title and values being given at all is checked generically (`@PropertyInfo(required)`).
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: AddressCampaign) {
        super.validate(validationErrors, dto)
        if (!dto.values.isNullOrBlank() && AddressCampaignDO.getValuesArray(dto.values) == null) {
            validationErrors.add(
                ValidationError(
                    translate("plugins.marketing.addressCampaign.values.invalidFormat"),
                    fieldId = "values",
                )
            )
        }
    }
}
