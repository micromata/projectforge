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

package org.projectforge.rest.hr

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.vacation.model.LeaveAccountEntryDO
import org.projectforge.business.vacation.repository.LeaveAccountEntryDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.LeaveAccountEntry
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.RoundingMode

/**
 * The layout-free REST endpoint of the leave account entries ("Urlaubskontokorrekturen"), serving the hand-built list
 * and edit page of projectforge-next (see components/features/leave-account-entry). Replaces the React UILayout
 * `LeaveAccountEntryPagesRest`.
 *
 * The vacation account (`VacationAccountRest`) opens an entry with `?returnTo=/vacationAccount` and adds one for the
 * employee it shows with `?employee=<id>` (see [newBaseDTO]).
 */
@RestController
@RequestMapping("${Rest.URL}/leaveAccountEntry")
class LeaveAccountEntryEntityRest :
    AbstractDTOEntityRest<LeaveAccountEntryDO, LeaveAccountEntry, LeaveAccountEntryDao>(
        LeaveAccountEntryDao::class.java,
        "vacation.leaveAccountEntry.title"
    ) {
    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    override fun transformForDB(dto: LeaveAccountEntry): LeaveAccountEntryDO {
        val entryDO = LeaveAccountEntryDO()
        dto.copyTo(entryDO)
        return entryDO
    }

    override fun transformFromDB(obj: LeaveAccountEntryDO, editMode: Boolean): LeaveAccountEntry {
        // From the cache: no lazy load of the employee per row of the list.
        obj.employee = employeeCache.getEmployeeIfNotInitialized(obj.employee)
        val entry = LeaveAccountEntry()
        entry.copyFrom(obj)
        return entry
    }

    /**
     * A new entry is dated today, in the user's time zone. The employee is preset from the parameter `employee` (the id), which the vacation
     * account passes for the employee it shows.
     */
    override fun newBaseDTO(request: HttpServletRequest?): LeaveAccountEntry {
        val entry = LeaveAccountEntry(date = PFDateTime.now().localDate)
        NumberHelper.parseLong(request?.getParameter("employee"))?.let { employeeId ->
            // Unchecked: only the employee's name is shown, and saving the entry requires the HR vacation right.
            employeeDao.find(employeeId, checkAccess = false)?.let {
                entry.employee = Employee().also { e -> e.copyFromMinimal(it) }
            }
        }
        return entry
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: LeaveAccountEntry) {
        val scaledAmount = dto.amount?.setScale(1, RoundingMode.HALF_UP) ?: return
        if (scaledAmount.compareTo(dto.amount) != 0) {
            validationErrors.add(
                ValidationError(
                    translate("vacation.leaveAccountEntry.amount.formatError"),
                    fieldId = "amount"
                )
            )
        }
    }

    override val autoCompleteSearchFields =
        arrayOf("employee.user.lastname", "employee.user.firstName", "employee.user.username")
}
