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

import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.plugins.marketing.AddressCampaignValueDO

/**
 * The value an address has in the campaign as shown: a deleted value reads as not set.
 */
internal val AddressCampaignValueDO.shownValue: String?
    get() = if (deleted) null else value?.takeIf { it.isNotBlank() }

/**
 * Filters the rows of a campaign by the value their address has in the comparison campaign (e.g. "2025 = VIP"),
 * applied after the merge of values and addresses like [ValueResultFilter].
 *
 * @param comparisonValues The (not deleted) values of the comparison campaign by address id.
 * @param selectedValues The comparison values to match.
 * @param includeEmpty Whether an address without a comparison value matches as well.
 */
class ComparisonValueResultFilter(
    private val comparisonValues: Map<Long, AddressCampaignValueDO>,
    private val selectedValues: List<String>,
    private val includeEmpty: Boolean,
) : CustomResultFilter<AddressCampaignValueDO> {
    override fun match(list: MutableList<AddressCampaignValueDO>, element: AddressCampaignValueDO): Boolean {
        val comparisonValue = element.address?.id?.let { comparisonValues[it] }?.shownValue
            ?: return includeEmpty
        return selectedValues.contains(comparisonValue)
    }
}

/**
 * Only the rows whose value differs from the one their address has in the comparison campaign: what is still to
 * be decided or adopted. A value set in one campaign only is a difference; the comment doesn't count.
 *
 * @param comparisonValues The (not deleted) values of the comparison campaign by address id.
 */
class ComparisonDifferenceResultFilter(
    private val comparisonValues: Map<Long, AddressCampaignValueDO>,
) : CustomResultFilter<AddressCampaignValueDO> {
    override fun match(list: MutableList<AddressCampaignValueDO>, element: AddressCampaignValueDO): Boolean {
        val comparisonValue = element.address?.id?.let { comparisonValues[it] }?.shownValue
        return element.shownValue != comparisonValue
    }
}
