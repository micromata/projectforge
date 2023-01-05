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

import org.junit.jupiter.api.Test
import org.projectforge.business.dvelop.DvelopConfiguration
import org.projectforge.business.dvelop.TradingPartner
import java.util.*
import kotlin.io.path.Path


class LoginTest {
  @Test
  fun login() {
    DvelopClient().login()
    println("Hello world")
  }
}

fun main(args: Array<String>) {
  // From documentation:
  // println("{    \"contactType\": {        \"value\": \"COMPANY\"    },    \"organization\": {        \"id\": \"\"    },    \"company\": \"d.velop AG\",    \"type\": {        \"value\": \"PARTNER\"    },    \"number\": \"0004\",    \"street\": \"Schildarpstraße 6\",    \"zip\": \"48712\",    \"city\": \"Gescher\",    \"country\": \"Deutschland\",    \"addressAdditional\": \"d.velop Campus Gebäude 3\",    \"website\": \"www.d-velop.de\",    \"active\": {        \"value\": \"TRUE\"    }}")
  val props = Properties()
  val path = Path(System.getProperty("user.home"), "ProjectForge", "projectforge.properties")
  props.load(path.toFile().bufferedReader())
  val baseUri = props.getProperty("projectforge.dvelop.baseUri")
  val apiKey = props.getProperty("projectforge.dvelop.apiKey")
  val organization = props.getProperty("projectforge.dvelop.organization")
  if (baseUri.isNullOrBlank() || apiKey.isNullOrBlank()) {
    println("projectforge.dvelop.baseUri and/or projectforge.dvelop.apiKey is not given in file: ${path.toFile().absolutePath}")
    System.exit(0)
  }
  val dvelopClient = DvelopClient()
  val config = DvelopConfiguration()
  config.baseUri = baseUri
  config.apiKey = apiKey
  config.organizationName = organization ?: ""
  dvelopClient.dvelopConfiguration = config
  dvelopClient.postConstruct()

  dvelopClient.getCustomFields("TradingPartner")
  dvelopClient.getOrganizationOptions()

  val tradingPartnerService = TradingPartnerService()
  tradingPartnerService.dvelopClient = dvelopClient
  tradingPartnerService.debugConsoleOutForTesting = true
  tradingPartnerService.postConstruct()
  //val session = DvelopClient.Session("sessionid", null)
  //val session = client.login()
  //println("authSessionId: ${session?.authSessionId}")
  val partner = TradingPartner()
  partner.active = TradingPartner.Active(TradingPartner.ActiveValue.TRUE)
  partner.number = "0002"
  partner.datevKonto = 1234
  partner.contactType = TradingPartner.ContactType(TradingPartner.ContactTypeValue.COMPANY)
  partner.company = "Hurzel2 GmbH"
  partner.type = TradingPartner.Type(TradingPartner.TypeValue.PARTNER)
  partner.organization = TradingPartner.Organization(dvelopClient.organizationId)
  // client.createTradingPartner(partner)
  val list = tradingPartnerService.getList()
  println(list.size)
  list.forEach { partner ->
    tradingPartnerService.delete(partner.id, partner)
  }
}
