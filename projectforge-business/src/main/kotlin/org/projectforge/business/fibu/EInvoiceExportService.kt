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

import de.micromata.merlin.word.WordDocument
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions
import mu.KotlinLogging
import org.mustangproject.BankDetails
import org.mustangproject.CashDiscount
import org.mustangproject.Contact
import org.mustangproject.FileAttachment
import org.mustangproject.Invoice
import org.mustangproject.Item
import org.mustangproject.Product
import org.mustangproject.TradeParty
import org.mustangproject.SchemedID
import org.mustangproject.ZUGFeRD.Profiles
import org.mustangproject.ZUGFeRD.XRExporter
import org.mustangproject.ZUGFeRD.ZUGFeRD2PullProvider
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromA3
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.jcr.RepoService
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

private val log = KotlinLogging.logger {}

@Service
class EInvoiceExportService(
    val sellerConfig: EInvoiceSellerConfig,
    private val invoiceService: InvoiceService,
    private val attachmentsService: AttachmentsService,
    private val repoService: RepoService,
) {
    companion object {
        const val JCR_PATH = "org.projectforge.rechnung"
    }

    fun exportAsXRechnung(invoice: RechnungDO): ByteArray {
        val validationErrors = validate(invoice)
        if (validationErrors.isNotEmpty()) {
            throw IllegalStateException(
                "Invoice ${invoice.nummer} cannot be exported as XRechnung: ${validationErrors.joinToString("; ")}"
            )
        }

        val mustangInvoice = buildMustangInvoice(invoice)
        val exporter = createXRExporter("XRechnung")
        exporter.setTransaction(mustangInvoice)

        val baos = ByteArrayOutputStream()
        exporter.export(baos)
        log.info { "XRechnung XML exported for invoice #${invoice.nummer}" }
        return baos.toByteArray()
    }

    fun exportAsZUGFeRD(invoice: RechnungDO): ByteArray {
        val validationErrors = validate(invoice)
        if (validationErrors.isNotEmpty()) {
            throw IllegalStateException(
                "Invoice ${invoice.nummer} cannot be exported as ZUGFeRD: ${validationErrors.joinToString("; ")}"
            )
        }

        val pdfBytes = generateInvoicePdf(invoice)
            ?: throw IllegalStateException("Could not generate PDF for invoice #${invoice.nummer} (no template configured?)")

        val mustangInvoice = buildMustangInvoice(invoice)

        val baos = ByteArrayOutputStream()
        val zugFeRDExporter = ZUGFeRDExporterFromA3()
            .ignorePDFAErrors()
            .load(ByteArrayInputStream(pdfBytes))
            .setProducer("ProjectForge")
            .setCreator("ProjectForge")
            .setZUGFeRDVersion(2)
            .setProfile(Profiles.getByName("EN16931"))
        zugFeRDExporter.setTransaction(mustangInvoice)
        zugFeRDExporter.export(baos)
        log.info { "ZUGFeRD PDF exported for invoice #${invoice.nummer}" }
        return embedAttachmentsInPdf(baos.toByteArray(), invoice)
    }

    private fun embedAttachmentsInPdf(pdfBytes: ByteArray, invoice: RechnungDO): ByteArray {
        val invoiceId = invoice.id ?: return pdfBytes
        val attachments = attachmentsService.internalGetAttachments(JCR_PATH, invoiceId)
        if (attachments.isEmpty()) return pdfBytes

        val doc = Loader.loadPDF(pdfBytes)
        try {
            val efTree = PDEmbeddedFilesNameTreeNode()
            val existingNames = mutableMapOf<String, PDComplexFileSpecification>()
            doc.documentCatalog.names?.embeddedFiles?.names?.let { existingNames.putAll(it) }

            attachments.forEach { attachment ->
                val fileId = attachment.fileId ?: return@forEach
                val nodePath = attachmentsService.getPath(JCR_PATH, invoiceId)
                val fileObject = repoService.getFileInfo(nodePath, AttachmentsService.DEFAULT_NODE, fileId = fileId)
                    ?: return@forEach
                if (!repoService.retrieveFile(fileObject)) return@forEach
                val content = fileObject.content ?: return@forEach
                val fileName = attachment.name ?: "attachment"
                val mimeType = java.net.URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"

                val embeddedFile = PDEmbeddedFile(doc, ByteArrayInputStream(content))
                embeddedFile.subtype = mimeType
                embeddedFile.size = content.size

                val fileSpec = PDComplexFileSpecification()
                fileSpec.file = fileName
                fileSpec.embeddedFile = embeddedFile

                existingNames[fileName] = fileSpec
                log.info { "Embedded attachment '$fileName' (${content.size} bytes) into ZUGFeRD PDF for invoice #${invoice.nummer}" }
            }

            efTree.names = existingNames
            val names = doc.documentCatalog.names ?: PDDocumentNameDictionary(doc.documentCatalog)
            names.embeddedFiles = efTree
            doc.documentCatalog.names = names

            val output = ByteArrayOutputStream()
            doc.save(output)
            return output.toByteArray()
        } finally {
            doc.close()
        }
    }

    private fun generateInvoicePdf(invoice: RechnungDO): ByteArray? {
        val docxStream = invoiceService.getInvoiceWordDocument(invoice, null) ?: return null
        val docxBytes = docxStream.toByteArray()
        ByteArrayInputStream(docxBytes).use { bais ->
            WordDocument(bais, "invoice.docx").use { word ->
                val options = PdfOptions.create()
                ByteArrayOutputStream().use { pdfBaos ->
                    PdfConverter.getInstance().convert(word.document, pdfBaos, options)
                    return pdfBaos.toByteArray()
                }
            }
        }
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
        if (street.isNullOrBlank()) {
            errors.add("Customer address/street is missing")
        } else if (zip.isNullOrBlank() || city.isNullOrBlank()) {
            if (!street.contains("\n") && !street.contains(",")) {
                errors.add("Customer address incomplete (zip and city required)")
            }
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

        // Delivery period (BR-FX-EN-04: required if no line-level periods)
        val deliveryBegin = invoice.periodOfPerformanceBegin ?: invoice.datum!!
        val deliveryEnd = invoice.periodOfPerformanceEnd ?: deliveryBegin
        mustangInvoice.setDetailedDeliveryPeriod(toDate(deliveryBegin), toDate(deliveryEnd))

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

        // Payment terms description (BR-CO-25: required if amount due is positive)
        val paymentTerms = buildPaymentTermsDescription(invoice)
        if (paymentTerms.isNotBlank()) {
            mustangInvoice.setPaymentTermDescription(paymentTerms)
        }

        // Line items
        invoice.positionen?.forEach { pos ->
            mustangInvoice.addItem(buildItem(pos, invoice))
        }

        // Embed file attachments from JCR
        embedAttachments(mustangInvoice, invoice)

        return mustangInvoice
    }

    private fun embedAttachments(mustangInvoice: Invoice, invoice: RechnungDO) {
        val invoiceId = invoice.id ?: return
        val attachments = attachmentsService.internalGetAttachments(JCR_PATH, invoiceId)
        attachments.forEach { attachment ->
            val fileId = attachment.fileId ?: return@forEach
            val nodePath = attachmentsService.getPath(JCR_PATH, invoiceId)
            val fileObject = repoService.getFileInfo(nodePath, AttachmentsService.DEFAULT_NODE, fileId = fileId)
                ?: return@forEach
            if (repoService.retrieveFile(fileObject)) {
                val content = fileObject.content ?: return@forEach
                val fileName = attachment.name ?: "attachment"
                val mimeType = java.net.URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"
                val fileAttachment = FileAttachment(fileName, mimeType, "Data", content)
                mustangInvoice.embedFileInXML(fileAttachment)
                log.debug { "Embedded attachment '$fileName' in invoice #${invoice.nummer}" }
            }
        }
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
        var street = invoice.customerAddress ?: kunde?.street ?: ""
        var zip = invoice.customerZipCode ?: kunde?.zipCode ?: ""
        var city = invoice.customerCity ?: kunde?.city ?: ""
        val country = invoice.customerCountry ?: kunde?.country ?: "DE"
        if (zip.isBlank() && city.isBlank() && street.contains("\n")) {
            val lines = street.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.size >= 2) {
                street = lines.dropLast(1).joinToString(", ")
                val lastLine = lines.last()
                val match = Regex("^(\\d{4,5})\\s+(.+)$").find(lastLine)
                if (match != null) {
                    zip = match.groupValues[1]
                    city = match.groupValues[2]
                } else {
                    city = lastLine
                }
            }
        }
        val buyer = TradeParty(
            kunde?.name ?: invoice.kundeText ?: "",
            street,
            zip,
            city,
            country
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
            ".",
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
        } else {
            parts.add("Zahlbar sofort.")
        }
        return parts.joinToString(" ")
    }

    private fun createXRExporter(profileName: String): XRExporter {
        val provider = ZUGFeRD2PullProvider().apply {
            setProfile(Profiles.getByName(profileName))
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

    fun getZUGFeRDExportFilename(invoice: RechnungDO): String {
        return "ZUGFeRD_${invoice.nummer ?: "draft"}.pdf"
    }
}
