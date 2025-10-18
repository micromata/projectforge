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

import org.projectforge.business.address.AddressDO
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.plugins.marketing.AddressCampaignValueDO

/**
 * Generic adapter that wraps any CustomResultFilter<AddressDO> to work with AddressCampaignValueDO.
 * This allows reusing existing address filters (like FavoritesResultFilter, DoubletsResultFilter)
 * with address campaign values without duplicating filter logic.
 *
 * @param addressFilter The address filter to wrap
 */
class CampaignValueFilterAdapter(
    private val addressFilter: CustomResultFilter<AddressDO>
) : CustomResultFilter<AddressCampaignValueDO> {

    /**
     * Delegates the match decision to the wrapped address filter.
     * Returns false if the campaign value has no associated address.
     */
    override fun match(list: MutableList<AddressCampaignValueDO>, element: AddressCampaignValueDO): Boolean {
        val address = element.address ?: return false
        val tempList = mutableListOf<AddressDO>()
        return addressFilter.match(tempList, address)
    }
}
