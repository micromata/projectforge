/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.excel.ExcelWorkbook
import org.projectforge.business.fibu.*
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * This class tries to extract trading partners from incoming and outgoing invoices and customers from
 * the database for creating an initial import for D-velop.
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
class ExtractPFTradingPartners {
  class Context {
    val vendors = mutableListOf<TradingPartner>()
    val customers = mutableListOf<TradingPartner>()
    private var sequence = 7000000

    val allPartners: List<TradingPartner>
      get() {
        val all = mutableListOf<TradingPartner>()
        all.addAll(vendors)
        all.addAll(customers)
        return all.sortedBy { it.number?.toIntOrNull() }
      }

    val nextVal: Int
      get() = sequence++

    fun getVendorByDatevKonto(konto: KontoDO): TradingPartner? {
      val kontoNumberString = konto.nummer?.toString() ?: return null
      vendors.find { it.importCode == kontoNumberString }?.let { return it }
      return null
    }

    /** Remove used importCode field during migration (not needed anymore, if not equal to number). */
    internal fun cleanUp() {
      vendors.filter { it.importCode != it.number }.forEach { it.importCode = null }
      customers.filter { it.importCode != it.number }.forEach { it.importCode = null }
    }
  }

  @Autowired
  private lateinit var eingangsrechnungDao: EingangsrechnungDao

  @Autowired
  private lateinit var kundeDao: KundeDao

  @Autowired
  private lateinit var rechnungDao: RechnungDao

  fun extractTradingPartners(): List<TradingPartner> {
    val fromDate = PFDay.now().beginOfYear.minusYears(5).date
    var filter: RechnungFilter = EingangsrechnungListFilter()
    filter.fromDate = fromDate
    val context = extractTradingVendors(eingangsrechnungDao.getList(filter))

    filter = RechnungListFilter()
    filter.fromDate = fromDate
    extractTradingCustomersInvoices(rechnungDao.getList(filter), context)

    extractTradingCustomers(kundeDao.internalLoadAll(), context)
    return context.allPartners
  }

  fun extractTradingPartnersAsExcel(dvelopTradingPartners: List<TradingPartner>): ByteArray {
    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale!!).use { workbook ->
      createTradingPartnersSheet(workbook, "D.velop TradingPartners", dvelopTradingPartners)
      createTradingPartnersSheet(workbook, "ProjectForge TradingPartners", extractTradingPartners())
      return workbook.asByteArrayOutputStream.toByteArray()
    }
  }

  private fun createTradingPartnersSheet(workbook: ExcelWorkbook, name: String, tradingPartners: List<TradingPartner>) {
    val sheet = workbook.createOrGetSheet(name)
    val boldFont = workbook.createOrGetFont("bold", bold = true)
    val boldStyle = workbook.createOrGetCellStyle("boldStyle")
    boldStyle.setFont(boldFont)
    val wrapStyle = workbook.createOrGetCellStyle("wrapText")
    wrapStyle.wrapText = true
    ExcelUtils.registerColumn(sheet, TradingPartner::number, 10)
    ExcelUtils.registerColumn(sheet, TradingPartner::datevKonto, 10)
    ExcelUtils.registerColumn(sheet, TradingPartner::importCode, 10)
    ExcelUtils.registerColumn(sheet, TradingPartner::type, 10)
    ExcelUtils.registerColumn(sheet, TradingPartner::company)
    ExcelUtils.registerColumn(sheet, TradingPartner::shortName)
    ExcelUtils.registerColumn(sheet, TradingPartner::remarks, 60)
    ExcelUtils.registerColumn(sheet, TradingPartner::active, 8)
    ExcelUtils.registerColumn(sheet, TradingPartner::billToStreet)
    ExcelUtils.registerColumn(sheet, TradingPartner::billToZip, 10)
    ExcelUtils.registerColumn(sheet, TradingPartner::billToCity)
    ExcelUtils.registerColumn(sheet, TradingPartner::billToCountry, 30)
    ExcelUtils.registerColumn(sheet, TradingPartner::billToAddressAdditional, 60)
    ExcelUtils.registerColumn(sheet, TradingPartner::contactType, 10)
    ExcelUtils.addHeadRow(sheet, boldStyle)
    tradingPartners.forEach { partner ->
      val row = sheet.createRow()
      row.autoFillFromObject(partner)
      ExcelUtils.getCell(row, TradingPartner::contactType)?.setCellValue(partner.contactType?.value?.toString())
      ExcelUtils.getCell(row, TradingPartner::type)?.setCellValue(partner.type?.value?.toString())
      ExcelUtils.getCell(row, TradingPartner::active)?.setCellValue(partner.active?.value?.toString())
      ExcelUtils.getCell(row, TradingPartner::remarks)?.setCellStyle(wrapStyle)
      ExcelUtils.getCell(row, TradingPartner::billToAddressAdditional)?.setCellStyle(wrapStyle)
    }
    sheet.setAutoFilter()
  }

  companion object {
    var organizationId: String = ""

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
          if (context.getVendorByDatevKonto(konto) != null) {
            return@forEach // Continue
          }
          val vendor = createVendor(kontoNumber.toString(), kreditor, konto)
          vendor.importCode = kontoNumber.toString()
          context.vendors.add(vendor)
        }
      }
      return context
    }

    internal fun extractTradingCustomersInvoices(invoices: List<RechnungDO>, context: Context) {
      invoices.forEach { invoice ->
        val kunde = invoice.kunde ?: invoice.projekt?.kunde
        if (kunde != null) {
          handleKunde(kunde, context, invoice)
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
          var existingCustomer = context.customers.find { it.number == kundeNumber }
          if (existingCustomer == null) {
            existingCustomer = context.getVendorByDatevKonto(konto)
            // Vendor is also customer -> partner:
            existingCustomer?.type = TradingPartner.Type(TradingPartner.TypeValue.PARTNER)
          }
          if (existingCustomer != null) {
            checkBillAddress(existingCustomer, invoice)
            return@forEach
          }
          val customer = createCustomer(invoice, kundeNumber, invoice.kundeText, konto = konto)
          context.customers.add(customer)
        }
      }
    }

    internal fun extractTradingCustomers(customers: List<KundeDO>, context: Context) {
      customers.forEach { kunde ->
        handleKunde(kunde, context)
      }
      context.cleanUp()
    }

    private fun handleKunde(kunde: KundeDO, context: Context, invoice: RechnungDO? = null) {
      val number = kunde.nummer
      val numberString = number?.toString() ?: return // shouldn't occur.
      val existingCustomer = context.customers.find { it.number == numberString }
      val konto = kunde.konto ?: invoice?.konto
      if (existingCustomer != null) {
        if (existingCustomer.datevKonto == null && konto != null) {
          existingCustomer.datevKonto = konto.nummer
          prependRemarks(existingCustomer, konto)
        }
        return
      }
      if (konto != null) {
        val existingPartner = context.getVendorByDatevKonto(konto)
        if (existingPartner != null) {
          // Vendor is also customer -> partner
          existingPartner.type = TradingPartner.Type(TradingPartner.TypeValue.PARTNER) // Must be partner
          existingPartner.number =
            kunde.nummer.toString() // Kundennummer ist nun die PF-Kundennummer (nicht mehr DATEV-Konto)
          existingPartner.shortName = kunde.identifier
          existingPartner.datevKonto = konto.nummer
          prependRemarks(existingPartner, konto)
          appendRemarks(existingPartner, kunde.description)
          checkBillAddress(existingPartner, invoice)
          return
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
        konto,
      )
      checkBillAddress(customer, invoice)
      context.customers.add(customer)
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
      if (partner.remarks?.contains(str) == true) {
        return
      }
      if (partner.remarks.isNullOrBlank()) {
        partner.remarks = str
      } else {
        partner.remarks = "$str\n${partner.remarks}"
      }
    }

    internal fun checkBillAddress(partner: TradingPartner, invoice: RechnungDO?) {
      invoice ?: return
      val customerAddress = invoice.customerAddress
      if (customerAddress.isNullOrBlank() || checkIfGiven(partner.billToCity, partner.billToZip)) {
        // Nothing to do.
        return
      }
      var lines = customerAddress.lines().filter { it.isNotBlank() } // lines of address without blank lines.
      if (lines.size < 2) {
        // Can't parse address.
        return
      }
      if (lineEquals(lines[0], "herr", "frau", "firma", "an", "portal", "klinikdienst")) {
        lines = lines.drop(1)
      }
      // Assuming first line is Customer's name
      var pos = lines.size - 1 // Start with last line
      // Last line is country or zip code with city:
      var zipCodeAndCity = extractZipCodeCity(lines[pos])
      var country: String? = null
      if (zipCodeAndCity == null) {
        country = lines[pos].trim()
        pos -= 1
        zipCodeAndCity = extractZipCodeCity(lines[pos]) ?: return // No zip code and city found.
      }
      pos -= 1
      if (pos < 0) {
        return // No street found.
      }
      val street = lines[pos].trim()
      pos -= 1
      val additionalAddressLines = mutableListOf<String>()
      while (pos > 0) {
        // An additional line is found before street.
        additionalAddressLines.add(0, lines[pos].trim())
        pos -= 1
      }
      if (additionalAddressLines.isNotEmpty()) {
        partner.billToAddressAdditional = additionalAddressLines.joinToString(" ")
      }
      partner.billToZip = zipCodeAndCity.first
      partner.billToCity = zipCodeAndCity.second
      partner.billToStreet = street
      partner.billToCountry = country
    }

    private fun lineEquals(line: String, vararg strs: String): Boolean {
      val trim = line.trim()
      strs.forEach {
        if (trim.equals(it, ignoreCase = true)) {
          return true
        }
      }
      return false
    }

    internal fun extractZipCodeCity(line: String): Pair<String, String>? {
      var str = line.trim()
      if (str.startsWith("D-") || str.startsWith("D ")) {
        str = str.substring(2)
      }
      val zipCodeMatch = "^[0-9 ]{5,10}".toRegex()
      val match = zipCodeMatch.find(str) ?: return null
      val zipCode = match.value.trim()
      val city = str.removePrefix(zipCode).trim()
      if (city.isBlank()) {
        return null
      }
      return Pair(zipCode, city)
    }

    private fun checkIfGiven(vararg str: String?): Boolean {
      return str.any { !it.isNullOrBlank() }
    }

    private fun createVendor(number: String, kreditor: String, konto: KontoDO): TradingPartner {
      val vendor = TradingPartner()
      vendor.number = number
      vendor.company = kreditor
      vendor.datevKonto = konto.nummer
      prependRemarks(vendor, konto)
      vendor.organization = TradingPartner.Organization(organizationId)
      vendor.type = TradingPartner.Type(TradingPartner.TypeValue.VENDOR)
      vendor.contactType = TradingPartner.ContactType(TradingPartner.ContactTypeValue.COMPANY)
      vendor.active = TradingPartner.Active(TradingPartner.ActiveValue.TRUE) // Nothing known about activity.
      return vendor
    }

    private fun createCustomer(
      invoice: RechnungDO?,
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
      customer.datevKonto = konto?.nummer
      prependRemarks(customer, konto)
      customer.number = number
      customer.importCode = number
      customer.organization = TradingPartner.Organization(organizationId)
      customer.type = TradingPartner.Type(TradingPartner.TypeValue.CUSTOMER)
      customer.contactType = TradingPartner.ContactType(TradingPartner.ContactTypeValue.COMPANY)
      customer.active = if (active) {
        TradingPartner.Active(TradingPartner.ActiveValue.TRUE)
      } else {
        TradingPartner.Active(TradingPartner.ActiveValue.FALSE)
      }
      appendRemarks(customer, kundeDescription)
      checkBillAddress(customer, invoice)
      return customer
    }
  }
}
