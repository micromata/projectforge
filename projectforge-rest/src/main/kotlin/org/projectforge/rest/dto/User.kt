/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.user.UserDao
import org.projectforge.business.user.service.UserService
import org.projectforge.common.StringHelper
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.user.entities.PFUserDO

class User(id: Int? = null,
           displayName: String? = null,
           var username: String? = null,
           var firstname: String? = null,
           var lastname: String? = null,
           var description: String? = null,
           var email: String? = null,
           var deactivated: Boolean = false
) : BaseDTODisplayObject<PFUserDO>(id = id, displayName = displayName) {
    override fun copyFromMinimal(src: PFUserDO) {
        super.copyFromMinimal(src)
        this.username = src.username
    }

    companion object {
        private val userDao = ApplicationContextProvider.getApplicationContext().getBean(UserDao::class.java)

        fun getUser(userId: Int?, minimal: Boolean = true): User? {
            userId ?: return null
            val userDO = userDao.getOrLoad(userId) ?: return null
            val user = User()
            if (minimal) {
                user.copyFromMinimal(userDO)
            } else {
                user.copyFrom(userDO)
            }
            return user
        }

        /**
         * Converts csv of user ids to list of user (only with id and displayName = "???", no other content).
         */
        fun toUserList(str: String?): List<User>? {
            if (str.isNullOrBlank()) return null
            return toIntArray(str)?.map {  User(it, "???") }
        }

        /**
         * Converts csv of user ids to list of user id's.
         */
        fun toIntArray(str: String?): IntArray? {
            if (str.isNullOrBlank()) return null
            return StringHelper.splitToInts(str, ",", false)
        }

        /**
         * Converts user list to ints (of format supported by [toUserList]).
         */
        fun toIntList(users: List<User>?): String? {
            return users?.joinToString { "${it.id}" }
        }

        /**
         * Set display names of any existing user in the given list.
         * @see UserService.getUser
         */
        fun restoreDisplayNames(users: List<User>?, userService: UserService) {
            users?.forEach { it.displayName = userService.getUser(it.id)?.displayName }
        }
    }
}
