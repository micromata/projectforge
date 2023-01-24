/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dvelop

import org.projectforge.business.dvelop.ListData
import org.projectforge.business.dvelop.TradingPartner
import org.projectforge.business.dvelop.TradingPartnerListData
import org.projectforge.framework.json.JsonUtils
import org.springframework.stereotype.Service

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
class TradingPartnerService :
  AbstractDvelopService<TradingPartner>("/alphaflow-tradingpartner/tradingpartnerservice/tradingpartners", "TradingPartner") {

  override fun buildUpdateEntity(localState: TradingPartner, remoteState: TradingPartner): TradingPartner? {
    if (localState.number != remoteState.number) {
      throw IllegalArgumentException("Number of existing TradingPartner (oldState) doesn't match new state!")
    }
    val updateContext = UpdateContext()
    remoteState.company = getPrioritizedString(localState.company, remoteState.company, updateContext)
    remoteState.shortName = getPrioritizedString(localState.shortName, remoteState.shortName, updateContext)
    remoteState.importCode = getPrioritizedString(localState.importCode, remoteState.importCode, updateContext)
    remoteState.datevKonto = getPrioritizedValue(localState.datevKonto, remoteState.datevKonto, updateContext)
    remoteState.remarks = getPrioritizedString(remoteState.remarks, localState.remarks, updateContext)
    if (remoteState.organization == null || remoteState.organization?.id.isNullOrBlank()) {
      remoteState.organization = localState.organization
      updateContext.modified = true
    }
    if (remoteState.isBillToAddressEmpty && !localState.isBillToAddressEmpty) {
      // Update remote address only, if remote address is empty.
      remoteState.billToStreet = localState.billToStreet
      remoteState.billToZip = localState.billToZip
      remoteState.billToCity = localState.billToCity
      remoteState.billToCountry = localState.billToCountry
      remoteState.billToRegion = localState.billToRegion
      remoteState.billToAddressAdditional = localState.billToAddressAdditional
      updateContext.modified = true
    }
    return if (updateContext.modified) {
      remoteState
    } else {
      null
    }
  }

  override fun fromJson(response: String): ListData<TradingPartner>? {
    return JsonUtils.fromJson(response, TradingPartnerListData::class.java, false)
  }
}
