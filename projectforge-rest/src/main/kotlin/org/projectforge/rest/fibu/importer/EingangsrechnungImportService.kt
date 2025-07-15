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

package org.projectforge.rest.fibu.importer

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.fibu.PaymentType
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.fibu.EingangsrechnungUploadPageRest
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val log = KotlinLogging.logger {}

@Service
class EingangsrechnungImportService {

    fun importFromCsv(request: HttpServletRequest, file: MultipartFile): String? {
        try {
            val importStorage = EingangsrechnungImportStorage()

            // Parse CSV file
            val csvData = parseCsvFile(file)
            if (csvData.isEmpty()) {
                importStorage.addError(translate("fibu.eingangsrechnung.import.error.noDataFound"))
                return null
            }

            // Convert CSV data to EingangsrechnungImport DTOs
            val invoices = convertCsvToInvoices(csvData, importStorage)
            //invoices.forEach { importStorage.addElement(it) }

            // Store in session
            ExpiringSessionAttributes.setAttribute(
                request,
                getSessionAttributeName(),
                importStorage,
                10 // TTL in minutes
            )

            log.info("Successfully imported ${invoices.size} invoices from CSV file: ${file.originalFilename}")

            // Return URL to import preview page
            return PagesResolver.getDynamicPageUrl(EingangsrechnungUploadPageRest::class.java, absolute = true)

        } catch (ex: Exception) {
            log.error("Error importing CSV file: ${file.originalFilename}", ex)
            return null
        }
    }

    private fun convertCsvToInvoices(csvData: List<Map<String, String?>>, importStorage: EingangsrechnungImportStorage): List<EingangsrechnungPosImportDTO> {
        val invoices = mutableListOf<EingangsrechnungPosImportDTO>()
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val dateFormatterAlt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        csvData.forEachIndexed { index, row ->
            try {
                val invoice = EingangsrechnungPosImportDTO()

                // Required fields
                invoice.kreditor = row["Kreditor"] ?: row["Supplier"] ?: row["kreditor"]
                invoice.referenz = row["Referenz"] ?: row["Reference"] ?: row["referenz"] ?: row["Rechnungsnummer"]
                invoice.betreff = row["Betreff"] ?: row["Subject"] ?: row["betreff"] ?: row["Description"]

                // Date fields
                invoice.datum = parseDate(row["Datum"] ?: row["Date"] ?: row["datum"], dateFormatter, dateFormatterAlt)
                invoice.faelligkeit = parseDate(row["Fälligkeit"] ?: row["DueDate"] ?: row["faelligkeit"], dateFormatter, dateFormatterAlt)
                invoice.bezahlDatum = parseDate(row["Bezahldatum"] ?: row["PaymentDate"] ?: row["bezahlDatum"], dateFormatter, dateFormatterAlt)

                // Amount fields
                //invoice.netSum = parseBigDecimal(row["Nettobetrag"] ?: row["NetAmount"] ?: row["netSum"])
                invoice.vatAmountSum = parseBigDecimal(row["MwSt"] ?: row["VAT"] ?: row["vatAmountSum"])
                invoice.grossSum = parseBigDecimal(row["Bruttobetrag"] ?: row["GrossAmount"] ?: row["grossSum"])
                invoice.zahlBetrag = parseBigDecimal(row["Zahlbetrag"] ?: row["PaymentAmount"] ?: row["zahlBetrag"])

                // If grossSum is missing but netSum and VAT are present, calculate it
                /*if (invoice.grossSum == null && invoice.netSum != null && invoice.vatAmountSum != null) {
                    invoice.grossSum = invoice.netSum!!.add(invoice.vatAmountSum)
                }*/

                // Banking fields
                invoice.iban = row["IBAN"] ?: row["iban"]
                invoice.bic = row["BIC"] ?: row["bic"]
                invoice.receiver = row["Empfänger"] ?: row["Receiver"] ?: row["receiver"]

                // Payment type
                invoice.paymentType = parsePaymentType(row["Zahlungsart"] ?: row["PaymentType"] ?: row["paymentType"])

                // Customer number
                invoice.customernr = row["Kundennummer"] ?: row["CustomerNumber"] ?: row["customernr"]

                // Remarks
                invoice.bemerkung = row["Bemerkung"] ?: row["Remarks"] ?: row["bemerkung"]

                // Validation
                if (invoice.kreditor.isNullOrBlank()) {
                    importStorage.addError("Row ${index + 1}: Missing required field 'Kreditor/Supplier'")
                    return@forEachIndexed
                }

                if (invoice.datum == null) {
                    importStorage.addError("Row ${index + 1}: Missing or invalid date field")
                    return@forEachIndexed
                }

                invoices.add(invoice)

            } catch (ex: Exception) {
                importStorage.addError("Row ${index + 1}: ${ex.message}")
                log.warn("Error parsing CSV row ${index + 1}: ${ex.message}")
            }
        }

        return invoices
    }

    private fun parseDate(dateStr: String?, vararg formatters: DateTimeFormatter): LocalDate? {
        if (dateStr.isNullOrBlank()) return null

        for (formatter in formatters) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter)
            } catch (ex: DateTimeParseException) {
                // Try next formatter
            }
        }

        log.warn("Could not parse date: $dateStr")
        return null
    }

    private fun parseBigDecimal(amountStr: String?): BigDecimal? {
        if (amountStr.isNullOrBlank()) return null

        try {
            // Remove currency symbols and normalize decimal separators
            val normalized = amountStr.trim()
                .replace("€", "")
                .replace("$", "")
                .replace(" ", "")
                .replace(",", ".")

            return BigDecimal(normalized)
        } catch (ex: NumberFormatException) {
            log.warn("Could not parse amount: $amountStr")
            return null
        }
    }

    private fun parsePaymentType(paymentTypeStr: String?): PaymentType? {
        if (paymentTypeStr.isNullOrBlank()) return null

        return try {
            PaymentType.valueOf(paymentTypeStr.uppercase().trim())
        } catch (ex: IllegalArgumentException) {
            // Try to match common payment type names
            when (paymentTypeStr.lowercase().trim()) {
                "überweisung", "transfer", "wire", "bank_transfer" -> PaymentType.BANK_TRANSFER
                "lastschrift", "debit", "direct debit" -> PaymentType.DEBIT
                "kreditkarte", "credit card", "credit_card" -> PaymentType.CREDIT_CARD
                "bar", "cash" -> PaymentType.CASH
                "gehalt", "salary" -> PaymentType.SALARY
                "kredit", "credit" -> PaymentType.CREDIT
                else -> {
                    log.warn("Unknown payment type: $paymentTypeStr")
                    null
                }
            }
        }
    }

    private fun parseCsvFile(file: MultipartFile): List<Map<String, String?>> {
        val result = mutableListOf<Map<String, String?>>()

        file.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val lines = reader.readLines()
            if (lines.isEmpty()) return result

            // Parse header
            val headers = lines[0].split(",").map { it.trim('"', ' ') }

            // Parse data rows
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isBlank()) continue

                val values = line.split(",").map { it.trim('"', ' ') }
                val row = mutableMapOf<String, String?>()

                headers.forEachIndexed { index, header ->
                    row[header] = if (index < values.size) values[index].ifBlank { null } else null
                }

                result.add(row)
            }
        }

        return result
    }

    private fun getSessionAttributeName(): String {
        return EingangsrechnungUploadPageRest::class.java.simpleName
    }
}
