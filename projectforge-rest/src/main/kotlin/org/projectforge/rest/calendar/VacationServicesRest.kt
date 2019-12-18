/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.calendar

import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Rest services for getting events.
 */
@RestController
@RequestMapping("${Rest.URL}/vacation")
class VacationServicesRest {
    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @GetMapping("users")
    fun getVacationUserObjects(@RequestParam("search") searchString: String?): List<User> {
        val autoCompleteSearchFields = arrayOf("user.username", "user.firstname", "user.lastname", "user.email")
        val filter = BaseSearchFilter()
        filter.searchString = searchString
        filter.setSearchFields(*autoCompleteSearchFields)
        val resultSet = ResultSet(employeeDao.getList(filter)).resultSet.filter { it.user?.deactivated == false && it.user?.isDeleted == false  }
        return resultSet.map {
            val user = User()
            user.copyFromMinimal(it.user!!)
            user
        }
    }
}
