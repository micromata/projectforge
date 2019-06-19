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

package org.projectforge.rest

import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.User
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("${Rest.URL}/user")
class UserRest
    : AbstractDTORest<PFUserDO, User, UserDao, BaseSearchFilter>(UserDao::class.java, BaseSearchFilter::class.java, "user.title") {

    override fun transformFromDB(obj: PFUserDO, editMode: Boolean): User {
        val user = User()
        val copy = PFUserDO.createCopyWithoutSecretFields(obj)
        if(copy != null) {
            user.copyFrom(copy)
        }
        return user
    }

    override fun transformForDB(dto: User): PFUserDO {
        val userDO = PFUserDO()
        dto.copyTo(userDO)
        return userDO
    }

    @Autowired
    private lateinit var userDao: UserDao

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "username", "deactivated", "lastname", "firstname", "personalPhoneIdentifiers",
                                "description", "rights", "ldapValues"))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: User): UILayout {
        val layout = super.createEditLayout(dto)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "username", "firstname", "lastname", "organization", "email",
                                        /*"authenticationToken",*/
                                        "jiraUsername", "hrPlanning", "deactivated"/*, "password"*/))
                        .add(UICol()
                                .add(lc, /*"lastLogin", "language", */ "dateformat", "dateformat.xls", "timeNotation",
                                        "_timeZoneObject", "personalPhoneIdentifiers", "sshPublicKey")))
                /*.add(UISelect<Int>("readonlyAccessUsers", lc,
                        multi = true,
                        label = "user.assignedGroups",
                        additionalLabel = "access.groups",
                        autoCompletion = AutoCompletion<Int>(url = "group/aco"),
                        labelProperty = "name",
                        valueProperty = "id"))
                .add(UISelect<Int>("readonlyAccessUsers", lc,
                        multi = true,
                        label = "multitenancy.assignedTenants",
                        additionalLabel = "access.groups",
                        autoCompletion = AutoCompletion<Int>(url = "group/aco"),
                        labelProperty = "name",
                        valueProperty = "id"))*/
                .add(lc, "description")

        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override val autoCompleteSearchFields = arrayOf("username", "firstname", "lastname", "email")

    override fun getAutoCompletionObjects(@RequestParam("search") searchString: String?): MutableList<PFUserDO> {
        val result = super.getAutoCompletionObjects(searchString)
        if (searchString.isNullOrBlank())
            result.removeIf { it.deactivated } // Remove deactivated users when returning all. Show deactivated users only if search string is given.
        return result
    }
}
