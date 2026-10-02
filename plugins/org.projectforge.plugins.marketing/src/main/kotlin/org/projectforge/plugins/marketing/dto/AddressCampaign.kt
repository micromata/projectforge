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

package org.projectforge.plugins.marketing.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.plugins.marketing.AddressCampaignDO
import org.projectforge.rest.dto.BaseDTO

class AddressCampaign(
  var title: String? = null,
  /** The raw, semicolon separated values as stored (and edited), e.g. "Value 1; Value 2". */
  var values: String? = null,
  var comment: String? = null
) : BaseDTO<AddressCampaignDO>() {
  /**
   * The parsed [values] (see [AddressCampaignDO.getValuesArray]), offered as the choices of a campaign value.
   * Read only: ignored when a client posts it back, and [copyTo] maps by field name and type, so only
   * [values] is written to the entity.
   */
  @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
  val valuesArray: Array<String>?
    get() = AddressCampaignDO.getValuesArray(values)

  override fun copyFrom(src: AddressCampaignDO) {
    super.copyFrom(src)
    title = src.title
    values = src.values
    comment = src.comment
  }
}
