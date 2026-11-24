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

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.Image
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.common.FilenameUtils
import org.projectforge.framework.i18n.translate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.awt.Color
import java.io.ByteArrayOutputStream

private val log = KotlinLogging.logger {}

/**
 * The customer data sheet of an account ("Kunden-Datenabgleich"): a PDF listing the data an invoice to the account
 * is written with, as the e-invoice fields (BT-…) of [KontoDO] — what is known filled in, the rest left blank. Finance
 * sends it to the customer after an order came in, so the customer can check it and complete what is missing before
 * the first invoice is written (fewer invoices to correct, and maintained data for follow-up orders).
 *
 * Every row has three columns: the field (with its BT number, as the account form labels it), the value as it is
 * stored, and an empty column for the customer's correction. The fields of the order and the invoice that the
 * account doesn't carry (contract number BT-12, order number BT-13, payment target) are listed blank, for the
 * customer to fill in.
 *
 * IText 2 (com.lowagie, as xdocreport brings it), like [org.projectforge.business.timesheet.TimesheetListPdfExport].
 * The texts are the ones of the current user's language, written without direct address.
 *
 * @author Kai Reinhard
 */
@Service
open class KontoCustomerDataSheetPdfExport {
    @Autowired
    private lateinit var sellerConfig: EInvoiceSellerConfig

    @Autowired
    private lateinit var configurationService: ConfigurationService

    /** One printed row: the field's label and its stored value, which may be blank. */
    internal class Row(val label: String, val value: String?)

    /** A titled block of rows. */
    internal class Section(val title: String, val rows: List<Row>)

    /**
     * The sheet of [konto] as PDF bytes. Always a valid document: a blank account yields the sheet with empty
     * value columns, which the customer fills in from scratch.
     */
    open fun export(konto: KontoDO): ByteArray {
        log.info { "Exporting customer data sheet of account #${konto.nummer}." }
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f)
        val introFont = FontFactory.getFont(FontFactory.HELVETICA, 9f)
        val sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f)
        val labelFont = FontFactory.getFont(FontFactory.HELVETICA, 9f)
        val valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f)
        val grayFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.GRAY)

        ByteArrayOutputStream().use { baos ->
            val document = Document(PageSize.A4, 40f, 40f, 40f, 40f)
            PdfWriter.getInstance(document, baos)
            document.open()

            addTitleBar(document, translate("fibu.konto.customerDataSheet.title"), titleFont)
            addSellerBlock(document, grayFont)
            document.add(Paragraph(translate("fibu.konto.customerDataSheet.intro"), introFont).apply {
                spacingAfter = 10f
            })
            for (section in sections(konto)) {
                document.add(Paragraph(section.title, sectionFont).apply {
                    spacingBefore = 8f
                    spacingAfter = 3f
                })
                document.add(sectionTable(section, headerFont, labelFont, valueFont))
            }
            document.add(Paragraph(translate("fibu.konto.customerDataSheet.confirmation"), grayFont).apply {
                spacingBefore = 28f
            })
            document.add(blankLine())
            document.close()
            return baos.toByteArray()
        }
    }

    /** The download's name: `<prefix>_<account number>_<name>.pdf`. */
    open fun filename(konto: KontoDO): String {
        val name = FilenameUtils.createSafeFilename(konto.bezeichnung ?: "", maxlength = 30)
        return listOfNotNull(
            translate("fibu.konto.customerDataSheet.filenamePrefix"),
            konto.nummer?.toString(),
            name.takeIf { it.isNotBlank() },
        ).joinToString("_") + ".pdf"
    }

    /**
     * The blocks of the sheet, in reading order. Internal for the test, which checks what is listed and which values
     * are carried without parsing the PDF.
     */
    internal fun sections(konto: KontoDO): List<Section> = listOf(
        Section(
            translate("fibu.konto.customerDataSheet.section.recipient"),
            listOf(
                Row(translate("fibu.konto.bezeichnung"), konto.bezeichnung),
                Row(translate("fibu.konto.street"), konto.street),
                Row(translate("fibu.konto.zipCode"), konto.zipCode),
                Row(translate("fibu.konto.city"), konto.city),
                Row(translate("fibu.konto.country"), konto.country),
                Row(translate("fibu.konto.vatId"), konto.vatId),
            ),
        ),
        Section(
            translate("fibu.konto.customerDataSheet.section.contact"),
            listOf(
                Row(translate("fibu.konto.contactPerson"), konto.contactPerson),
                Row(translate("fibu.konto.contactDepartment"), konto.contactDepartment),
                Row(translate("fibu.konto.contactEmail"), konto.contactEmail),
            ),
        ),
        Section(
            translate("fibu.konto.customerDataSheet.section.eInvoice"),
            listOf(
                Row(translate("fibu.konto.eInvoiceEmail"), konto.eInvoiceEmail),
                Row(translate("fibu.konto.leitwegId"), konto.leitwegId),
            ),
        ),
        Section(
            translate("fibu.konto.customerDataSheet.section.references"),
            listOf(
                Row(translate("fibu.konto.costCenter"), konto.costCenter),
                // Not carried by the account, they belong to the order: listed blank for the customer to fill in.
                Row(translate("fibu.auftrag.customerOrderNumber"), null),
                Row(translate("fibu.auftrag.contractNumber"), null),
                Row(translate("fibu.rechnung.zahlungsZiel"), null),
            ),
        ),
    )

    /**
     * The configured custom logo at the top right and the title below it, both on the white page: no colored bar, so the
     * sheet brings no color scheme of its own — the logo is the only branding. Without a (valid) logo the title stands alone.
     */
    private fun addTitleBar(document: Document, title: String, titleFont: Font) {
        logoImage()?.let { logo ->
            logo.scaleToFit(LOGO_MAX_WIDTH, LOGO_MAX_HEIGHT)
            logo.alignment = Image.ALIGN_RIGHT
            document.add(logo)
        }
        document.add(Paragraph(title, titleFont).apply {
            spacingBefore = 6f
            spacingAfter = 10f
        })
    }

    /** The configured custom logo, or null where none is set or readable (see [ConfigurationService]). */
    private fun logoImage(): Image? {
        if (!configurationService.isLogoFileValid) {
            return null
        }
        return try {
            Image.getInstance(configurationService.logoFileObject!!.absolutePath)
        } catch (ex: Exception) {
            log.warn { "Can't load logo file for the customer data sheet: ${ex.message}" }
            null
        }
    }

    /** The seller, as far as configured ([EInvoiceSellerConfig]), so the customer knows whom to send the sheet back to. */
    private fun addSellerBlock(document: Document, font: Font) {
        val lines = listOf(
            sellerConfig.name,
            sellerConfig.street,
            listOf(sellerConfig.zip, sellerConfig.city).filter { it.isNotBlank() }.joinToString(" "),
            sellerConfig.email,
            sellerConfig.phone,
        ).filter { it.isNotBlank() }
        if (lines.isNotEmpty()) {
            document.add(Paragraph(lines.joinToString(", "), font).apply { spacingAfter = 8f })
        }
    }

    private fun sectionTable(section: Section, headerFont: Font, labelFont: Font, valueFont: Font): PdfPTable {
        val table = PdfPTable(floatArrayOf(2.4f, 2.6f, 2.6f))
        table.widthPercentage = 100f
        table.headerRows = 1
        listOf("field", "current", "correction").forEach {
            table.addCell(
                PdfPCell(Phrase(translate("fibu.konto.customerDataSheet.column.$it"), headerFont)).apply {
                    backgroundColor = HEADER_BG
                    setPadding(4f)
                },
            )
        }
        for (row in section.rows) {
            table.addCell(cell(row.label, labelFont))
            table.addCell(cell(row.value, valueFont))
            table.addCell(cell(null, labelFont))
        }
        return table
    }

    /** A cell tall enough to write into by hand, also where it is empty. */
    private fun cell(text: String?, font: Font): PdfPCell {
        return PdfPCell(Phrase(text ?: "", font)).apply {
            setPadding(4f)
            minimumHeight = 22f
            verticalAlignment = Element.ALIGN_MIDDLE
        }
    }

    private fun blankLine(): PdfPTable {
        val line = PdfPTable(1)
        line.widthPercentage = 60f
        line.horizontalAlignment = Element.ALIGN_LEFT
        line.addCell(PdfPCell(Phrase(" ")).apply {
            border = Rectangle.BOTTOM
            minimumHeight = 24f
        })
        return line
    }

    companion object {
        private val HEADER_BG = Color(230, 230, 230)

        private const val LOGO_MAX_WIDTH = 75f
        private const val LOGO_MAX_HEIGHT = 20f
    }
}
