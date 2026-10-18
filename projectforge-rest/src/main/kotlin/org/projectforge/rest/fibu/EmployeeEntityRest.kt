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

import de.micromata.merlin.excel.ExcelWorkbook
import jakarta.servlet.http.HttpServletRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.poi.ss.util.WorkbookUtil
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.*
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.user.UserGroupCache
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.getObjectList
import org.projectforge.rest.dto.*
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Employees (list + form in projectforge-next, `/next/employee`), including the time-dependent attributes
 * (status, weekly working hours, annual leave days) of an employee, see [EmployeeValidSinceAttrDO].
 */
@RestController
@RequestMapping("${Rest.URL}/employee")
class EmployeeEntityRest :
    AbstractDTOEntityRest<EmployeeDO, Employee, EmployeeDao>(EmployeeDao::class.java, "fibu.employee.title") {

    /**
     * Response of the valid-since attribute endpoints: the fresh entries of the modified attribute type.
     */
    class ValidSinceAttrEntries(val entries: List<EmployeeValidSinceAttr>)

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    override fun transformFromDB(obj: EmployeeDO, editMode: Boolean): Employee {
        val employee = Employee()
        caches.initialize(obj)
        employee.copyFrom(obj)
        userGroupCache.getUser(obj.user?.id)?.let { userDO ->
            User(userDO).let { user ->
                user.firstname = userDO.firstname
                user.lastname = userDO.lastname
                employee.user = user
            }
        }
        kostCache.getKost1(obj.kost1?.id)?.let { kost ->
            Kost1(kost).let { dto ->
                employee.kost1 = dto
            }
        }
        employee.konto = kontoCache.getKontoIfNotInitialized(obj.konto)?.let { konto ->
            // displayName is a computed getter of KontoDO, so it isn't copied by the constructor.
            Konto(konto).also { it.displayName = konto.displayName }
        }
        employeeCache.setTimeDependentAttrs(obj)
        employee.status = obj.status
        employee.annualLeave = obj.annualLeave
        employee.weeklyWorkingHours = obj.weeklyWorkingHours
        if (editMode) {
            obj.id?.let { id ->
                employee.statusEntries = selectEntries(id, EmployeeValidSinceAttrType.STATUS)
                employee.weeklyWorkingHoursEntries = selectEntries(id, EmployeeValidSinceAttrType.WEEKLY_HOURS)
                employee.annualLeaveEntries = selectEntries(id, EmployeeValidSinceAttrType.ANNUAL_LEAVE)
            }
        }
        return employee
    }

    override fun transformForDB(dto: Employee): EmployeeDO {
        val employeeDO = EmployeeDO()
        dto.copyTo(employeeDO)
        return employeeDO
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        /*elements.add(
            UIFilterListElement("status", label = translate("fibu.employee.status"), defaultFilter = true)
                .buildValues(EmployeeStatus::class.java)
        )*/
        elements.add(
            UIFilterBooleanElement(
                "onlyActives",
                label = translate("label.onlyActiveEntries"),
                defaultFilter = true
            )
        )
    }

    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<EmployeeDO>> {
        val filters = mutableListOf<CustomResultFilter<EmployeeDO>>()
        /*val assignmentFilterEntry = source.entries.find { it.field == "status" }
        if (assignmentFilterEntry != null) {
            assignmentFilterEntry.synthetic = true
            val values = assignmentFilterEntry.value.values
            if (!values.isNullOrEmpty()) {
                val enums = values.map { EmployeeStatus.safeValueOf(it) }
                filters.add(EmployeeStatusFilter(enums))
            }
        }*/
        source.entries.find { it.field == "onlyActives" }?.let { entry ->
            entry.synthetic = true
            val conflictsOnly = entry.value.value
            if (conflictsOnly == "true") {
                filters.add(EmployeeActiveFilter())
            }
        }
        return filters
    }

    /**
     * Exports the employee list as an Excel file. The given [filter] is the currently active list filter, so the
     * exported rows match exactly the rows shown in the list view (e.g. "only active entries").
     */
    @AccessChecked("DAO: select access (list result filtered by baseDao)")
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info { "Exporting employees as Excel file." }
        val list = getObjectList(this, baseDao, filter)
        // Ensure the transient, time-dependent attributes (status, weekly working hours) reflect the current values.
        employeeCache.setTimeDependentAttrs(list)
        ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale).use { workbook ->
            // Excel sheet names must not contain the chars \ / ? * [ ] : (e.g. "Mitarbeiter:in"), so sanitize.
            val sheetName = WorkbookUtil.createSafeSheetName(translate("fibu.employee.title.heading"))
            val sheet = workbook.createOrGetSheet(sheetName)
            val boldFont = ExcelUtils.createFont(workbook, "bold", bold = true)
            val boldStyle = workbook.createOrGetCellStyle("hr", font = boldFont)
            // Use Excel's built-in "General" format for the numeric columns instead of merlin's default float format
            // "#.#": "#.#" always renders the decimal separator, so whole numbers show as "35," in the German locale.
            // General renders whole numbers as "35" and fractions as "37,5" and, unlike a custom format code, is
            // displayed correctly by every viewer (Excel, LibreOffice, macOS Quick Look, Numbers).
            val numberStyle = workbook.createOrGetCellStyle("number")
            numberStyle.dataFormat = workbook.createDataFormat().getFormat("General")
            sheet.registerColumn(translate("name"), "lastname").withSize(20)
            sheet.registerColumn(translate("firstName"), "firstname").withSize(20)
            sheet.registerColumn(translate("user.username"), "username").withSize(20)
            sheet.registerColumn(translate("email"), "email").withSize(30)
            sheet.registerColumn(translate("organization"), "organization").withSize(25)
            sheet.registerColumn(translate("fibu.employee.status"), "status").withSize(15)
            ExcelUtils.registerColumn(sheet, EmployeeDO::staffNumber, 15)
            sheet.registerColumn(translate("fibu.kost1"), "kost1").withSize(15)
            sheet.registerColumn(translate("fibu.employee.konto"), "konto").withSize(30)
            ExcelUtils.registerColumn(sheet, EmployeeDO::position, 20)
            ExcelUtils.registerColumn(sheet, EmployeeDO::abteilung, 20)
            ExcelUtils.registerColumn(sheet, EmployeeDO::eintrittsDatum)
            ExcelUtils.registerColumn(sheet, EmployeeDO::austrittsDatum)
            // Use the class-based overload for these two: their setters are 'internal' (set by the cache), so the
            // KProperty reference resolves to a read-only KProperty1 and the KProperty overload would silently drop
            // the column instead of registering it.
            ExcelUtils.registerColumn(sheet, EmployeeDO::class.java, "weeklyWorkingHours", 12)
            ExcelUtils.registerColumn(sheet, EmployeeDO::class.java, "annualLeave", 12)
            sheet.registerColumn(translate("user.deactivated"), "deactivated").withSize(12)
            ExcelUtils.registerColumn(sheet, EmployeeDO::comment, 50)
            // Register the number format at column level (before any cell is created): merlin then applies numberStyle
            // to every cell of these columns on creation and setCellValue(BigDecimal) keeps it (protectCellStyle),
            // so whole numbers render as "35" instead of merlin's default float format "#.#" ("35,").
            sheet.setColumnStyle("weeklyWorkingHours", numberStyle)
            sheet.setColumnStyle("annualLeave", numberStyle)
            ExcelUtils.addHeadRow(sheet, boldStyle)
            list.forEach { employeeDO ->
                val row = sheet.createRow()
                val userDO = userGroupCache.getUser(employeeDO.user?.id)
                userDO?.lastname?.let { row.getCell("lastname")?.setCellValue(it) }
                userDO?.firstname?.let { row.getCell("firstname")?.setCellValue(it) }
                userDO?.username?.let { row.getCell("username")?.setCellValue(it) }
                userDO?.email?.let { row.getCell("email")?.setCellValue(it) }
                userDO?.organization?.let { row.getCell("organization")?.setCellValue(it) }
                employeeDO.status?.let { row.getCell("status")?.setCellValue(translate(it.i18nKey)) }
                employeeDO.staffNumber?.let { row.getCell("staffNumber")?.setCellValue(it) }
                kostCache.getKost1(employeeDO.kost1?.id)?.formattedNumber
                    ?.let { row.getCell("kost1")?.setCellValue(it) }
                kontoCache.getKontoIfNotInitialized(employeeDO.konto)?.displayName
                    ?.let { row.getCell("konto")?.setCellValue(it) }
                employeeDO.position?.let { row.getCell("position")?.setCellValue(it) }
                employeeDO.abteilung?.let { row.getCell("abteilung")?.setCellValue(it) }
                employeeDO.eintrittsDatum?.let { row.getCell("eintrittsDatum")?.setCellValue(it) }
                employeeDO.austrittsDatum?.let { row.getCell("austrittsDatum")?.setCellValue(it) }
                employeeDO.weeklyWorkingHours?.let { row.getCell("weeklyWorkingHours")?.setCellValue(it)?.setCellStyle(numberStyle) }
                employeeDO.annualLeave?.let { row.getCell("annualLeave")?.setCellValue(it)?.setCellStyle(numberStyle) }
                userDO?.let { row.getCell("deactivated")?.setCellValue(translate(if (it.deactivated) "yes" else "no")) }
                employeeDO.comment?.let { row.getCell("comment")?.setCellValue(it) }
            }
            sheet.setAutoFilter()
            val filename = "EmployeeList_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
            val resource = ByteArrayResource(workbook.asByteArrayOutputStream.toByteArray())
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
                .body(resource)
        }
    }

    /**
     * Inserts a time-dependent attribute (status, weekly working hours or annual leave days) of an employee.
     */
    @AccessChecked("EmployeeService: insert access to the employee (EmployeeDao)")
    @PostMapping("validSinceAttr/insert")
    fun insertValidSinceAttr(@RequestBody postData: PostData<EmployeeValidSinceAttr>): ResponseEntity<*> {
        val dto = postData.data
        requiredFields(dto, idNull = true)
        validate(dto)?.let { return it }
        employeeService.insert(employeeId = dto.employeeId!!, attrDO = dto.cloneAsDO())
        return entriesResponse(dto)
    }

    /**
     * Updates a time-dependent attribute of an employee.
     */
    @AccessChecked("EmployeeService: update access to the employee (EmployeeDao)")
    @PostMapping("validSinceAttr/update")
    fun updateValidSinceAttr(@RequestBody postData: PostData<EmployeeValidSinceAttr>): ResponseEntity<*> {
        val dto = postData.data
        requiredFields(dto)
        validate(dto)?.let { return it }
        employeeService.updateValidSinceAttr(employeeId = dto.employeeId, attrDO = dto.cloneAsDO())
        return entriesResponse(dto)
    }

    /**
     * Marks a time-dependent attribute of an employee as deleted.
     */
    @AccessChecked("EmployeeService: update access to the employee (EmployeeDao)")
    @PostMapping("validSinceAttr/delete")
    fun deleteValidSinceAttr(@RequestBody postData: PostData<EmployeeValidSinceAttr>): ResponseEntity<*> {
        val dto = postData.data
        requiredFields(dto)
        employeeService.markValidSinceAttrAsDeleted(employeeId = dto.employeeId, attrId = dto.id)
        return entriesResponse(dto)
    }

    private fun selectEntries(employeeId: Long, type: EmployeeValidSinceAttrType): List<EmployeeValidSinceAttr> {
        val entries = when (type) {
            EmployeeValidSinceAttrType.STATUS -> employeeService.selectStatusEntries(employeeId)
            EmployeeValidSinceAttrType.WEEKLY_HOURS -> employeeService.selectWeeklyWorkingHoursEntries(employeeId)
            EmployeeValidSinceAttrType.ANNUAL_LEAVE -> employeeService.selectAnnualLeaveDayEntries(employeeId)
        }
        return entries.map { EmployeeValidSinceAttr(it) }
    }

    private fun entriesResponse(dto: EmployeeValidSinceAttr): ResponseEntity<ValidSinceAttrEntries> {
        return ResponseEntity.ok(ValidSinceAttrEntries(selectEntries(dto.employeeId!!, dto.type!!)))
    }

    private fun requiredFields(dto: EmployeeValidSinceAttr, idNull: Boolean = false) {
        if (idNull) {
            require(dto.id == null) { "Can't insert EmployeeValidSinceAttr entry with given id." }
        } else {
            requireNotNull(dto.id) { "Can't update/delete EmployeeValidSinceAttr entry without id." }
        }
        requireNotNull(dto.employeeId) { "Can't modify EmployeeValidSinceAttr entry without employeeId." }
        requireNotNull(dto.type) { "Can't modify EmployeeValidSinceAttr entry without type." }
    }

    private fun validate(dto: EmployeeValidSinceAttr): ResponseEntity<ResponseAction>? {
        val validationErrors = mutableListOf<ValidationError>()
        when (dto.type) {
            EmployeeValidSinceAttrType.STATUS -> {
                if (EmployeeStatus.safeValueOf(dto.value) == null) {
                    validationErrors.add(
                        ValidationError.createFieldRequired(fieldId = "value", fieldName = translate("fibu.employee.status"))
                    )
                }
            }

            EmployeeValidSinceAttrType.ANNUAL_LEAVE -> {
                val annualLeave = dto.value?.toIntOrNull()
                if (annualLeave == null) {
                    validationErrors.add(
                        ValidationError.createFieldRequired(
                            fieldId = "value",
                            fieldName = translate("fibu.employee.urlaubstage")
                        )
                    )
                } else if (annualLeave < 0 || annualLeave > 100) {
                    validationErrors.add(
                        ValidationError(
                            translateMsg("validation.error.range.integerOutOfRange", "0", "100"),
                            fieldId = "value",
                        )
                    )
                }
            }

            EmployeeValidSinceAttrType.WEEKLY_HOURS -> {
                val weeklyWorkingHours = dto.value?.toBigDecimalOrNull()
                if (weeklyWorkingHours == null) {
                    validationErrors.add(
                        ValidationError.createFieldRequired(
                            fieldId = "value",
                            fieldName = translate("fibu.employee.wochenstunden")
                        )
                    )
                } else if (weeklyWorkingHours < BigDecimal.ZERO || weeklyWorkingHours > NumberHelper.HUNDRED) {
                    validationErrors.add(
                        ValidationError(
                            translateMsg("validation.error.range.integerOutOfRange", "0", "100"),
                            fieldId = "value",
                        )
                    )
                }
            }

            null -> throw IllegalArgumentException("Unknown type: ${dto.type}")
        }
        if (dto.validSince == null) {
            validationErrors.add(
                ValidationError.createFieldRequired(EmployeeValidSinceAttrDO::class.java, fieldId = "validSince")
            )
        } else {
            employeeService.validate(dto.cloneAsDO())?.let { msg ->
                validationErrors.add(ValidationError.create(msg))
            }
        }
        if (validationErrors.isNotEmpty()) {
            return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
        }
        return null
    }

    override val autoCompleteSearchFields = arrayOf("user.username", "user.firstname", "user.lastname", "user.email")

    /** Name hits first, see [autoCompletePrimarySearchFields]. */
    override val autoCompletePrimarySearchFields = arrayOf("user.username", "user.firstname", "user.lastname")

    /**
     * Active employees (recent leavers included, see [EmployeeDO.deactivated]); the others only on request
     * ([AutoCompletion.SHOW_INACTIVE_PARAM]) and only for the members of
     * [AutoCompletion.SHOW_INACTIVE_PERSONS_GROUPS], see [isShowInactiveAllowed].
     */
    override fun queryAutocompleteObjects(
        request: HttpServletRequest,
        filter: BaseSearchFilter
    ): MutableList<EmployeeDO> {
        return baseDao.selectWithActiveStatus(
            filter,
            checkAccess = true,
            showOnlyActiveEntries = !showInactive(request),
            showRecentlyLeavers = true
        ).toMutableList()
    }

    override fun isShowInactiveAllowed(): Boolean =
        accessChecker.isLoggedInUserMemberOfGroup(*AutoCompletion.SHOW_INACTIVE_PERSONS_GROUPS)
}
