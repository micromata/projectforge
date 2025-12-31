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

package org.projectforge.rest.dto

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeValidSinceAttrDO
import org.projectforge.business.fibu.EmployeeValidSinceAttrType
import java.time.LocalDate

class EmployeeValidSinceAttr(
    var validSince: LocalDate? = null,
    var value: String? = null,
    var comment: String? = null,
    var type: EmployeeValidSinceAttrType? = null,
    var employeeId: Long? = null,
) : BaseDTODisplayObject<EmployeeValidSinceAttrDO>() {
    constructor(src: EmployeeValidSinceAttrDO?) : this() {
        id = src?.id
        validSince = src?.validSince
        value = src?.value
        comment = src?.comment
        type = src?.type
        employeeId = src?.employee?.id
    }

    fun cloneAsDO(): EmployeeValidSinceAttrDO {
        val result = EmployeeValidSinceAttrDO()
        result.id = id
        result.validSince = validSince
        result.value = value
        result.comment = comment
        result.type = type
        employeeId?.let {
            result.employee = EmployeeDO().also { it.id = employeeId }
        }
        return result
    }
}
