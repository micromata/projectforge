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
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.mustangproject.FileAttachment
import org.mustangproject.Invoice
import org.mustangproject.TradeParty
import org.mustangproject.ZUGFeRD.ZUGFeRDImporter
import org.mustangproject.ZUGFeRD.ZUGFeRDInvoiceImporter
import org.mustangproject.ZUGFeRD.XRechnungImporter
import org.projectforge.framework.utils.NumberHelper
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date

private val log = KotlinLogging.logger {}

@Service
class EInvoiceReadService {

    data class ParseResult(
        val invoiceData: EInvoiceData,
        val attachmentBytes: Map<Int, ByteArray> = emptyMap(),
    )

    fun parseFile(filename: String, content: ByteArray): ParseResult {
        return if (filename.endsWith(".pdf", ignoreCase = true)) {
            parseZUGFeRD(content)
        } else {
            parseXRechnung(content)
        }
    }

    private fun parseZUGFeRD(content: ByteArray): ParseResult {
        val importer = ZUGFeRDImporter(ByteArrayInputStream(content))
        if (!importer.canParse()) {
            return ParseResult(
                EInvoiceData(
                    validationErrors = listOf("No ZUGFeRD/Factur-X data found in PDF."),
                    format = "PDF (no e-invoice data)",
                )
            )
        }

        val invoice = extractInvoice(importer)
        val profile = importer.zugFeRDProfil

        val pdfAttachments = try { importer.fileAttachmentsPDF ?: emptyList() } catch (e: Exception) { emptyList() }

        val attachmentBytes = mutableMapOf<Int, ByteArray>()
        val seen = mutableSetOf<String>()
        val attachmentList = mutableListOf<EInvoiceAttachment>()
        var idx = 0
        for (att in pdfAttachments) {
            val key = "${att.filename}:${att.data?.size}"
            if (!seen.add(key)) continue
            att.data?.let { attachmentBytes[idx] = it }
            val attSize = att.data?.size ?: 0
            attachmentList.add(
                EInvoiceAttachment(
                    id = idx + 1,
                    filename = att.filename ?: "attachment_$idx",
                    mimeType = att.mimetype,
                    description = att.description,
                    size = attSize,
                    sizeFormatted = NumberHelper.formatBytes(attSize),
                    index = idx,
                )
            )
            idx++
        }

        var invoiceData = buildInvoiceData(invoice, importer).copy(
            format = "ZUGFeRD",
            profile = profile,
            attachments = attachmentList,
        )

        // Check if seller IBAN from XML is present in the PDF text
        val sellerIban = invoiceData.seller?.iban
        if (!sellerIban.isNullOrBlank()) {
            val pdfText = extractPdfText(content)
            if (pdfText != null && !pdfText.contains(sellerIban.replace(" ", ""))) {
                invoiceData = invoiceData.copy(
                    warnings = invoiceData.warnings +
                        "Die IBAN des Verkäufers ($sellerIban) aus den XML-Daten konnte im PDF-Dokument nicht gefunden werden (bitte prüfen!)."
                )
            }
        }

        return ParseResult(invoiceData, attachmentBytes)
    }

    private fun parseXRechnung(content: ByteArray): ParseResult {
        val importer = XRechnungImporter(content)
        val invoice = extractInvoice(importer)
        val profile = importer.zugFeRDProfil

        val xmlAttachments = try {
            importer.fileAttachmentsXML ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val attachmentBytes = mutableMapOf<Int, ByteArray>()
        val attachmentList = xmlAttachments.mapIndexed { index, att ->
            att.data?.let { attachmentBytes[index] = it }
            val attSize = att.data?.size ?: 0
            EInvoiceAttachment(
                id = index + 1,
                filename = att.filename ?: "attachment_$index",
                mimeType = att.mimetype,
                description = att.description,
                size = attSize,
                sizeFormatted = NumberHelper.formatBytes(attSize),
                index = index,
            )
        }

        val standard = try {
            importer.standard?.name ?: "XRechnung"
        } catch (e: Exception) {
            "XRechnung"
        }

        val invoiceData = buildInvoiceData(invoice, importer).copy(
            format = standard,
            profile = profile,
            attachments = attachmentList,
        )

        return ParseResult(invoiceData, attachmentBytes)
    }

    private fun extractInvoice(importer: ZUGFeRDImporter): Invoice? {
        return try {
            importer.extractInvoice()
        } catch (e: Exception) {
            log.warn("Failed to extract invoice object: ${e.message}", e)
            null
        }
    }

    private fun buildInvoiceData(invoice: Invoice?, importer: ZUGFeRDImporter): EInvoiceData {
        val validationErrors = mutableListOf<String>()

        if (invoice == null) {
            return EInvoiceData(
                invoiceNumber = safeGet { importer.invoiceID },
                issueDate = safeGet { importer.issueDate },
                dueDate = safeGet { importer.dueDate },
                currency = safeGet { importer.invoiceCurrencyCode },
                documentTypeCode = safeGet { importer.documentCode },
                buyerReference = safeGet { importer.reference },
                paymentTerms = safeGet { importer.paymentTerms },
                totalNetAmount = parseBigDecimal(safeGet { importer.lineTotalAmount }),
                totalGrossAmount = parseBigDecimal(safeGet { importer.amount }),
                totalTaxAmount = parseBigDecimal(safeGet { importer.taxTotalAmount }),
                seller = buildPartyFromImporter(importer, isSeller = true),
                buyer = buildPartyFromImporter(importer, isSeller = false),
                validationErrors = validationErrors,
            )
        }

        val seller = invoice.sender?.let { buildParty(it) }
        val buyer = invoice.recipient?.let { buildParty(it) }

        val lineItems = invoice.zfItems?.mapIndexed { index, item ->
            val qty = item.quantity
            val price = item.price
            val netAmount = if (qty != null && price != null) qty.multiply(price) else null
            EInvoiceLineItem(
                id = index + 1,
                position = index + 1,
                description = item.product?.name ?: item.product?.description,
                quantity = qty,
                unit = item.product?.unit,
                unitPrice = price,
                netAmount = netAmount,
                vatPercent = item.product?.vatPercent,
            )
        } ?: emptyList()

        return EInvoiceData(
            invoiceNumber = invoice.number ?: safeGet { importer.invoiceID },
            issueDate = formatDate(invoice.issueDate) ?: safeGet { importer.issueDate },
            dueDate = formatDate(invoice.dueDate) ?: safeGet { importer.dueDate },
            currency = invoice.currency ?: safeGet { importer.invoiceCurrencyCode },
            documentTypeCode = invoice.documentCode ?: safeGet { importer.documentCode },
            buyerReference = invoice.referenceNumber ?: safeGet { importer.reference },
            orderReference = invoice.buyerOrderReferencedDocumentID,
            paymentTerms = invoice.paymentTermDescription ?: safeGet { importer.paymentTerms },
            deliveryDate = formatDate(invoice.deliveryDate),
            seller = seller,
            buyer = buyer,
            totalNetAmount = parseBigDecimal(safeGet { importer.lineTotalAmount }),
            totalGrossAmount = parseBigDecimal(safeGet { importer.amount }),
            totalTaxAmount = parseBigDecimal(safeGet { importer.taxTotalAmount }),
            amountDue = parseBigDecimal(safeGet { importer.amount }),
            lineItems = lineItems,
            validationErrors = validationErrors,
        ).let { data ->
            data.copy(validationErrors = data.validationErrors + validate(data))
        }
    }

    private fun validate(data: EInvoiceData): List<String> {
        val errors = mutableListOf<String>()
        if (data.invoiceNumber.isNullOrBlank()) {
            errors.add("[BT-1] Invoice number is missing.")
        }
        if (data.issueDate.isNullOrBlank()) {
            errors.add("[BT-2] Invoice issue date is missing.")
        }
        if (data.documentTypeCode.isNullOrBlank()) {
            errors.add("[BT-3] Invoice type code is missing.")
        }
        if (data.currency.isNullOrBlank()) {
            errors.add("[BT-5] Invoice currency code is missing.")
        }
        if (data.seller?.name.isNullOrBlank()) {
            errors.add("[BT-27] Seller name is missing.")
        }
        if (data.buyer?.name.isNullOrBlank()) {
            errors.add("[BT-44] Buyer name is missing.")
        }
        if (data.buyerReference.isNullOrBlank()) {
            errors.add("[BR-DE-15] Buyer reference (BT-10) is missing.")
        }
        if (data.seller?.city.isNullOrBlank()) {
            errors.add("[BR-DE-3] Seller city (BT-37) is missing.")
        }
        if (data.seller?.zip.isNullOrBlank()) {
            errors.add("[BR-DE-4] Seller post code (BT-38) is missing.")
        }
        if (data.buyer?.city.isNullOrBlank()) {
            errors.add("[BR-DE-7] Buyer city (BT-52) is missing.")
        }
        if (data.buyer?.zip.isNullOrBlank()) {
            errors.add("[BR-DE-8] Buyer post code (BT-53) is missing.")
        }
        if (data.lineItems.isEmpty()) {
            errors.add("[BR-16] An invoice shall have at least one invoice line.")
        }
        return errors
    }

    private fun buildParty(party: TradeParty): EInvoiceParty {
        val bankDetails = party.bankDetails?.firstOrNull()
        return EInvoiceParty(
            name = party.name,
            street = party.street,
            zip = party.zip,
            city = party.location,
            country = party.country,
            vatId = party.vatid,
            email = null,
            contactName = null,
            iban = bankDetails?.iban,
            bic = bankDetails?.bic,
        )
    }

    private fun buildPartyFromImporter(importer: ZUGFeRDImporter, isSeller: Boolean): EInvoiceParty? {
        if (isSeller) {
            val address = safeGet { importer.sellerTradePartyAddress } ?: return null
            return EInvoiceParty(
                name = safeGet { importer.holder },
                street = address.lineOne,
                zip = address.postcodeCode,
                city = address.cityName,
                country = address.countryID,
                iban = safeGet { importer.iban },
                bic = safeGet { importer.bic },
            )
        } else {
            val address = safeGet { importer.buyerTradePartyAddress } ?: return null
            return EInvoiceParty(
                name = safeGet { importer.buyerTradePartyName },
                street = address.lineOne,
                zip = address.postcodeCode,
                city = address.cityName,
                country = address.countryID,
            )
        }
    }

    private fun formatDate(date: Date?): String? {
        if (date == null) return null
        return SimpleDateFormat("yyyy-MM-dd").format(date)
    }

    private fun parseBigDecimal(value: String?): BigDecimal? {
        if (value.isNullOrBlank()) return null
        return try {
            BigDecimal(value)
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun extractPdfText(content: ByteArray): String? {
        return try {
            Loader.loadPDF(content).use { document ->
                PDFTextStripper().getText(document).replace(" ", "")
            }
        } catch (e: Exception) {
            log.warn("Failed to extract PDF text for IBAN check: ${e.message}")
            null
        }
    }

    private fun <T> safeGet(block: () -> T?): T? {
        return try {
            block()
        } catch (e: Exception) {
            null
        }
    }
}
