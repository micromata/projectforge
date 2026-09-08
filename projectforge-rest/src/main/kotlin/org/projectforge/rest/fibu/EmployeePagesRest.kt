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
import mu.KotlinLogging
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.*
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.user.UserGroupCache
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.getObjectList
import org.projectforge.rest.dto.*
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/employee")
class EmployeePagesRest :
    AbstractDTOPagesRest<EmployeeDO, Employee, EmployeeDao>(EmployeeDao::class.java, "fibu.employee.title") {

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var employeeService: EmployeeService

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
        employeeCache.setTimeDependentAttrs(obj)
        employee.status = obj.status
        employee.annualLeave = obj.annualLeave
        employee.weeklyWorkingHours = obj.weeklyWorkingHours
        return employee
    }

    override fun transformForDB(dto: Employee): EmployeeDO {
        val employeeDO = EmployeeDO()
        dto.copyTo(employeeDO)
        return employeeDO
    }

    override fun onBeforeGetItemAndLayout(request: HttpServletRequest, dto: Employee, userAccess: UILayout.UserAccess) {
        dto.id?.let { id ->
            dto.statusEntries = employeeService.selectStatusEntries(id).map { EmployeeValidSinceAttr(it) }
            dto.weeklyWorkingHoursEntries =
                employeeService.selectWeeklyWorkingHoursEntries(id).map { EmployeeValidSinceAttr(it) }
            dto.annualLeaveEntries =
                employeeService.selectAnnualLeaveDayEntries(id).map { EmployeeValidSinceAttr(it) }
        }
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            userAccess = userAccess,
        )
            // Name	Vorname	Status	Personalnummer	Kost1	Position	Team	Eintrittsdatum	Austrittsdatum	Wochenstunden	Bemerkung
            .add(
                lc,
                "user.firstname",
                "user.lastname",
                "status",
                "staffNumber",
                "kost1",
                "position",
                "abteilung",
                "eintrittsDatum",
                "austrittsDatum",
                "weeklyWorkingHours",
                "comment"
            )
        layout.excelExportSupported = true
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
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting employees as Excel file.")
        val list = getObjectList(this, baseDao, filter)
        // Ensure the transient, time-dependent attributes (status, weekly working hours) reflect the current values.
        employeeCache.setTimeDependentAttrs(list)
        ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale!!).use { workbook ->
            val sheet = workbook.createOrGetSheet(translate("fibu.employee.title.heading"))
            val boldFont = ExcelUtils.createFont(workbook, "bold", bold = true)
            val boldStyle = workbook.createOrGetCellStyle("hr", font = boldFont)
            sheet.registerColumn(translate("name"), "lastname").withSize(20)
            sheet.registerColumn(translate("firstName"), "firstname").withSize(20)
            sheet.registerColumn(translate("user.username"), "username").withSize(20)
            sheet.registerColumn(translate("email"), "email").withSize(30)
            sheet.registerColumn(translate("fibu.employee.status"), "status").withSize(15)
            ExcelUtils.registerColumn(sheet, EmployeeDO::staffNumber, 15)
            sheet.registerColumn(translate("fibu.kost1"), "kost1").withSize(15)
            ExcelUtils.registerColumn(sheet, EmployeeDO::position, 20)
            ExcelUtils.registerColumn(sheet, EmployeeDO::abteilung, 20)
            ExcelUtils.registerColumn(sheet, EmployeeDO::eintrittsDatum)
            ExcelUtils.registerColumn(sheet, EmployeeDO::austrittsDatum)
            ExcelUtils.registerColumn(sheet, EmployeeDO::weeklyWorkingHours, 12)
            ExcelUtils.registerColumn(sheet, EmployeeDO::comment, 50)
            ExcelUtils.addHeadRow(sheet, boldStyle)
            list.forEach { employeeDO ->
                val row = sheet.createRow()
                val userDO = userGroupCache.getUser(employeeDO.user?.id)
                userDO?.lastname?.let { row.getCell("lastname")?.setCellValue(it) }
                userDO?.firstname?.let { row.getCell("firstname")?.setCellValue(it) }
                userDO?.username?.let { row.getCell("username")?.setCellValue(it) }
                userDO?.email?.let { row.getCell("email")?.setCellValue(it) }
                employeeDO.status?.let { row.getCell("status")?.setCellValue(translate(it.i18nKey)) }
                employeeDO.staffNumber?.let { row.getCell("staffNumber")?.setCellValue(it) }
                kostCache.getKost1(employeeDO.kost1?.id)?.formattedNumber
                    ?.let { row.getCell("kost1")?.setCellValue(it) }
                employeeDO.position?.let { row.getCell("position")?.setCellValue(it) }
                employeeDO.abteilung?.let { row.getCell("abteilung")?.setCellValue(it) }
                employeeDO.eintrittsDatum?.let { row.getCell("eintrittsDatum")?.setCellValue(it) }
                employeeDO.austrittsDatum?.let { row.getCell("austrittsDatum")?.setCellValue(it) }
                employeeDO.weeklyWorkingHours?.let { row.getCell("weeklyWorkingHours")?.setCellValue(it) }
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
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Employee, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(lc, "user", "kost1", "abteilung", "position")
                    )
                    .add(
                        UICol()
                            .add(
                                lc, "staffNumber", "eintrittsDatum", "austrittsDatum"
                            )
                    )
            )
            .add(
                UIRow()
                    .add(UICol().add(lc, "comment"))
            )
        if (dto.id == null) {
            layout.add(UIAlert("fibu.employee.insert.hint", color = UIColor.INFO))
        } else {
            val attrLc = LayoutContext(EmployeeValidSinceAttrDO::class.java)
            layout.layoutBelowActions
                .add(
                    UIRow()
                        .add(
                            UICol().add(
                                UIFieldset(title = "fibu.employee.urlaubstage")
                                    .add(
                                        UIAgGrid("annualLeaveEntries")
                                            .add(UIAgGridColumnDef.createCol(attrLc, "validSince"))
                                            .add(UIAgGridColumnDef.createCol(attrLc, "value", headerName = "days"))
                                            .add(UIAgGridColumnDef.createCol(attrLc, "comment"))
                                            .withRowClickRedirectUrl(
                                                createModalUrl(dto, EmployeeValidSinceAttrType.ANNUAL_LEAVE),
                                                openModal = true,
                                            )
                                    ).add(
                                        UIButton.createAddButton(
                                            responseAction = ResponseAction(
                                                createModalUrl(dto, EmployeeValidSinceAttrType.ANNUAL_LEAVE, true),
                                                targetType = TargetType.MODAL
                                            ),
                                            default = false,
                                        )
                                    )
                            )
                        )
                        .add(
                            UICol().add(
                                UIFieldset(title = "fibu.employee.wochenstunden")
                                    .add(
                                        UIAgGrid("weeklyWorkingHoursEntries")
                                            .add(UIAgGridColumnDef.createCol(attrLc, "validSince"))
                                            .add(UIAgGridColumnDef.createCol(attrLc, "value", headerName = "fibu.employee.sollstunden"))
                                            .add(UIAgGridColumnDef.createCol(attrLc, "comment"))
                                            .withRowClickRedirectUrl(
                                                createModalUrl(dto, EmployeeValidSinceAttrType.WEEKLY_HOURS),
                                                openModal = true,
                                            )
                                    ).add(
                                        UIButton.createAddButton(
                                            responseAction = ResponseAction(
                                                createModalUrl(dto, EmployeeValidSinceAttrType.WEEKLY_HOURS, true),
                                                targetType = TargetType.MODAL
                                            ),
                                            default = false,
                                        )
                                    )
                            )
                        )
                )
            layout.layoutBelowActions
                .add(
                    UIRow()
                        .add(
                            UICol()
                                .add(
                                    UIFieldset(title = "fibu.employee.status")
                                        .add(
                                            UIAgGrid("statusEntries")
                                                .add(UIAgGridColumnDef.createCol(attrLc, "validSince"))
                                                .add(UIAgGridColumnDef.createCol(attrLc, "value", headerName = "status"))
                                                .add(UIAgGridColumnDef.createCol(attrLc, "comment"))
                                                .withRowClickRedirectUrl(
                                                    createModalUrl(dto, EmployeeValidSinceAttrType.STATUS),
                                                    openModal = true,
                                                )
                                        ).add(
                                            UIButton.createAddButton(
                                                responseAction = ResponseAction(
                                                    createModalUrl(dto, EmployeeValidSinceAttrType.STATUS, true),
                                                    targetType = TargetType.MODAL
                                                ),
                                                default = false,
                                            )
                                        )
                                )
                        )
                )
        }
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun onAfterSave(request: HttpServletRequest, obj: EmployeeDO, postData: PostData<Employee>): ResponseAction {
        // Redirect to edit page after insert for allowing user to add vacation, status and weekly hours entries.
        return ResponseAction(PagesResolver.getEditPageUrl(EmployeePagesRest::class.java, obj.id, absolute = true))
            .addVariable("id", obj.id ?: -1)
    }

    override val autoCompleteSearchFields = arrayOf("user.username", "user.firstname", "user.lastname", "user.email")

    override fun queryAutocompleteObjects(
        request: HttpServletRequest,
        filter: BaseSearchFilter
    ): MutableList<EmployeeDO> {
        return baseDao.selectWithActiveStatus(
            filter,
            checkAccess = true,
            showOnlyActiveEntries = true,
            showRecentlyLeavers = true
        ).toMutableList()
    }

    private fun createModalUrl(
        employee: Employee,
        type: EmployeeValidSinceAttrType,
        newEntry: Boolean = false
    ): String {
        return PagesResolver.getDynamicPageUrl(
            EmployeeValidSinceAttrPageRest::class.java,
            id = if (newEntry) "-1" else "{id}",
            params = mapOf(
                "employeeId" to employee.id,
                "type" to type,
            ),
            absolute = true,
        )
    }
}
