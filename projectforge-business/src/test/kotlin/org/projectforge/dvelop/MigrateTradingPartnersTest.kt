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

package org.projectforge.dvelop

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.dvelop.ExtractPFTradingPartners
import org.projectforge.business.dvelop.TradingPartner
import org.projectforge.business.fibu.*

class MigrateTradingPartnersTest {
  private var pk = 1L
  private var incomingInvoices = mutableListOf<EingangsrechnungDO>()
  private var invoices = mutableListOf<RechnungDO>()

  @Test
  fun migrateTest() {
    val kontoDiverseA = createKonto(70000, "A diverse")
    val kontoDiverseDE = createKonto(70077, "Diverse D,E")
    val kontoAcme = createKonto(71780, "ACME ltd.")
    val kontoPartner = createKonto(10001, "ACME ltd.")
    val kontoCustomerBravo = createKonto(10002, "Bravo ltd.")
    val kontoCustomerCharlie = createKonto(10003, "Charlie Inc.")
    val kontoCustomerDelta = createKonto(10004, "Delta ltd.")
    val kontoCustomerFoxtrott = createKonto(10005, "Foxtrott ltd.")
    val kundeBravoLimited = createKunde(42, "Bravo limited", "Bravo", kontoCustomerBravo)
    val kundeCharlie = createKunde(43, "Charlie Incorp.", "Charlie", kontoCustomerCharlie)

    createIncomingInvoice("Alpha ltd.", kontoDiverseA)
    createIncomingInvoice("Alpha industries ltd.", kontoDiverseA)
    createIncomingInvoice("Apple Inc.", kontoDiverseA)
    createIncomingInvoice("Alpha ltd.", kontoDiverseA) // Should be ignored, Alpha ltd. already added.

    createIncomingInvoice("Dolphin Inc.", kontoDiverseDE)

    createIncomingInvoice("ACME Germany ltd.", kontoAcme) // Later defined as customer No. 45.
    createIncomingInvoice("ACME Germ, ltd.", kontoAcme) // Should be ignored (same konto)

    createIncomingInvoice("Partner Systems ltd.", kontoPartner) // Should be added as partner.

    val context = ExtractPFTradingPartners.extractTradingVendors(incomingInvoices)
    createInvoice(kunde = kundeBravoLimited)
    createInvoice(projekt = createProjekt(kundeCharlie))
    createInvoice(kunde = createKunde(44, "Delta limited", "Delta")).konto = kontoCustomerDelta
    createInvoice(kundeText = "Echo limited").konto = kontoCustomerDelta // Wrong kundeText: is handled as 10004!
    createInvoice(kundeText = "Foxtrott Inc.") // Ignored as customer, not much info.
    createInvoice(kundeText = "Partner Systems", konto = kontoPartner)

    val customerAcme = createKunde(45, "ACME Germany ltd.", "ACME")
    customerAcme.konto = kontoAcme
    createInvoice(kunde = customerAcme)

    ExtractPFTradingPartners.extractTradingCustomersInvoices(invoices, context)

    val customers = mutableListOf<KundeDO>()
    customers.add(kundeBravoLimited) // Already processed.
    customers.add(kundeCharlie)      // Already processed.
    customers.add(createKunde(46, "Foxtrott ltd.", "Foxtrott", kontoCustomerFoxtrott)) // New
    ExtractPFTradingPartners.extractTradingCustomers(customers, context)

    val tradingVendors = context.vendors
    Assertions.assertEquals(6, tradingVendors.size)
    Assertions.assertEquals(2, tradingVendors.filter { it.type?.value == TradingPartner.TypeValue.PARTNER }.size)
    val tradingCustomers = context.customers
    Assertions.assertEquals(5, tradingCustomers.size)
    tradingVendors.forEach { consoleOut(it) }
    tradingCustomers.forEach { consoleOut(it) }
  }

  @Test
  fun extractZipCodeAndCityTest() {
    Assertions.assertNull(ExtractPFTradingPartners.extractZipCodeCity(""))
    Assertions.assertNull(ExtractPFTradingPartners.extractZipCodeCity("   "))
    Assertions.assertNull(ExtractPFTradingPartners.extractZipCodeCity("  Kassel 12345 "))
    assertZipCodeAndCity("12345", "Kassel", ExtractPFTradingPartners.extractZipCodeCity("12345  Kassel"))
    assertZipCodeAndCity("12345", "Kassel", ExtractPFTradingPartners.extractZipCodeCity("  12345  Kassel"))
    assertZipCodeAndCity("021 15", "Zolona", ExtractPFTradingPartners.extractZipCodeCity("021 15 Zolona"))
  }

  @Test
  fun extractBillAddressTest() {
    checkAddress("Herr\nKai Reinhard\nABC-Str. 5\n12345 Kassel", "ABC-Str. 5", "12345", "Kassel")
    checkAddress("\nFrau\nBerta Reinhard\n\nABC-Str. 5\n12345 Kassel\n", "ABC-Str. 5", "12345", "Kassel")
    checkAddress(
      "ACME mbH\nRechnungswesen\nReichenbaum Str. 29\n\n09987 Chemnitz\n",
      "Reichenbaum Str. 29",
      "09987",
      "Chemnitz",
      "Rechnungswesen",
    )
    checkAddress(
      "Acme Holing h. c.\nAnlagenbuchhaltung\nBrunsplatz 1\nD-71888 Stuttgart",
      "Brunsplatz 1",
      "71888",
      "Stuttgart",
      "Anlagenbuchhaltung",
    )
    checkAddress(
      "Acme Holing h. c.\nAnlagenbuchhaltung\nBrunsplatz 1\nD 71888 Stuttgart",
      "Brunsplatz 1",
      "71888",
      "Stuttgart",
      "Anlagenbuchhaltung",
    )
    checkAddress(
      "ACME (Sweden) AB\nBox 3742\n21999 Malmö\nSWEDEN",
      "Box 3742",
      "21999",
      "Malmö",
      expectedCountry = "SWEDEN",
    )
    checkAddress(
      "ACME Technologies AG & Co. KG, Industriestr. 1-3, 99999 Berlin c/o ACME Financial and Accounting Center\nP.O.BOX c. C32\n021 15 Zolona\nSlovakia",
      "P.O.BOX c. C32",
      "021 15",
      "Zolona",
      expectedCountry = "Slovakia",
    )
    checkAddress(
      "ACME Systems International GmbH (PG 8862)\nOttostraße 87d\n12348 Berlin\nc/o Postfach 4242\n99999 Köln",
      "c/o Postfach 4242",
      "99999",
      "Köln",
      "Ottostraße 87d 12348 Berlin",
    )
    checkAddress(
      "Klinikdienst\nACME KG\nHerr Horst Bravo\nFlussallee 92\n12345 Bad Wimpfen",
      "Flussallee 92",
      "12345",
      "Bad Wimpfen",
      "Herr Horst Bravo",
    )
    checkAddress(
      "Portal\nACME KG\nHerr Horst Bravo\nFlussallee 92\n12345 Bad Wimpfen",
      "Flussallee 92",
      "12345",
      "Bad Wimpfen",
      "Herr Horst Bravo",
    )
  }

  private fun checkAddress(
    address: String,
    expectedStreet: String,
    expectedZip: String,
    expectedCity: String,
    expectedAdditionalAddress: String? = null,
    expectedCountry: String? = null,
  ) {
    val partner = TradingPartner()
    val invoice = RechnungDO()
    invoice.customerAddress = address
    ExtractPFTradingPartners.checkBillAddress(partner, invoice)
    Assertions.assertEquals(expectedStreet, partner.billToStreet)
    Assertions.assertEquals(expectedZip, partner.billToZip)
    Assertions.assertEquals(expectedCity, partner.billToCity)
    Assertions.assertEquals(expectedAdditionalAddress, partner.billToAddressAdditional)
    invoice.customerAddress = "ACME\nABC street 5\n12345 Berlin"
    ExtractPFTradingPartners.checkBillAddress(partner, invoice)
    Assertions.assertEquals(expectedStreet, partner.billToStreet)
    Assertions.assertEquals(expectedZip, partner.billToZip)
    Assertions.assertEquals(expectedCity, partner.billToCity)
    Assertions.assertEquals(expectedAdditionalAddress, partner.billToAddressAdditional)
    Assertions.assertEquals(expectedCountry, partner.billToCountry)
  }

  private fun assertZipCodeAndCity(expectedZip: String, expectedCity: String, pair: Pair<String, String>?) {
    Assertions.assertEquals(expectedZip, pair!!.first)
    Assertions.assertEquals(expectedCity, pair.second)
  }

  private fun consoleOut(partner: TradingPartner) {
    val importCode = if (partner.importCode != null) {
      ", importCode=${partner.importCode}"
    } else {
      ""
    }
    val shortName = if (partner.shortName.isNullOrBlank()) {
      ""
    } else {
      ", shortName=${partner.shortName}"
    }
    // println("${partner.type?.value}: ${partner.number} ${partner.company}: ${partner.remarks ?: "ohne Konto"}$shortName$importCode")
  }

  private fun createIncomingInvoice(kreditor: String, konto: KontoDO): EingangsrechnungDO {
    val invoice = EingangsrechnungDO()
    invoice.konto = konto
    invoice.kreditor = kreditor
    incomingInvoices.add(invoice)
    return invoice
  }

  private fun createInvoice(
    kunde: KundeDO? = null,
    projekt: ProjektDO? = null,
    konto: KontoDO? = null,
    kundeText: String? = null,
  ): RechnungDO {
    val invoice = RechnungDO()
    invoice.konto = konto
    invoice.kunde = kunde
    invoice.projekt = projekt
    invoice.kundeText = kundeText
    invoices.add(invoice)
    return invoice
  }

  private fun createKonto(number: Int, bezeichnung: String): KontoDO {
    val konto = KontoDO()
    konto.id = pk++
    konto.nummer = number
    konto.bezeichnung = bezeichnung
    return konto
  }

  private fun createKunde(number: Long, name: String, identifier: String? = null, konto: KontoDO? = null): KundeDO {
    val kunde = KundeDO()
    kunde.id = number
    kunde.name = name
    kunde.identifier = identifier
    kunde.konto = konto
    return kunde
  }

  private fun createProjekt(kunde: KundeDO): ProjektDO {
    val projekt = ProjektDO()
    projekt.kunde = kunde
    return projekt
  }
}
