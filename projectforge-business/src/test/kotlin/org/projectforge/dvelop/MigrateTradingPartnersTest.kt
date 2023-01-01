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

package org.projectforge.dvelop

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.dvelop.MigrateTradingPartners
import org.projectforge.business.dvelop.TradingPartner
import org.projectforge.business.fibu.*

class MigrateTradingPartnersTest {
  private var pk = 1
  private var incomingInvoices = mutableListOf<EingangsrechnungDO>()
  private var invoices = mutableListOf<RechnungDO>()

  @Test
  fun migrateTest() {
    val kontoDiverseA = createKonto(10000, "A diverse")
    val kontoDiverseDE = createKonto(10077, "Diverse D,E")
    val kontoAcme = createKonto(11780, "ACME ltd.")
    val kontoPartner = createKonto(70001, "ACME ltd.")
    val kontoCustomerBravo = createKonto(70002, "Bravo ltd.")
    val kontoCustomerCharlie = createKonto(70003, "Charlie Inc.")
    val kontoCustomerDelta = createKonto(70004, "Delta ltd.")
    val kontoCustomerEcho = createKonto(70005, "Echo ltd.")

    createIncomingInvoice("Alpha ltd.", kontoDiverseA)
    createIncomingInvoice("Alpha industries ltd.", kontoDiverseA)
    createIncomingInvoice("Apple Inc.", kontoDiverseA)
    createIncomingInvoice("Alpha ltd.", kontoDiverseA) // Should be ignored, Alpha ltd. already added.

    createIncomingInvoice("Dolphin Inc.", kontoDiverseDE)

    createIncomingInvoice("ACME Germany ltd.", kontoAcme)
    createIncomingInvoice("ACME Germ, ltd.", kontoAcme) // Should be ignored (same konto)

    createIncomingInvoice("Partner Systems ltd.", kontoPartner) // Should be added as partner.

    val context = MigrateTradingPartners.extractTradingVendors(incomingInvoices)
    val vendors = context.vendors
    // vendors.forEach { println("${it.number} ${it.company}: ${it.remarks}")}
    Assertions.assertEquals(5, vendors.size)
    val partners = context.partners
    // partners.forEach { println("${it.number} ${it.company}: ${it.remarks}")}
    Assertions.assertEquals(1, partners.size)

    createInvoice(kunde = createKunde(42, "Bravo limited", "Bravo", kontoCustomerBravo))
    createInvoice(projekt = createProjekt(createKunde(43, "Charlie Incorp.", "Charlie", kontoCustomerCharlie)))
    createInvoice(kunde = createKunde(44, "Delta limited", "Delta")).konto = kontoCustomerDelta
    createInvoice(kundeText = "Echo limited").konto = kontoCustomerDelta
    createInvoice(kundeText = "Foxtrott Inc.") // Ignored as customer, not much info.

    val customerAcme = createKunde(45, "ACME Germany ltd.", "ACME")
    customerAcme.konto = kontoAcme
    createInvoice(kunde = customerAcme)

    MigrateTradingPartners.extractTradingCustomers(invoices, context)
    val customers = context.customers
    // Assertions.assertEquals(4, customers.size)
    context.cleanUp()
    vendors.forEach { consoleOut(it) }
    partners.forEach { consoleOut(it) }
    customers.forEach { consoleOut(it) }
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
    println("${partner.type?.value}: ${partner.number} ${partner.company}: ${partner.remarks ?: "ohne Konto"}$shortName$importCode")
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

  private fun createKunde(number: Int, name: String, identifier: String? = null, konto: KontoDO? = null): KundeDO {
    val kunde = KundeDO()
    kunde.nummer = number
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
