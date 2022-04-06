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

package org.projectforge.plugins.marketing.dto

import org.projectforge.business.address.AddressDO
import org.projectforge.plugins.marketing.AddressCampaignValueDO
import org.projectforge.rest.dto.Address
import org.projectforge.rest.dto.BaseDTO

class AddressCampaignValue(
  var addressCampaign: AddressCampaign? = null,
  var address: Address? = null,
  var value: String? = null,
  var comment: String? = null,
) : BaseDTO<AddressDO>() {
  override fun copyFrom(src: AddressDO) {
    this.id = src.id // Id is address id!!!!
    val address = Address()
    address.copyFrom(src)
    this.address = address
  }

  fun copyFrom(src: AddressCampaignValueDO) {
    src.address?.let { copyFrom(it) }
    this.value = src.value
    this.comment = src.comment
  }
}
