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

package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.availability.AvailabilityConfig
import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityDao
import org.projectforge.business.availability.AvailabilityLocation
import org.projectforge.business.availability.AvailabilityStatus
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Availability
import org.projectforge.rest.dto.Employee
import org.projectforge.ui.*
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
    private lateinit var availabilityConfig: AvailabilityConfig

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeService: EmployeeService

    override fun transformForDB(dto: Availability): AvailabilityDO {
        val availabilityDO = AvailabilityDO()
        dto.copyTo(availabilityDO)
        return availabilityDO
    }

    override fun transformFromDB(obj: AvailabilityDO, editMode: Boolean): Availability {
        val availability = Availability()
        caches.initialize(obj)
        availability.copyFrom(obj)
        return availability
    }

    override fun newBaseDTO(request: HttpServletRequest?): Availability {
        val result = Availability()
        val employeeDO = employeeService.findByUserId(ThreadLocalUserContext.loggedInUserId)
        if (employeeDO != null) {
            val employee = Employee()
            employee.copyFromMinimal(employeeDO)
            result.employee = employee
        }
        result.status = AvailabilityStatus.NOT_AVAILABLE
        result.location = AvailabilityLocation.REMOTE
        return result
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
            .add(lc, "employee", "startDate", "endDate", "type")
            .add(lc, "statusAsString", lcField = "status")
            .add(lc, "locationAsString", lcField = "location")
            .add(lc, "description")
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterListElement("status", label = translate("availability.status"), defaultFilter = true)
                .buildValues(AvailabilityStatus::class.java)
        )
        elements.add(
            UIFilterListElement("location", label = translate("availability.location"), defaultFilter = true)
                .buildValues(AvailabilityLocation::class.java)
        )
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
                label = translate("availability.employee"),
                autoCompletion = AutoCompletion.getAutoCompletion4Employees(),
            )
        )
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter
    ): List<CustomResultFilter<AvailabilityDO>> {
        val filters = mutableListOf<CustomResultFilter<AvailabilityDO>>()
        source.entries.find { it.field == "period" }?.let { entry ->
            entry.synthetic = true
            val fromDate = PFDayUtils.parseDate(entry.value.fromValue)
            val toDate = PFDayUtils.parseDate(entry.value.toValue)
            if (fromDate != null) {
                filters.add(AvailabilityPeriodFilter(fromDate, toDate))
            }
        }
        source.entries.find { it.field == "employee" }?.let { entry ->
            entry.synthetic = true
            val employeeId = entry.value.id
            if (employeeId != null) {
                target.add(QueryFilter.eq("employee.id", employeeId))
            }
        }
        return filters
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Availability, userAccess: UILayout.UserAccess): UILayout {
        val employeeCol = UICol(6)
        if (availabilityDao.hasHrRights(ThreadLocalUserContext.loggedInUser)) {
            employeeCol.add(UISelect.createEmployeeSelect(lc, "employee", required = true))
        } else {
            employeeCol.add(UIReadOnlyField("employee.displayName", label = "availability.employee"))
        }
        val typeValues = availabilityConfig.getTypes().map { UISelectValue(it, it) }
        val layout = super.createEditLayout(dto, userAccess)
            .add(
                UIRow()
                    .add(employeeCol)
                    .add(
                        UICol(6)
                            .add(
                                UISelect(
                                    "status", lc,
                                    values = AvailabilityStatus.entries.map {
                                        UISelectValue(it.name, translate(it.i18nKey))
                                    },
                                    required = true,
                                )
                            )
                    )
            )
            .add(
                UIRow()
                    .add(UICol(6).add(lc, "startDate"))
                    .add(UICol(6).add(lc, "endDate"))
            )
            .add(
                UIRow()
                    .add(
                        UICol(6)
                            .add(
                                UISelect(
                                    "type", lc,
                                    values = typeValues,
                                    label = "availability.type",
                                )
                            )
                    )
                    .add(
                        UICol(6)
                            .add(
                                UISelect(
                                    "location", lc,
                                    values = AvailabilityLocation.entries.map {
                                        UISelectValue(it.name, translate(it.i18nKey))
                                    },
                                )
                            )
                    )
            )
            .add(lc, "description")

        layout.watchFields.addAll(arrayOf("startDate", "endDate"))
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
}

/**
 * Filter availability entries by period (start and end date).
 */
class AvailabilityPeriodFilter(
    private val fromDate: java.time.LocalDate,
    private val toDate: java.time.LocalDate?
) : CustomResultFilter<AvailabilityDO> {
    override fun match(list: MutableList<AvailabilityDO>, element: AvailabilityDO): Boolean {
        val startDate = element.startDate ?: return false
        val endDate = element.endDate ?: return false
        return if (toDate != null) {
            endDate >= fromDate && startDate <= toDate
        } else {
            endDate >= fromDate
        }
    }
}
