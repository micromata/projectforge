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

package org.projectforge.rest.fibu

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.EmployeeSalaryDao
import org.projectforge.business.fibu.datev.EmployeeSalaryExportDao
import org.projectforge.common.StringHelper
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.time.DateHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.EmployeeSalary
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Date

/**
 * The employee-salary list and edit page of projectforge-next (`EmployeeSalaryDO`, "Gehälter"). Migrated
 * from Wicket's `EmployeeSalaryListPage`; serves no layout - projectforge-next hand-builds it (see
 * `components/features/employee-salary`). The DATEV import page stays on Wicket (separate menu item).
 */
@RestController
@RequestMapping("${Rest.URL}/employeeSalary")
class EmployeeSalaryEntityRest : AbstractDTOEntityRest<EmployeeSalaryDO, EmployeeSalary, EmployeeSalaryDao>(
    EmployeeSalaryDao::class.java,
    "fibu.employee.salary.title",
) {
    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeSalaryExportDao: EmployeeSalaryExportDao

    override fun transformFromDB(obj: EmployeeSalaryDO, editMode: Boolean): EmployeeSalary {
        // Resolve the lazy employee from the cache: a list would otherwise fire one select per row for the
        // employee (and its user). The cache hands out a fully initialized, detached instance.
        obj.employee = caches.getEmployeeIfNotInitialized(obj.employee)
        val dto = EmployeeSalary()
        dto.copyFrom(obj)
        return dto
    }

    override fun transformForDB(dto: EmployeeSalary): EmployeeSalaryDO {
        val obj = EmployeeSalaryDO()
        dto.copyTo(obj)
        return obj
    }

    /**
     * The year (values from the database) and month single-choice filters Wicket's list offered. Both are
     * default-visible; the year is the one the list is usually narrowed by.
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        val years = baseDao.years.sortedDescending()
        elements.add(
            UIFilterListElement(FILTER_YEAR, label = translate("calendar.year"), multi = false, defaultFilter = true)
                .also { it.values = years.map { year -> UISelectValue(year.toString(), year.toString()) } }
        )
        elements.add(
            UIFilterListElement(FILTER_MONTH, label = translate("calendar.month"), multi = false, defaultFilter = true)
                .also {
                    it.values = (1..12).map { month ->
                        UISelectValue(month.toString(), StringHelper.format2DigitNumber(month))
                    }
                }
        )
    }

    /**
     * Turns the picked year/month into `eq` predicates on the real columns (mirrors `EmployeeSalaryDao.select`)
     * and marks the entries synthetic, so the generic processor doesn't also try to match them as properties.
     */
    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<EmployeeSalaryDO>>? {
        consumeInt(source, FILTER_YEAR)?.let { target.add(eq("year", it)) }
        consumeInt(source, FILTER_MONTH)?.let { target.add(eq("month", it)) }
        return null
    }

    /**
     * Sorts by the `year-MM` the list shows, which no database column holds ([EmployeeSalaryDO.formattedYearAndMonth]
     * is a transient getter). Its parts in order - year, then month - are exactly that sort.
     */
    override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
        val index = target.sortProperties.indexOfFirst { it.property == "formattedYearAndMonth" }
        if (index < 0) {
            return
        }
        val sortOrder = target.sortProperties[index].sortOrder
        target.sortProperties.removeAt(index)
        target.sortProperties.addAll(
            index,
            listOf(SortProperty("year", sortOrder), SortProperty("month", sortOrder)),
        )
    }

    /**
     * The generic list export - one row per salary, the columns the list shows (`year-MM`, employee last
     * and first name, staff number, type, gross with employer's share, comment). This is Wicket's plain
     * "Excel export" content-menu entry ([EmployeeSalaryListPage], `DOListExcelExporter`), as opposed to
     * the cost-assignment export below. The rows come from [getResultList], i.e. through the same pipeline
     * the list uses. An empty result answers 404 rather than a file.
     */
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting employee salaries as Excel file (one row per salary).")
        val salaries = getResultList(filter).map { transformFromDB(it, false) }
        if (salaries.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        ExcelUtils.prepareWorkbook().use { workbook ->
            val sheet = workbook.createOrGetSheet(translate("fibu.employee.salaries"))
            val currencyStyle = workbook.createOrGetCellStyle("currency")
            currencyStyle.dataFormat = workbook.createDataFormat().getFormat(CURRENCY_FORMAT)
            sheet.registerColumn(translate("calendar.month"), COL_YEAR_MONTH).withSize(10)
            sheet.registerColumn(translate("name"), COL_LAST_NAME).withSize(20)
            sheet.registerColumn(translate("firstName"), COL_FIRST_NAME).withSize(20)
            sheet.registerColumn(translate("fibu.employee.staffNumber"), COL_STAFF_NUMBER).withSize(14)
            sheet.registerColumn(translate("fibu.employee.salary.type"), COL_TYPE).withSize(16)
            sheet.registerColumn(translate("fibu.employee.salary.bruttoMitAgAnteil"), COL_BRUTTO).withSize(16)
            sheet.registerColumn(translate("comment"), COL_COMMENT).withSize(40)
            ExcelUtils.addHeadRow(sheet)
            salaries.forEach { salary ->
                val row = sheet.createRow()
                row.getCell(COL_YEAR_MONTH)?.setCellValue(salary.formattedYearAndMonth)
                row.getCell(COL_LAST_NAME)?.setCellValue(salary.lastName)
                row.getCell(COL_FIRST_NAME)?.setCellValue(salary.firstName)
                row.getCell(COL_STAFF_NUMBER)?.setCellValue(salary.staffNumber)
                salary.type?.let { row.getCell(COL_TYPE)?.setCellValue(translate(it.i18nKey)) }
                salary.bruttoMitAgAnteil?.let {
                    row.getCell(COL_BRUTTO)?.setCellValue(it)?.setCellStyle(currencyStyle)
                }
                row.getCell(COL_COMMENT)?.setCellValue(salary.comment)
            }
            sheet.setAutoFilter()
            val filename =
                "ProjectForge-${translate("fibu.employee.salaries")}_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
            return RestUtils.downloadFile(filename, workbook.asByteArrayOutputStream.toByteArray())
        }
    }

    /**
     * The cost-assignment export ([EmployeeSalaryExportDao]) - the DATEV sheet with one row per Kost2, the
     * gross split over the employee's time-sheet bookings. Wicket's "Kostenzuweisungen exportieren"
     * content-menu entry ([EmployeeSalaryListPage], `fibu.rechnung.kostExcelExport`).
     *
     * A month must be picked, as in Wicket: a whole-year export is not offered (400 with the reason as its
     * body). An empty result answers 404 rather than a file, so a filter matching nothing does not look
     * like a successful export in the download folder.
     */
    @PostMapping(EXPORT_COST_ASSIGNMENTS_PATH)
    fun exportCostAssignmentsAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        val year = readInt(filter, FILTER_YEAR)
        val month = readInt(filter, FILTER_MONTH)
            ?: return ResponseEntity.badRequest().body(translate("fibu.employee.salary.error.monthNotGiven"))
        log.info("Exporting employee salary cost assignments as Excel file for ${DateHelper.formatMonth(year ?: 0, month)}.")
        val list = getResultList(filter)
        if (list.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val xls = employeeSalaryExportDao.export(list)
        if (xls == null || xls.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val filename =
            "ProjectForge-EmployeeSalaries_${DateHelper.formatMonth(year ?: 0, month)}_${DateHelper.getDateAsFilenameSuffix(Date())}.xls"
        return RestUtils.downloadFile(filename, xls)
    }

    /** Reads a single-choice filter value and marks the entry synthetic (consumed here, not matched generically). */
    private fun consumeInt(source: MagicFilter, field: String): Int? {
        val entry = source.entries.find { it.field == field } ?: return null
        entry.synthetic = true
        return pickedValue(entry.value)?.toIntOrNull()
    }

    /** Reads a single-choice filter value without consuming it (for the export, which needs it after the pipeline). */
    private fun readInt(source: MagicFilter, field: String): Int? {
        val entry = source.entries.find { it.field == field } ?: return null
        return pickedValue(entry.value)?.toIntOrNull()
    }

    private fun pickedValue(value: MagicFilterEntry.Value): String? {
        return value.value?.takeIf { it.isNotBlank() }
            ?: value.values?.firstOrNull { it.isNotBlank() }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(EmployeeSalaryEntityRest::class.java)

        /** Ids of the synthetic year/month filter elements (see [addMagicFilterElements]). */
        private const val FILTER_YEAR = "year"
        private const val FILTER_MONTH = "month"

        /** Path of the cost-assignment export, next to the generic list export at [RestPaths.REST_EXCEL_SUB_PATH]. */
        internal const val EXPORT_COST_ASSIGNMENTS_PATH = "exportCostAssignmentsAsExcel"

        private const val CURRENCY_FORMAT = "#,##0.00;[Red]-#,##0.00"

        // Column aliases of the generic list export.
        private const val COL_YEAR_MONTH = "yearMonth"
        private const val COL_LAST_NAME = "lastName"
        private const val COL_FIRST_NAME = "firstName"
        private const val COL_STAFF_NUMBER = "staffNumber"
        private const val COL_TYPE = "type"
        private const val COL_BRUTTO = "bruttoMitAgAnteil"
        private const val COL_COMMENT = "comment"
    }
}
