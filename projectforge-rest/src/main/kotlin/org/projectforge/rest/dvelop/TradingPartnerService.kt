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

  override fun fromJson(response: String): ListData<TradingPartner>? {
    return JsonUtils.fromJson(response, TradingPartnerListData::class.java, false)
  }

}
