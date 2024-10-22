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

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeValidityPeriodAttrDO
import org.projectforge.business.fibu.EmployeeValidityPeriodAttrType
import java.time.LocalDate

class EmployeeValidityPeriodAttr(
    var validFrom: LocalDate? = null,
    var value: String? = null,
    var comment: String? = null,
    var type: EmployeeValidityPeriodAttrType? = null,
    var employeeId: Long? = null,
) : BaseDTODisplayObject<EmployeeValidityPeriodAttrDO>() {
    constructor(src: EmployeeValidityPeriodAttrDO?) : this() {
        id = src?.id
        validFrom = src?.validFrom
        value = src?.value
        comment = src?.comment
        type = src?.type
        employeeId = src?.employee?.id
    }

    fun cloneAsDO(): EmployeeValidityPeriodAttrDO {
        val result = EmployeeValidityPeriodAttrDO()
        result.id = id
        result.validFrom = validFrom
        result.value = value
        result.comment = comment
        result.type = type
        employeeId?.let {
            result.employee = EmployeeDO().also { it.id = employeeId }
        }
        return result
    }
}
