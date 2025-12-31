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

package org.projectforge.plugins.marketing

import org.projectforge.framework.persistence.api.impl.CustomResultFilter

/**
 * Filters campaign values to show only entries with invalid addresses.
 * An address is considered invalid when isAddressValid is false.
 */
class InvalidAddressesResultFilter : CustomResultFilter<AddressCampaignValueDO> {

    override fun match(list: MutableList<AddressCampaignValueDO>, element: AddressCampaignValueDO): Boolean {
        // Filter for addresses where isAddressValid is explicitly false
        return element.address?.let { address ->
            val mailingAddress = org.projectforge.business.address.MailingAddress(address)
            !mailingAddress.isValid
        } ?: false
    }
}
