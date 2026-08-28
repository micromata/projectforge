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

import de.micromata.merlin.excel.ExcelCell
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import mu.KotlinLogging
import org.projectforge.business.fibu.ForecastExportContext.*
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.fibu.orderbooksnapshots.OrderbookSnapshotsService
import org.projectforge.business.scripting.ScriptLogger
import org.projectforge.business.scripting.ThreadLocalScriptingContext
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.common.FilenameUtils
import org.projectforge.common.extensions.format2Digits
import org.projectforge.common.extensions.formatCurrency
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import org.projectforge.business.fibu.ForecastExportContext as Context

private val log = KotlinLogging.logger {}

/**
 * Forcast excel export based on order book with probabilities as well as on already invoiced orders.
 *
 * @author Kai Reinhard
 * @author Florian Blumenstein
 */
@Service
open class ForecastExport { // open needed by Wicket.

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var forecastExportInvoices: ForecastExportInvoices

    @Autowired
    private lateinit var orderbookSnapshotsService: OrderbookSnapshotsService

    @Autowired
    private lateinit var orderDao: AuftragDao

    @Autowired
    private lateinit var ordersCache: AuftragsCache

    @Autowired
    private lateinit var projectCache: ProjektCache

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    /**
     * Global default for distributing unused budget of an order position in the forecast (configurable via
     * application.properties). Used for single-order analyses and as the fallback for the Excel export when the
     * script doesn't override it. See [ForecastOrderPosInfo.distributeUnusedBudget].
     */
    @Value("\${projectforge.fibu.forecast.distributeUnusedBudget:true}")
    private var distributeUnusedBudget = true

    @PostConstruct
    private fun init() {
        ForecastOrderPosInfo.defaultDistributeUnusedBudget = distributeUnusedBudget
    }

    /**
     * Export the forecast sheet.
     * @param origFilter The filter for the orders to export.
     * @param planningDate If given, the monthly forecast will be calculated with the specified date and inserted as plan data.
     * @param snapshotDate Today (null) or, the day of the snapshot, if the orderList is loaded from order book snapshots.
     * @param copyAllFilterCriteria Whether the status, position and date criteria of [origFilter] are applied as well.
     * False (the Wicket page and the forecast scripts) keeps this to the search string, the projects and the
     * user, which is all these two callers ever set. True is for a caller that hands over the filter of a
     * whole list page and expects the export to show what the list shows (see `OrderEntityRest`).
     *
     * The period of performance is **not** part of it either way: it is what the start date of the forecast
     * is derived from, and the query needs to reach three years further back than that (the sheets of the
     * two prior years' invoices), so an end date or a later begin from the filter would leave them empty.
     * @param fillUnitCol The function to get the unit of the order to show in the unit column.
     */
    @JvmOverloads
    @Throws(IOException::class)
    open fun xlsExport(
        origFilter: AuftragFilter,
        planningDate: LocalDate? = null,
        snapshotDate: LocalDate? = null,
        distributeUnusedBudget: Boolean? = null,
        copyAllFilterCriteria: Boolean = false,
        fillUnitCol: ((orderInfo: OrderInfo) -> String)? = null,
    ): ByteArray? {
        val startDate = getStartDate(origFilter)
        val filter = buildQueryFilter(origFilter, startDate, copyAllFilterCriteria)
        val scriptLogger = ThreadLocalScriptingContext.getLogger()
        val closestPlanningDate = getClosestSnapshotDate(planningDate, scriptLogger, "planning")
        val closestSnapshotDate = getClosestSnapshotDate(snapshotDate, scriptLogger, "snapshot")
        val msgSB = StringBuilder("Exporting forecast script with start date ${startDate.isoString}")
        if (closestPlanningDate != null) {
            msgSB.append(" with planningDate $closestPlanningDate")
        }
        if (closestSnapshotDate != null) {
            msgSB.append(" with snapshotDate $closestSnapshotDate")
        }
        if (!filter.searchString.isNullOrBlank()) {
            msgSB.append(" with filter: str='${filter.searchString}'")
        }
        // scriptLogger?.info { msgSB } ?: log.info { msgSB }
        val orderList = if (closestSnapshotDate != null) {
            readSnapshot(closestSnapshotDate, filter)
        } else {
            orderDao.select(filter)
        }
        if (!filter.projectList.isNullOrEmpty()) {
            log.info {
                "$msgSB, projects=${
                    filter.projectList?.sortedBy { it.name }?.joinToString { it.name ?: "???" }
                }"
            }
        }
        val showAll = accessChecker.isLoggedInUserMemberOfGroup(
            ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.CONTROLLING_GROUP
        ) &&
                filter.searchString.isNullOrBlank() &&
                filter.projectList.isNullOrEmpty()
        try {
            return xlsExport(
                orderList,
                startDate = startDate,
                planningDate = closestPlanningDate,
                snapshotDate = closestSnapshotDate,
                showAll = showAll,
                auftragFilter = filter,
                scriptLogger = scriptLogger,
                distributeUnusedBudget = distributeUnusedBudget ?: ForecastOrderPosInfo.defaultDistributeUnusedBudget,
                fillUnitCol = fillUnitCol,
            )
        } catch (ex: Exception) {
            log.error(ex) { "Error exporting forecast: $ex" }
            throw ex
        }
    }

    private fun getStartDate(origFilter: AuftragFilter): PFDay {
        val startDateParam = origFilter.periodOfPerformanceStartDate
        return if (startDateParam != null) PFDay.from(startDateParam).beginOfMonth else PFDay.now().beginOfYear
    }

    /**
     * The filter the orders are actually queried with — a fresh one rather than the caller's, since the
     * period of performance has to be rewritten and the caller's filter also serves the sheet's own
     * filter selection.
     *
     * `internal` so [ForecastExportTest] can assert what [copyAllFilterCriteria] does and does not copy:
     * the criteria only show up in which orders the sheet holds, so getting one wrong is invisible in the
     * export itself.
     */
    internal fun buildQueryFilter(
        origFilter: AuftragFilter,
        startDate: PFDay,
        copyAllFilterCriteria: Boolean,
    ): AuftragFilter {
        val filter = AuftragFilter()
        filter.searchString = origFilter.searchString
        filter.projectList = origFilter.projectList
        if (copyAllFilterCriteria) {
            filter.auftragsStatuses.addAll(origFilter.auftragsStatuses)
            filter.auftragsPositionsArten.addAll(origFilter.auftragsPositionsArten)
            filter.auftragsPositionsPaymentType = origFilter.auftragsPositionsPaymentType
            filter.auftragFakturiertFilterStatus = origFilter.auftragFakturiertFilterStatus
            filter.startDate = origFilter.startDate
            filter.endDate = origFilter.endDate
        }
        filter.periodOfPerformanceStartDate =
            startDate.plusYears(-3).localDate // Go 3 years back for getting all orders referred by invoices of the two prior years.
        filter.user = origFilter.user
        return filter
    }

    @JvmOverloads
    open fun getExcelFilenmame(origFilter: AuftragFilter, distributeUnusedBudget: Boolean? = null): String {
        return getFilename(
            getStartDate(origFilter),
            extension = ".xlsx",
            distributeUnusedBudget = distributeUnusedBudget,
        )
    }

    private fun getClosestSnapshotDate(date: LocalDate?, scriptLogger: ScriptLogger?, name: String): LocalDate? {
        date ?: return null
        val closestDate = orderbookSnapshotsService.findClosestSnapshotDate(date)
        if (closestDate != date) {
            val msg = "No $name found for date $date. Using closest $name date $closestDate."
            scriptLogger?.warn { msg } ?: log.warn { msg }
        }
        return closestDate
    }

    /**
     * Get the export filename. Example: 'Forecast-start-2021-01_optimistisch_2025-01-02_22-48.xlsx'
     * or 'Forecast-ACME-snapshot-2023-08-01-start-2023-01_konservativ-2025-01-02_22-48.zip'.
     * @param startDate The start date for the forecast.
     * @param extension The optional file extension ('.xlsx' or '.zip').
     * @param part The optional part of the export file (e.g. 'ACME', 'Customer', ...).
     * @param distributeUnusedBudget The variant the forecast was calculated with, shown as '_optimistisch' resp.
     * '_konservativ' in the filename. If null, the configured default is used
     * ([ForecastOrderPosInfo.defaultDistributeUnusedBudget]).
     */
    @JvmOverloads
    open fun getFilename(
        startDate: PFDay,
        extension: String? = null,
        part: String? = null,
        planningDate: LocalDate? = null,
        snapshot: LocalDate? = null,
        distributeUnusedBudget: Boolean? = null,
    ): String {
        val startDateString = "-start_${startDate.year}-${startDate.monthValue.format2Digits()}"
        val partString = if (part.isNullOrBlank()) "" else "-${FilenameUtils.escapeFilename(part)}"
        val usePlanningDate = orderbookSnapshotsService.findClosestSnapshotDate(planningDate)
        val planningDateString = if (planningDate != null) "-plan_${usePlanningDate}" else ""
        val useSnapshot = orderbookSnapshotsService.findClosestSnapshotDate(snapshot)
        val snapshotString = if (useSnapshot != null) "-snapshot_${useSnapshot}" else ""
        val variantString = variantSuffix(distributeUnusedBudget)
        val created = DateHelper.getDateAsFilenameSuffix()
        return "${created}_Forecast$partString$planningDateString$snapshotString$startDateString$variantString${extension ?: ""}"
    }

    /**
     * @return '_optimistisch', if the unused budget is distributed, otherwise '_konservativ' (see
     * [ForecastOrderPosInfo.distributeUnusedBudget]). Both variants of the same forecast run may be compared, so the
     * variant must be part of every filename of the zip archive as well as of the archive itself.
     */
    open fun variantSuffix(distributeUnusedBudget: Boolean? = null): String {
        return if (distributeUnusedBudget ?: ForecastOrderPosInfo.defaultDistributeUnusedBudget) {
            "_optimistisch"
        } else {
            "_konservativ"
        }
    }

    /**
     * @return The human readable information (label and explanation) how the unused budget of time&amp;material
     * positions is handled, shown on the 'Info' sheet of the Excel export (multiline, the cell is wrapped).
     * The same wording is used by the single order analysis ([ForecastOrderAnalysis]).
     */
    open fun variantInfo(distributeUnusedBudget: Boolean): String {
        val i18nPrefix = "fibu.auftrag.forecast.analysis.variants.$distributeUnusedBudget"
        // The conservative explanation contains the ramp-up threshold as message parameter {0}:
        val explanation = translateMsg(i18nPrefix, ForecastOrderPosInfo.RUN_RATE_MIN_ELAPSED_MONTHS)
        return "${translate("$i18nPrefix.label")}\n$explanation"
    }

    /**
     * Export the forecast sheet.
     * @param orderList The list of orders to export.
     * @param startDate The start date for the forecast.
     * @param showAll True, if no filter is given, for financial and controlling staff only.
     * @param planningDate If given, the monthly forecast will be calculated with the specified date and inserted as plan data.
     * @param snapshotDate Today (null) or, the day of the snapshot, if the orderList is loaded from order book snapshots.
     *              If the date is in the past, the forecast will be simulated with the specified date.
     *              If date is given, no caches will be used.
     * @return The byte array of the Excel file.
     */
    @Throws(IOException::class)
    private fun xlsExport(
        orderList: Collection<AuftragDO>,
        startDate: PFDay,
        showAll: Boolean,
        planningDate: LocalDate?,
        snapshotDate: LocalDate?,
        auftragFilter: AuftragFilter,
        scriptLogger: ScriptLogger?,
        distributeUnusedBudget: Boolean,
        fillUnitCol: ((orderInfo: OrderInfo) -> String)?,
    ): ByteArray? {
        if (orderList.isEmpty()) {
            val msg = "No orders found for export."
            scriptLogger?.info { msg } ?: log.info { msg } // scriptLogger does also log.info
            // No orders found, so we don't need the forecast sheet.
            return null
        }
        val useAuftragsCache = snapshotDate == null
        val prevYearBaseDate = startDate.plusYears(-1) // One year back for getting all invoices.
        val prevPrevYearBaseDate = startDate.plusYears(-2) // Two years back for the prev-prev-year comparison.
        val invoiceFilter = RechnungFilter()
        invoiceFilter.fromDate =
            prevPrevYearBaseDate.plusDays(-1).localDate // Go 1 day back, paranoia setting for getting all invoices for given time period.
        if (snapshotDate != null) {
            // Don't load invoices later than snapshotDate:
            invoiceFilter.toDate = snapshotDate.minusDays(1)
        }
        val queryFilter = AuftragAndRechnungDaoHelper.createQueryFilterWithDateRestriction(invoiceFilter)
        queryFilter.addOrder(desc("datum"))
        queryFilter.addOrder(desc("nummer"))
        var invoices = rechnungDao.select(queryFilter, checkAccess = false)
        if (snapshotDate != null) {
            // For time travel: Filter invoices before the given date.
            invoices = invoices.filter { it.datum?.let { datum -> PFDay(datum).isBefore(snapshotDate) } ?: false }
        }
        val forecastTemplate = applicationContext.getResource("classpath:officeTemplates/ForecastTemplate.xlsx")

        ExcelWorkbook(forecastTemplate.inputStream, "ForecastTemplate.xlsx").use { workbook ->
            val forecastSheet = workbook.getSheet(Sheet.FORECAST.title)!!
            log.debug { "Forecast sheet: $forecastSheet" }
            ForecastCol.entries.forEach { forecastSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { forecastSheet.registerColumn(it.header) }

            val planningSheet = workbook.getSheet(Sheet.PLANNING.title)!!
            log.debug { "Planning forecast sheet: $planningSheet" }
            ForecastCol.entries.forEach { planningSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { planningSheet.registerColumn(it.header) }

            val invoicesSheet = workbook.getSheet(Sheet.INVOICES.title)!!
            log.debug { "Invoices sheet: $invoicesSheet" }
            InvoicesCol.entries.forEach { invoicesSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { invoicesSheet.registerColumn(it.header) }

            val invoicesPrevYearSheet = workbook.getSheet(Sheet.INVOICES_PREV_YEAR.title)!!
            log.debug { "InvoicesPriorYearSheet sheet: $invoicesPrevYearSheet" }
            InvoicesCol.entries.forEach { invoicesPrevYearSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { invoicesPrevYearSheet.registerColumn(it.header) }

            val invoicesPrevPrevYearSheet = workbook.getSheet(Sheet.INVOICES_PREV_PREV_YEAR.title)!!
            log.debug { "InvoicesPriorPriorYearSheet sheet: $invoicesPrevPrevYearSheet" }
            InvoicesCol.entries.forEach { invoicesPrevPrevYearSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { invoicesPrevPrevYearSheet.registerColumn(it.header) }

            val planningInvoicesSheet = workbook.getSheet(Sheet.PLANNING_INVOICES.title)!!
            log.debug { "PlanningInvoicesSheet sheet: $planningInvoicesSheet" }
            InvoicesCol.entries.forEach { planningInvoicesSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { planningInvoicesSheet.registerColumn(it.header) }

            // Optional overview sheet (one row per project). The user creates it in the template with header row and
            // autofilter; the code only fills the value rows. Missing sheet -> skip (older templates without it).
            val projectOverviewSheet = workbook.getSheet(Sheet.PROJECT_OVERVIEW.title)
            if (projectOverviewSheet != null) {
                log.debug { "Project overview sheet: $projectOverviewSheet" }
                ProjectOverviewCol.entries.forEach { projectOverviewSheet.registerColumn(it.header) }
            } else {
                log.info { "Template has no '${Sheet.PROJECT_OVERVIEW.title}' sheet, skipping project overview." }
            }

            val ctx = Context(
                workbook,
                forecastSheet = forecastSheet,
                invoicesSheet = invoicesSheet,
                invoicesPrevYearSheet = invoicesPrevYearSheet,
                invoicesPrevPrevYearSheet = invoicesPrevPrevYearSheet,
                planningSheet = planningSheet,
                planningInvoicesSheet = planningInvoicesSheet,
                projectOverviewSheet = projectOverviewSheet,
                startDate = startDate,
                invoices = invoices,
                baseDate = PFDay.fromOrNow(snapshotDate),
                planningDate = planningDate,
                snapshot = snapshotDate != null,
                fillUnitCol = fillUnitCol,
                distributeUnusedBudget = distributeUnusedBudget,
            )
            ctx.showAll = showAll

            val infoSheet = workbook.getSheet(Sheet.INFO.title)!!
            infoSheet.setDateValue(0, 1, Date(), ctx.excelDateFormat)
            planningDate?.let { infoSheet.setDateValue(1, 1, it, ctx.excelDateFormat) }
            snapshotDate?.let { infoSheet.setDateValue(2, 1, it, ctx.excelDateFormat) }
            // Row 4 (0-based 3) of the info sheet: how the unused budget of time&material positions is handled in
            // this run (see projectforge.fibu.forecast.distributeUnusedBudget):
            infoSheet.setStringValue(3, 1, variantInfo(ctx.distributeUnusedBudget))
            log.debug { "info sheet: $infoSheet" }

            analyzeOrderPositions(orderList, ctx, planningData = false)
            analyzePlanningForecast(planningDate, auftragFilter, ctx)
            forecastExportInvoices.fillInvoices(ctx)
            val orderPositionsFound =
                fillOrderPositions(
                    orderList,
                    ctx,
                    ctx.forecastSheet,
                    baseDate = snapshotDate,
                    useAuftragsCache,
                )
            if (!orderPositionsFound && ctx.invoicedProjectIds.isEmpty()) {
                val msg = "Neither orders positions nor invoices found for export."
                scriptLogger?.info { msg } ?: log.info { msg } // scriptLogger does also log.info
                // No order positions found, so we don't need the forecast sheet.
                return null
            }
            replaceMonthDatesInHeaderRow(forecastSheet, startDate, true)
            replaceMonthDatesInHeaderRow(planningSheet, startDate, true)
            replaceMonthDatesInHeaderRow(invoicesSheet, startDate)
            replaceMonthDatesInHeaderRow(invoicesPrevYearSheet, prevYearBaseDate)
            replaceMonthDatesInHeaderRow(invoicesPrevPrevYearSheet, prevPrevYearBaseDate)
            replaceMonthDatesInHeaderRow(planningInvoicesSheet, startDate)
            if (!ctx.hasUnitColEntries) {
                ExcelUtils.setColumnHidden(forecastSheet, ForecastCol.UNIT.header, true)
                ExcelUtils.setColumnHidden(planningSheet, ForecastCol.UNIT.header, true)
            }
            ExcelUtils.setAutoFilter(forecastSheet, FORECAST_HEAD_ROW, 0, FORECAST_NUMBER_OF_COLS_AUTOFILTER)
            invoicesSheet.setAutoFilter()
            invoicesPrevYearSheet.setAutoFilter()
            invoicesPrevPrevYearSheet.setAutoFilter()
            ExcelUtils.setAutoFilter(planningSheet, FORECAST_HEAD_ROW, 0, FORECAST_NUMBER_OF_COLS_AUTOFILTER)
            planningInvoicesSheet.setAutoFilter()

            fillPlanningForecast(planningDate, ctx)
            fillProjectOverviewSheet(ctx)
            workbook.pOIWorkbook.creationHelper.createFormulaEvaluator().evaluateAll()
            return workbook.asByteArrayOutputStream.toByteArray()
        }
    }

    private fun analyzeOrderPositions(
        orderList: Collection<AuftragDO>,
        ctx: Context,
        planningData: Boolean,
    ) {
        for (auftragDO in orderList) {
            auftragDO.projekt?.id?.let { projektId ->
                ctx.projectIds.add(projektId)
            }
            val orderInfo = if (planningData || ctx.snapshot) {
                auftragDO.info // Can't load planning data from cache (it's read from snapshots).
            } else {
                ordersCache.getOrderInfo(auftragDO)
            }
            auftragDO.id?.let { id ->
                if (planningData) {
                    ctx.planningOrderMap[id] = orderInfo
                } else {
                    ctx.orderMap[id] = orderInfo
                }
            }
            orderInfo.infoPositions?.forEach { pos ->
                pos.id?.let {
                    ctx.orderPositionMap[it] = pos // Register all order positions for invoice handling.
                    ctx.orderMapByPositionId[it] = orderInfo
                }
            }
        }
    }

    private fun analyzePlanningForecast(planningDate: LocalDate?, auftragFilter: AuftragFilter, ctx: Context) {
        planningDate ?: return
        val orderList = readSnapshot(planningDate, auftragFilter)
        analyzeOrderPositions(orderList, ctx, planningData = true)
        ctx.planningOrderList = orderList
    }

    /**
     * Fill the forecast data of order positions.
     * @param orderList The list of orders to export.
     * @param ctx The context for the export.
     * @param sheet The Excelsheet to use (forecast or planning)
     * @param useAuftragsCache True, if the orders cache should be used for updated order info, otherwise false (for snapshots and plannings the
     *                         cache shouldn't be used.).
     * @return true, if order positions found and filled, otherwise false.
     */
    private fun fillOrderPositions(
        orderList: Collection<AuftragDO>,
        ctx: Context,
        sheet: ExcelSheet,
        baseDate: LocalDate?,
        useAuftragsCache: Boolean,
    ): Boolean {
        val planning = (sheet == ctx.planningSheet)
        // Set the date in the upper left corner (red and bold) for showing date of snapshot/orderbook.
        sheet.getCell(1, 0)?.setCellValue(baseDate ?: ctx.baseDate.localDate)
        sheet.getRow(FORECAST_HEAD_ROW)
        var currentRow = FORECAST_FISRT_ORDER_ROW
        var orderPositionFound = false
        for (auftragDO in orderList) {
            val orderInfo = if (planning) {
                ctx.planningOrderMap[auftragDO.id] // Must be set by analyzeOrderPositions.
            } else {
                ctx.orderMap[auftragDO.id] // Must be set by analyzeOrderPositions.
            }
            if (orderInfo == null) {
                log.error { "Shouldn't occur: orderInfo not found for order: $auftragDO" }
                continue
            }
            if (auftragDO.deleted || orderInfo.infoPositions.isNullOrEmpty()) {
                continue
            }
            if (ForecastUtils.auftragsStatusToShow.contains(auftragDO.status)) {
                orderInfo.infoPositions?.forEach { pos ->
                    if (pos.status in ForecastUtils.auftragsPositionsStatusToShow && isRelevant(ctx, orderInfo, pos)) {
                        addOrderPosition(
                            ctx,
                            sheet,
                            currentRow++,
                            orderInfo,
                            pos,
                            baseDate = baseDate,
                            useAuftragsCache = useAuftragsCache,
                        )
                        orderPositionFound = true
                    }
                }
            }
        }
        if (sheet == ctx.forecastSheet) {
            if (ctx.hasInvoicesWithoutProject) {
                // Invoices without any assignable project can't be filtered by unit, customer or project. They are
                // represented by this single pseudo order row: visible (and thus part of the sums) as long as no filter
                // is set, hidden as soon as the user filters.
                OrderInfo().let { orderInfo ->
                    orderInfo.nummer = 0
                    orderInfo.projektId = Context.PROJECT_ID_NONE
                    orderInfo.status = AuftragsStatus.IN_ERSTELLUNG
                    orderInfo.angebotsDatum = baseDate
                    orderInfo.titel = translate("fibu.auftrag.forecast.withoutProject")
                    orderInfo.projektAsString = orderInfo.titel
                    OrderPositionInfo().let { posInfo ->
                        posInfo.auftrag = orderInfo
                        posInfo.status = AuftragsStatus.IN_ERSTELLUNG
                        posInfo.titel = orderInfo.titel
                        posInfo.number = 0
                        addOrderPosition(
                            ctx, sheet, currentRow++, orderInfo, posInfo, baseDate = baseDate,
                            useAuftragsCache = useAuftragsCache
                        )
                    }
                }
            }
            val missedProjectIds = ctx.invoicedProjectIds - ctx.orderProjectIds
            missedProjectIds.forEach { projectId ->
                // For all projects that have been invoiced but for which no
                // order is included in the forecast, pseudo orders are entered in the forecast in order to have all projects
                // visible in the forecast.
                val project = projectCache.getProjekt(projectId)
                OrderInfo().let { orderInfo ->
                    orderInfo.nummer = 0
                    orderInfo.projektId = projectId
                    orderInfo.status = AuftragsStatus.IN_ERSTELLUNG
                    orderInfo.angebotsDatum = baseDate
                    orderInfo.titel = "Pseudo order for project $projectId, because this project was invoiced."
                    orderInfo.kundeAsString = project?.kundeAsString
                    orderInfo.projektAsString = project?.name
                    OrderPositionInfo().let { posInfo ->
                        posInfo.auftrag = orderInfo
                        posInfo.status = AuftragsStatus.IN_ERSTELLUNG
                        posInfo.titel = orderInfo.titel
                        posInfo.number = 0
                        addOrderPosition(
                            ctx, sheet, currentRow++, orderInfo, posInfo, baseDate = baseDate,
                            useAuftragsCache = useAuftragsCache
                        )
                    }
                }
            }
        }
        return orderPositionFound
    }

    /**
     * The order list is loaded 3 years back, because invoices of the two prior years must be able to find their orders
     * (see [xlsExport]). Such old orders would only clutter the sheet and the filter drop downs, so only positions are
     * written as rows which are relevant for this export:
     * - their period of performance overlaps the exported 12 months, or
     * - their project was invoiced in one of the three year windows: these rows carry unit/customer for propagating the
     *   filter selection to the (prev/prev-prev year) invoice sheets and must not be dropped.
     */
    private fun isRelevant(ctx: Context, order: OrderInfo, pos: OrderPositionInfo): Boolean {
        if (order.projektId?.let { ctx.invoicedProjectIds.contains(it) } == true) {
            return true
        }
        // The forecast revenue of a position may appear one month after the end of the performance period
        // (AuftragForecastType.FOLLOWING_MONTH), so start one month earlier:
        val from = ctx.startDate.plusMonths(-1)
        val end = ForecastUtils.getEndLeistungszeitraum(order, pos)
        if (!end.isBefore(from) && !ForecastUtils.getStartLeistungszeitraum(order, pos).isAfter(ctx.endDate)) {
            return true
        }
        // Payment schedules may be scheduled after the end of the performance period (see ForecastOrderPosInfo.createMonths):
        return ForecastUtils.getPaymentSchedule(order, pos).any { schedule ->
            schedule.scheduleDate?.let { it >= from.localDate && it <= ctx.endDate.localDate } == true
        }
    }

    private fun fillPlanningForecast(planningDate: LocalDate?, ctx: Context) {
        planningDate ?: return
        val orderList = ctx.planningOrderList ?: return
        fillOrderPositions(
            orderList,
            ctx,
            ctx.planningSheet,
            baseDate = planningDate,
            useAuftragsCache = false,
        )
    }

    private fun replaceMonthDatesInHeaderRow(
        sheet: ExcelSheet,
        baseDate: PFDay,
        planningSheet: Boolean = false
    ) { // Adding month columns
        var currentMonth = baseDate
        var firstMonthCell: ExcelCell? = null
        var lastMonthCell: ExcelCell? = null
        MonthCol.entries.forEach {
            sheet.headRow!!.getCell(sheet.getColumnDef(it.header)!!).also { cell ->
                if (firstMonthCell == null) {
                    firstMonthCell = cell
                }
                lastMonthCell = cell
                cell.setCellValue(formatMonthHeader(currentMonth))
            }
            if (planningSheet) {
                // Second head row for planning sheet:
                sheet.getRow(FORECAST_HEAD_ROW).getCell(sheet.getColumnDef(it.header)!!)
                    .setCellValue(formatMonthHeader(currentMonth))
            }
            currentMonth = currentMonth.plusMonths(1)
        }
        if (planningSheet) {
            // Clear first heading row for planning sheet:
            sheet.headRow?.let { row ->
                ExcelUtils.clearCells(row, 0, firstMonthCell!!.colNumber - 1)
                ExcelUtils.clearCells(row, lastMonthCell!!.colNumber + 1, FORECAST_NUMBER_OF_COLS)
            }
        }
    }

    private fun addOrderPosition(
        ctx: Context,
        sheet: ExcelSheet,
        row: Int,
        order: OrderInfo,
        pos: OrderPositionInfo,
        baseDate: LocalDate?,
        useAuftragsCache: Boolean,
    ) {
        order.projektId?.let { ctx.orderProjectIds.add(it) }
        val isPlanningSheet = ctx.planningSheet == sheet
        sheet.setIntValue(row, ForecastCol.ORDER_NR.header, order.nummer)
        sheet.setStringValue(row, ForecastCol.POS_NR.header, "#${pos.number}")
        ExcelUtils.setLongValue(sheet, row, ForecastCol.PROJECT_ID.header, order.projektId)
        val excelRow = row + 1 // Excel row number for formulas, 1-based.
        if (isPlanningSheet) {
            // Planning sheet: visible column is true, if the project is visible in the forecast sheet.
            val visibleProjectIdCol =
                ctx.forecastSheet.getColumnDef(ForecastCol.VISIBLE_PROJECT_ID.header)?.columnNumberAsLetters
            val projectIdCol = ctx.forecastSheet.getColumnDef(ForecastCol.PROJECT_ID.header)?.columnNumberAsLetters
            ExcelUtils.setCellFormula(
                sheet,
                row,
                ForecastCol.VISIBLE.header,
                "COUNTIF(Forecast_Data!$visibleProjectIdCol$11:$visibleProjectIdCol$100000, $projectIdCol$excelRow) > 0"
            )
        } else {
            // Visible cell is 1, if row is visible (by filter), otherwise, 0.
            ExcelUtils.setCellFormula(sheet, row, ForecastCol.VISIBLE.header, "SUBTOTAL(3, A$excelRow)")
            val visibleCol = ctx.forecastSheet.getColumnDef(ForecastCol.VISIBLE.header)?.columnNumberAsLetters
            val projectIdCol = ctx.forecastSheet.getColumnDef(ForecastCol.PROJECT_ID.header)?.columnNumberAsLetters
            // Blank project ids must not fall through as numeric 0: COUNTIF of the invoice sheets would then match
            // every invoice without project against every order row without project, no matter which filter is set.
            ExcelUtils.setCellFormula(
                sheet,
                row,
                ForecastCol.VISIBLE_PROJECT_ID.header,
                "IF(AND($visibleCol$excelRow=1, $projectIdCol$excelRow<>\"\"), $projectIdCol$excelRow, \"\")"
            )
        }
        order.angebotsDatum?.let {
            sheet.setDateValue(row, ForecastCol.DATE_OF_OFFER.header, PFDay(it).localDate, ctx.excelDateFormat)
        }
        ForecastUtils.ensureErfassungsDatum(order).let {
            sheet.setDateValue(row, ForecastCol.DATE.header, PFDay(it).localDate, ctx.excelDateFormat)
        }
        order.entscheidungsDatum?.let {
            sheet.setDateValue(row, ForecastCol.DATE_OF_DECISION.header, PFDay(it).localDate, ctx.excelDateFormat)
        }
        ctx.fillUnitCol?.invoke(order)?.let {
            if (it.isNotBlank()) {
                ctx.hasUnitColEntries = true
            }
            sheet.setStringValue(row, ForecastCol.UNIT.header, it)
        }
        sheet.setStringValue(row, ForecastCol.CUSTOMER.header, order.kundeAsString)
        sheet.setStringValue(row, ForecastCol.PROJECT.header, order.projektAsString)
        sheet.setStringValue(row, ForecastCol.TITEL.header, order.titel)
        if (pos.titel != order.titel)
            sheet.setStringValue(row, ForecastCol.POS_TITLE.header, pos.titel)
        pos.art.let { art ->
            sheet.setStringValue(row, ForecastCol.ART.header, if (art != null) translate(art.i18nKey) else "")
        }
        pos.paymentType.let { paymentType ->
            sheet.setStringValue(
                row,
                ForecastCol.ABRECHNUNGSART.header,
                if (paymentType != null) translate(paymentType.i18nKey) else ""
            )
        }
        sheet.setStringValue(row, ForecastCol.AUFTRAG_STATUS.header, translate(order.status.i18nKey))
        sheet.setStringValue(row, ForecastCol.POSITION_STATUS.header, translate(pos.status.i18nKey))
        sheet.setIntValue(row, ForecastCol.PT.header, pos.personDays?.toInt() ?: 0)
        sheet.setBigDecimalValue(
            row, ForecastCol.NETTOSUMME.header, pos.netSum
                ?: BigDecimal.ZERO
        ).cellStyle = ctx.currencyCellStyle

        val orderInfo = if (useAuftragsCache) {
            ordersCache.getOrderInfo(order.id)
        } else {
            order
        }
        val posInfo = orderInfo?.getInfoPosition(pos.id)
        val netSum = pos.netSum ?: BigDecimal.ZERO
        val invoicedSum = posInfo?.invoicedSum ?: BigDecimal.ZERO
        val forecastInfo = ForecastOrderPosInfo(order, pos)
        forecastInfo.distributeUnusedBudget = ctx.distributeUnusedBudget
        // Distribution must not re-forecast months already covered by actual invoices (respecting baseDate):
        forecastInfo.lastInvoiceMonth = rechnungCache.getRechnungsPosInfosByAuftragsPositionId(pos.id)
            ?.filter { baseDate == null || (it.rechnungInfo?.date ?: LocalDate.MAX) <= baseDate }
            ?.mapNotNull { it.rechnungInfo?.date }
            ?.maxOrNull()
            ?.let { PFDay.from(it).beginOfMonth }
        forecastInfo.calculate()
        sheet.setBigDecimalValue(row, ForecastCol.NETTOSUMME.header, netSum).cellStyle = ctx.currencyCellStyle
        if (invoicedSum.compareTo(BigDecimal.ZERO) != 0) {
            sheet.setBigDecimalValue(row, ForecastCol.FAKTURIERT.header, invoicedSum).cellStyle =
                ctx.currencyCellStyle
        }
        val toBeInvoicedSum = forecastInfo.toBeInvoicedSum
        if (toBeInvoicedSum.compareTo(BigDecimal.ZERO) != 0) {
            sheet.setBigDecimalValue(row, ForecastCol.TO_BE_INVOICED.header, toBeInvoicedSum).cellStyle =
                ctx.currencyCellStyle
        }
        sheet.setStringValue(
            row,
            ForecastCol.VOLLSTAENDIG_FAKTURIERT.header,
            if (pos.vollstaendigFakturiert) "x" else ""
        )

        val invoicePositions = rechnungCache.getRechnungsPosInfosByAuftragsPositionId(pos.id)?.filter {
            // Don't load invoices later than snapshotDate or planningDate (baseDate):
            baseDate == null || (it.rechnungInfo?.date ?: LocalDate.MAX) <= baseDate
        }
        sheet.setStringValue(
            row,
            ForecastCol.DEBITOREN_RECHNUNGEN.header,
            ForecastUtils.getInvoices(invoicePositions)
        )
        val leistungsZeitraumColDef = sheet.getColumnDef(ForecastCol.LEISTUNGSZEITRAUM.header)!!
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) { // use "own" period -> from pos
            // Log warning if position dates are null but type is OWN (data integrity issue)
            if (pos.periodOfPerformanceBegin == null || pos.periodOfPerformanceEnd == null) {
                log.warn {
                    "Order position #${pos.number} (Order #${order.nummer}) has periodOfPerformanceType=OWN " +
                    "but null dates. Falling back to order dates. " +
                    "Position dates: begin=${pos.periodOfPerformanceBegin}, end=${pos.periodOfPerformanceEnd}"
                }
            }

            sheet.setDateValue(
                row,
                leistungsZeitraumColDef,
                PFDay.fromOrNull(pos.periodOfPerformanceBegin ?: order.periodOfPerformanceBegin)?.localDate,
                ctx.excelDateFormat
            )
            sheet.setDateValue(
                row,
                leistungsZeitraumColDef.columnNumber + 1,
                PFDay.fromOrNull(pos.periodOfPerformanceEnd ?: order.periodOfPerformanceEnd)?.localDate,
                ctx.excelDateFormat
            )
        } else { // use "see above" period -> from order
            sheet.setDateValue(
                row,
                leistungsZeitraumColDef,
                PFDay.fromOrNull(order.periodOfPerformanceBegin)?.localDate,
                ctx.excelDateFormat
            )
            sheet.setDateValue(
                row,
                leistungsZeitraumColDef.columnNumber + 1,
                PFDay.fromOrNull(order.periodOfPerformanceEnd)?.localDate,
                ctx.excelDateFormat
            )
        }

        sheet.setBigDecimalValue(
            row,
            ForecastCol.EINTRITTSWAHRSCHEINLICHKEIT.header,
            forecastInfo.probability,
        ).cellStyle =
            ctx.percentageCellStyle

        sheet.setBigDecimalValue(
            row,
            ForecastCol.PROBABILITY_NETSUM.header,
            forecastInfo.weightedNetSum
        ).cellStyle =
            ctx.currencyCellStyle

        sheet.setStringValue(row, ForecastCol.ANSPRECHPARTNER.header, order.contactPerson?.getFullname())
        val node = TaskTree.instance.getTaskNodeById(pos.taskId)
        sheet.setStringValue(row, ForecastCol.STRUKTUR_ELEMENT.header, node?.task?.title ?: "")
        sheet.setStringValue(row, ForecastCol.BEMERKUNG.header, pos.bemerkung)

        sheet.setBigDecimalValue(
            row,
            ForecastCol.ANZAHL_MONATE.header,
            ForecastUtils.getMonthCountForOrderPosition(order, pos)
        )
        sheet.setStringValue(
            row,
            ForecastCol.FORECAST_TYPE.header,
            translate(ForecastUtils.getForecastType(order, pos).i18nKey)
        )
        val remaining = forecastInfo.getRemainingForecastSumAfter(ctx.endDate)
        if (remaining.compareTo(BigDecimal.ZERO) != 0) {
            sheet.setBigDecimalValue(row, ForecastCol.REMAINING.header, remaining).cellStyle =
                ctx.currencyCellStyle
        }
        if (forecastInfo.difference.compareTo(BigDecimal.ZERO) != 0) {
            sheet.setBigDecimalValue(row, ForecastCol.DIFFERENCE.header, forecastInfo.difference).cellStyle =
                ctx.currencyCellStyle
        }

        if (forecastInfo.paymentEntries.isNotEmpty()) {
            val str = forecastInfo.paymentEntries.joinToString {
                "${it.scheduleDate.format(ctx.dateFormat)}: ${
                    ctx.currencyFormat.format(it.amount)
                }"
            }
            sheet.setStringValue(row, ForecastCol.PAYMENT_SCHEDULE.header, str)
        }
        order.statusBeschreibung?.let {
            sheet.setStringValue(row, ForecastCol.STATUS_BESCHREIBUNG.header, it)
        }
        forecastInfo.months.forEach { monthEntry ->
            val monthDate = monthEntry.date
            val offset = ctx.startDate.monthsBetween(monthDate).toInt()
            if (offset !in 0..11) {
                return@forEach // continue
            }
            if (monthEntry.toBeInvoicedSum.abs() < BigDecimal.ONE) {
                return@forEach // continue
            }
            val columnDef = sheet.getColumnDef(MonthCol.entries[offset].header)!!
            val cell =
                sheet.setBigDecimalValue(
                    row,
                    columnDef,
                    monthEntry.toBeInvoicedSum.setScale(2, RoundingMode.HALF_UP),
                )
            cell.cellStyle = ctx.currencyCellStyle
            if (monthEntry.lostBudgetWarning) {
                val errorStyle = when {
                    monthEntry.lostBudget > NumberHelper.HUNDRED_THOUSAND -> ctx.hugeErrorCellStyle
                    monthEntry.lostBudget > NumberHelper.TEN_THOUSAND -> ctx.largeErrorCellStyle
                    else -> ctx.errorCellStyle
                }
                sheet.setStringValue(
                    row,
                    ForecastCol.WARNING.header,
                    translateMsg(
                        "fibu.auftrag.forecast.lostBudgetWarning",
                        monthEntry.lostBudget.formatCurrency(true, scale = 0),
                        monthEntry.lostBudgetPercent,
                        ForecastOrderPosInfo.PERCENTAGE_OF_LOST_BUDGET_WARNING,
                    )
                ).cellStyle = errorStyle
                sheet.getCell(row, columnDef.columnNumber)?.cellStyle = ctx.errorCurrencyCellStyle
            }
        }
    }

    /**
     * Per-project accumulator for the [Sheet.PROJECT_OVERVIEW] sheet. All values are summed over the month columns of
     * the respective detail sheet for a single project id.
     */
    private class ProjectOverviewAgg {
        var unit: String? = null
        var customer: String? = null
        var project: String? = null
        var forecastRemaining: BigDecimal = BigDecimal.ZERO // Rest forecast (Forecast_Data)
        var ist: BigDecimal = BigDecimal.ZERO               // already invoiced (Rechnungen)
        var planRemaining: BigDecimal = BigDecimal.ZERO     // Rest plan (Planning_Data)
        var planIst: BigDecimal = BigDecimal.ZERO           // plan already invoiced (Planning_Invoices)
        var prevYear: BigDecimal = BigDecimal.ZERO          // Rechnungen Vorjahr
        var prevPrevYear: BigDecimal = BigDecimal.ZERO      // Rechnungen Vorvorjahr
    }

    /**
     * Fills the optional [Sheet.PROJECT_OVERVIEW] sheet with one row per project. This is a pure re-presentation of the
     * data already written to the detail sheets — no new calculation. All values are aggregated per project id from the
     * literal month cells of the detail sheets (read before [org.apache.poi.ss.usermodel.FormulaEvaluator.evaluateAll]
     * is fine, because those cells are literal numbers, not formulas).
     *
     * Column mapping (sum per project id over the detail rows):
     * - Plan       = Σ Planning_Data months + Σ Planning_Invoices months
     * - Forecast   = Σ Forecast_Data months (remaining forecast) + Σ Rechnungen months (already invoiced)
     * - Vorjahr    = Σ Rechnungen Vorjahr months
     * - Vorvorjahr = Σ Rechnungen Vorvorjahr months
     * - Differenz Plan/Vorjahr/Vorvorjahr = Forecast − Plan/Vorjahr/Vorvorjahr
     *
     * All columns are addressed by header name only, so the user may reorder the columns in the template.
     */
    private fun fillProjectOverviewSheet(ctx: Context) {
        val sheet = ctx.projectOverviewSheet ?: return
        val map = mutableMapOf<Long, ProjectOverviewAgg>()

        // Forecast_Data: remaining (weighted) forecast + unit/customer/project labels.
        accumulateMonths(ctx.forecastSheet, ForecastCol.PROJECT_ID.header, map) { agg, row ->
            agg.forecastRemaining = agg.forecastRemaining.add(sumMonths(ctx.forecastSheet, row))
            if (agg.unit.isNullOrBlank()) agg.unit = ctx.forecastSheet.getCellString(row, ForecastCol.UNIT.header)
            if (agg.customer.isNullOrBlank()) agg.customer =
                ctx.forecastSheet.getCellString(row, ForecastCol.CUSTOMER.header)
            if (agg.project.isNullOrBlank()) agg.project =
                ctx.forecastSheet.getCellString(row, ForecastCol.PROJECT.header)
        }
        // Rechnungen: already invoiced part of the current year (IST).
        accumulateMonths(ctx.invoicesSheet, InvoicesCol.PROJECT_ID.header, map) { agg, row ->
            agg.ist = agg.ist.add(sumMonths(ctx.invoicesSheet, row))
            if (agg.customer.isNullOrBlank()) agg.customer =
                ctx.invoicesSheet.getCellString(row, InvoicesCol.CUSTOMER.header)
            if (agg.project.isNullOrBlank()) agg.project =
                ctx.invoicesSheet.getCellString(row, InvoicesCol.PROJECT.header)
        }
        // Planning_Data: remaining plan.
        accumulateMonths(ctx.planningSheet, ForecastCol.PROJECT_ID.header, map) { agg, row ->
            agg.planRemaining = agg.planRemaining.add(sumMonths(ctx.planningSheet, row))
        }
        // Planning_Invoices: already invoiced part of the plan (IST at planning date).
        accumulateMonths(ctx.planningInvoicesSheet, InvoicesCol.PROJECT_ID.header, map) { agg, row ->
            agg.planIst = agg.planIst.add(sumMonths(ctx.planningInvoicesSheet, row))
        }
        // Rechnungen Vorjahr / Vorvorjahr.
        accumulateMonths(ctx.invoicesPrevYearSheet, InvoicesCol.PROJECT_ID.header, map) { agg, row ->
            agg.prevYear = agg.prevYear.add(sumMonths(ctx.invoicesPrevYearSheet, row))
        }
        accumulateMonths(ctx.invoicesPrevPrevYearSheet, InvoicesCol.PROJECT_ID.header, map) { agg, row ->
            agg.prevPrevYear = agg.prevPrevYear.add(sumMonths(ctx.invoicesPrevPrevYearSheet, row))
        }

        // Resolve missing customer/project labels via the project cache (invoices without an order carry no labels).
        map.forEach { (id, agg) ->
            if (id != Context.PROJECT_ID_NONE && (agg.customer.isNullOrBlank() || agg.project.isNullOrBlank())) {
                projectCache.getProjekt(id)?.let { projekt ->
                    if (agg.project.isNullOrBlank()) agg.project = projekt.name
                    if (agg.customer.isNullOrBlank()) agg.customer = projekt.kunde?.name
                }
            }
        }

        val entries = map.entries.sortedWith(
            compareBy({ it.value.unit ?: "" }, { it.value.customer ?: "" }, { it.value.project ?: "" })
        )
        // Write directly below the header row (row 2 in Excel if the header is row 1). Don't use createRow(), which
        // would append after any empty/pre-formatted rows the template carries.
        var rowNum = (sheet.headRow?.rowNum ?: 0) + 1
        for ((id, agg) in entries) {
            val plan = agg.planRemaining.add(agg.planIst)
            val forecast = agg.forecastRemaining.add(agg.ist)
            val prevYear = agg.prevYear
            val prevPrevYear = agg.prevPrevYear
            sheet.getRow(rowNum) // Ensure the row exists before setting values.
            setOverviewString(sheet, rowNum, ProjectOverviewCol.UNIT.header, agg.unit)
            setOverviewString(sheet, rowNum, ProjectOverviewCol.CUSTOMER.header, agg.customer)
            val projectLabel = if (id == Context.PROJECT_ID_NONE && agg.project.isNullOrBlank()) {
                "(ohne Projekt)"
            } else {
                agg.project
            }
            setOverviewString(sheet, rowNum, ProjectOverviewCol.PROJECT.header, projectLabel)
            setOverviewCurrency(ctx, sheet, rowNum, ProjectOverviewCol.PLAN.header, plan)
            setOverviewCurrency(ctx, sheet, rowNum, ProjectOverviewCol.FORECAST.header, forecast)
            setOverviewCurrency(ctx, sheet, rowNum, ProjectOverviewCol.PREV_YEAR.header, prevYear)
            setOverviewCurrency(ctx, sheet, rowNum, ProjectOverviewCol.PREV_PREV_YEAR.header, prevPrevYear)
            setOverviewCurrency(ctx, sheet, rowNum, ProjectOverviewCol.DIFF_PLAN.header, forecast.subtract(plan))
            setOverviewCurrency(ctx, sheet, rowNum, ProjectOverviewCol.DIFF_PREV_YEAR.header, forecast.subtract(prevYear))
            setOverviewCurrency(
                ctx, sheet, rowNum, ProjectOverviewCol.DIFF_PREV_PREV_YEAR.header, forecast.subtract(prevPrevYear)
            )
            rowNum++
        }
        // Re-apply the autofilter over the header row so it stays consistent after the rows were added.
        sheet.setAutoFilter()
    }

    /**
     * Iterates the data rows of [sheet] and invokes [block] per row that carries a numeric project id in the given
     * [projectIdHeader] column. Rows without that column or without a numeric project id are skipped. No-op if the
     * sheet doesn't contain the project id column (columns are addressed by header name only).
     */
    private fun accumulateMonths(
        sheet: ExcelSheet,
        projectIdHeader: String,
        map: MutableMap<Long, ProjectOverviewAgg>,
        block: (agg: ProjectOverviewAgg, row: Row) -> Unit,
    ) {
        if (sheet.getColumnDef(projectIdHeader) == null) {
            log.warn { "Sheet '${sheet.sheetName}' has no column '$projectIdHeader', skipping for project overview." }
            return
        }
        val it = sheet.dataRowIterator
        while (it.hasNext()) {
            val row = it.next()
            // Skip header/summary rows and rows without a project id: their ProjectID cell is non-numeric (text or
            // empty), so readNumeric returns null. This also guards against merlin picking a head row above row 9.
            val id = readNumeric(sheet, row, projectIdHeader)?.toLong() ?: continue
            block(map.getOrPut(id) { ProjectOverviewAgg() }, row)
        }
    }

    /**
     * Sums the 12 month columns (Month 1..Month 12) of [row] on [sheet]. Missing columns and non-numeric/empty cells
     * count as zero.
     */
    private fun sumMonths(sheet: ExcelSheet, row: Row): BigDecimal {
        var sum = BigDecimal.ZERO
        MonthCol.entries.forEach { monthCol ->
            readNumeric(sheet, row, monthCol.header)?.let { sum = sum.add(it) }
        }
        return sum
    }

    /**
     * Reads the numeric value of [header] in [row], or null if the column is missing or the cell isn't numeric (text,
     * empty or blank). Unlike merlin's getCellDouble this doesn't log a warning for non-numeric cells, which are
     * expected here (header/summary rows, empty month cells).
     */
    private fun readNumeric(sheet: ExcelSheet, row: Row, header: String): BigDecimal? {
        val cell = sheet.getCell(row, header, false) ?: return null
        if (cell.cellType != CellType.NUMERIC) return null
        return BigDecimal.valueOf(cell.numericCellValue)
    }

    private fun setOverviewString(sheet: ExcelSheet, rowNum: Int, header: String, value: String?) {
        if (sheet.getColumnDef(header) == null) return
        sheet.setStringValue(rowNum, header, value ?: "")
    }

    private fun setOverviewCurrency(ctx: Context, sheet: ExcelSheet, rowNum: Int, header: String, value: BigDecimal) {
        if (sheet.getColumnDef(header) == null) return
        sheet.setBigDecimalValue(rowNum, header, value.setScale(2, RoundingMode.HALF_UP)).cellStyle =
            ctx.currencyCellStyle
    }

    private fun readSnapshot(date: LocalDate, filter: AuftragFilter): List<AuftragDO> {
        return orderbookSnapshotsService.readSnapshot(date)?.filter { filter.match(it) }
            ?.sortedByDescending { it.nummer } ?: emptyList()
    }

    companion object {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

        private const val FORECAST_HEAD_ROW = 9
        private const val FORECAST_FISRT_ORDER_ROW = FORECAST_HEAD_ROW + 1
        private const val FORECAST_NUMBER_OF_COLS_AUTOFILTER = 47

        // Two more technical cols: ProjectID, visible and visibleID
        private const val FORECAST_NUMBER_OF_COLS = FORECAST_NUMBER_OF_COLS_AUTOFILTER + 3

        fun formatMonthHeader(date: PFDay): String {
            return date.format(formatter)
        }
    }
}
