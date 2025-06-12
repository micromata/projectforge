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

package org.projectforge.rest.fibu

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.PaymentType
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val log = KotlinLogging.logger {}

/**
 * REST page for importing incoming invoices (Eingangsrechnungen) via drag and drop interface.
 * Analog to MyMenuPageRest but for invoice import.
 */
@RestController
@RequestMapping("${Rest.URL}/incomingInvoiceImport")
class EingangsrechnungImportRest : AbstractDynamicPageRest() {

    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val layout = createLayout(request)
        return FormLayoutData(null, layout, createServerData(request))
    }

    private fun createLayout(
        request: HttpServletRequest,
        result: String? = null,
        hasError: Boolean = false,
    ): UILayout {
        val layout = UILayout("fibu.eingangsrechnung.import.title")
        
        // Description and instructions
        layout.add(UIAlert("fibu.eingangsrechnung.import.description", markdown = true, color = UIColor.INFO))

        // Drop Area for file upload
        layout.add(
            UIDropArea(
                "fibu.eingangsrechnung.import.dropArea",
                uploadUrl = RestResolver.getRestUrl(EingangsrechnungImportRest::class.java, "upload")
            )
        )

        // CSV Template download
        layout.add(
            UIAlert("fibu.eingangsrechnung.import.templateInfo", markdown = true, color = UIColor.SECONDARY)
        )
        
        layout.addAction(
            UIButton.createDownloadButton(
                title = "fibu.eingangsrechnung.import.downloadTemplate",
                responseAction = ResponseAction(
                    RestResolver.getRestUrl(this.javaClass, "template"),
                    targetType = TargetType.DOWNLOAD
                )
            )
        )

        // Show result/error messages
        if (result != null) {
            layout.add(UIAlert(result, markdown = false, color = if (hasError) UIColor.DANGER else UIColor.SUCCESS))
        }

        LayoutUtils.process(layout)
        return layout
    }

    @PostMapping("upload")
    fun upload(
        request: HttpServletRequest,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<*> {
        val filename = file.originalFilename ?: "unknown"
        log.info {
            "User tries to upload invoice import file: '$filename', size=${file.size} bytes."
        }

        try {
            if (file.isEmpty) {
                return result(request, translate("file.upload.error.empty"), hasError = true)
            }

            // Check file extension
            val allowedExtensions = listOf("csv", "txt")
            val fileExtension = filename.substringAfterLast('.', "").lowercase()
            if (fileExtension !in allowedExtensions) {
                return result(
                    request, 
                    translate("fibu.eingangsrechnung.import.error.unsupportedFormat"), 
                    hasError = true
                )
            }

            // Check file size (max 10MB)
            if (file.size > 10 * 1024 * 1024) {
                return result(
                    request,
                    translate("file.upload.error.tooLarge"),
                    hasError = true
                )
            }

            // Process the file
            val result = processUploadedFile(file)
            return result(request, result.message, result.hasError)

        } catch (ex: Exception) {
            log.error("Error processing uploaded file: $filename", ex)
            return result(
                request,
                translate("fibu.eingangsrechnung.import.error.unexpected") + ": ${ex.message}",
                hasError = true
            )
        }
    }

    private fun processUploadedFile(file: MultipartFile): ProcessResult {
        var imported = 0
        var errors = 0
        val errorMessages = mutableListOf<String>()

        try {
            file.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val lines = reader.readLines()
                if (lines.isEmpty()) {
                    return ProcessResult("Keine Daten in der Datei gefunden", true)
                }

                // Parse header
                val headers = lines[0].split(",").map { it.trim('"', ' ') }
                
                // Process data rows
                for (i in 1 until lines.size) {
                    val line = lines[i]
                    if (line.isBlank()) continue

                    try {
                        val values = line.split(",").map { it.trim('"', ' ') }
                        val row = mutableMapOf<String, String?>()
                        
                        headers.forEachIndexed { index, header ->
                            row[header] = if (index < values.size) values[index].ifBlank { null } else null
                        }

                        val invoice = createInvoiceFromRow(row)
                        if (invoice != null) {
                            eingangsrechnungDao.insert(invoice)
                            imported++
                        }
                    } catch (ex: Exception) {
                        errors++
                        errorMessages.add("Zeile ${i + 1}: ${ex.message}")
                        log.warn("Error processing row ${i + 1}: ${ex.message}")
                    }
                }
            }

            val message = buildString {
                append("Import abgeschlossen: $imported Rechnungen importiert")
                if (errors > 0) {
                    append(", $errors Fehler")
                    if (errorMessages.isNotEmpty()) {
                        append(":\n${errorMessages.take(5).joinToString("\n")}")
                        if (errorMessages.size > 5) {
                            append("\n... und ${errorMessages.size - 5} weitere Fehler")
                        }
                    }
                }
            }

            return ProcessResult(message, errors > 0)

        } catch (ex: Exception) {
            log.error("Error processing CSV file", ex)
            return ProcessResult("Fehler beim Verarbeiten der CSV-Datei: ${ex.message}", true)
        }
    }

    private fun createInvoiceFromRow(row: Map<String, String?>): EingangsrechnungDO? {
        val invoice = EingangsrechnungDO()

        // Required fields
        invoice.kreditor = row["Kreditor"] ?: row["Supplier"] ?: row["kreditor"]
        invoice.referenz = row["Referenz"] ?: row["Reference"] ?: row["referenz"] ?: row["Rechnungsnummer"]
        invoice.betreff = row["Betreff"] ?: row["Subject"] ?: row["betreff"] ?: row["Description"]

        // Validation
        if (invoice.kreditor.isNullOrBlank()) {
            throw IllegalArgumentException("Kreditor/Supplier fehlt")
        }

        // Date fields
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val dateFormatterAlt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        invoice.datum = parseDate(row["Datum"] ?: row["Date"] ?: row["datum"], dateFormatter, dateFormatterAlt)
            ?: throw IllegalArgumentException("Datum fehlt oder ungültig")
        
        invoice.faelligkeit = parseDate(row["Fälligkeit"] ?: row["DueDate"] ?: row["faelligkeit"], dateFormatter, dateFormatterAlt)
        invoice.bezahlDatum = parseDate(row["Bezahldatum"] ?: row["PaymentDate"] ?: row["bezahlDatum"], dateFormatter, dateFormatterAlt)

        // Banking fields
        invoice.iban = row["IBAN"] ?: row["iban"]
        invoice.bic = row["BIC"] ?: row["bic"]
        invoice.receiver = row["Empfänger"] ?: row["Receiver"] ?: row["receiver"]

        // Payment type
        invoice.paymentType = parsePaymentType(row["Zahlungsart"] ?: row["PaymentType"] ?: row["paymentType"])

        // Customer number
        invoice.customernr = row["Kundennummer"] ?: row["CustomerNumber"] ?: row["customernr"]

        return invoice
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

    @GetMapping("template")
    fun downloadTemplate(): ResponseEntity<*> {
        log.info("User downloads CSV template for invoice import.")
        
        val csvTemplate = """Kreditor,Referenz,Betreff,Datum,Fälligkeit,Bezahldatum,IBAN,BIC,Empfänger,Zahlungsart,Kundennummer
"Musterfirma GmbH","RE-2025-001","Büromaterial","01.01.2025","31.01.2025","","DE89370400440532013000","COBADEFFXXX","Unsere Firma GmbH","BANK_TRANSFER","K12345"
"Lieferant AG","INV-2025-002","Software-Lizenz","02.01.2025","01.02.2025","","DE75512108001245126199","SOGEDEFFXXX","Unsere Firma GmbH","DEBIT","K67890"
"Service Partner","SRV-2025-003","Beratungsleistung","03.01.2025","02.02.2025","","","","Unsere Firma GmbH","BANK_TRANSFER","K13579""""

        return org.projectforge.rest.config.RestUtils.downloadFile(
            "eingangsrechnungen_template.csv", 
            csvTemplate.toByteArray(Charsets.UTF_8)
        )
    }

    private fun result(
        request: HttpServletRequest,
        text: String? = null,
        hasError: Boolean = false,
    ): ResponseEntity<*> {
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("ui", createLayout(request, text, hasError = hasError))
        )
    }

    private data class ProcessResult(val message: String, val hasError: Boolean)
}