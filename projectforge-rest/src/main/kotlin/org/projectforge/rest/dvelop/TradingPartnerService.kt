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
    val updateEntity = TradingPartner()
    val updateContext = UpdateContext()
    updateEntity.id = remoteState.id
    updateEntity.company = getPrioritizedValue(localState.company, remoteState.company, updateContext)
    updateEntity.shortName = getPrioritizedValue(localState.shortName, remoteState.shortName, updateContext)
    updateEntity.importCode = getPrioritizedValue(localState.importCode, remoteState.importCode, updateContext)
    updateEntity.datevKonto = getPrioritizedValue(localState.datevKonto, remoteState.datevKonto, updateContext)
    if (remoteState.organization == null || remoteState.organization?.id.isNullOrBlank()) {
      remoteState.organization = localState.organization
      updateContext.modified = true
    }
    if (remoteState.isBillToAddressEmpty && !localState.isBillToAddressEmpty) {
      // Update remote address only, if remote address is empty.
      updateEntity.billToStreet = localState.billToStreet
      updateEntity.billToZip = localState.billToZip
      updateEntity.billToCity = localState.billToCity
      updateEntity.billToCountry = localState.billToCountry
      updateEntity.billToRegion = localState.billToRegion
      updateEntity.billToAddressAdditional = localState.billToAddressAdditional
      updateContext.modified = true
    }
    return if (updateContext.modified) {
      updateEntity
    } else {
      null
    }
  }

  override fun fromJson(response: String): ListData<TradingPartner>? {
    return JsonUtils.fromJson(response, TradingPartnerListData::class.java, false)
  }
}
