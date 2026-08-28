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
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import mu.KotlinLogging
import org.projectforge.business.common.OutputType
import org.projectforge.business.task.TaskFormatter.Companion.getTaskPath
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.DateTimeFormatter
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
 * Same input as [TimesheetExport] (the Excel export), so [org.projectforge.rest.TimesheetPagesRest] calls
 * both the same way. The layout is deliberately built fresh (not pixel-identical to the old FOP PDF): a
 * landscape table with the columns the list shows on screen, and a footer with the summed duration and,
 * where the installation tracks it, the AI share — the two numbers the list's statistics line shows.
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

    /**
     * Exports the filtered list as a PDF, returning its bytes. Always a valid document, header row
     * included even for an empty result — so the download never yields a file that reads as broken.
     */
    open fun export(list: List<TimesheetDO>): ByteArray {
        log.info("Exporting timesheet list as PDF.")
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f)
        val subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9f, Color.DARK_GRAY)
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f)
        val cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8f)

        ByteArrayOutputStream().use { baos ->
            // Landscape, so the seven columns of the on-screen list have room to breathe.
            val document = Document(PageSize.A4.rotate(), 36f, 36f, 36f, 36f)
            PdfWriter.getInstance(document, baos)
            document.open()

            document.add(Paragraph(translate("timesheet.title.list"), titleFont))
            document.add(Paragraph(dateTimeFormatter.getFormattedDateTime(Date()), subtitleFont).apply {
                spacingAfter = 8f
            })

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

            // One pass for both numbers, exactly as the list footer computes them (AITimeSavings.buildStats).
            val stats = AITimeSavings.buildStats(list)
            val footer = StringBuilder()
                .append(translate("timesheet.totalDuration")).append(": ")
                .append(dateTimeFormatter.getPrettyFormattedDuration(stats.totalDurationMillis))
            if (AITimeSavings.timeSavingsByAIEnabled) {
                footer.append("     ").append(translate("timesheet.ai.timeSavedByAI")).append(": ")
                    .append(stats.percentageString)
            }
            document.add(Paragraph(footer.toString(), headerFont).apply { spacingBefore = 8f })

            document.close()
            return baos.toByteArray()
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

    companion object {
        /** Column headers, in the order of the next list's columns (see timesheet.page.tsx). */
        private val HEADER_KEYS = listOf(
            "timesheet.user", "task", "timesheet.startTime", "timesheet.stopTime",
            "timesheet.location", "timesheet.reference", "description",
        )
        private val HEADER_BG = Color(230, 230, 230)
    }
}
