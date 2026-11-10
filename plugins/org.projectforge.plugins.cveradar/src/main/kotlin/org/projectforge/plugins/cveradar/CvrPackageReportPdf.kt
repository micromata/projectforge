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

package org.projectforge.plugins.cveradar

import com.lowagie.text.Chunk
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.ColumnText
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfPageEventHelper
import com.lowagie.text.pdf.PdfWriter
import org.projectforge.plugins.cveradar.PackageReportTexts.text
import org.projectforge.plugins.cveradar.rest.PackageReport
import org.projectforge.plugins.cveradar.rest.PackageVulnerability
import org.projectforge.plugins.cveradar.rest.ReportPackage
import java.awt.Color
import java.io.ByteArrayOutputStream

/**
 * The PDF of a [PackageReport] ("CVE report by package"), with iText 2 (com.lowagie, as `TimesheetListPdfExport`):
 * the tiles of the severities, the overview of the packages, then per package its vulnerabilities with score,
 * description, the affected range and a link to the NVD or the advisory. Made of the package report only.
 */
object CvrPackageReportPdf {
    private val TEXT = Color(0x21, 0x25, 0x29)
    private val MUTED = Color(0x6C, 0x75, 0x7D)
    private val LINK = Color(0x1F, 0x5F, 0xA8)
    private val HEADER_BG = Color(0xEE, 0xF1, 0xF4)
    private val RULE = Color(0xDE, 0xE2, 0xE6)

    private val titleFont = font(FontFactory.HELVETICA_BOLD, 18f)
    private val subtitleFont = font(FontFactory.HELVETICA, 8f, MUTED)
    private val headingFont = font(FontFactory.HELVETICA_BOLD, 11f)
    private val headingVersionFont = font(FontFactory.HELVETICA_BOLD, 11f, MUTED)
    private val headerFont = font(FontFactory.HELVETICA_BOLD, 8f)
    private val cellFont = font(FontFactory.HELVETICA, 8f)
    private val cellBoldFont = font(FontFactory.HELVETICA_BOLD, 8f)
    private val smallFont = font(FontFactory.HELVETICA, 6.5f, MUTED)
    private val smallItalicFont = font(FontFactory.HELVETICA_OBLIQUE, 6.5f, MUTED)
    private val linkFont = Font(Font.HELVETICA, 8f, Font.BOLD or Font.UNDERLINE, LINK)
    private val tileNumberFont = font(FontFactory.HELVETICA_BOLD, 14f, Color.WHITE)
    private val tileLabelFont = font(FontFactory.HELVETICA_BOLD, 7f, Color.WHITE)
    private val scoreFont = font(FontFactory.HELVETICA_BOLD, 9f, Color.WHITE)
    private val scoreLabelFont = font(FontFactory.HELVETICA_BOLD, 6.5f, Color.WHITE)

    private fun font(name: String, size: Float, color: Color = TEXT): Font = FontFactory.getFont(name, size, color)

    fun render(report: PackageReport): ByteArray {
        ByteArrayOutputStream().use { out ->
            val document = Document(PageSize.A4, 40f, 40f, 40f, 48f)
            val writer = PdfWriter.getInstance(document, out)
            writer.pageEvent = FooterEvent(text("footer", PackageReportTexts.date(report.generated)))
            document.addTitle(text("title"))
            document.open()
            document.add(Paragraph(text("title"), titleFont))
            document.add(Paragraph(subtitle(report), subtitleFont).apply { spacingAfter = 8f })
            document.add(tiles(report))
            if (report.packages.isEmpty()) {
                document.add(Paragraph(text("empty"), cellFont))
            } else {
                document.add(overview(report))
                document.add(Paragraph(text("footnote.fixVersion"), smallFont).apply { spacingAfter = 10f })
                report.packages.forEach { document.add(packageTable(it)) }
                document.add(Paragraph(text("legend"), smallFont).apply { spacingBefore = 6f })
            }
            if (report.excludedCveIds.isNotEmpty()) {
                document.add(Paragraph(text("excluded", report.excludedCveIds.joinToString(", ")), smallFont))
            }
            document.close()
            return out.toByteArray()
        }
    }

    private fun subtitle(report: PackageReport): String = listOfNotNull(
        PackageReportTexts.scope(report),
        text("anonymized").takeIf { report.anonymized },
        text("generated", PackageReportTexts.dateTime(report.generated)),
        text("counts", report.vulnerabilityCount, report.packageCount),
    ).joinToString("  ·  ")

    /** One coloured tile per severity with its number. */
    private fun tiles(report: PackageReport): PdfPTable {
        val table = PdfPTable(PackageReportTexts.TILES.size)
        table.widthPercentage = 60f
        table.horizontalAlignment = Element.ALIGN_LEFT
        table.setSpacingAfter(10f)
        PackageReportTexts.TILES.forEach { severity ->
            val phrase = Phrase()
            phrase.add(Chunk("${report.severityCounts[severity] ?: 0}\n", tileNumberFont))
            phrase.add(Chunk(PackageReportTexts.severity(severity), tileLabelFont))
            table.addCell(PdfPCell(phrase).apply {
                backgroundColor = PackageReportTexts.color(severity)
                border = Rectangle.BOX
                borderColor = Color.WHITE
                borderWidth = 2f
                horizontalAlignment = Element.ALIGN_CENTER
                setPadding(5f)
            })
        }
        return table
    }

    private fun overview(report: PackageReport): PdfPTable {
        val table = PdfPTable(floatArrayOf(3.4f, 1f, 0.8f, 0.7f, 1.4f, 1.4f))
        table.widthPercentage = 100f
        table.headerRows = 1
        listOf("column.package", "column.version", "column.type", "column.count", "column.maxScore")
            .forEach { table.addCell(headerCell(text(it))) }
        table.addCell(headerCell(text("column.fixVersion") + "¹"))
        report.packages.forEach { pkg ->
            table.addCell(cell(Phrase(pkg.name, cellBoldFont)))
            table.addCell(cell(Phrase(pkg.version ?: "", cellFont)))
            table.addCell(cell(Phrase(pkg.ecosystemLabel ?: "", cellFont)))
            table.addCell(cell(Phrase(pkg.vulnerabilityCount.toString(), cellFont)))
            table.addCell(cell(Phrase().apply {
                val color = PackageReportTexts.color(pkg.maxSeverity)
                add(Chunk(PackageReportTexts.score(pkg.maxScore) + " ", font(FontFactory.HELVETICA_BOLD, 8f, color)))
                add(Chunk(PackageReportTexts.severity(pkg.maxSeverity), font(FontFactory.HELVETICA, 8f, color)))
                if (pkg.kev) add(Chunk("  KEV", font(FontFactory.HELVETICA_BOLD, 7f, PackageReportTexts.color("CRITICAL"))))
            }))
            table.addCell(cell(Phrase(PackageReportTexts.fixVersion(pkg.fixVersion, pkg.withoutFixCount), cellFont)))
        }
        return table
    }

    /**
     * The vulnerabilities of a package; its title is part of the header, so it is never separated from the table and
     * shown again where the table goes on on the next page.
     */
    private fun packageTable(pkg: ReportPackage): PdfPTable {
        val table = PdfPTable(floatArrayOf(1.5f, 0.75f, 5f))
        table.widthPercentage = 100f
        table.headerRows = 2
        table.setSpacingAfter(8f)
        val title = Paragraph()
        title.add(Chunk(pkg.name, headingFont))
        pkg.version?.let { title.add(Chunk(" $it", headingVersionFont)) }
        val details = Paragraph(
            listOfNotNull(
                pkg.ecosystemLabel,
                if (pkg.vulnerabilityCount == 1) text("vulnerability") else text("vulnerabilities", pkg.vulnerabilityCount),
                pkg.components.takeIf { it.isNotEmpty() }?.let { text("foundIn", it.joinToString(", ")) },
            ).joinToString("  ·  "),
            smallFont,
        )
        table.addCell(PdfPCell().apply {
            colspan = 3
            border = Rectangle.NO_BORDER
            setPadding(0f)
            paddingBottom = 4f
            addElement(title)
            addElement(details)
        })
        listOf("column.id", "column.score", "column.description").forEach { table.addCell(headerCell(text(it))) }
        pkg.vulnerabilities.forEach { vulnerability ->
            table.addCell(cell(Phrase()).apply {
                addElement(Paragraph(Chunk(vulnerability.id, linkFont).setAnchor(vulnerability.url)))
                addElement(Paragraph(vulnerability.source, font(FontFactory.HELVETICA_BOLD, 6.5f, MUTED)).apply { spacingBefore = 1f })
            })
            table.addCell(scoreCell(vulnerability))
            table.addCell(cell(description(vulnerability)))
        }
        return table
    }

    private fun scoreCell(vulnerability: PackageVulnerability): PdfPCell {
        val phrase = Phrase()
        phrase.add(Chunk(PackageReportTexts.score(vulnerability.cvssScore) + "\n", scoreFont))
        phrase.add(Chunk(PackageReportTexts.severity(vulnerability.severity), scoreLabelFont))
        return PdfPCell(phrase).apply {
            backgroundColor = PackageReportTexts.color(vulnerability.severity)
            border = Rectangle.BOTTOM
            borderColor = Color.WHITE
            horizontalAlignment = Element.ALIGN_CENTER
            verticalAlignment = Element.ALIGN_MIDDLE
            setPadding(4f)
        }
    }

    private fun description(vulnerability: PackageVulnerability): Phrase {
        val phrase = Phrase()
        phrase.add(Chunk(vulnerability.description?.trim().orEmpty(), cellFont))
        vulnerability.kevAdded?.let {
            phrase.add(Chunk("\n" + text("kev", PackageReportTexts.date(it)), font(FontFactory.HELVETICA_BOLD, 7f, PackageReportTexts.color("CRITICAL"))))
        }
        PackageReportTexts.case(vulnerability)?.let { phrase.add(Chunk("\n" + it, font(FontFactory.HELVETICA_BOLD, 7f, TEXT))) }
        val details = listOfNotNull(vulnerability.affectedRange?.let { text("affected", it) })
        phrase.add(Chunk("\n" + details.joinToString("  ·  "), smallFont))
        if (vulnerability.possible) {
            phrase.add(Chunk((if (details.isEmpty()) "" else "  ·  ") + text("possible") + "²", smallItalicFont))
        }
        return phrase
    }

    private fun headerCell(text: String) = PdfPCell(Phrase(text, headerFont)).apply {
        backgroundColor = HEADER_BG
        border = Rectangle.BOTTOM
        borderColor = RULE
        setPadding(5f)
    }

    private fun cell(phrase: Phrase) = PdfPCell(phrase).apply {
        border = Rectangle.BOTTOM
        borderColor = RULE
        verticalAlignment = Element.ALIGN_TOP
        setPadding(5f)
        setLeading(0f, 1.25f)
    }

    /** The footer of every page: the title with the date at the left, the page number at the right. */
    private class FooterEvent(private val label: String) : PdfPageEventHelper() {
        override fun onEndPage(writer: PdfWriter, document: Document) {
            val y = document.bottom() - 24f
            ColumnText.showTextAligned(writer.directContent, Element.ALIGN_LEFT, Phrase(label, smallFont), document.left(), y, 0f)
            ColumnText.showTextAligned(
                writer.directContent, Element.ALIGN_RIGHT, Phrase(text("page", writer.pageNumber), smallFont), document.right(), y, 0f,
            )
        }
    }
}
