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
class MigrateTradingPartners {
  class Context {
    val vendors = mutableListOf<TradingPartner>()
    val partners = mutableListOf<TradingPartner>()
    val customers = mutableListOf<TradingPartner>()
    private var sequence = 7000000

    val allPartners: List<TradingPartner>
      get() {
        val all = mutableListOf<TradingPartner>()
        all.addAll(vendors)
        all.addAll(partners)
        all.addAll(customers)
        return all.sortedBy { it.number?.toIntOrNull() }
      }

    val nextVal: Int
      get() = sequence++

    fun getVendorByDatevKonto(konto: KontoDO): TradingPartner? {
      val kontoNumberString = konto.nummer?.toString() ?: return null
      vendors.find { it.importCode == kontoNumberString }?.let { return it }
      partners.find { it.importCode == kontoNumberString }?.let { return it }
      return null
    }

    /** Remove used importCode field during migration (not needed anymore, if not equal to number). */
    internal fun cleanUp() {
      vendors.filter { it.importCode != it.number }.forEach { it.importCode = null }
      partners.filter { it.importCode != it.number }.forEach { it.importCode = null }
      customers.filter { it.importCode != it.number }.forEach { it.importCode = null }
    }
  }

  @Autowired
  private lateinit var eingangsrechnungDao: EingangsrechnungDao

  @Autowired
  private lateinit var rechnungDao: RechnungDao

  fun extractTradingPartnersAsExcel(): ByteArray {
    val fromDate = PFDay.now().beginOfYear.minusYears(5).date
    var filter: RechnungFilter = EingangsrechnungListFilter()
    filter.fromDate = fromDate
    val context = extractTradingVendors(eingangsrechnungDao.getList(filter))

    filter = RechnungListFilter()
    filter.fromDate = fromDate
    extractTradingCustomers(rechnungDao.getList(filter), context)

    ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale!!).use { workbook ->
      val sheet = workbook.createOrGetSheet("D-velop TradingPartners")
      val boldFont = workbook.createOrGetFont("bold", bold = true)
      val boldStyle = workbook.createOrGetCellStyle("boldStyle")
      boldStyle.setFont(boldFont)
      val wrapStyle = workbook.createOrGetCellStyle("wrapText")
      wrapStyle.wrapText = true
      ExcelUtils.registerColumn(sheet, TradingPartner::number)
      ExcelUtils.registerColumn(sheet, TradingPartner::type)
      ExcelUtils.registerColumn(sheet, TradingPartner::company)
      ExcelUtils.registerColumn(sheet, TradingPartner::shortName)
      ExcelUtils.registerColumn(sheet, TradingPartner::active)
      ExcelUtils.registerColumn(sheet, TradingPartner::billToStreet)
      ExcelUtils.registerColumn(sheet, TradingPartner::billToZip)
      ExcelUtils.registerColumn(sheet, TradingPartner::billToCity)
      ExcelUtils.registerColumn(sheet, TradingPartner::billToCountry)
      ExcelUtils.registerColumn(sheet, TradingPartner::billToAddressAdditional)
      ExcelUtils.registerColumn(sheet, TradingPartner::remarks)
      ExcelUtils.registerColumn(sheet, TradingPartner::contactType)
      ExcelUtils.registerColumn(sheet, TradingPartner::importCode)
      ExcelUtils.addHeadRow(sheet, boldStyle)
      context.allPartners.forEach { partner ->
        val row = sheet.createRow()
        row.autoFillFromObject(partner)
        ExcelUtils.getCell(row, TradingPartner::contactType)?.setCellValue(partner.contactType?.value?.toString())
        ExcelUtils.getCell(row, TradingPartner::type)?.setCellValue(partner.type?.value?.toString())
        ExcelUtils.getCell(row, TradingPartner::active)?.setCellValue(partner.active?.value?.toString())
        ExcelUtils.getCell(row, TradingPartner::remarks)?.setCellStyle(wrapStyle)
      }
      sheet.setAutoFilter()
      return workbook.asByteArrayOutputStream.toByteArray()
    }
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
          if (context.getVendorByDatevKonto(konto) != null) {
            return@forEach // Continue
          }
          val vendor = createVendor(kontoNumber.toString(), kreditor, konto)
          if (kontoNumber < 70000) {
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
              existingPartner.number =
                kunde.nummer.toString() // Kundennummer ist nun die PF-Kundennummer (nicht mehr DATEV-Konto)
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
      context.cleanUp()
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

    internal fun checkBillAddress(partner: TradingPartner, invoice: RechnungDO) {
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
