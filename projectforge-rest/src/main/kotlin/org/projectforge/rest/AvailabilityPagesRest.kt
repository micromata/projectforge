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

package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.availability.AvailabilityTypeConfiguration
import org.projectforge.business.availability.model.AvailabilityDO
import org.projectforge.business.availability.repository.AvailabilityDao
import org.projectforge.business.availability.service.AvailabilityService
import org.projectforge.business.availability.service.ConflictingAvailabilitiesCache
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Availability
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.UIFilterObjectElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/availability")
class AvailabilityPagesRest :
    AbstractDTOPagesRest<AvailabilityDO, Availability, AvailabilityDao>(
        AvailabilityDao::class.java,
        "availability.title"
    ) {

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var conflictingAvailabilitiesCache: ConflictingAvailabilitiesCache

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var availabilityService: AvailabilityService

    @Autowired
    private lateinit var availabilityTypeConfiguration: AvailabilityTypeConfiguration

    override fun transformForDB(dto: Availability): AvailabilityDO {
        val availabilityDO = AvailabilityDO()
        dto.copyTo(availabilityDO)
        return availabilityDO
    }

    override fun transformFromDB(obj: AvailabilityDO, editMode: Boolean): Availability {
        val availability = Availability()
        // Initialize employee entities to avoid lazy loading
        obj.employee?.let { caches.initialize(it) }
        obj.replacement?.let { caches.initialize(it) }
        obj.otherReplacements?.forEach { caches.initialize(it) }
        availability.copyFrom(obj)
        if (conflictingAvailabilitiesCache.hasConflict(obj.id)) {
            availability.conflict = true
        }
        Employee.restoreDisplayNames(availability.otherReplacements)
        return availability
    }

    override fun newBaseDTO(request: HttpServletRequest?): Availability {
        val result = Availability()
        var employeeDO: EmployeeDO? = null
        if (availabilityDao.hasHrRights(ThreadLocalUserContext.loggedInUser)) {
            val employeeId = NumberHelper.parseInteger(request?.getParameter("employee"))
            if (employeeId != null) {
                employeeDO = employeeDao.find(employeeId)
            }
        }
        if (employeeDO == null) {
            // For non HR staff members, choose always the logged in employee:
            employeeDO = employeeService.findByUserId(ThreadLocalUserContext.loggedInUserId)
        }
        result.employee = createEmployee(employeeDO!!)
        // Set first availability type as default
        availabilityTypeConfiguration.types.firstOrNull()?.let {
            result.availabilityType = it.key
        }
        return result
    }

    private fun createEmployee(employeeDO: EmployeeDO?): Employee? {
        employeeDO?.id ?: return null
        val employee = Employee()
        employeeDO.id?.let {
            employee.copyFromMinimal(employeeDao.find(it, checkAccess = false)!!)
        }
        return employee
    }

    override fun onAfterSave(obj: AvailabilityDO, postData: PostData<Availability>): ResponseAction {
        return super.onAfterSave(obj, postData)
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
            .add(lc, "employee", "startDate", "endDate")
            .add(lc, "availabilityTypeFormatted", lcField = "availabilityType")
            .add(lc, "percentage")
            .add(lc, "replacement")
            .add(lc, "otherReplacements", formatter = UIAgGridColumnDef.Formatter.SHOW_LIST_OF_DISPLAYNAMES)
            .add(lc, "comment")
            .withGetRowClass(
                """if (params.node.data.conflict) {
            return 'ag-row-red';
        }"""
            )
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        // For now, skip the availability type filter as it requires custom values
        // TODO: Add availability type filter with custom implementation
        elements.add(UIFilterBooleanElement("conflicts", label = translate("vacation.conflicts"), defaultFilter = true))
        elements.add(UIFilterElement("year", label = translate("calendar.year"), defaultFilter = true))
        elements.add(
            UIFilterElement(
                "period",
                label = translate("timePeriod"),
                filterType = UIFilterElement.FilterType.DATE
            )
        )
        elements.add(
            UIFilterObjectElement(
                "employee",
                label = translate("vacation.employee"),
                autoCompletion = AutoCompletion.getAutoCompletion4Employees(),
            )
        )
        elements.add(
            UIFilterObjectElement(
                "replacement",
                label = translate("vacation.replacement"),
                autoCompletion = AutoCompletion.getAutoCompletion4Employees()
            )
        )
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Availability, userAccess: UILayout.UserAccess): UILayout {
        val employeeCol = UICol(6)
        if (availabilityDao.hasHrRights(ThreadLocalUserContext.loggedInUser)) {
            employeeCol.add(lc, "employee")
        } else {
            employeeCol.add(UIReadOnlyField("employee.displayName", label = "vacation.employee"))
        }

        val layout = super.createEditLayout(dto, userAccess)
            .add(UIRow().add(employeeCol))
            .add(
                UIRow()
                    .add(
                        UICol(6)
                            .add(
                                UISelect(
                                    "availabilityType",
                                    lc,
                                    values = availabilityTypeConfiguration.types.map {
                                        UISelectValue(it.key, translate(it.i18nKey))
                                    }
                                )
                            )
                    )
                    .add(
                        UICol(6)
                            .add(
                                UIInput(
                                    "percentage",
                                    lc,
                                    dataType = UIDataType.INT,
                                    label = "availability.percentage"
                                ).also {
                                    // Only show percentage for types that use it
                                    it.additionalLabel = "%"
                                }
                            )
                    )
            )
            .add(
                UIRow()
                    .add(
                        UICol(6)
                            .add(lc, "startDate")
                    )
                    .add(
                        UICol(6)
                            .add(lc, "endDate")
                    )
            )
            .add(
                UIRow()
                    .add(
                        UICol(12)
                            .add(UISelect.createEmployeeSelect(lc, "replacement", required = false))
                    )
            )
            .add(UISelect.createEmployeeSelect(lc, "otherReplacements", true))
            .add(lc, "comment")

        if (dto.conflict == true) {
            layout.add(UIAlert("availability.conflict.info", color = UIColor.DANGER))
        }
        layout.add(
            UIFieldset(title = "availability.availabilitiesOfReplacements").add(
                UIAgGrid("conflictingAvailabilities")
                    .add(UIAgGridColumnDef.createCol(lc, "employee"))
                    .add(UIAgGridColumnDef.createCol(lc, "startDate"))
                    .add(UIAgGridColumnDef.createCol(lc, "endDate"))
                    .add(UIAgGridColumnDef.createCol(lc, "availabilityTypeFormatted", lcField = "availabilityType"))
                    .add(UIAgGridColumnDef.createCol(lc, "percentage"))
                    .add(UIAgGridColumnDef.createCol(lc, "replacement"))
                    .add(
                        UIAgGridColumnDef.createCol(
                            lc,
                            "otherReplacements",
                            formatter = UIAgGridColumnDef.Formatter.SHOW_LIST_OF_DISPLAYNAMES
                        )
                    )
            )
        )

        layout.watchFields.addAll(
            arrayOf(
                "startDate",
                "endDate",
                "replacement",
                "otherReplacements"
            )
        )
        updateStats(dto)
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun onWatchFieldsUpdate(
        request: HttpServletRequest,
        dto: Availability,
        watchFieldsTriggered: Array<String>?
    ): ResponseEntity<ResponseAction> {
        val startDate = dto.startDate
        val endDate = dto.endDate
        if (watchFieldsTriggered?.contains("startDate") == true && startDate != null) {
            if (endDate == null || endDate.isBefore(startDate)) {
                dto.endDate = startDate
            }
        } else if (watchFieldsTriggered?.contains("endDate") == true && endDate != null) {
            if (startDate == null || endDate.isBefore(startDate)) {
                dto.startDate = dto.endDate
            }
        }
        updateStats(dto)
        val userAccess = UILayout.UserAccess()
        val availability = AvailabilityDO()
        dto.copyTo(availability)
        checkUserAccess(availability, userAccess)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("data", dto)
                .addVariable("ui", createEditLayout(dto, userAccess))
        )
    }

    private fun updateStats(dto: Availability) {
        val startDate = dto.startDate
        val endDate = dto.endDate
        if (startDate != null && endDate != null) {
            dto.startDateFormatted = PFDayUtils.format(startDate, org.projectforge.common.DateFormatType.DATE)
            dto.endDateFormatted = PFDayUtils.format(endDate, org.projectforge.common.DateFormatType.DATE)
        }
        val availabilityDO = AvailabilityDO()
        dto.copyTo(availabilityDO)
        val conflicts = mutableListOf<Availability>()
        val availabilityOverlaps = availabilityService.getAvailabilityOverlaps(availabilityDO)
        availabilityOverlaps.otherAvailabilities.forEach {
            val conflict = Availability()
            conflict.copyFrom(it)
            conflicts.add(conflict)
        }
        dto.conflictingAvailabilities = conflicts
        dto.conflict = availabilityOverlaps.conflict
    }
}
