/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.orga.VisitorType
import org.projectforge.business.orga.VisitorbookDO
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.dto.User.Companion.toUserList
import java.time.LocalDate

class Visitorbook(
    var lastname: String? = null,
    var firstname: String? = null,
    var company: String? = null,
    var contactPersons: List<Employee>? = null,
    var contactPersonsAsString: String? = null,
    var comment: String? = null,
    var visitortype: VisitorType? = null
) : BaseDTO<VisitorbookDO>() {
    var lastDateOfVisit: LocalDate? = null

    var latestArrived: String? = null

    var latestDeparted: String? = null

    var numberOfVisits: Int? = 0

    var entries = emptyList<VisitorbookEntry>() // empty list required by frontend.

    @get:JsonProperty
    val visitortypeAsString: String?
        get() {
            visitortype?.let { return translate(it.i18nKey) }
            return null
        }

    override fun copyFrom(src: VisitorbookDO) {
        super.copyFrom(src)
        contactPersons = Employee.toEmployeeList(src.contactPersons)?.also {
            contactPersonsAsString = it.joinToString { it.displayName ?: "???" }
        }
    }

    override fun copyTo(dest: VisitorbookDO) {
        super.copyTo(dest)
        dest.contactPersons = Employee.toEmployeeDOList(contactPersons)
    }
}
