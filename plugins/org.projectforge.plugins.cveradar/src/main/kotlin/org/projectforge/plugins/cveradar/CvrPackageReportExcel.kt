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

import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.plugins.cveradar.PackageReportTexts.text
import org.projectforge.plugins.cveradar.rest.PackageReport

/**
 * The Excel file of a [PackageReport]: the sheet of the packages (as the overview of the PDF) and the sheet of the
 * vulnerabilities, one row per package and vulnerability, to filter and sort. The scores are numbers in the format
 * "General" (a custom format like `#,##0.##` shows "7," in a German Excel).
 */
object CvrPackageReportExcel {
    fun render(report: PackageReport): ByteArray {
        ExcelUtils.prepareWorkbook().use { workbook ->
            write(workbook.pOIWorkbook as XSSFWorkbook, report)
            return workbook.asByteArrayOutputStream.toByteArray()
        }
    }

    private class Styles(private val workbook: XSSFWorkbook) {
        val header: CellStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply { bold = true })
            setFillForegroundColor(XSSFColor(byteArrayOf(0xEE.toByte(), 0xF1.toByte(), 0xF4.toByte())))
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val bold: CellStyle = workbook.createCellStyle().apply { setFont(workbook.createFont().apply { bold = true }) }
        val wrap: CellStyle = workbook.createCellStyle().apply {
            wrapText = true
            verticalAlignment = VerticalAlignment.TOP
        }
        val link: CellStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                underline = org.apache.poi.ss.usermodel.Font.U_SINGLE
                color = org.apache.poi.ss.usermodel.IndexedColors.BLUE.index
            })
        }
        private val severities = mutableMapOf<String?, CellStyle>()

        /** White bold text on the colour of the severity. */
        fun severity(severity: String?): CellStyle = severities.getOrPut(severity) {
            val fill = PackageReportTexts.color(severity)
            (workbook.createCellStyle() as XSSFCellStyle).apply {
                setFillForegroundColor(XSSFColor(byteArrayOf(fill.red.toByte(), fill.green.toByte(), fill.blue.toByte())))
                fillPattern = FillPatternType.SOLID_FOREGROUND
                setFont(workbook.createFont().apply {
                    bold = true
                    setColor(XSSFColor(byteArrayOf(-1, -1, -1)))
                })
            }
        }
    }

    internal fun write(workbook: XSSFWorkbook, report: PackageReport) {
        val styles = Styles(workbook)
        packagesSheet(workbook, report, styles)
        vulnerabilitiesSheet(workbook, report, styles)
    }

    private fun packagesSheet(workbook: XSSFWorkbook, report: PackageReport, styles: Styles) {
        val sheet = workbook.createSheet(text("sheet.packages"))
        var rowIndex = 0
        sheet.createRow(rowIndex++).createCell(0).apply {
            setCellValue(text("title"))
            setCellStyle(styles.bold)
        }
        sheet.createRow(rowIndex++).createCell(0).setCellValue(
            listOfNotNull(
                PackageReportTexts.scope(report),
                text("anonymized").takeIf { report.anonymized },
                text("generated", PackageReportTexts.dateTime(report.generated)),
                text("counts", report.vulnerabilityCount, report.packageCount),
            ).joinToString("  ·  "),
        )
        sheet.createRow(rowIndex++).let { row ->
            PackageReportTexts.TILES.forEachIndexed { index, severity ->
                row.createCell(index * 2).apply {
                    setCellValue(PackageReportTexts.severity(severity))
                    setCellStyle(styles.severity(severity))
                }
                row.createCell(index * 2 + 1).setCellValue((report.severityCounts[severity] ?: 0).toDouble())
            }
        }
        rowIndex++
        val headers = listOf(
            "column.package", "column.version", "column.type", "column.count", "column.maxScore", "column.severity",
            "column.kev", "column.fixVersion", "column.withoutFix", "column.foundIn", "column.purl",
        )
        val headerIndex = rowIndex
        header(sheet.createRow(rowIndex++), headers, styles)
        report.packages.forEach { pkg ->
            val row = sheet.createRow(rowIndex++)
            row.createCell(0).setCellValue(pkg.name)
            row.createCell(1).setCellValue(pkg.version ?: "")
            row.createCell(2).setCellValue(pkg.ecosystemLabel ?: "")
            row.createCell(3).setCellValue(pkg.vulnerabilityCount.toDouble())
            pkg.maxScore?.let { row.createCell(4).setCellValue(it.toDouble()) }
            row.createCell(5).apply {
                setCellValue(PackageReportTexts.severity(pkg.maxSeverity))
                setCellStyle(styles.severity(pkg.maxSeverity))
            }
            row.createCell(6).setCellValue(if (pkg.kev) translate("yes") else "")
            row.createCell(7).setCellValue(pkg.fixVersion ?: "")
            row.createCell(8).setCellValue(pkg.withoutFixCount.toDouble())
            row.createCell(9).setCellValue(pkg.components.joinToString(", "))
            row.createCell(10).setCellValue(pkg.purl ?: "")
        }
        finish(sheet, headerIndex, rowIndex, intArrayOf(40, 12, 10, 8, 10, 12, 6, 14, 10, 40, 50))
        val footnote = sheet.createRow(rowIndex + 1).createCell(0)
        footnote.setCellValue(text("footnote.fixVersion").removePrefix("¹").trim())
        if (report.excludedCveIds.isNotEmpty()) {
            sheet.createRow(rowIndex + 2).createCell(0)
                .setCellValue(text("excluded", report.excludedCveIds.joinToString(", ")))
        }
    }

    private fun vulnerabilitiesSheet(workbook: XSSFWorkbook, report: PackageReport, styles: Styles) {
        val sheet = workbook.createSheet(text("sheet.vulnerabilities"))
        val headers = listOf(
            "column.package", "column.version", "column.type", "column.id", "column.source", "column.score",
            "column.severity", "column.affected", "column.fixVersion", "column.possible", "column.kev", "column.case",
            "column.foundIn", "column.description",
        )
        header(sheet.createRow(0), headers, styles)
        var rowIndex = 1
        val helper = workbook.creationHelper
        report.packages.forEach { pkg ->
            pkg.vulnerabilities.forEach { vulnerability ->
                val row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(pkg.name)
                row.createCell(1).setCellValue(pkg.version ?: "")
                row.createCell(2).setCellValue(pkg.ecosystemLabel ?: "")
                row.createCell(3).apply {
                    setCellValue(vulnerability.id)
                    hyperlink = helper.createHyperlink(HyperlinkType.URL).also { it.address = vulnerability.url }
                    setCellStyle(styles.link)
                }
                row.createCell(4).setCellValue(vulnerability.source)
                vulnerability.cvssScore?.let { row.createCell(5).setCellValue(it.toDouble()) }
                row.createCell(6).apply {
                    setCellValue(PackageReportTexts.severity(vulnerability.severity))
                    setCellStyle(styles.severity(vulnerability.severity))
                }
                row.createCell(7).setCellValue(vulnerability.affectedRange ?: "")
                row.createCell(8).setCellValue(vulnerability.fixVersion ?: "")
                row.createCell(9).setCellValue(if (vulnerability.possible) translate("yes") else "")
                row.createCell(10).setCellValue(vulnerability.kevAdded?.let { PackageReportTexts.date(it) } ?: "")
                row.createCell(11).setCellValue(PackageReportTexts.case(vulnerability) ?: "")
                row.createCell(12).setCellValue(pkg.components.joinToString(", "))
                row.createCell(13).apply {
                    setCellValue(vulnerability.description?.trim()?.take(MAX_CELL_TEXT) ?: "")
                    setCellStyle(styles.wrap)
                }
            }
        }
        finish(sheet, 0, rowIndex, intArrayOf(36, 12, 10, 20, 14, 7, 12, 22, 12, 10, 12, 30, 36, 90))
    }

    private fun header(row: Row, keys: List<String>, styles: Styles) {
        keys.forEachIndexed { index, key ->
            row.createCell(index).apply {
                setCellValue(text(key))
                setCellStyle(styles.header)
            }
        }
    }

    /** The widths in characters, the header row frozen, an auto filter over the rows. */
    private fun finish(sheet: Sheet, headerIndex: Int, endIndex: Int, widths: IntArray) {
        widths.forEachIndexed { index, width -> sheet.setColumnWidth(index, width * 256) }
        sheet.createFreezePane(0, headerIndex + 1)
        sheet.setAutoFilter(CellRangeAddress(headerIndex, (endIndex - 1).coerceAtLeast(headerIndex), 0, widths.size - 1))
    }

    /** Excel holds at most 32767 characters in a cell. */
    private const val MAX_CELL_TEXT = 32_000
}
