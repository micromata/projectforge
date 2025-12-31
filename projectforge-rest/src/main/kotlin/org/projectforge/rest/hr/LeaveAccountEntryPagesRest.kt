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
import org.projectforge.business.vacation.model.LeaveAccountEntryDO
import org.projectforge.business.vacation.repository.LeaveAccountEntryDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.VacationAccountPageRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.LeaveAccountEntry
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.RoundingMode

@RestController
@RequestMapping("${Rest.URL}/leaveAccountEntry")
class LeaveAccountEntryPagesRest() : AbstractDTOPagesRest<LeaveAccountEntryDO, LeaveAccountEntry, LeaveAccountEntryDao>(
    LeaveAccountEntryDao::class.java,
    "vacation.leaveAccountEntry.title"
) {
    @Autowired
    private lateinit var employeeCache: EmployeeCache

    override fun transformForDB(dto: LeaveAccountEntry): LeaveAccountEntryDO {
        val entryDO = LeaveAccountEntryDO()
        dto.copyTo(entryDO)
        return entryDO
    }

    override fun transformFromDB(obj: LeaveAccountEntryDO, editMode: Boolean): LeaveAccountEntry {
        obj.employee = employeeCache.getEmployeeIfNotInitialized(obj.employee)
        val entry = LeaveAccountEntry()
        entry.copyFrom(obj)
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

    /**
     * LAYOUT List page
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        layout.add(
            UITable.createUIResultSetTable()
                .add(lc, "created", "employee", "date", "amount", "description")
        )
        layout.getTableColumnById("employee").formatter = UITableColumn.Formatter.EMPLOYEE
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: LeaveAccountEntry, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
            .add(lc, "employee", "date", "amount", "description")
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun createReturnToCallerResponseAction(returnToCaller: String): ResponseAction {
        if (returnToCaller == "account") {
            return ResponseAction(PagesResolver.getDynamicPageUrl(VacationAccountPageRest::class.java, absolute = true))
        }
        return super.createReturnToCallerResponseAction(returnToCaller)
    }
}
