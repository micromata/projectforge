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

package org.projectforge.business.timesheet

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.Image
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.ColumnText
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfPageEventHelper
import com.lowagie.text.pdf.PdfWriter
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.common.OutputType
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.task.TaskFormatter.Companion.getTaskPath
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.PFDateTimeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * PDF export of the filtered timesheet list — the "PDF export" of the legacy list, reimplemented for the
 * REST/Next stack with OpenPDF instead of the wicket-bound Apache FOP path (`TimesheetListPage.exportPDF`).
 *
 * Same rows as [TimesheetExport] (the Excel export), plus the filter the list was narrowed by ([Context]).
 * The layout follows the old FOP PDF without copying it: a slim per-page header carrying the configured
 * organization and logo (both smaller than the old version, [HeaderEvent]), then on the first page a blue
 * title bar and the filter summary (period, search text, user, summed duration), then the landscape table
 * of the columns the list shows on screen.
 *
 * @author Kai Reinhard
 */
@Service
open class TimesheetListPdfExport {
    @Autowired
    private lateinit var dateTimeFormatter: DateTimeFormatter

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var configurationService: ConfigurationService

    /**
     * The filter the exported list was narrowed by, shown as the summary block on the first page. Every
     * field is optional: an unset one is simply left out of the block, so a full export reads without it.
     */
    class Context(
        /** ISO strings as the client sends them (see MagicFilter), formatted as dates here. */
        val periodFrom: String? = null,
        val periodTo: String? = null,
        val searchString: String? = null,
        /** The picked user's display name, as the object filter carries it. */
        val userName: String? = null,
    )

    /**
     * Exports the filtered list as a PDF, returning its bytes. Always a valid document, header row
     * included even for an empty result — so the download never yields a file that reads as broken.
     */
    open fun export(list: List<TimesheetDO>, context: Context = Context()): ByteArray {
        log.info("Exporting timesheet list as PDF.")
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15f, Color.WHITE)
        val labelFont = FontFactory.getFont(FontFactory.HELVETICA, 9f, Color.GRAY)
        val valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f)
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f)
        val cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8f)

        ByteArrayOutputStream().use { baos ->
            // Landscape, so the seven columns have room; the wide top margin leaves space for the per-page header.
            val document = Document(PageSize.A4.rotate(), 36f, 36f, 56f, 36f)
            val writer = PdfWriter.getInstance(document, baos)
            writer.pageEvent = HeaderEvent(organization(), logoImage())
            document.open()

            addTitleBar(document, titleFont)
            addFilterSummary(document, context, list, labelFont, valueFont)

            // Widths mirror the emphasis of the next list's columns (see timesheet.page.tsx): task path and
            // description are the wide ones, the timestamps and short fields the narrow ones.
            val table = PdfPTable(floatArrayOf(1.4f, 2.6f, 1.3f, 1.3f, 1.1f, 1.1f, 3.2f))
            table.widthPercentage = 100f
            table.headerRows = 1
            HEADER_KEYS.forEach { key -> table.addCell(headerCell(translate(key), headerFont)) }

            list.forEach { timesheet ->
                table.addCell(dataCell(userGroupCache.getUser(timesheet.userId)?.getFullname(), cellFont))
                table.addCell(dataCell(getTaskPath(timesheet.taskId, null, true, OutputType.PLAIN), cellFont))
                table.addCell(dataCell(dateTimeFormatter.getFormattedDateTime(timesheet.startTime), cellFont))
                table.addCell(dataCell(dateTimeFormatter.getFormattedDateTime(timesheet.stopTime), cellFont))
                table.addCell(dataCell(timesheet.location, cellFont))
                table.addCell(dataCell(timesheet.reference, cellFont))
                table.addCell(dataCell(timesheet.description, cellFont))
            }
            document.add(table)

            document.close()
            return baos.toByteArray()
        }
    }

    /** The blue bar carrying the list's title, as the legacy PDF heads its first page. */
    private fun addTitleBar(document: Document, titleFont: Font) {
        val bar = PdfPTable(1)
        bar.widthPercentage = 100f
        bar.addCell(PdfPCell(Phrase(translate("timesheet.title.list"), titleFont)).apply {
            backgroundColor = TITLE_BG
            border = Rectangle.NO_BORDER
            setPadding(6f)
        })
        bar.setSpacingAfter(8f)
        document.add(bar)
    }

    /**
     * The two-column summary of what the list was filtered by, plus the summed duration: the label/value
     * pairs the legacy PDF prints above the table. The on-screen AI share is deliberately left out of the
     * export.
     */
    private fun addFilterSummary(
        document: Document,
        context: Context,
        list: List<TimesheetDO>,
        labelFont: Font,
        valueFont: Font,
    ) {
        val stats = AITimeSavings.buildStats(list)
        val rows = mutableListOf<Pair<String, String>>()
        periodText(context)?.let { rows.add(translate("timePeriod") to it) }
        context.searchString?.takeIf { it.isNotBlank() }?.let { rows.add(translate("searchString") to it) }
        context.userName?.takeIf { it.isNotBlank() }?.let { rows.add(translate("timesheet.user") to it) }
        rows.add(translate("timesheet.totalDuration") to dateTimeFormatter.getPrettyFormattedDuration(stats.totalDurationMillis))
        // Two label/value pairs per row, borderless — the compact grid the reference PDF uses.
        val table = PdfPTable(floatArrayOf(1.1f, 2.6f, 1.1f, 2.6f))
        table.widthPercentage = 100f
        table.setSpacingAfter(10f)
        rows.forEach { (label, value) ->
            table.addCell(summaryCell(label, labelFont))
            table.addCell(summaryCell(value, valueFont))
        }
        if (rows.size % 2 != 0) { // Fill the trailing half-row so the grid stays rectangular.
            table.addCell(summaryCell("", labelFont))
            table.addCell(summaryCell("", labelFont))
        }
        document.add(table)
    }

    /** "01.07.2025 - 31.08.2025", or an open end where only one bound is set; null if neither is. */
    private fun periodText(context: Context): String? {
        val from = context.periodFrom?.let { formatDate(it) }
        val to = context.periodTo?.let { formatDate(it) }
        return when {
            from != null && to != null -> "$from - $to"
            from != null -> "$from - …"
            to != null -> "… - $to"
            else -> null
        }
    }

    private fun formatDate(iso: String): String? =
        PFDateTimeUtils.parseAndCreateDateTime(iso)?.utilDate?.let { dateTimeFormatter.getFormattedDate(it) }

    private fun organization(): String =
        StringUtils.defaultString(Configuration.instance.getStringValue(ConfigurationParam.ORGANIZATION))

    /** The configured custom logo, or null where none is set or readable (see ConfigurationService). */
    private fun logoImage(): Image? {
        if (!configurationService.isLogoFileValid) {
            return null
        }
        return try {
            Image.getInstance(configurationService.logoFileObject!!.absolutePath)
        } catch (ex: Exception) {
            log.warn("Can't load logo file for timesheet PDF export: ${ex.message}")
            null
        }
    }

    private fun summaryCell(text: String, font: Font): PdfPCell {
        return PdfPCell(Phrase(text, font)).apply {
            border = Rectangle.NO_BORDER
            setPadding(2f)
            verticalAlignment = Element.ALIGN_TOP
        }
    }

    private fun headerCell(text: String, font: Font): PdfPCell {
        val cell = PdfPCell(Phrase(text, font))
        cell.backgroundColor = HEADER_BG
        cell.setPadding(4f)
        return cell
    }

    private fun dataCell(text: String?, font: Font): PdfPCell {
        val cell = PdfPCell(Phrase(text ?: "", font))
        cell.setPadding(3f)
        cell.verticalAlignment = Element.ALIGN_TOP
        return cell
    }

    /**
     * Draws the slim running header on every page: the organization name at the left, the custom logo at
     * the right. Both deliberately small (the logo scaled to [LOGO_MAX_HEIGHT]), unlike the oversized old
     * FOP header — it should mark the page, not dominate it.
     */
    private class HeaderEvent(private val organization: String, private val logo: Image?) : PdfPageEventHelper() {
        override fun onEndPage(writer: PdfWriter, document: Document) {
            val orgFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color.GRAY)
            val top = document.top() + 30f // Into the top margin reserved for the header.
            if (organization.isNotBlank()) {
                ColumnText.showTextAligned(
                    writer.directContent, Element.ALIGN_LEFT, Phrase(organization, orgFont),
                    document.left(), top - 8f, 0f,
                )
            }
            logo?.let { image ->
                image.scaleToFit(LOGO_MAX_WIDTH, LOGO_MAX_HEIGHT)
                image.setAbsolutePosition(document.right() - image.scaledWidth, top - image.scaledHeight)
                writer.directContent.addImage(image)
            }
        }
    }

    companion object {
        /** Column headers, in the order of the next list's columns (see timesheet.page.tsx). */
        private val HEADER_KEYS = listOf(
            "timesheet.user", "task", "timesheet.startTime", "timesheet.stopTime",
            "timesheet.location", "timesheet.reference", "description",
        )
        private val HEADER_BG = Color(230, 230, 230)

        /** The ProjectForge blue of the on-screen list's title bar. */
        private val TITLE_BG = Color(0x1E, 0x5A, 0xA8)

        /** The running logo is kept small (see HeaderEvent), a fraction of the old FOP header's. */
        private const val LOGO_MAX_WIDTH = 130f
        private const val LOGO_MAX_HEIGHT = 22f
    }
}
