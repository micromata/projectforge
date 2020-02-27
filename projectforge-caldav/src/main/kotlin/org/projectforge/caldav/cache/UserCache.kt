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

package org.projectforge.caldav.cache

import org.springframework.stereotype.Component

/**
 * Created by blumenstein on 06.11.16.
 */
@Component
class UserCache {

 /*   private val authorizedUserMap = mutableMapOf<User, Date>()

    fun getAuthorizedUserMap(): Map<User, Date> {
        return authorizedUserMap
    }

    fun isUserAuthenticationValid(user: User): Boolean {
        var userIsInList = false
        val it: MutableIterator<Map.Entry<User, Date>> = authorizedUserMap.entries.iterator()
        while (it.hasNext()) {
            val item = it.next()
            if (item.key.pk == user.pk) {
                val now = Date()
                if (now.time - item.value.time < FIVE_MINUTES) {
                    userIsInList = true
                } else {
                    it.remove()
                }
            }
        }
        return userIsInList
    }*/
}
