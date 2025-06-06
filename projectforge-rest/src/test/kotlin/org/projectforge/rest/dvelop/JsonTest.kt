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

package org.projectforge.rest.dvelop

import com.fasterxml.jackson.core.type.TypeReference
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.dvelop.TradingPartner
import org.projectforge.framework.json.JsonUtils


class JsonTest {
  @Test
  fun deserializeTradingPartnerListTest() {
    TradingPartner.datevKontoFieldId = "2"
    // [{"id":"63b6a413f07ce12218a9c55d","dmsDocumentType":null,"importCode":null,"sealed":false,"createdAt":"2023-01-05 11:18:59","creator":"2F4732D5-E451-423C-BB45-BFFA73FD52BB","updatedAt":null,"deletedAt":null,"draftedAt":null,"hasDocuments":false,"hasComments":false,"displayTitle":"Hurzel GmbH (1)","optionTitle":"Hurzel GmbH (1)","organization":null,"organizationAccountingArea":null,"number":"1","name":"Hurzel GmbH","shortName":"","contactType":{"value":"COMPANY","name":"Firma"},"firstName":"","lastName":"","company":"Hurzel GmbH","type":{"value":"VENDOR","name":"Lieferant"},"street":"","zip":"","city":"","region":"","country":"","postBoxZip":"","postBox":"","addressAdditional":"","billToStreet":"","billToZip":"","billToCity":"","billToRegion":"","billToCountry":"","billToAddressAdditional":"","shipToStreet":"","shipToZip":"","shipToCity":"","shipToRegion":"","shipToCountry":"","shipToAddressAdditional":"","phoneNumber":"","faxNumber":"","website":"","sector":"","revenue":0.0,"employeeCount":0,"remarks":"","responsible":null,"relationSince":null,"relationUntil":null,"category":"","division":"","purchaseArea":"","vatRate":null,"maturity":null,"outgoingDiscountDays1":0,"outgoingDiscountDays2":0,"outgoingDiscountRate1":0,"outgoingDiscountRate2":0,"invoiceReceiver":"","invoiceReceiverCC":"","automaticInvoiceReminder":{"value":null,"name":""},"hourlyRate":null,"active":{"value":"TRUE","name":"Ja"},"archived":null,"iban":"","bic":"","bank":"","vatid":"","taxnumber":"","customFields":{"63b69f8cf07ce12218a9c558":{"configID":"63b69f8cf07ce12218a9c558","name":"datevKonto","value":1234,"valueName":"1.234","selectOptionDtos":[],"label":"DATEV-Konto","type":"NUMBER"}},"paymentTerminsIdentifier":"","paymentTerminsName":"","dueDays":0,"discountDays1":0,"discountDays2":0,"discountRate1":0,"discountRate2":0,"email":""},{"id":"63b6a44cf07ce12218a9c560","dmsDocumentType":null,"importCode":null,"sealed":false,"createdAt":"2023-01-05 11:19:56","creator":"E6FFA8D1-D867-452A-8F65-425DB951013C","updatedAt":null,"deletedAt":null,"draftedAt":null,"hasDocuments":false,"hasComments":false,"displayTitle":"Hurzel2 GmbH (0002)","optionTitle":"Hurzel2 GmbH (0002)","organization":null,"organizationAccountingArea":null,"number":"0002","name":"Hurzel2 GmbH","shortName":null,"contactType":{"value":"COMPANY","name":"Firma"},"firstName":null,"lastName":null,"company":"Hurzel2 GmbH","type":{"value":"PARTNER","name":"Partner"},"street":null,"zip":null,"city":null,"region":null,"country":null,"postBoxZip":null,"postBox":null,"addressAdditional":null,"billToStreet":null,"billToZip":null,"billToCity":null,"billToRegion":null,"billToCountry":null,"billToAddressAdditional":null,"shipToStreet":null,"shipToZip":null,"shipToCity":null,"shipToRegion":null,"shipToCountry":null,"shipToAddressAdditional":null,"phoneNumber":null,"faxNumber":null,"website":null,"sector":null,"revenue":0.0,"employeeCount":0,"remarks":null,"responsible":null,"relationSince":null,"relationUntil":null,"category":null,"division":null,"purchaseArea":null,"vatRate":null,"maturity":null,"outgoingDiscountDays1":0,"outgoingDiscountDays2":0,"outgoingDiscountRate1":0,"outgoingDiscountRate2":0,"invoiceReceiver":"","invoiceReceiverCC":"","automaticInvoiceReminder":{"value":null,"name":""},"hourlyRate":null,"active":{"value":"TRUE","name":"Ja"},"archived":null,"iban":null,"bic":null,"bank":null,"vatid":null,"taxnumber":null,"customFields":null,"paymentTerminsIdentifier":null,"paymentTerminsName":null,"dueDays":0,"discountDays1":0,"discountDays2":0,"discountRate1":0,"discountRate2":0,"email":null}]
    val json =
      "[{\"id\":\"63b6a413f07ce12218a9c55d\",\"dmsDocumentType\":null,\"importCode\":null,\"sealed\":false,\"createdAt\":\"2023-01-05 11:18:59\",\"creator\":\"2F4732D5-E451-423C-BB45-BFFA73FD52BB\",\"updatedAt\":null,\"deletedAt\":null,\"draftedAt\":null,\"hasDocuments\":false,\"hasComments\":false,\"displayTitle\":\"Hurzel GmbH (1)\",\"optionTitle\":\"Hurzel GmbH (1)\",\"organization\":null,\"organizationAccountingArea\":null,\"number\":\"1\",\"name\":\"Hurzel GmbH\",\"shortName\":\"\",\"contactType\":{\"value\":\"COMPANY\",\"name\":\"Firma\"},\"firstName\":\"\",\"lastName\":\"\",\"company\":\"Hurzel GmbH\",\"type\":{\"value\":\"VENDOR\",\"name\":\"Lieferant\"},\"street\":\"\",\"zip\":\"\",\"city\":\"\",\"region\":\"\",\"country\":\"\",\"postBoxZip\":\"\",\"postBox\":\"\",\"addressAdditional\":\"\",\"billToStreet\":\"\",\"billToZip\":\"\",\"billToCity\":\"\",\"billToRegion\":\"\",\"billToCountry\":\"\",\"billToAddressAdditional\":\"\",\"shipToStreet\":\"\",\"shipToZip\":\"\",\"shipToCity\":\"\",\"shipToRegion\":\"\",\"shipToCountry\":\"\",\"shipToAddressAdditional\":\"\",\"phoneNumber\":\"\",\"faxNumber\":\"\",\"website\":\"\",\"sector\":\"\",\"revenue\":0.0,\"employeeCount\":0,\"remarks\":\"\",\"responsible\":null,\"relationSince\":null,\"relationUntil\":null,\"category\":\"\",\"division\":\"\",\"purchaseArea\":\"\",\"vatRate\":null,\"maturity\":null,\"outgoingDiscountDays1\":0,\"outgoingDiscountDays2\":0,\"outgoingDiscountRate1\":0,\"outgoingDiscountRate2\":0,\"invoiceReceiver\":\"\",\"invoiceReceiverCC\":\"\",\"automaticInvoiceReminder\":{\"value\":null,\"name\":\"\"},\"hourlyRate\":null,\"active\":{\"value\":\"TRUE\",\"name\":\"Ja\"},\"archived\":null,\"iban\":\"\",\"bic\":\"\",\"bank\":\"\",\"vatid\":\"\",\"taxnumber\":\"\",\"customFields\":{\"63b69f8cf07ce12218a9c558\":{\"configID\":\"63b69f8cf07ce12218a9c558\",\"name\":\"datevKonto\",\"value\":1234,\"valueName\":\"1.234\",\"selectOptionDtos\":[],\"label\":\"DATEV-Konto\",\"type\":\"NUMBER\"}},\"paymentTerminsIdentifier\":\"\",\"paymentTerminsName\":\"\",\"dueDays\":0,\"discountDays1\":0,\"discountDays2\":0,\"discountRate1\":0,\"discountRate2\":0,\"email\":\"\"},{\"id\":\"63b6a44cf07ce12218a9c560\",\"dmsDocumentType\":null,\"importCode\":null,\"sealed\":false,\"createdAt\":\"2023-01-05 11:19:56\",\"creator\":\"E6FFA8D1-D867-452A-8F65-425DB951013C\",\"updatedAt\":null,\"deletedAt\":null,\"draftedAt\":null,\"hasDocuments\":false,\"hasComments\":false,\"displayTitle\":\"Hurzel2 GmbH (0002)\",\"optionTitle\":\"Hurzel2 GmbH (0002)\",\"organization\":null,\"organizationAccountingArea\":null,\"number\":\"0002\",\"name\":\"Hurzel2 GmbH\",\"shortName\":null,\"contactType\":{\"value\":\"COMPANY\",\"name\":\"Firma\"},\"firstName\":null,\"lastName\":null,\"company\":\"Hurzel2 GmbH\",\"type\":{\"value\":\"PARTNER\",\"name\":\"Partner\"},\"street\":null,\"zip\":null,\"city\":null,\"region\":null,\"country\":null,\"postBoxZip\":null,\"postBox\":null,\"addressAdditional\":null,\"billToStreet\":null,\"billToZip\":null,\"billToCity\":null,\"billToRegion\":null,\"billToCountry\":null,\"billToAddressAdditional\":null,\"shipToStreet\":null,\"shipToZip\":null,\"shipToCity\":null,\"shipToRegion\":null,\"shipToCountry\":null,\"shipToAddressAdditional\":null,\"phoneNumber\":null,\"faxNumber\":null,\"website\":null,\"sector\":null,\"revenue\":0.0,\"employeeCount\":0,\"remarks\":null,\"responsible\":null,\"relationSince\":null,\"relationUntil\":null,\"category\":null,\"division\":null,\"purchaseArea\":null,\"vatRate\":null,\"maturity\":null,\"outgoingDiscountDays1\":0,\"outgoingDiscountDays2\":0,\"outgoingDiscountRate1\":0,\"outgoingDiscountRate2\":0,\"invoiceReceiver\":\"\",\"invoiceReceiverCC\":\"\",\"automaticInvoiceReminder\":{\"value\":null,\"name\":\"\"},\"hourlyRate\":null,\"active\":{\"value\":\"TRUE\",\"name\":\"Ja\"},\"archived\":null,\"iban\":null,\"bic\":null,\"bank\":null,\"vatid\":null,\"taxnumber\":null,\"customFields\":null,\"paymentTerminsIdentifier\":null,\"paymentTerminsName\":null,\"dueDays\":0,\"discountDays1\":0,\"discountDays2\":0,\"discountRate1\":0,\"discountRate2\":0,\"email\":null}]\n"
    val list = JsonUtils.fromJson(json, object : TypeReference<List<TradingPartner?>?>() {}, false)
    assertList(list)
    // println(list?.joinToString { "${it?.name}, datev=${it?.customFields?.values?.first()?.value}" })
  }

  @Test
  fun serializeTradingPartnerListTest() {
    var partner = TradingPartner()
    partner.name = "Hurzel GmbH"
    partner.datevKonto = 1234
    val list = mutableListOf(partner)
    partner = TradingPartner()
    partner.name = "Hurzel2 GmbH"
    list.add(partner)
    assertList(list)
    val json = JsonUtils.toJson(list)
    // println(json)
    val deserialized = JsonUtils.fromJson(json, object : TypeReference<List<TradingPartner?>?>() {}, false)
    assertList(deserialized)
  }

  private fun assertList(list: List<TradingPartner?>?) {
    Assertions.assertEquals("Hurzel GmbH", list!![0]!!.name)
    Assertions.assertEquals(1234, list[0]!!.customFields!!.values.first().value)
    Assertions.assertEquals(1234, list[0]!!.datevKonto)
    Assertions.assertEquals("Hurzel2 GmbH", list[1]!!.name)
    Assertions.assertNull(list[1]!!.datevKonto)
    Assertions.assertNull(list[1]!!.customFields)
  }
}
