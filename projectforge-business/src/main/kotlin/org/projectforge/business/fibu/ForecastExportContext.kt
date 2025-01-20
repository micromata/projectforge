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

import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.poi.ss.usermodel.IndexedColors
import org.projectforge.business.excel.ExcelDateFormats
import org.projectforge.business.excel.XlsContentProvider
import org.projectforge.common.DateFormatType
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Context for the forecast export.
 */
internal class ForecastExportContext(
    workbook: ExcelWorkbook,
    val forecastSheet: ExcelSheet,
    val invoicesSheet: ExcelSheet,
    val invoicesPrevYearSheet: ExcelSheet,
    val planningSheet: ExcelSheet,
    val planningInvoicesSheet: ExcelSheet,
    val startDate: PFDay,
    val invoices: List<RechnungDO>,
    val baseDate: PFDay = PFDay.now(),
    val planningDate: LocalDate? = null,
    val snapshot: Boolean = false,
) {
    enum class Sheet(val title: String) {
        FORECAST("Forecast_Data"),
        INVOICES("Rechnungen"),
        INVOICES_PREV_YEAR("Rechnungen Vorjahr"),
        PLANNING("Planning_Data"),
        PLANNING_INVOICES("Planning_Invoices"),
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
        REMAINING("Rest"), DIFFERENCE("Abweichung"), WARNING("Warnung")
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

    val endDate = startDate.plusMonths(11).endOfMonth
    val excelDateFormat =
        ThreadLocalUserContext.loggedInUser?.excelDateFormat ?: ExcelDateFormats.EXCEL_DEFAULT_DATE
    val dateFormat = DateTimeFormatter.ofPattern(DateFormats.getFormatString(DateFormatType.DATE_SHORT))!!
    val currencyFormat = NumberHelper.getCurrencyFormat(ThreadLocalUserContext.locale)
    val currencyCellStyle = workbook.createOrGetCellStyle("DataFormat.currency")
    val percentageCellStyle = workbook.createOrGetCellStyle("DataFormat.percentage")
    val boldRedFont = ExcelUtils.createFont(
        workbook,
        "boldRedFont",
        bold = true,
        heightInPoints = 10,
        color = IndexedColors.DARK_RED.index,
    )
    val boldRedLargeFont =
        ExcelUtils.createFont(workbook, "boldRedLargeFont", heightInPoints = 12, origFont = boldRedFont)
    val boldRedHugeFont =
        ExcelUtils.createFont(workbook, "boldRedLargeFont", heightInPoints = 14, origFont = boldRedFont)
    val errorCellStyle = ExcelUtils.createCellStyle(workbook, "error", font = boldRedFont)
    val largeErrorCellStyle = ExcelUtils.createCellStyle(
        workbook,
        "largeError",
        fillForegroundColor = IndexedColors.LIGHT_YELLOW,
        font = boldRedLargeFont,
    )
    val hugeErrorCellStyle = ExcelUtils.createCellStyle(
        workbook,
        "hugeError",
        fillForegroundColor = IndexedColors.LIGHT_ORANGE,
        font = boldRedHugeFont,
        origStyle = errorCellStyle,
    )

    init {
        currencyCellStyle.dataFormat = workbook.getDataFormat(XlsContentProvider.FORMAT_CURRENCY)
        percentageCellStyle.dataFormat = workbook.getDataFormat("0%")
        boldRedFont.fontName = "Arial"
    }

    val errorCurrencyCellStyle = // currencyCellStyle of errorCellStyle must be set before.
        ExcelUtils.createCellStyle(
            workbook,
            "errorCurrency",
            font = boldRedFont,
            fillForegroundColor = IndexedColors.LIGHT_YELLOW,
            origStyle = currencyCellStyle,
        )
    val orderMap = mutableMapOf<Long, OrderInfo>()

    // All projects of the user used in the orders to show also invoices without order, but with assigned project:
    val projectIds = mutableSetOf<Long>()
    var showAll: Boolean =
        false // showAll is true, if no filter is given and for financial and controlling staff only.
    val orderPositionMap = mutableMapOf<Long, OrderPositionInfo>()
    val orderMapByPositionId = mutableMapOf<Long, OrderInfo>()
}
