package org.projectforge.rest.dvelop

import org.projectforge.business.dvelop.ListData
import org.projectforge.business.dvelop.TradingPartner
import org.projectforge.business.dvelop.TradingPartnerListData
import org.projectforge.framework.json.JsonUtils
import org.springframework.stereotype.Service

/**
 * Handles the login to the dvelop server (if configured and in use).
 *
 * Fragen
 * * Datev-Konto als Entität
 *
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
class TradingPartnerService :
  AbstractService<TradingPartner>("/alphaflow-tradingpartner/tradingpartnerservice/tradingpartners", "TradingPartner") {

  override fun buildUpdateEntity(localState: TradingPartner, remoteState: TradingPartner): TradingPartner? {
    if (localState.number != remoteState.number) {
      throw IllegalArgumentException("Number of existing TradingPartner (oldState) doesn't match new state!")
    }
    val updateContext = UpdateContext()
    remoteState.company = getPrioritizedValue(localState.company, remoteState.company, updateContext)
    remoteState.shortName = getPrioritizedString(localState.shortName, remoteState.shortName, updateContext)
    remoteState.importCode = getPrioritizedString(localState.importCode, remoteState.importCode, updateContext)
    remoteState.datevKonto = getPrioritizedValue(localState.datevKonto, remoteState.datevKonto, updateContext)
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
