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

import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.commons.io.FileUtils
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.orderbooksnapshots.OrderbookSnapshotsService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDay
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.test.WorkFileHelper
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Locale

class ForecastExportTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var forecastExport: ForecastExport

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var forecastOrderAnalysis: ForecastOrderAnalysis

    @Autowired
    private lateinit var orderbookSnapshotsService: OrderbookSnapshotsService

    /**
     * The order analysis shows both cases of projectforge.fibu.forecast.distributeUnusedBudget, but only if they
     * differ (T&M orders with a run rate below the even distribution).
     */
    @Test
    fun orderAnalysisDistributeUnusedBudgetTest() {
        logon(TEST_FINANCE_USER)
        val baseDate = PFDay.now().plusMonths(-4)

        // T&M, 5000 for 5 months (1000 per month), but only 500 invoiced so far: the run rate (distributeUnusedBudget
        // = false) is far below the even distribution (= true), so both variants must be shown.
        val tmOrder = createOrder(baseDate, AuftragsStatus.BEAUFTRAGT, baseDate, baseDate.plusMonths(4))
        addPosition(tmOrder, 1, AuftragsStatus.BEAUFTRAGT, 5000.0, AuftragsPositionsPaymentType.TIME_AND_MATERIALS)
        val tmOrderId = auftragDao.insert(tmOrder)
        auftragsCache.setExpired()
        auftragsCache.forceReload()
        val invoice = createInvoice(baseDate.plusMonths(1))
        addPosition(invoice, 500.0, auftragDao.find(tmOrderId)!!.getPosition(1))
        rechnungDao.insert(invoice)

        val withDistribution = translate("fibu.auftrag.forecast.analysis.variants.true.label")
        val withoutDistribution = translate("fibu.auftrag.forecast.analysis.variants.false.label")
        val tmHtml = forecastOrderAnalysis.htmlExport(orderId = tmOrderId, checkAccess = false)
        Assertions.assertTrue(
            tmHtml.contains(withDistribution) && tmHtml.contains(withoutDistribution),
            "Both variants expected for a T&M order with a run rate below the even distribution."
        )
        assertTranslated(tmHtml)

        // The whole page must be translated, so check the German version as well:
        val user = ThreadLocalUserContext.loggedInUser!!
        val locale = user.locale
        user.locale = Locale.GERMAN
        try {
            val germanHtml = forecastOrderAnalysis.htmlExport(orderId = tmOrderId, checkAccess = false)
            assertTranslated(germanHtml)
            Assertions.assertTrue(
                germanHtml.contains("Forecast-Analyse des Auftrags"),
                "German title expected: ${germanHtml.take(500)}"
            )
        } finally {
            user.locale = locale
        }

        // Fixed price without any invoice: the whole net sum is scheduled at the end of the performance period in both
        // cases, so only one variant may be shown.
        val fixedOrder = createOrder(baseDate, AuftragsStatus.BEAUFTRAGT, baseDate, baseDate.plusMonths(4))
        addPosition(fixedOrder, 1, AuftragsStatus.BEAUFTRAGT, 5000.0, AuftragsPositionsPaymentType.FESTPREISPAKET)
        val fixedOrderId = auftragDao.insert(fixedOrder)

        val fixedHtml = forecastOrderAnalysis.htmlExport(orderId = fixedOrderId, checkAccess = false)
        Assertions.assertFalse(
            fixedHtml.contains(withDistribution) || fixedHtml.contains(withoutDistribution),
            "Only one variant expected for a fixed price order without invoices (both variants are equal)."
        )
        assertTranslated(fixedHtml)
    }

    /**
     * The zip archive and all Excel files of a forecast script run must carry the variant they were calculated with,
     * so the results of both runs can be compared without mixing them up.
     */
    @Test
    fun filenameVariantTest() {
        val startDate = PFDay.withDate(2025, java.time.Month.JANUARY, 1)
        Assertions.assertTrue(
            forecastExport.getFilename(startDate, extension = ".zip", distributeUnusedBudget = true)
                .endsWith("_optimistisch.zip")
        )
        Assertions.assertTrue(
            forecastExport.getFilename(startDate, extension = ".xlsx", part = "ACME", distributeUnusedBudget = false)
                .endsWith("_konservativ.xlsx")
        )
        // Without an explicit value the configured default is used:
        val default = if (ForecastOrderPosInfo.defaultDistributeUnusedBudget) "_optimistisch" else "_konservativ"
        Assertions.assertTrue(forecastExport.getFilename(startDate).endsWith(default))
        Assertions.assertEquals("_optimistisch", forecastExport.variantSuffix(true))
        Assertions.assertEquals("_konservativ", forecastExport.variantSuffix(false))
    }

    /**
     * copyAllFilterCriteria is what a caller handing over the filter of a whole list page relies on (the
     * order list of the next frontend, see `OrderEntityRest.exportForecast`) — but the period of performance
     * must stay out of it in either case: the query has to reach three years further back than the start of
     * the forecast, or the sheets of the two prior years' invoices come out empty.
     */
    @Test
    fun queryFilterCriteriaTest() {
        val startDate = PFDay.withDate(2025, java.time.Month.MARCH, 1)
        val origFilter = AuftragFilter()
        origFilter.searchString = "ACME"
        origFilter.auftragsStatuses.add(AuftragsStatus.BEAUFTRAGT)
        origFilter.auftragsPositionsArten.add(AuftragsPositionsArt.WARTUNG)
        origFilter.auftragsPositionsPaymentType = AuftragsPositionsPaymentType.TIME_AND_MATERIALS
        origFilter.auftragFakturiertFilterStatus = AuftragFakturiertFilterStatus.ZU_FAKTURIEREN
        origFilter.startDate = LocalDate.of(2024, 1, 1)
        origFilter.endDate = LocalDate.of(2024, 12, 31)
        origFilter.periodOfPerformanceStartDate = startDate.localDate
        origFilter.periodOfPerformanceEndDate = LocalDate.of(2025, 12, 31)

        val copied = forecastExport.buildQueryFilter(origFilter, startDate, copyAllFilterCriteria = true)
        Assertions.assertEquals("ACME", copied.searchString)
        Assertions.assertEquals(listOf(AuftragsStatus.BEAUFTRAGT), copied.auftragsStatuses)
        Assertions.assertEquals(listOf(AuftragsPositionsArt.WARTUNG), copied.auftragsPositionsArten)
        Assertions.assertEquals(AuftragsPositionsPaymentType.TIME_AND_MATERIALS, copied.auftragsPositionsPaymentType)
        Assertions.assertEquals(AuftragFakturiertFilterStatus.ZU_FAKTURIEREN, copied.auftragFakturiertFilterStatus)
        Assertions.assertEquals(LocalDate.of(2024, 1, 1), copied.startDate)
        Assertions.assertEquals(LocalDate.of(2024, 12, 31), copied.endDate)
        Assertions.assertEquals(
            LocalDate.of(2022, 3, 1), copied.periodOfPerformanceStartDate,
            "Three years before the start of the forecast, never the filter's own value."
        )
        Assertions.assertNull(copied.periodOfPerformanceEndDate, "An end would cut off the prior years' invoices.")

        // Wicket and the forecast scripts: only the search string, the projects and the user.
        val plain = forecastExport.buildQueryFilter(origFilter, startDate, copyAllFilterCriteria = false)
        Assertions.assertEquals("ACME", plain.searchString)
        Assertions.assertTrue(plain.auftragsStatuses.isEmpty())
        Assertions.assertTrue(plain.auftragsPositionsArten.isEmpty())
        Assertions.assertNull(plain.auftragsPositionsPaymentType)
        Assertions.assertNull(plain.startDate)
        Assertions.assertNull(plain.endDate)
        Assertions.assertEquals(LocalDate.of(2022, 3, 1), plain.periodOfPerformanceStartDate)
    }

    /**
     * I18nHelper renders missing i18n keys as '???key???', so the page must not contain any '???'.
     */
    private fun assertTranslated(html: String) {
        Assertions.assertFalse(html.contains("???"), "All i18n keys of the analysis page must be defined: $html")
    }

    /**
     * The filter selection of the first tab (Forecast_Data) is propagated to the invoice sheets (Rechnungen, Vorjahr,
     * Vorvorjahr) by project id only: visibleID contains the project ids of the visible rows, and each invoice row's
     * visible column does a COUNTIF against it. So every invoice row needs a project id, and visibleID must never
     * contain a numeric 0 (a blank project id falling through the IF), otherwise all invoices without project would
     * count in every filter selection.
     */
    @Test
    fun filterPropagationTest() {
        logon(TEST_FINANCE_USER)
        val today = PFDay.now()
        val baseDate = today.plusMonths(-4)

        val projekt = ProjektDO()
        projekt.nummer = 1
        projekt.name = "ForecastExportTest - project"
        val projektId = projektDao.insert(projekt, checkAccess = false)

        // Order with project, invoiced. The invoice itself has no project: it must inherit the order's project.
        val orderWithProject = createOrder(baseDate, AuftragsStatus.BEAUFTRAGT, baseDate, baseDate.plusMonths(4))
        // Attached is important, otherwise deadlock.
        orderWithProject.projekt = projektDao.find(projektId, checkAccess = false, attached = true)
        val pos = addPosition(
            orderWithProject, 1, AuftragsStatus.BEAUFTRAGT, 5000.0, AuftragsPositionsPaymentType.TIME_AND_MATERIALS
        )
        val orderId = auftragDao.insert(orderWithProject)

        // Order without any project, invoiced: neither the invoice nor the order references a project.
        val orderWithoutProject = createOrder(baseDate, AuftragsStatus.BEAUFTRAGT, baseDate, baseDate.plusMonths(4))
        addPosition(
            orderWithoutProject, 1, AuftragsStatus.BEAUFTRAGT, 3000.0, AuftragsPositionsPaymentType.TIME_AND_MATERIALS
        )
        val orderWithoutProjectId = auftragDao.insert(orderWithoutProject)
        // The invoice positions are only linked to their order positions via AuftragsCache:
        auftragsCache.setExpired()
        auftragsCache.forceReload()
        Assertions.assertNotNull(pos)

        // Both invoices have no own project: the first one must inherit the project of its order, the second one has
        // no project at all.
        val invoice = createInvoice(baseDate.plusMonths(1))
        addPosition(invoice, 1000.0, auftragDao.find(orderId)!!.getPosition(1))
        rechnungDao.insert(invoice)
        val invoice2 = createInvoice(baseDate.plusMonths(1))
        addPosition(invoice2, 500.0, auftragDao.find(orderWithoutProjectId)!!.getPosition(1))
        rechnungDao.insert(invoice2)

        val filter = AuftragFilter()
        filter.periodOfPerformanceStartDate = baseDate.localDate
        val ba = forecastExport.xlsExport(filter, distributeUnusedBudget = true)
        Assertions.assertNotNull(ba, "Export expected.")

        // Plain POI, no merlin: merlin's head row analysis runs into an endless recursion on formula cells.
        XSSFWorkbook(ByteArrayInputStream(ba)).use { workbook ->
            val forecastSheet = workbook.getSheet(ForecastExportContext.Sheet.FORECAST.title)!!
            val forecastHeadRow = findHeadRow(forecastSheet, ForecastExportContext.ForecastCol.PROJECT_ID.header)
            val projectIdCol = findColumn(forecastHeadRow, ForecastExportContext.ForecastCol.PROJECT_ID.header)
            val projectIdColLetters = CellReference.convertNumToColString(projectIdCol)
            val visibleIdCol = findColumn(forecastHeadRow, ForecastExportContext.ForecastCol.VISIBLE_PROJECT_ID.header)
            var visibleIdCells = 0
            val forecastProjectIds = mutableSetOf<Long>()
            val invoiceProjectIds = mutableSetOf<Long>()
            for (rowNum in forecastHeadRow.rowNum + 1..forecastSheet.lastRowNum) {
                val row = forecastSheet.getRow(rowNum) ?: continue
                row.getCell(projectIdCol)?.let {
                    if (it.cellType == CellType.NUMERIC) {
                        forecastProjectIds.add(it.numericCellValue.toLong())
                    }
                }
                val cell = row.getCell(visibleIdCol) ?: continue
                if (cell.cellType != CellType.FORMULA) {
                    continue
                }
                ++visibleIdCells
                // Blank project ids must not fall through the IF as numeric 0, otherwise COUNTIF of the invoice sheets
                // would match every invoice without project against every order row without project:
                val formula = cell.cellFormula
                Assertions.assertTrue(
                    formula.contains("AND(") && formula.contains("$projectIdColLetters${rowNum + 1}<>\"\""),
                    "visibleID formula of row ${rowNum + 1} must guard against blank project ids: $formula"
                )
            }
            Assertions.assertTrue(visibleIdCells > 0, "visibleID cells expected.")

            // Every invoice row must carry a project id, otherwise the filter can't be propagated to it:
            var invoiceRows = 0
            ForecastExportContext.Sheet.entries.filter { it.title.startsWith("Rechnungen") }.forEach { sheetEnum ->
                val sheet = workbook.getSheet(sheetEnum.title)!!
                val headRow = findHeadRow(sheet, ForecastExportContext.InvoicesCol.PROJECT_ID.header)
                val invoiceProjectIdCol = findColumn(headRow, ForecastExportContext.InvoicesCol.PROJECT_ID.header)
                for (rowNum in headRow.rowNum + 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowNum) ?: continue
                    val cell = row.getCell(invoiceProjectIdCol)
                    Assertions.assertNotNull(cell, "${sheetEnum.title} row ${rowNum + 1}: ProjectID cell expected.")
                    Assertions.assertEquals(
                        CellType.NUMERIC, cell!!.cellType,
                        "${sheetEnum.title} row ${rowNum + 1}: ProjectID must be set (order project or PROJECT_ID_NONE)."
                    )
                    Assertions.assertNotEquals(
                        0.0, cell.numericCellValue,
                        "${sheetEnum.title} row ${rowNum + 1}: ProjectID must not be 0."
                    )
                    ++invoiceRows
                    invoiceProjectIds.add(cell.numericCellValue.toLong())
                }
            }
            Assertions.assertTrue(invoiceRows >= 2, "At least both invoices of this test expected: $invoiceRows")
            // The invoice without own project inherits the project of its order, the one without any project gets the
            // pseudo id, which is only visible as long as no filter is set:
            Assertions.assertTrue(
                invoiceProjectIds.containsAll(listOf(projektId, ForecastExportContext.PROJECT_ID_NONE)),
                "Order project (inherited) and PROJECT_ID_NONE expected: $invoiceProjectIds"
            )
            Assertions.assertTrue(
                forecastProjectIds.contains(ForecastExportContext.PROJECT_ID_NONE),
                "Pseudo order row for invoices without project expected in the forecast sheet."
            )
        }
    }

    /**
     * A completed (ABGESCHLOSSEN) but invoiced order position must not drop out of the IST sums. auftragsStatusToShow
     * allows ABGESCHLOSSEN on order level while auftragsPositionsStatusToShow does not, so before the fix such a
     * position was skipped and its project only appeared as an anonymous pseudo order row (ORDER_NR 0). It must now
     * get a real forecast row so its project id enters visibleID and the invoice stays visible = TRUE (part of the
     * unfiltered "Ist" sums of the chart).
     */
    @Test
    fun completedOrderInvoiceStaysVisibleTest() {
        logon(TEST_FINANCE_USER)
        val today = PFDay.now()
        val baseDate = today.plusMonths(-4)

        val projekt = ProjektDO()
        projekt.nummer = 2
        projekt.name = "ForecastExportTest - completed project"
        val projektId = projektDao.insert(projekt, checkAccess = false)

        // Completed order with a completed position, invoiced. The position status (ABGESCHLOSSEN) is not in
        // auftragsPositionsStatusToShow, so it gets no real forecast row; the project's IST visibility is instead
        // ensured by a single pseudo row (see missedProjectIds in ForecastExport).
        val order = createOrder(baseDate, AuftragsStatus.ABGESCHLOSSEN, baseDate, baseDate.plusMonths(4))
        order.projekt = projektDao.find(projektId, checkAccess = false, attached = true)
        addPosition(order, 1, AuftragsStatus.ABGESCHLOSSEN, 5000.0, AuftragsPositionsPaymentType.TIME_AND_MATERIALS)
        val orderId = auftragDao.insert(order)
        auftragsCache.setExpired()
        auftragsCache.forceReload()
        val orderNummer = auftragDao.find(orderId)!!.nummer!!

        val invoice = createInvoice(baseDate.plusMonths(1))
        addPosition(invoice, 1234.0, auftragDao.find(orderId)!!.getPosition(1))
        rechnungDao.insert(invoice)

        // No search string and no project list -> showAll (unfiltered total) for a finance user:
        val filter = AuftragFilter()
        filter.periodOfPerformanceStartDate = baseDate.localDate
        val ba = forecastExport.xlsExport(filter, distributeUnusedBudget = true)
        Assertions.assertNotNull(ba, "Export expected.")

        XSSFWorkbook(ByteArrayInputStream(ba)).use { workbook ->
            val forecastSheet = workbook.getSheet(ForecastExportContext.Sheet.FORECAST.title)!!
            val forecastHeadRow = findHeadRow(forecastSheet, ForecastExportContext.ForecastCol.PROJECT_ID.header)
            val projectIdCol = findColumn(forecastHeadRow, ForecastExportContext.ForecastCol.PROJECT_ID.header)
            val orderNrCol = findColumn(forecastHeadRow, ForecastExportContext.ForecastCol.ORDER_NR.header)

            // The completed position must NOT create a real forecast row (that would bloat the sheet with a near-empty
            // row per completed position). Instead the invoiced project gets exactly one pseudo row (ORDER_NR 0), which
            // still carries the project id so the invoice's visibleID lookup matches.
            var pseudoRowFound = false
            var realRowFound = false
            for (rowNum in forecastHeadRow.rowNum + 1..forecastSheet.lastRowNum) {
                val row = forecastSheet.getRow(rowNum) ?: continue
                val projectCell = row.getCell(projectIdCol) ?: continue
                if (projectCell.cellType != CellType.NUMERIC || projectCell.numericCellValue.toLong() != projektId) {
                    continue
                }
                val orderNrCell = row.getCell(orderNrCol)
                val orderNr = if (orderNrCell?.cellType == CellType.NUMERIC) orderNrCell.numericCellValue.toInt() else null
                if (orderNr == orderNummer) {
                    realRowFound = true
                } else if (orderNr == 0) {
                    pseudoRowFound = true
                }
            }
            Assertions.assertTrue(
                pseudoRowFound,
                "The invoiced project $projektId must get a single pseudo forecast row (order number 0) for IST visibility."
            )
            Assertions.assertFalse(
                realRowFound,
                "The completed position must not get a real forecast row (order $orderNummer); the pseudo row suffices."
            )

            // End-to-end: the invoice's visible column (COUNTIF against visibleID) must evaluate to TRUE, so it is part
            // of the IST sums.
            val evaluator = workbook.creationHelper.createFormulaEvaluator()
            evaluator.evaluateAll()
            val invoicesSheet = workbook.getSheet(ForecastExportContext.Sheet.INVOICES.title)!!
            val invoicesHeadRow = findHeadRow(invoicesSheet, ForecastExportContext.InvoicesCol.PROJECT_ID.header)
            val invProjectIdCol = findColumn(invoicesHeadRow, ForecastExportContext.InvoicesCol.PROJECT_ID.header)
            val invVisibleCol = findColumn(invoicesHeadRow, ForecastExportContext.InvoicesCol.VISIBLE.header)
            var visibleTrueFound = false
            for (rowNum in invoicesHeadRow.rowNum + 1..invoicesSheet.lastRowNum) {
                val row = invoicesSheet.getRow(rowNum) ?: continue
                val projectCell = row.getCell(invProjectIdCol) ?: continue
                if (projectCell.cellType != CellType.NUMERIC || projectCell.numericCellValue.toLong() != projektId) {
                    continue
                }
                val visibleCell = row.getCell(invVisibleCol) ?: continue
                val evaluated = evaluator.evaluate(visibleCell)
                if (evaluated?.cellType == CellType.BOOLEAN && evaluated.booleanValue) {
                    visibleTrueFound = true
                }
            }
            Assertions.assertTrue(
                visibleTrueFound,
                "The invoice of the completed order (project $projektId) must evaluate visible = TRUE."
            )
        }
    }

    private fun findHeadRow(sheet: Sheet, header: String): Row {
        for (rowNum in 0..sheet.lastRowNum) {
            val row = sheet.getRow(rowNum) ?: continue
            if (row.any { it.cellType == CellType.STRING && it.stringCellValue == header }) {
                return row
            }
        }
        throw AssertionError("Head row with column '$header' not found in sheet '${sheet.sheetName}'.")
    }

    private fun findColumn(headRow: Row, header: String): Int {
        return headRow.first { it.cellType == CellType.STRING && it.stringCellValue == header }.columnIndex
    }

    @Test
    fun exportTest() {
        logon(TEST_FINANCE_USER)
        val today = PFDay.now()
        val baseDate = today.plusMonths(-4)
        val order1 = createTimeAndMaterials(AuftragsStatus.BEAUFTRAGT, AuftragsStatus.BEAUFTRAGT,
                1000.0, baseDate,
                baseDate.plusMonths(1), baseDate.plusMonths(4),
                baseDate.plusMonths(2), baseDate.plusMonths(3), baseDate.plusMonths(4))
        //order1.addPaymentSchedule()

        createTimeAndMaterials(AuftragsStatus.BEAUFTRAGT, AuftragsStatus.BEAUFTRAGT,
                1000.0, baseDate,
                baseDate.plusMonths(1), baseDate.plusMonths(4),
                baseDate.plusMonths(2), baseDate.plusMonths(3))

        val order3 = createOrder(today, AuftragsStatus.IN_ERSTELLUNG,
                today.plusMonths(1), today.plusMonths(5))
        //order1.addPaymentSchedule()
        addPosition(order3, 1, AuftragsStatus.IN_ERSTELLUNG, 4000.00, AuftragsPositionsPaymentType.FESTPREISPAKET)
        auftragDao.insert(order3)

        val filter = AuftragFilter()
        filter.periodOfPerformanceStartDate = baseDate.localDate
        // Assert even distribution explicitly, independent of the configured default (application.properties may set
        // projectforge.fibu.forecast.distributeUnusedBudget=false).
        val ba = forecastExport.xlsExport(filter, distributeUnusedBudget = true)
        val excelFile = WorkFileHelper.getWorkFile("forecast.xlsx")
        baseLog.info { "Writing forecast Excel file to work directory: " + excelFile.absolutePath }
        FileUtils.writeByteArrayToFile(excelFile, ba)

        ExcelWorkbook(ByteArrayInputStream(ba), excelFile.name).use { workbook ->
            val forecastSheet = workbook.getSheet(ForecastExportContext.Sheet.FORECAST.title)!!
            val monthCols = Array(12) {
                forecastSheet.registerColumn(ForecastExport.formatMonthHeader(baseDate.plusMonths(it.toLong())))
            }
            val firstRow = 10
            forecastSheet.headRow // Enforce analyzing the column definitions.

            // This row is a T&M FOLLOWING_MONTH order: performance period monthCols[1]..monthCols[4], with actual
            // invoices in monthCols[2] and monthCols[3] only (toBeInvoiced = 2000). Today is monthCols[4].
            // Since the current month (monthCols[4]) has NOT been invoiced yet, it must still carry forecast:
            // the remaining 2000 is distributed over monthCols[4] and monthCols[5] = 1000 each (see orders 6809/5503).
            // The invoiced/past month monthCols[3] must be blank.
            Assertions.assertTrue(forecastSheet.getCell(firstRow + 1, monthCols[3])!!.stringCellValue.isNullOrBlank())
            assertAmount(BigDecimal(1000), forecastSheet.getCell(firstRow + 1, monthCols[4])!!.numericCellValue)
            assertAmount(BigDecimal(1000), forecastSheet.getCell(firstRow + 1, monthCols[5])!!.numericCellValue)

            // The info sheet must document the variant this forecast was calculated with:
            val infoSheet = workbook.getSheet(ForecastExportContext.Sheet.INFO.title)!!
            Assertions.assertEquals(
                forecastExport.variantInfo(true),
                infoSheet.getCell(3, 1)?.stringCellValue,
                "Info sheet must contain the optimistic variant info."
            )
            Assertions.assertTrue(
                forecastExport.variantInfo(true).startsWith(
                    translate("fibu.auftrag.forecast.analysis.variants.true.label")
                )
            )
            Assertions.assertTrue(
                forecastExport.variantInfo(false).startsWith(
                    translate("fibu.auftrag.forecast.analysis.variants.false.label")
                )
            )
        }
    }

    /**
     * The chart data ([ForecastExport.chartData]) must equal what the charts of the Excel export show: the evaluated
     * template formulas of the summary rows of Forecast_Data (monthly sums by position status, IST) and of the sheet
     * 'Umsatz kumuliert' (previous years).
     */
    @Test
    fun chartDataMatchesExcelTest() {
        logon(TEST_FINANCE_USER)
        val today = PFDay.now()
        val baseDate = today.plusMonths(-4)
        createTimeAndMaterials(
            AuftragsStatus.BEAUFTRAGT, AuftragsStatus.BEAUFTRAGT, 1000.0, baseDate,
            baseDate.plusMonths(1), baseDate.plusMonths(4), baseDate.plusMonths(2), baseDate.plusMonths(3)
        )
        // Invoice of the previous year's window:
        createTimeAndMaterials(
            AuftragsStatus.BEAUFTRAGT, AuftragsStatus.BEAUFTRAGT, 700.0, baseDate.plusMonths(-12),
            baseDate.plusMonths(-12), baseDate.plusMonths(-10), baseDate.plusMonths(-11)
        )
        val order = createOrder(today, AuftragsStatus.GELEGT, today.plusMonths(1), today.plusMonths(3))
        addPosition(order, 1, AuftragsStatus.GELEGT, 3000.00, AuftragsPositionsPaymentType.PAUSCHALE)
        addPosition(order, 2, AuftragsStatus.LOI, 900.00, AuftragsPositionsPaymentType.FESTPREISPAKET)
        auftragDao.insert(order)
        auftragsCache.setExpired()

        val filter = AuftragFilter()
        filter.periodOfPerformanceStartDate = baseDate.localDate
        val chartData = forecastExport.chartData(filter, distributeUnusedBudget = true)
        Assertions.assertNotNull(chartData, "Chart data expected.")
        chartData!!
        Assertions.assertNull(chartData.plan, "No plan without planning date.")
        Assertions.assertEquals(12, chartData.months.size)
        Assertions.assertEquals("${baseDate.year}-${baseDate.monthValue.toString().padStart(2, '0')}", chartData.months[0])
        Assertions.assertTrue(
            chartData.forecastByStatus.values.any { values -> values.any { it.signum() != 0 } },
            "Forecast values expected."
        )

        // The status sums of the template (SUMIFS) compare against the German status labels:
        val user = ThreadLocalUserContext.loggedInUser!!
        val locale = user.locale
        user.locale = Locale.GERMAN
        val ba = try {
            forecastExport.xlsExport(filter, distributeUnusedBudget = true)
        } finally {
            user.locale = locale
        }
        XSSFWorkbook(ByteArrayInputStream(ba)).use { workbook ->
            val forecastSheet = workbook.getSheet(ForecastExportContext.Sheet.FORECAST.title)!!
            val firstMonthCol = CellReference.convertColStringToIndex("AD")
            fun excelRow(sheet: Sheet, rowNum: Int, firstCol: Int): List<BigDecimal> = List(12) { i ->
                BigDecimal(sheet.getRow(rowNum).getCell(firstCol + i)?.numericCellValue ?: 0.0)
            }
            // Rows 2-6 (0-based 1-5) of Forecast_Data in the order of the template:
            listOf(
                AuftragsStatus.BEAUFTRAGT, AuftragsStatus.GELEGT, AuftragsStatus.LOI,
                AuftragsStatus.IN_ERSTELLUNG, AuftragsStatus.POTENZIAL,
            ).forEachIndexed { index, status ->
                assertAmounts(excelRow(forecastSheet, index + 1, firstMonthCol), chartData.forecastByStatus[status]!!, "$status")
            }
            assertAmounts(excelRow(forecastSheet, 7, firstMonthCol), chartData.ist, "IST")
            val cumulatedSheet = workbook.getSheet("Umsatz kumuliert")!!
            assertAmounts(excelRow(cumulatedSheet, 3, 1), chartData.total, "Gesamt")
            assertAmounts(excelRow(cumulatedSheet, 7, 1), chartData.prevYear, "Vorjahr")
            assertAmounts(excelRow(cumulatedSheet, 10, 1), chartData.prevPrevYear, "Vorvorjahr")
        }
    }

    /**
     * The tables of [ForecastExport.statistics] are the rows behind its charts: the months of the positions sum up
     * to the forecast by status, the invoice rows to IST and the previous years, and the project overview to both.
     */
    @Test
    fun statisticsTablesMatchChartTest() {
        logon(TEST_FINANCE_USER)
        val today = PFDay.now()
        val baseDate = today.plusMonths(-4)
        createTimeAndMaterials(
            AuftragsStatus.BEAUFTRAGT, AuftragsStatus.BEAUFTRAGT, 1200.0, baseDate,
            baseDate.plusMonths(1), baseDate.plusMonths(4), baseDate.plusMonths(2), baseDate.plusMonths(3)
        )
        createTimeAndMaterials(
            AuftragsStatus.BEAUFTRAGT, AuftragsStatus.BEAUFTRAGT, 800.0, baseDate.plusMonths(-12),
            baseDate.plusMonths(-12), baseDate.plusMonths(-10), baseDate.plusMonths(-11)
        )
        val order = createOrder(today, AuftragsStatus.GELEGT, today.plusMonths(1), today.plusMonths(3))
        addPosition(order, 1, AuftragsStatus.GELEGT, 2500.00, AuftragsPositionsPaymentType.PAUSCHALE)
        auftragDao.insert(order)
        auftragsCache.setExpired()

        val origFilter = AuftragFilter()
        origFilter.periodOfPerformanceStartDate = baseDate.localDate
        val orders = auftragDao.select(forecastExport.buildQueryFilter(origFilter, baseDate.beginOfMonth, true))
        val statistics = forecastExport.statistics(orders, baseDate.localDate, unfiltered = true)
        Assertions.assertNotNull(statistics, "Statistics expected.")
        val chart = statistics!!.chart
        val tables = statistics.tables
        Assertions.assertEquals(chart.months, tables.months)
        Assertions.assertTrue(tables.positions.isNotEmpty(), "Position rows expected.")
        fun monthSums(rows: List<List<BigDecimal?>>): List<BigDecimal> = List(12) { i ->
            rows.fold(BigDecimal.ZERO) { acc, months -> acc + (months[i] ?: BigDecimal.ZERO) }
        }
        chart.forecastByStatus.forEach { (status, values) ->
            val rows = tables.positions.filter { it.positionStatus == translate(status.i18nKey) }
            assertAmounts(values, monthSums(rows.map { it.months }), "Positions of $status")
        }
        fun invoiceSums(kind: ForecastInvoiceRow.Kind): List<BigDecimal> = monthSums(
            tables.invoices.filter { it.kind == kind }.map { row ->
                List(12) { i -> if (i == row.monthIndex) row.netSum else null }
            }
        )
        assertAmounts(chart.ist, invoiceSums(ForecastInvoiceRow.Kind.IST), "IST")
        assertAmounts(chart.prevYear, invoiceSums(ForecastInvoiceRow.Kind.PREV_YEAR), "Previous year")
        assertAmounts(chart.prevPrevYear, invoiceSums(ForecastInvoiceRow.Kind.PREV_PREV_YEAR), "Previous previous year")
        val forecast = chart.forecastByStatus.values.flatten().sumOf { it } + chart.ist.sumOf { it }
        assertAmount(tables.projects.sumOf { it.forecast }, forecast.toDouble())
        assertAmount(tables.projects.sumOf { it.prevYear }, chart.prevYear.sumOf { it }.toDouble())
        Assertions.assertTrue(tables.projects.all { it.plan == null }, "No plan without planning date.")
        Assertions.assertEquals(tables.projects.size, tables.projects.map { it.projectId }.toSet().size)
    }

    /**
     * The lost budget warning of the conservative forecast reaches the position rows, with the months the Excel
     * marks red; the optimistic one distributes the unused budget, so it has none.
     */
    @Test
    fun statisticsLostBudgetWarningTest() {
        logon(TEST_FINANCE_USER)
        val baseDate = PFDay.now().plusMonths(-4)
        // T&M, 5000 for 5 months, but only 500 invoiced: the run rate loses most of the budget.
        val order = createOrder(baseDate, AuftragsStatus.BEAUFTRAGT, baseDate, baseDate.plusMonths(4))
        addPosition(order, 1, AuftragsStatus.BEAUFTRAGT, 5000.0, AuftragsPositionsPaymentType.TIME_AND_MATERIALS)
        val orderId = auftragDao.insert(order)
        auftragsCache.setExpired()
        auftragsCache.forceReload()
        val invoice = createInvoice(baseDate.plusMonths(1))
        addPosition(invoice, 500.0, auftragDao.find(orderId)!!.getPosition(1))
        rechnungDao.insert(invoice)

        fun tables(distributeUnusedBudget: Boolean) = forecastExport.statistics(
            listOf(auftragDao.find(orderId)!!),
            baseDate.localDate,
            unfiltered = false,
            distributeUnusedBudget = distributeUnusedBudget,
        )!!.tables
        fun row(distributeUnusedBudget: Boolean): ForecastPositionRow =
            tables(distributeUnusedBudget).positions.single { it.orderId == orderId }
        val conservative = row(false)
        Assertions.assertNotNull(conservative.warning, "Lost budget warning expected in the conservative forecast.")
        Assertions.assertTrue(conservative.warningMonths.isNotEmpty(), "Warning months expected.")
        conservative.warningMonths.forEach {
            Assertions.assertNotNull(conservative.months[it], "A warning month carries a value, as its Excel cell.")
        }
        // The project overview sums them up, so the project with the warning stands out there already.
        val project = tables(false).projects.single { it.projectId == (conservative.projectId ?: ForecastExportContext.PROJECT_ID_NONE) }
        Assertions.assertEquals(1, project.warnings.size)
        Assertions.assertEquals(conservative.warning, project.warnings.single().text)
        assertAmount(project.difference, conservative.difference.toDouble())
        val optimistic = row(true)
        Assertions.assertNull(optimistic.warning, "No warning expected in the optimistic forecast.")
        Assertions.assertTrue(optimistic.warningMonths.isEmpty())
    }

    /**
     * A planning date must not undo the search string: the order book snapshot used as plan can't be searched in
     * full text, so its orders are restricted to those the search found. Neither may the snapshot's positions draw
     * the invoices of other orders into IST and the previous years.
     */
    @Test
    fun planningDateKeepsSearchStringTest() {
        logon(TEST_FINANCE_USER)
        val today = PFDay.now()
        val baseDate = today.plusMonths(-4)
        fun createInvoicedOrder(titel: String, projectNumber: Int, amount: Double) {
            val projekt = ProjektDO()
            projekt.nummer = projectNumber
            projekt.name = "ForecastExportTest - $titel"
            val projektId = projektDao.insert(projekt, checkAccess = false)
            val order = createOrder(baseDate, AuftragsStatus.BEAUFTRAGT, baseDate, baseDate.plusMonths(4))
            order.titel = titel
            order.projekt = projektDao.find(projektId, checkAccess = false, attached = true)
            addPosition(order, 1, AuftragsStatus.BEAUFTRAGT, 5 * amount, AuftragsPositionsPaymentType.TIME_AND_MATERIALS)
            val orderId = auftragDao.insert(order)
            val invoice = createInvoice(baseDate.plusMonths(1))
            addPosition(invoice, amount, auftragDao.find(orderId)!!.getPosition(1))
            rechnungDao.insert(invoice)
        }
        // Unique per run: the test's search index survives the run, its database doesn't, so a fixed term could
        // match a stale index entry whose id now belongs to another order.
        val searchTerm = "Plansearchmatch${System.currentTimeMillis()}"
        createInvoicedOrder("$searchTerm order", 3, 1000.0)
        createInvoicedOrder("Other order", 4, 7000.0)
        auftragsCache.setExpired()
        auftragsCache.forceReload()
        orderbookSnapshotsService.storeOrderbookSnapshot(date = today.localDate)

        val filter = AuftragFilter()
        filter.searchString = searchTerm
        filter.periodOfPerformanceStartDate = baseDate.localDate
        val withoutPlan = forecastExport.chartData(filter, distributeUnusedBudget = true)!!
        val withPlan = forecastExport.chartData(filter, planningDate = today.localDate, distributeUnusedBudget = true)!!
        assertAmount(withoutPlan.ist.sumOf { it }, 1000.0)
        assertAmounts(withoutPlan.ist, withPlan.ist, "IST with planning date")
        assertAmounts(withoutPlan.prevYear, withPlan.prevYear, "Previous year with planning date")
        assertAmounts(withoutPlan.total, withPlan.total, "Total with planning date")
        // The plan of today's snapshot is the forecast of today, of the found order only:
        assertAmounts(withoutPlan.total, withPlan.plan!!, "Plan")
    }

    private fun assertAmounts(expected: List<BigDecimal>, actual: List<BigDecimal>, name: String) {
        Assertions.assertEquals(
            expected.map { it.setScale(2, RoundingMode.HALF_UP) },
            actual.map { it.setScale(2, RoundingMode.HALF_UP) },
            name,
        )
    }

    private fun createTimeAndMaterials(orderStatus: AuftragsStatus, posStatus: AuftragsStatus,
                                       monthlyAmount: Double, date: PFDay, periodStart: PFDay, periodEnd: PFDay, vararg invoiceMonth: PFDay)
            : AuftragDO {
        var order = createOrder(date, orderStatus, periodStart, periodEnd)
        //order1.addPaymentSchedule()
        addPosition(order, 1, posStatus, monthlyAmount * (1 + periodStart.monthsBetween(periodEnd)), AuftragsPositionsPaymentType.TIME_AND_MATERIALS)
        val id = auftragDao.insert(order)
        order = auftragDao.find(id)!!
        val pos1_1 = order.getPosition(1)!!
        invoiceMonth.forEach {
            val invoice1 = createInvoice(it)
            addPosition(invoice1, monthlyAmount, pos1_1)
            rechnungDao.insert(invoice1)
        }
        return order
    }

    private fun createOrder(date: PFDay,
                            status: AuftragsStatus,
                            periodOfPerformanceBegin: PFDay? = null,
                            periodOfPerformanceEnd: PFDay? = null,
                            probability: Int? = null): AuftragDO {
        val order = AuftragDO()
        order.nummer = auftragDao.nextNumber
        order.status = status
        order.angebotsDatum = date.localDate
        order.periodOfPerformanceBegin = periodOfPerformanceBegin?.localDate
        order.periodOfPerformanceEnd = periodOfPerformanceEnd?.localDate
        order.probabilityOfOccurrence = probability
        return order
    }

    private fun addPosition(order: AuftragDO,
                            number: Short,
                            status: AuftragsStatus,
                            netSum: Double,
                            paymentType: AuftragsPositionsPaymentType,
                            periodOfPerformanceBegin: PFDay? = null,
                            periodOfPerformanceEnd: PFDay? = null,
                            periodOfPerformanceType: PeriodOfPerformanceType? = null): AuftragsPositionDO {
        val pos = AuftragsPositionDO()
        pos.number = number
        pos.status = status
        pos.paymentType = paymentType
        pos.nettoSumme = BigDecimal(netSum)
        pos.periodOfPerformanceBegin = periodOfPerformanceBegin?.localDate
        pos.periodOfPerformanceEnd = periodOfPerformanceEnd?.localDate
        pos.periodOfPerformanceType = periodOfPerformanceType
        order.addPosition(pos)
        return pos
    }

    private fun createInvoice(date: PFDay): RechnungDO {
        val invoice = RechnungDO()
        invoice.nummer = rechnungDao.nextNumber
        invoice.datum = date.localDate
        invoice.faelligkeit = date.plusDays(30).localDate
        invoice.status = RechnungStatus.GESTELLT
        invoice.typ = RechnungTyp.RECHNUNG
        invoice.kundeText = "ACME Inc."
        return invoice
    }

    private fun addPosition(invoice: RechnungDO, netSum: Double, orderPos: AuftragsPositionDO?): RechnungsPositionDO {
        val pos = RechnungsPositionDO()
        pos.auftragsPosition = orderPos
        pos.einzelNetto = BigDecimal(netSum)
        invoice.addPosition(pos)
        return pos
    }

    private fun assertAmount(v1: BigDecimal, v2: Double) {
        Assertions.assertEquals(v1.setScale(2, RoundingMode.HALF_UP), BigDecimal(v2).setScale(2, RoundingMode.HALF_UP))
    }
}
