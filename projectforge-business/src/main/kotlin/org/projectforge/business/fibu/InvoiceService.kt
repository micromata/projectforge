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

package org.projectforge.business.fibu

import de.micromata.merlin.utils.ReplaceUtils
import de.micromata.merlin.word.RunsProcessor
import de.micromata.merlin.word.WordDocument
import de.micromata.merlin.word.templating.Variables
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.commons.lang3.StringUtils
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableRow
import org.apache.xmlbeans.XmlException
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.util.*
import java.util.stream.Collectors

/**
 * Created by blumenstein on 08.05.17.
 * Migrated by Kai Reinhard
 */
@Service
open class InvoiceService {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Value("\${projectforge.invoiceTemplate}")
    protected open var customInvoiceTemplateName: String? = null

    /**
     * InvoiceTemplate.docx, InvoiceTemplate_Deutsch.docx, InvoiceTemplate_English results in ["", "Deutsch", "English"].
     */
    open fun getTemplateVariants(): Array<String> {
        val templateName = customInvoiceTemplateName
        if (templateName.isNullOrBlank()) {
            return arrayOf("")
        }
        val resourceDir = configurationService.resourceDirName
        val baseDir = File("$resourceDir/officeTemplates")
        return getTemplateVariants(baseDir.list(), customInvoiceTemplateName)
    }

    internal fun getTemplateVariants(files: Array<String>?, templateName: String?): Array<String> {
        if (templateName.isNullOrBlank()) {
            return arrayOf("")
        }
        val variants = mutableListOf<String>()
        files?.filter { it.startsWith(templateName) }?.forEach {
            val baseFilename = it.removePrefix(templateName).removeSuffix(".docx")
            if (baseFilename.isBlank()) {
                variants.add("")
            } else if (baseFilename.startsWith("")) {
                variants.add(baseFilename.substring(1))
            } else {
                log.warn("Language of invoice template '$it' not supported. Should be of form '$templateName.docx' or '${templateName}_xxxx.docx'.")
            }
        }
        return variants.sorted().toTypedArray()
    }

    open fun getInvoiceWordDocument(data: RechnungDO, variant: String?): ByteArrayOutputStream? {
        return try {
            var invoiceTemplate: Resource? = null
            val isSkonto = data.discountMaturity != null && data.discountPercent != null && data.discountZahlungsZielInTagen != null
            if (!customInvoiceTemplateName.isNullOrEmpty()) {
                val variantSuffix = if (variant.isNullOrBlank()) "" else "_$variant"
                invoiceTemplate = configurationService.getOfficeTemplateFile("$customInvoiceTemplateName$variantSuffix.docx",
                    "InvoiceTemplate.docx")
            }
            val variables = Variables()
            variables.put("table", "") // Marker for finding table (should be removed).
            variables.put("Rechnungsadresse", data.customerAddress)
            val type = if (variant.isNullOrEmpty()) {
                I18nHelper.getLocalizedMessage(data.typ?.i18nKey)
            } else {
                val locale = Locale(variant)
                I18nHelper.getLocalizedMessage(locale, data.typ?.i18nKey)
            }
            variables.put("Typ", type)
            variables.put("Kundenreferenz", data.customerref1)
            variables.put("Auftragsnummer", data.positionen!!.stream()
                    .filter { pos: RechnungsPositionDO -> pos.auftragsPosition != null && pos.auftragsPosition!!.auftrag != null }
                    .map { pos: RechnungsPositionDO -> pos.auftragsPosition!!.auftrag!!.nummer.toString() }
                    .distinct()
                    .collect(Collectors.joining(", ")))
            variables.put("VORNAME_NACHNAME", ThreadLocalUserContext.loggedInUser?.getFullname()?.uppercase() ?: "")
            variables.put("Rechnungsnummer", data.nummer?.toString() ?: "")
            variables.put("Rechnungsdatum", DateTimeFormatter.instance().getFormattedDate(data.datum))
            variables.put("Faelligkeit", DateTimeFormatter.instance().getFormattedDate(data.faelligkeit))
            variables.put("Anlage", getReplacementForAttachment(data))
            variables.put("isSkonto", isSkonto)
            if (isSkonto) {
                variables.put("Skonto", formatBigDecimal(data.discountPercent!!.stripTrailingZeros()) + "%")
                variables.put("Faelligkeit_Skonto", DateTimeFormatter.instance().getFormattedDate(data.discountMaturity))
            }
            variables.put("Zwischensumme", formatCurrencyAmount(data.info.netSum))
            variables.put("MwSt", formatCurrencyAmount(data.info.vatAmount))
            val sharedVat = extractSharedVat(data)
            if (sharedVat != null) {
                variables.put("MwStSatz", formatBigDecimal(sharedVat.multiply(NumberHelper.HUNDRED)))
            } else {
                variables.put("MwStSatz", "??????????")
            }
            variables.put("Gesamtbetrag", formatCurrencyAmount(data.info.grossSum))
            WordDocument(invoiceTemplate!!.inputStream, invoiceTemplate.file.name).use { document ->
                generatePosTableRows(document.document, data)
                document.process(variables)
                document.asByteArrayOutputStream
            }
        } catch (e: IOException) {
            log.error("Could not read invoice template", e)
            null
        }
    }

    /**
     * @return The VAT (percentage) of the positions of the given invoice if all positions have the same VAT, otherwise null.
     */
    internal fun extractSharedVat(data: RechnungDO): BigDecimal? {
        var sharedVat: BigDecimal? = null // Will only be set, if vat of all positions are equal.
        data.positionen?.let {
            for (pos in it) {
                if (pos.vat == null) {
                    // At least one position has no VAT amount! Can't determine VAT.
                    return null
                }
                if (sharedVat == null) {
                    sharedVat = pos.vat
                } else if (sharedVat != pos.vat) {
                    // At least one position has no VAT amount! Can't determine VAT.
                    return null
                }
            }
        }
        return sharedVat
    }

    private fun getReplacementForAttachment(data: RechnungDO): String {
        return if (!StringUtils.isEmpty(data.attachment)) {
            I18nHelper.getLocalizedMessage("fibu.attachment") + ":\r\n" + data.attachment
        } else {
            ""
        }
    }

    private fun formatCurrencyAmount(value: BigDecimal?): String {
        value ?: return ""
        return formatBigDecimal(value.setScale(2, RoundingMode.HALF_UP))
    }

    private fun formatBigDecimal(value: BigDecimal?): String {
        value ?: return ""
        val df = when {
            value.scale() == 0 -> DecimalFormat("#,##0")
            value.scale() == 1 -> DecimalFormat("#,##0.0")
            value.scale() == 2 -> DecimalFormat("#,##0.00")
            else -> DecimalFormat("#,###.#")
        }
        return df.format(value)
    }

    private fun getPeriodOfPerformance(position: RechnungsPositionDO, invoice: RechnungDO): String {
        val begin: LocalDate?
        val end: LocalDate?
        if (position.periodOfPerformanceType == PeriodOfPerformanceType.OWN) {
            begin = position.periodOfPerformanceBegin
            end = position.periodOfPerformanceEnd
        } else {
            begin = invoice.periodOfPerformanceBegin
            end = invoice.periodOfPerformanceEnd
        }
        return DateTimeFormatter.instance().getFormattedDate(begin) + " - " + DateTimeFormatter.instance().getFormattedDate(end)
    }

    private fun generatePosTableRows(templateDocument: XWPFDocument, invoice: RechnungDO): XWPFTable? {
        var posTbl: XWPFTable? = null
        for (tbl in templateDocument.tables) {
            val cell = tbl.getRow(0).getCell(0)
            cell.paragraphs?.let { paragraphs ->
                for (paragraph in paragraphs) {
                    val runsProcessor = RunsProcessor(paragraph)
                    if (runsProcessor.text.contains("\${table}")) {
                        posTbl = tbl
                        break
                    }
                }
            }
        }
        if (posTbl == null) {
            log.error("Table with marker '\${table}' in first row and first column not found. Can't process invoice positions.")
            return null
        }
        var rowCounter = 2
        for (position in invoice.positionen!!) {
            createInvoicePositionRow(posTbl, rowCounter++, invoice, position)
        }
        posTbl!!.removeRow(1)
        return posTbl
    }

    private fun createInvoicePositionRow(posTbl: XWPFTable?, rowCounter: Int, invoice: RechnungDO, position: RechnungsPositionDO) {
        try {
            val sourceRow = posTbl!!.getRow(1)
            val ctrow = CTRow.Factory.parse(sourceRow.ctRow.newInputStream())
            val newRow = XWPFTableRow(ctrow, posTbl)
            val variables = Variables()
            variables.put("id", position.number.toString())
            variables.put("Posnummer", position.number.toString())
            variables.put("Text", position.text)
            variables.put("Leistungszeitraum", getPeriodOfPerformance(position, invoice))
            variables.put("Menge", formatCurrencyAmount(position.menge))
            variables.put("Einzelpreis", formatCurrencyAmount(position.einzelNetto))
            variables.put("Betrag", formatCurrencyAmount(position.info.netSum))
            for (cell in newRow.tableCells) {
                for (cellParagraph in cell.paragraphs) {
                    RunsProcessor(cellParagraph).replace(variables)
                }
            }
            posTbl.addRow(newRow, rowCounter)
        } catch (ex: IOException) {
            log.error("Error while trying to copy row: " + ex.message, ex)
        } catch (ex: XmlException) {
            log.error("Error while trying to copy row: " + ex.message, ex)
        }
    }

    open fun getInvoiceFilename(invoice: RechnungDO?): String {
        val suffix = ".docx"
        if (invoice == null) {
            return suffix
        }
        //Rechnungsnummer_Kunde_Projekt_Betreff(mit Unterstrichen statt Leerzeichen)_Datum(2017-07-04)
        val number = if (invoice.nummer != null) invoice.nummer.toString() else ""
        var customer = if (invoice.kunde != null) "_" + invoice.kunde!!.name else ""
        if (StringUtils.isEmpty(customer)) {
            customer = if (invoice.kundeText != null) "_" + invoice.kundeText else ""
        }
        val project = if (invoice.projekt != null) "_" + invoice.projekt!!.name else ""
        val subject = if (invoice.betreff != null) "_" + invoice.betreff else ""
        val invoiceDate = "_" + PFDay.fromOrNow(invoice.datum).isoString
        return StringUtils.abbreviate(
                ReplaceUtils.encodeFilename(number + customer + project + subject + invoiceDate, true),
                "...", FILENAME_MAXLENGTH) + suffix
    }

    companion object {
        private val log = LoggerFactory.getLogger(InvoiceService::class.java)
        private const val FILENAME_MAXLENGTH = 100 // Higher values result in filename issues in Safari 13-
    }
}
