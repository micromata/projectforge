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

import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.plugins.marketing.AddressCampaignValueDO

/**
 * CustomResultFilter for filtering AddressCampaignValueDO by value field.
 * Supports multiple values and empty value filtering (null or blank).
 *
 * @param selectedValues List of values to match (e.g., ["premium", "normal"])
 * @param includeEmpty Whether to include entries with null or empty value
 */
class ValueResultFilter(
    private val selectedValues: List<String>,
    private val includeEmpty: Boolean
) : CustomResultFilter<AddressCampaignValueDO> {

    override fun match(list: MutableList<AddressCampaignValueDO>, element: AddressCampaignValueDO): Boolean {
        val value = element.value

        // Check if value is empty (null or blank)
        val isEmpty = value.isNullOrBlank()

        return when {
            // If both values and empty are selected
            selectedValues.isNotEmpty() && includeEmpty -> {
                isEmpty || selectedValues.contains(value)
            }
            // Only values selected
            selectedValues.isNotEmpty() -> {
                value != null && selectedValues.contains(value)
            }
            // Only empty selected
            includeEmpty -> {
                isEmpty
            }
            // Nothing selected (should not happen, but match all)
            else -> true
        }
    }
}
