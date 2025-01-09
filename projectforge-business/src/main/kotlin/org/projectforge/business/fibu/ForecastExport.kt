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

package org.projectforge.business.fibu

import de.micromata.merlin.I18n
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import de.micromata.merlin.excel.ExcelWriterContext
import mu.KotlinLogging
import org.apache.poi.ss.usermodel.IndexedColors
import org.projectforge.Constants
import org.projectforge.business.excel.ExcelDateFormats
import org.projectforge.business.excel.XlsContentProvider
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.fibu.orderbooksnapshots.OrderbookSnapshotsService
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.common.DateFormatType
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Forcast excel export based on order book with probabilities as well as on already invoiced orders.
 *
 * @author Florian Blumenstein
 * @author Kai Reinhard
 */
@Service
open class ForecastExport { // open needed by Wicket.
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var orderbookSnapshotsService: OrderbookSnapshotsService

    @Autowired
    private lateinit var orderDao: AuftragDao

    @Autowired
    private lateinit var ordersCache: AuftragsCache

    @Autowired
    private lateinit var projektCache: ProjektCache

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    enum class Sheet(val title: String) {
        FORECAST("Forecast_Data"),
        INVOICES("Rechnungen"),
        INVOICES_PREV_YEAR("Rechnungen Vorjahr"),
        INFO("Info")
    }

    enum class ForecastCol(val header: String) {
        ORDER_NR("Nr."), POS_NR("Pos."), DATE_OF_OFFER("Angebotsdatum"), DATE("Erfassungsdatum"),
        DATE_OF_DECISION("Entscheidungsdatum"), CUSTOMER("Kunde"), PROJECT("Projekt"),
        TITEL("Titel"), POS_TITLE("Pos.-Titel"), ART("Art"), ABRECHNUNGSART("Abrechnungsart"),
        AUFTRAG_STATUS("Auftrag Status"), POSITION_STATUS("Position Status"),
        PT("PT"), NETTOSUMME("Nettosumme"), FAKTURIERT("fakturiert"),
        TO_BE_INVOICED("gewichtet offen"), VOLLSTAENDIG_FAKTURIERT("vollständig fakturiert"),
        DEBITOREN_RECHNUNGEN("Debitorenrechnungen"), LEISTUNGSZEITRAUM("Leistungszeitraum"),
        EINTRITTSWAHRSCHEINLICHKEIT("Eintrittswahrsch. in %"), ANSPRECHPARTNER("Ansprechpartner"),
        STRUKTUR_ELEMENT("Strukturelement"), BEMERKUNG("Bemerkung"), PROBABILITY_NETSUM("gewichtete Nettosumme"),
        ANZAHL_MONATE("Anzahl Monate"), PAYMENT_SCHEDULE("Zahlplan"),
        REMAINING("Offen"), DIFFERENCE("Abweichung")
    }

    enum class InvoicesCol(val header: String) {
        INVOICE_NR("Nr."), POS_NR("Pos."), DATE("Datum"), CUSTOMER("Kunde"), PROJECT("Projekt"),
        SUBJECT("Betreff"), POS_TEXT("Positionstext"), DATE_OF_PAYMENT("Bezahldatum"),
        LEISTUNGSZEITRAUM("Leistungszeitraum"), ORDER("Auftrag"), NETSUM("Netto")
    }

    enum class MonthCol(val header: String) {
        MONTH1("Month 1"), MONTH2("Month 2"), MONTH3("Month 3"), MONTH4("Month 4"), MONTH5("Month 5"), MONTH6("Month 6"),
        MONTH7("Month 7"), MONTH8("Month 8"), MONTH9("Month 9"), MONTH10("Month 10"), MONTH11("Month 11"), MONTH12("Month 12")
    }

    private class Context(
        workbook: ExcelWorkbook,
        val forecastSheet: ExcelSheet,
        val invoicesSheet: ExcelSheet,
        val invoicesPriorYearSheet: ExcelSheet,
        val startDate: PFDay,
        val invoices: List<RechnungDO>,
        val baseDate: PFDay = PFDay.now(),
        val snapshot: Boolean = false,
    ) {
        val endDate = startDate.plusMonths(11).endOfMonth
        val excelDateFormat =
            ThreadLocalUserContext.loggedInUser?.excelDateFormat ?: ExcelDateFormats.EXCEL_DEFAULT_DATE
        val dateFormat = DateTimeFormatter.ofPattern(DateFormats.getFormatString(DateFormatType.DATE_SHORT))!!
        val currencyFormat = NumberHelper.getCurrencyFormat(ThreadLocalUserContext.locale)
        val currencyCellStyle = workbook.createOrGetCellStyle("DataFormat.currency")
        val percentageCellStyle = workbook.createOrGetCellStyle("DataFormat.percentage")
        val writerContext =
            ExcelWriterContext(I18n(Constants.RESOURCE_BUNDLE_NAME, ThreadLocalUserContext.locale), workbook)
        val orderMap = mutableMapOf<Long, OrderInfo>()

        // All projects of the user used in the orders to show also invoices without order, but with assigned project:
        val projectIds = mutableSetOf<Long>()
        var showAll: Boolean =
            false // showAll is true, if no filter is given and for financial and controlling staff only.
        val orderPositionMap = mutableMapOf<Long, OrderPositionInfo>()
        val orderMapByPositionId = mutableMapOf<Long, OrderInfo>()

        init {
            currencyCellStyle.dataFormat = workbook.getDataFormat(XlsContentProvider.FORMAT_CURRENCY)
            percentageCellStyle.dataFormat = workbook.getDataFormat("0%")
        }
    }

    @JvmOverloads
    @Throws(IOException::class)
    open fun export(origFilter: AuftragFilter, snapshotDate: LocalDate? = null): ByteArray? {
        val startDateParam = origFilter.periodOfPerformanceStartDate
        val startDate = if (startDateParam != null) PFDay.from(startDateParam).beginOfMonth else PFDay.now().beginOfYear
        val filter = AuftragFilter()
        filter.searchString = origFilter.searchString
        filter.projectList = origFilter.projectList
        //filter.auftragFakturiertFilterStatus = origFilter.auftragFakturiertFilterStatus
        //filter.auftragsPositionsPaymentType = origFilter.auftragsPositionsPaymentType
        filter.periodOfPerformanceStartDate =
            startDate.plusYears(-2).localDate // Go 2 years back for getting all orders referred by invoices of prior year.
        filter.user = origFilter.user
        val orderList = if (snapshotDate != null) {
            log.info { "Exporting forecast script for date ${startDate.isoString} with snapshotDate ${snapshotDate}, projects=${filter.projectList?.joinToString { it.name ?: "???" }}" }
            orderbookSnapshotsService.readSnapshot(snapshotDate)?.filter { filter.match(it) } ?: emptyList()
        } else {
            log.info { "Exporting forecast script for date ${startDate.isoString} with filter: str='${filter.searchString ?: ""}', projects=${filter.projectList?.joinToString { it.name ?: "???" }}" }
            orderDao.select(filter)
        }
        val showAll = accessChecker.isLoggedInUserMemberOfGroup(
            ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.CONTROLLING_GROUP
        ) &&
                filter.searchString.isNullOrBlank() &&
                filter.projectList.isNullOrEmpty()
        return export(orderList, startDate = startDate, snapshotDate = snapshotDate, showAll = showAll)
    }

    fun exportOrderAnalysis(orderId: Long?): List<ForecastOrderPosInfo>? {
        val orderInfo = ordersCache.getOrderInfo(orderId)
        return orderInfo?.infoPositions?.map { posInfo ->
            ForecastOrderPosInfo(orderInfo, posInfo).also {
                it.calculate()
            }
        }
    }


    private fun getStartDate(origFilter: AuftragFilter): PFDay {
        val startDateParam = origFilter.periodOfPerformanceStartDate
        return if (startDateParam != null) PFDay.from(startDateParam).beginOfMonth else PFDay.now().beginOfYear
    }

    open fun getExcelFilenmame(origFilter: AuftragFilter): String {
        return getFilename(getStartDate(origFilter), extension = ".xlsx")
    }

    /**
     * Get the export filename. Example: 'Forecast-start-2021-01-01_2025-01-02_22-48.xlsx'
     * or 'Forecast-ACME-snapshot-2023-08-01-start-2023-01-012025-01-02_22-48.zip'.
     * @param startDate The start date for the forecast.
     * @param extension The optional file extension ('.xlsx' or '.zip').
     * @param part The optional part of the export file (e.g. 'ACME', 'Customer', ...).
     */
    open fun getFilename(
        startDate: PFDay,
        extension: String? = null,
        part: String? = null,
        snapshot: LocalDate? = null
    ): String {
        val startDateString = "-start_${startDate.isoString}"
        val partString = if (part.isNullOrBlank()) "" else "-$part"
        val snapshotString = if (snapshot != null) "-snapshot_${snapshot}" else ""
        return "Forecast$partString$snapshotString${startDateString}-${DateHelper.getDateAsFilenameSuffix(Date())}${extension ?: ""}"
    }

    /**
     * Export the forecast sheet.
     * @param orderList The list of orders to export.
     * @param startDate The start date for the forecast.
     * @param showAll True, if no filter is given, for financial and controlling staff only.
     * @param snapshot Today (null) or, the day of the snapshot, if the orderList is loaded from order book snapshots.
     *              If the date is in the past, the forecast will be simulated with the specified date.
     *              If date is given, no caches will be used.
     * @return The byte array of the Excel file.
     */
    @Throws(IOException::class)
    open fun export(
        orderList: Collection<AuftragDO>,
        startDate: PFDay,
        showAll: Boolean,
        snapshotDate: LocalDate? = null,
    ): ByteArray? {
        if (orderList.isEmpty()) {
            log.info { "No orders found for export." }
            // No orders found, so we don't need the forecast sheet.
            return null
        }
        val useAuftragsCache = snapshotDate == null
        val prevYearBaseDate = startDate.plusYears(-1) // One year back for getting all invoices.
        val invoiceFilter = RechnungFilter()
        invoiceFilter.fromDate =
            prevYearBaseDate.plusDays(-1).localDate // Go 1 day back, paranoia setting for getting all invoices for given time period.
        if (snapshotDate != null) {
            // Don't load invoices later than snapshotDate:
            invoiceFilter.toDate = startDate.localDate.minusDays(1)
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

            val invoicesSheet = workbook.getSheet(Sheet.INVOICES.title)!!
            log.debug { "Invoices sheet: $forecastSheet" }
            InvoicesCol.entries.forEach { invoicesSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { invoicesSheet.registerColumn(it.header) }

            val invoicesPriorYearSheet = workbook.getSheet(Sheet.INVOICES_PREV_YEAR.title)!!
            log.debug { "InvoicesPriorYearSheet sheet: $forecastSheet" }
            InvoicesCol.entries.forEach { invoicesPriorYearSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { invoicesPriorYearSheet.registerColumn(it.header) }

            val ctx = Context(
                workbook,
                forecastSheet = forecastSheet,
                invoicesSheet = invoicesSheet,
                invoicesPriorYearSheet = invoicesPriorYearSheet,
                startDate = startDate,
                invoices = invoices,
                baseDate = PFDay.fromOrNow(snapshotDate),
                snapshot = snapshotDate != null,
            )
            ctx.showAll = showAll

            val infoSheet = workbook.getSheet(Sheet.INFO.title)!!
            infoSheet.setDateValue(0, 1, Date(), ctx.excelDateFormat)
            snapshotDate?.let {
                infoSheet.getCell(1, 1, ensureCell = true)?.let { cell ->
                    val bold =
                        workbook.createOrGetFont(
                            "bold",
                            bold = true,
                            heightInPoints = 18,
                            color = IndexedColors.RED.index
                        )
                    val cellStyle = workbook.createOrGetCellStyle("snapshotDate")
                    cellStyle.dataFormat = workbook.getDataFormat(ctx.excelDateFormat)
                    cellStyle.setFont(bold)
                    cell.setCellValue(it)
                    cell.cellStyle = cellStyle
                }
            }
            log.debug { "info sheet: $infoSheet" }

            var currentRow = 9
            var orderPositionFound = false
            for (auftragDO in orderList) {
                auftragDO.projekt?.id?.let { projektId ->
                    ctx.projectIds.add(projektId)
                }
                val orderInfo = if (useAuftragsCache) ordersCache.getOrderInfo(auftragDO) else auftragDO.info
                auftragDO.id?.let { ctx.orderMap[it] = orderInfo }
                orderInfo.infoPositions?.forEach { pos ->
                    pos.id?.let {
                        ctx.orderPositionMap[it] = pos // Register all order positions for invoice handling.
                        ctx.orderMapByPositionId[it] = orderInfo
                    }
                }
                if (auftragDO.deleted || orderInfo.infoPositions.isNullOrEmpty()) {
                    continue
                }

                if (ForecastUtils.auftragsStatusToShow.contains(auftragDO.status)) {
                    orderInfo.infoPositions?.forEach { pos ->
                        if (pos.status in ForecastUtils.auftragsPositionsStatusToShow) {
                            addOrderPosition(ctx, currentRow++, orderInfo, pos)
                            orderPositionFound = true
                        }
                    }
                }
            }
            if (!orderPositionFound) {
                log.info { "No orders positions found for export." }
                // No order positions found, so we don't need the forecast sheet.
                return null
            }
            fillInvoices(ctx)
            replaceMonthDatesInHeaderRow(forecastSheet, startDate)
            replaceMonthDatesInHeaderRow(invoicesSheet, startDate)
            replaceMonthDatesInHeaderRow(invoicesPriorYearSheet, prevYearBaseDate)
            forecastSheet.setAutoFilter()
            invoicesSheet.setAutoFilter()
            invoicesPriorYearSheet.setAutoFilter()

            // Now: evaluate the formulars:
            for (row in 1..7) {
                val excelRow = forecastSheet.getRow(row)
                MonthCol.entries.forEach {
                    val cell = excelRow.getCell(forecastSheet.getColumnDef(it.header)!!)
                    cell.evaluateFormularCell()
                }
            }
            val revenueSheet = workbook.getSheet("Umsatz kumuliert")!!
            for (row in 0..8) {
                val excelRow = revenueSheet.getRow(row)
                for (col in 1..12) {
                    val cell = excelRow.getCell(col)
                    cell.evaluateFormularCell()
                }
            }

            return workbook.asByteArrayOutputStream.toByteArray()
        }
    }

    private fun fillInvoices(ctx: Context) {
        val firstMonthCol = ctx.invoicesSheet.getColumnDef(MonthCol.MONTH1.header)!!.columnNumber
        for (invoice in ctx.invoices) {
            if (invoice.status == RechnungStatus.GEPLANT || invoice.status == RechnungStatus.STORNIERT) {
                continue // Ignoriere stornierte oder geplante Rechnungen.
            }
            rechnungCache.getRechnungInfo(invoice.id)?.positions?.forEach { pos ->
                // for (pos in rechnungCache.getRechnungsPositionVOSetByRechnungId(invoice.id) ?: continue) {
                val orderPosId = pos.auftragsPositionId
                val orderPositionFound = orderPosId != null && ctx.orderPositionMap.containsKey(orderPosId)
                // val invoiceProjektId = invoice.projektId
                // val orderProjectId = orderPos?.auftrag?.projektId // may differ from invoiceProjektId
                // val projectFound = invoiceProjektId != null && ctx.projectIds.contains(invoiceProjektId) ||
                //     orderProjectId != null && ctx.projectIds.contains(orderProjectId)
                if (!ctx.showAll && !orderPositionFound) { // !projectFound && !orderPositionFound) {
                    return@forEach // Ignore invoices referring an order position or project which isn't part of the order list filtered by the user.
                }
                var order = if (orderPosId != null) {
                    ctx.orderMapByPositionId[orderPosId]
                } else {
                    null
                }
                if (orderPosId != null && order == null) {
                    val orderId = pos.auftragsId ?: ordersCache.getOrderPositionInfo(orderPosId)?.auftragId
                    order = ordersCache.getOrderInfo(orderId)
                    if (order == null) {
                        log.error("Shouldn't occur: can't determine order from order position: $orderPosId")
                        return@forEach // continue
                    }
                    ctx.orderMapByPositionId[orderPosId] = order
                }
                var monthIndex = getMonthIndex(ctx, PFDay.fromOrNow(invoice.datum))
                if (monthIndex !in -12..11) {
                    return@forEach // continue
                }
                val sheet = if (monthIndex < 0) ctx.invoicesPriorYearSheet else ctx.invoicesSheet
                if (monthIndex < 0) {
                    monthIndex += 12
                }
                val rowNumber = sheet.createRow().rowNum
                sheet.setIntValue(rowNumber, InvoicesCol.INVOICE_NR.header, invoice.nummer)
                sheet.setStringValue(rowNumber, InvoicesCol.POS_NR.header, "#${pos.number}")
                sheet.setDateValue(
                    rowNumber,
                    InvoicesCol.DATE.header,
                    PFDay(invoice.datum!!).localDate,
                    ctx.excelDateFormat
                )
                val projekt = projektCache.getProjektIfNotInitialized(invoice.projekt)
                sheet.setStringValue(rowNumber, InvoicesCol.CUSTOMER.header, invoice.kundeAsString)
                sheet.setStringValue(rowNumber, InvoicesCol.PROJECT.header, projekt?.name)
                sheet.setStringValue(rowNumber, InvoicesCol.SUBJECT.header, invoice.betreff)
                sheet.setStringValue(rowNumber, InvoicesCol.POS_TEXT.header, pos.text)
                invoice.bezahlDatum?.let {
                    sheet.setDateValue(
                        rowNumber,
                        InvoicesCol.DATE_OF_PAYMENT.header,
                        PFDay(it).localDate,
                        ctx.excelDateFormat
                    )
                }
                val leistungsZeitraumColDef = sheet.getColumnDef(InvoicesCol.LEISTUNGSZEITRAUM.header)
                invoice.periodOfPerformanceBegin?.let {
                    sheet.setDateValue(rowNumber, leistungsZeitraumColDef, PFDay(it).localDate, ctx.excelDateFormat)
                }
                invoice.periodOfPerformanceEnd?.let {
                    sheet.setDateValue(
                        rowNumber,
                        leistungsZeitraumColDef!!.columnNumber + 1,
                        PFDay(it).localDate,
                        ctx.excelDateFormat
                    )
                }
                if (order != null && orderPosId != null) {
                    sheet.setStringValue(
                        rowNumber,
                        InvoicesCol.ORDER.header,
                        "${order.nummer}.${pos.auftragsPositionNummer}"
                    )
                }
                sheet.setBigDecimalValue(rowNumber, InvoicesCol.NETSUM.header, pos.netSum).cellStyle =
                    ctx.currencyCellStyle
                sheet.setBigDecimalValue(rowNumber, firstMonthCol + monthIndex, pos.netSum).cellStyle =
                    ctx.currencyCellStyle
            }
        }
    }

    private fun replaceMonthDatesInHeaderRow(sheet: ExcelSheet, baseDate: PFDay) { // Adding month columns
        var currentMonth = baseDate
        MonthCol.entries.forEach {
            val cell = sheet.headRow!!.getCell(sheet.getColumnDef(it.header)!!)
            cell.setCellValue(formatMonthHeader(currentMonth))
            currentMonth = currentMonth.plusMonths(1)
        }
    }

    private fun addOrderPosition(ctx: Context, row: Int, order: OrderInfo, pos: OrderPositionInfo) {
        val sheet = ctx.forecastSheet
        sheet.setIntValue(row, ForecastCol.ORDER_NR.header, order.nummer)
        sheet.setStringValue(row, ForecastCol.POS_NR.header, "#${pos.number}")
        order.angebotsDatum?.let {
            sheet.setDateValue(row, ForecastCol.DATE_OF_OFFER.header, PFDay(it).localDate, ctx.excelDateFormat)
        }
        ForecastUtils.ensureErfassungsDatum(order).let {
            sheet.setDateValue(row, ForecastCol.DATE.header, PFDay(it).localDate, ctx.excelDateFormat)
        }
        order.entscheidungsDatum?.let {
            sheet.setDateValue(row, ForecastCol.DATE_OF_DECISION.header, PFDay(it).localDate, ctx.excelDateFormat)
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

        val orderInfo = if (ctx.snapshot) order else {
            ordersCache.getOrderInfo(order.id)
        }
        val posInfo = orderInfo?.getInfoPosition(pos.id)
        val netSum = pos.netSum ?: BigDecimal.ZERO
        val invoicedSum = posInfo?.invoicedSum ?: BigDecimal.ZERO
        val forecastInfo = ForecastOrderPosInfo(order, pos)
        forecastInfo.calculate()
        sheet.setBigDecimalValue(row, ForecastCol.NETTOSUMME.header, netSum).cellStyle = ctx.currencyCellStyle
        if (invoicedSum.compareTo(BigDecimal.ZERO) != 0) {
            sheet.setBigDecimalValue(row, ForecastCol.FAKTURIERT.header, invoicedSum).cellStyle = ctx.currencyCellStyle
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

        val invoicePositions = rechnungCache.getRechnungsPosInfosByAuftragsPositionId(pos.id)
        sheet.setStringValue(row, ForecastCol.DEBITOREN_RECHNUNGEN.header, ForecastUtils.getInvoices(invoicePositions))
        val leistungsZeitraumColDef = sheet.getColumnDef(ForecastCol.LEISTUNGSZEITRAUM.header)!!
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) { // use "own" period -> from pos
            sheet.setDateValue(
                row,
                leistungsZeitraumColDef,
                PFDay(pos.periodOfPerformanceBegin!!).localDate,
                ctx.excelDateFormat
            )
            sheet.setDateValue(
                row,
                leistungsZeitraumColDef.columnNumber + 1,
                PFDay(pos.periodOfPerformanceEnd!!).localDate,
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

        sheet.setBigDecimalValue(row, ForecastCol.PROBABILITY_NETSUM.header, forecastInfo.probabilityNetSum).cellStyle =
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
        forecastInfo.months.forEach { monthEntry ->
            val monthDate = monthEntry.date
            val offset = ctx.startDate.monthsBetween(monthDate).toInt()
            if (offset !in 0..11) {
                return@forEach // continue
            }
            if (monthEntry.toBeInvoicedSum.abs() < BigDecimal.ONE) {
                return@forEach // continue
            }
            val columnDef = ctx.forecastSheet.getColumnDef(MonthCol.entries[offset].header)!!
            val cell =
                ctx.forecastSheet.setBigDecimalValue(
                    row,
                    columnDef,
                    monthEntry.toBeInvoicedSum.setScale(2, RoundingMode.HALF_UP),
                )
            cell.cellStyle = ctx.currencyCellStyle
            if (monthEntry.error) {
                highlightErrorCell(ctx, row, columnDef.columnNumber)
            }
        }
    }

    private fun highlightErrorCell(ctx: Context, rowNumber: Int, colNumber: Int, comment: String? = null) {
        val excelRow = ctx.forecastSheet.getRow(rowNumber)
        val excelCell = excelRow.getCell(colNumber)
        ctx.writerContext.cellHighlighter.highlightErrorCell(
            excelCell,
            ctx.writerContext,
            ctx.forecastSheet,
            ctx.forecastSheet.getColumnDef(0),
            excelRow
        )
        if (comment != null)
            ctx.writerContext.cellHighlighter.setCellComment(excelCell, comment)
    }

    private fun getMonthIndex(ctx: Context, date: PFDay): Int {
        val monthDate = date.year * 12 + date.monthValue
        val monthBaseDate = ctx.startDate.year * 12 + ctx.startDate.monthValue
        return monthDate - monthBaseDate // index from 0 to 11
    }

    companion object {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

        fun formatMonthHeader(date: PFDay): String {
            return date.format(formatter)
        }
    }
}
