/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu

import mu.KotlinLogging
import org.mustangproject.BankDetails
import org.mustangproject.CashDiscount
import org.mustangproject.Contact
import org.mustangproject.Invoice
import org.mustangproject.Item
import org.mustangproject.Product
import org.mustangproject.TradeParty
import org.mustangproject.SchemedID
import org.mustangproject.ZUGFeRD.Profiles
import org.mustangproject.ZUGFeRD.XRExporter
import org.mustangproject.ZUGFeRD.ZUGFeRD2PullProvider
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

private val log = KotlinLogging.logger {}

@Service
class EInvoiceExportService(
    val sellerConfig: EInvoiceSellerConfig,
) {
    fun exportAsXRechnung(invoice: RechnungDO): ByteArray {
        val validationErrors = validate(invoice)
        if (validationErrors.isNotEmpty()) {
            throw IllegalStateException(
                "Invoice ${invoice.nummer} cannot be exported as XRechnung: ${validationErrors.joinToString("; ")}"
            )
        }

        val mustangInvoice = buildMustangInvoice(invoice)
        val exporter = createXRExporter()
        exporter.setTransaction(mustangInvoice)

        val baos = ByteArrayOutputStream()
        exporter.export(baos)
        log.info { "XRechnung XML exported for invoice #${invoice.nummer}" }
        return baos.toByteArray()
    }

    fun validate(invoice: RechnungDO): List<String> {
        val errors = mutableListOf<String>()

        if (!sellerConfig.isConfigured()) {
            errors.add("Seller configuration incomplete (projectforge.einvoice.seller.*)")
        }
        if (invoice.nummer == null) {
            errors.add("Invoice number is missing")
        }
        if (invoice.datum == null) {
            errors.add("Invoice date is missing")
        }
        if (invoice.positionen.isNullOrEmpty()) {
            errors.add("Invoice has no positions")
        }

        if (sellerConfig.bankAccounts.isEmpty()) {
            errors.add("No bank accounts configured (projectforge.einvoice.seller.bankAccounts)")
        } else if (invoice.sellerBankAccount.isNullOrBlank()) {
            errors.add("No bank account selected for this invoice")
        } else if (sellerConfig.findBankAccount(invoice.sellerBankAccount) == null) {
            errors.add("Selected bank account '${invoice.sellerBankAccount}' not found in configuration")
        }

        val kunde = invoice.kunde
        val customerName = kunde?.name ?: invoice.kundeText
        if (customerName.isNullOrBlank()) {
            errors.add("Customer name is missing (no customer assigned and no customer text)")
        }
        val street = invoice.customerAddress ?: kunde?.street
        val zip = invoice.customerZipCode ?: kunde?.zipCode
        val city = invoice.customerCity ?: kunde?.city
        if (street.isNullOrBlank() || zip.isNullOrBlank() || city.isNullOrBlank()) {
            errors.add("Customer address incomplete (street, zip, city required)")
        }
        val buyerEmail = invoice.customerEInvoiceEmail ?: kunde?.eInvoiceEmail
        if (buyerEmail.isNullOrBlank()) {
            log.warn { "Buyer electronic address (e-invoice email) is missing for invoice #${invoice.nummer} – PEPPOL validation may warn." }
        }

        return errors
    }

    private fun buildMustangInvoice(invoice: RechnungDO): Invoice {
        val mustangInvoice = Invoice()
            .setNumber(invoice.nummer.toString())
            .setIssueDate(toDate(invoice.datum!!))
            .setCurrency(invoice.currency ?: "EUR")
            .setSender(buildSeller(invoice))
            .setRecipient(buildBuyer(invoice))

        invoice.faelligkeit?.let { mustangInvoice.setDueDate(toDate(it)) }

        // Document type code
        val documentCode = when (invoice.typ) {
            RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN -> "381"
            RechnungTyp.CANCELLATION -> "457"
            else -> "380"
        }
        mustangInvoice.setDocumentCode(documentCode)

        // Delivery period
        invoice.periodOfPerformanceBegin?.let { begin ->
            val end = invoice.periodOfPerformanceEnd ?: begin
            mustangInvoice.setDetailedDeliveryPeriod(toDate(begin), toDate(end))
        }

        // Buyer reference: Leitweg-ID (BT-10, required for XRechnung to public sector)
        val buyerReference = invoice.customerLeitwegId ?: invoice.kunde?.leitwegId ?: invoice.customerref1
        if (!buyerReference.isNullOrBlank()) {
            mustangInvoice.setReferenceNumber(buyerReference)
        }

        // Buyer order reference (BT-13)
        if (!invoice.customerref1.isNullOrBlank() && invoice.kunde?.leitwegId != null) {
            mustangInvoice.setBuyerOrderReferencedDocumentID(invoice.customerref1)
        }

        // Cash discount (Skonto)
        if (invoice.discountPercent != null && invoice.discountMaturity != null) {
            val days = java.time.temporal.ChronoUnit.DAYS.between(invoice.datum, invoice.discountMaturity).toInt()
            if (days > 0) {
                mustangInvoice.addCashDiscount(CashDiscount(invoice.discountPercent, days))
            }
        }

        // Payment terms description
        if (invoice.faelligkeit != null) {
            val paymentTerms = buildPaymentTermsDescription(invoice)
            if (paymentTerms.isNotBlank()) {
                mustangInvoice.setPaymentTermDescription(paymentTerms)
            }
        }

        // Line items
        invoice.positionen?.forEach { pos ->
            mustangInvoice.addItem(buildItem(pos, invoice))
        }

        return mustangInvoice
    }

    private fun buildSeller(invoice: RechnungDO): TradeParty {
        val seller = TradeParty(
            sellerConfig.name,
            sellerConfig.street,
            sellerConfig.zip,
            sellerConfig.city,
            sellerConfig.country
        )
        if (sellerConfig.vatId.isNotBlank()) {
            seller.addVATID(sellerConfig.vatId)
        }
        if (sellerConfig.taxNumber.isNotBlank()) {
            seller.addTaxID(sellerConfig.taxNumber)
        }
        if (sellerConfig.email.isNotBlank()) {
            seller.setEmail(sellerConfig.email)
        }
        val contactName = sellerConfig.contactName.ifBlank { sellerConfig.name }
        seller.setContact(Contact(contactName, sellerConfig.phone, sellerConfig.email))
        val bankAccount = sellerConfig.findBankAccount(invoice.sellerBankAccount)
        if (bankAccount != null) {
            val bankDetails = BankDetails(bankAccount.iban, bankAccount.bic)
            if (bankAccount.name.isNotBlank()) {
                bankDetails.setAccountName(bankAccount.name)
            }
            seller.addBankDetails(bankDetails)
        }
        return seller
    }

    private fun buildBuyer(invoice: RechnungDO): TradeParty {
        val kunde = invoice.kunde
        val buyer = TradeParty(
            kunde?.name ?: invoice.kundeText ?: "",
            invoice.customerAddress ?: kunde?.street ?: "",
            invoice.customerZipCode ?: kunde?.zipCode ?: "",
            invoice.customerCity ?: kunde?.city ?: "",
            invoice.customerCountry ?: kunde?.country ?: "DE"
        )
        val vatId = invoice.customerVatId ?: kunde?.vatId
        if (!vatId.isNullOrBlank()) {
            buyer.addVATID(vatId)
        }
        val buyerEmail = invoice.customerEInvoiceEmail ?: kunde?.eInvoiceEmail
        if (!buyerEmail.isNullOrBlank()) {
            buyer.setEmail(buyerEmail)
            buyer.addUriUniversalCommunicationID(SchemedID("EM", buyerEmail))
        }
        return buyer
    }

    private fun buildItem(pos: RechnungsPositionDO, invoice: RechnungDO): Item {
        val vatPercent = pos.vat?.multiply(BigDecimal(100)) ?: BigDecimal.ZERO
        val product = Product(
            pos.text ?: "Position ${pos.number}",
            "",
            "C62",
            vatPercent
        )

        val item = Item(product, pos.einzelNetto ?: BigDecimal.ZERO, pos.menge ?: BigDecimal.ONE)
        item.setId(pos.number.toString())

        // Per-position delivery period
        val periodBegin = if (pos.periodOfPerformanceType == PeriodOfPerformanceType.OWN) {
            pos.periodOfPerformanceBegin
        } else {
            invoice.periodOfPerformanceBegin
        }
        val periodEnd = if (pos.periodOfPerformanceType == PeriodOfPerformanceType.OWN) {
            pos.periodOfPerformanceEnd
        } else {
            invoice.periodOfPerformanceEnd
        }
        if (periodBegin != null) {
            item.setDetailedDeliveryPeriod(toDate(periodBegin), toDate(periodEnd ?: periodBegin))
        }

        return item
    }

    private fun buildPaymentTermsDescription(invoice: RechnungDO): String {
        val parts = mutableListOf<String>()
        if (invoice.discountPercent != null && invoice.discountMaturity != null) {
            val days = java.time.temporal.ChronoUnit.DAYS.between(invoice.datum, invoice.discountMaturity).toInt()
            if (days > 0) {
                parts.add("${invoice.discountPercent}% Skonto bei Zahlung innerhalb $days Tagen.")
            }
        }
        if (invoice.faelligkeit != null) {
            val netDays = java.time.temporal.ChronoUnit.DAYS.between(invoice.datum, invoice.faelligkeit).toInt()
            parts.add("Zahlbar innerhalb $netDays Tagen.")
        }
        return parts.joinToString(" ")
    }

    private fun createXRExporter(): XRExporter {
        val provider = ZUGFeRD2PullProvider().apply {
            setProfile(Profiles.getByName("XRechnung"))
        }
        val exporter = XRExporter()
        val field = XRExporter::class.java.getDeclaredField("xmlProvider")
        field.isAccessible = true
        field.set(exporter, provider)
        return exporter
    }

    private fun toDate(localDate: LocalDate): Date {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    fun getExportFilename(invoice: RechnungDO): String {
        return "XRechnung_${invoice.nummer ?: "draft"}.xml"
    }
}
