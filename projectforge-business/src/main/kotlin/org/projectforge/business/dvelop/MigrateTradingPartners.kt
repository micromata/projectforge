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

package org.projectforge.business.dvelop

import org.projectforge.business.fibu.*
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * This class tries to extract trading partners from incoming and outgoing invoices and customers from
 * the database for creating an initial import for D-velop.
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
class MigrateTradingPartners {
  class Context {
    val vendors = mutableListOf<TradingPartner>()
    val partners = mutableListOf<TradingPartner>()
    val customers = mutableListOf<TradingPartner>()
    private var sequence = 100000

    val nextVal: Int
      get() = sequence++

    fun getVendorByDatevKonto(konto: KontoDO): TradingPartner? {
      val kontoNumberString = konto.nummer?.toString() ?: return null
      vendors.find { it.importCode == kontoNumberString }?.let { return it }
      partners.find { it.importCode == kontoNumberString }?.let { return it }
      return null
    }

    /** Remove used importCode field during migration (not needed anymore, if not equal to number). */
    fun cleanUp() {
      vendors.filter { it.importCode != it.number }.forEach { it.importCode = null }
      partners.filter { it.importCode != it.number }.forEach { it.importCode = null }
      customers.filter { it.importCode != it.number }.forEach { it.importCode = null }
    }
  }

  @Autowired
  private lateinit var eingangsrechnungDao: EingangsrechnungDao

  fun extractTradingVendors() {
    val filter = EingangsrechnungListFilter()
    filter.fromDate = PFDay.now().beginOfYear.minusYears(5).date
    extractTradingVendors(eingangsrechnungDao.getList(filter))
  }

  companion object {
    internal fun extractTradingVendors(invoices: List<EingangsrechnungDO>): Context {
      val context = Context()
      invoices.forEach { invoice ->
        val kreditor = invoice.kreditor
        if (kreditor.isNullOrBlank()) {
          return@forEach // Continue
        }
        val konto = invoice.konto ?: return@forEach // Continue
        val kontoNumber = konto.nummer ?: return@forEach // Continue
        val kontoBezeichnung = invoice.konto?.bezeichnung ?: ""
        if (kontoBezeichnung.contains("divers", ignoreCase = true)) {
          // Mehrere Kreditoren unter einem Konto vereint.
          if (context.vendors.any { it.company == kreditor }) {
            return@forEach // Continue
          }
          context.vendors.add(createVendor(context.nextVal.toString(), kreditor, konto))
        } else {
          if (context.vendors.any { it.importCode == kontoNumber.toString() }) {
            return@forEach // Continue
          }
          val vendor = createVendor(kontoNumber.toString(), kreditor, konto)
          if (kontoNumber >= 70000) {
            // Kreditor is customer as well! So mark as partner:
            vendor.type = TradingPartner.Type(TradingPartner.TypeValue.PARTNER)
            context.partners.add(vendor)
          } else {
            context.vendors.add(vendor)
          }
          vendor.importCode = kontoNumber.toString()
        }
      }
      return context
    }

    internal fun extractTradingCustomers(invoices: List<RechnungDO>, context: Context) {
      invoices.forEach { invoice ->
        val kunde = invoice.kunde ?: invoice.projekt?.kunde
        if (kunde != null) {
          val number = kunde.nummer
          val numberString = number?.toString() ?: return@forEach // shouldn't occur.
          val existingCustomer = context.customers.find { it.number == numberString }
          if (existingCustomer != null) {
            checkBillAddress(existingCustomer, invoice)
            return@forEach
          }
          val konto = kunde.konto
          if (konto != null) {
            val existingPartner = context.getVendorByDatevKonto(konto)
            if (existingPartner != null) {
              existingPartner.type = TradingPartner.Type(TradingPartner.TypeValue.PARTNER) // Must be partner
              existingPartner.number = kunde.nummer.toString() // Kundennummer ist nun die PF-Kundennummer (nicht mehr DATEV-Konto)
              existingPartner.shortName = kunde.identifier
              appendRemarks(existingPartner, kunde.description)
              checkBillAddress(existingPartner, invoice)
              return@forEach
            }
          }
          val status = kunde.status
          val active = status == null || status.isIn(KundeStatus.ACQUISISTION, KundeStatus.ACTIVE)
          val customer = createCustomer(
            invoice,
            kunde.nummer?.toString(),
            kunde.name,
            kunde.identifier,
            active,
            kunde.description,
            kunde.konto
          )
          checkBillAddress(customer, invoice)
          context.customers.add(customer)
        } else {
          // appendRemarks(customer, kunde.description)
          val konto = invoice.konto ?: return@forEach
          val kontoNumber = konto.nummer ?: return@forEach // Continue
          val kundeNumber = kontoNumber.toString()
          val kontoBezeichnung = invoice.konto?.bezeichnung ?: ""
          if (kontoBezeichnung.contains("divers", ignoreCase = true)) {
            // Keinen TradingPartner für diverse anlegen.
            return@forEach // Continue
          }
          val existingCustomer = context.customers.find { it.number == kundeNumber }
          if (existingCustomer != null) {
            checkBillAddress(existingCustomer, invoice)
            return@forEach
          }
          val customer = createCustomer(invoice, kundeNumber, invoice.kundeText, konto = konto)
          context.customers.add(customer)
        }
      }
    }

    private fun appendRemarks(partner: TradingPartner, remarks: String?) {
      remarks ?: return
      if (partner.remarks.isNullOrBlank()) {
        partner.remarks = remarks
      } else {
        partner.remarks = "${partner.remarks}\n$remarks"
      }
    }

    private fun prependRemarks(partner: TradingPartner, konto: KontoDO?) {
      konto ?: return
      val str = "${konto.nummer} ${konto.bezeichnung} (DATEV-Konto)"
      if (partner.remarks.isNullOrBlank()) {
        partner.remarks = str
      } else {
        partner.remarks = "$str\n${partner.remarks}"
      }
    }

    private fun checkBillAddress(partner: TradingPartner, invoice: RechnungDO) {
      println("*** TODO: checkBillAddress")
    }

    private fun createVendor(number: String, kreditor: String, konto: KontoDO): TradingPartner {
      val vendor = TradingPartner()
      vendor.number = number
      vendor.company = kreditor
      vendor.organization = TradingPartner.Organization("")
      vendor.type = TradingPartner.Type(TradingPartner.TypeValue.VENDOR)
      prependRemarks(vendor, konto)
      return vendor
    }

    private fun createCustomer(
      invoice: RechnungDO,
      number: String? = null,
      kundeName: String? = null,
      kundeShortName: String? = null,
      active: Boolean = true,
      kundeDescription: String? = null,
      konto: KontoDO? = null,
    ): TradingPartner {
      val customer = TradingPartner()
      customer.company = kundeName
      customer.shortName = kundeShortName
      customer.number = number
      customer.organization = TradingPartner.Organization("")
      customer.type = TradingPartner.Type(TradingPartner.TypeValue.CUSTOMER)
      customer.active = if (active) {
        TradingPartner.Active(TradingPartner.ActiveValue.TRUE)
      } else {
        TradingPartner.Active(TradingPartner.ActiveValue.FALSE)
      }
      prependRemarks(customer, konto)
      appendRemarks(customer, kundeDescription)
      checkBillAddress(customer, invoice)
      return customer
    }
  }
}
